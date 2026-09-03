# RAG 기반 Hybrid Search 도입기 (Keyword + Semantic + Gemini)

> 이 문서는 `usinsa-be`에 RAG(Retrieval Augmented Generation) 기반 상품 추천 기능을
> 도입한 과정을 정리한 기술 문서다. 루트 `README.md`(프로젝트 전체 소개)는 건드리지 않고,
> 이번 작업만 별도로 정리했다.

## 1. 배경 및 목표

기존 `usinsa-be`는 Elasticsearch(dev) / ZincSearch(prod)를 이용한 **키워드 검색**만 지원했다.
검색어와 상품명이 정확히 일치하지 않으면(동의어, 오타, 의역 등) 결과를 못 찾는 한계가 있었다.

이번 작업의 목표는 다음 4가지를 하나의 파이프라인으로 엮는 것이었다.

1. **Keyword Search** — 기존 구조 그대로 유지
2. **Semantic Search** — pgvector + Gemini Embedding으로 의미 기반 검색 추가
3. **Hybrid Search** — 위 둘을 RRF(Reciprocal Rank Fusion)로 병합
4. **RAG 응답 생성** — 병합된 Top N 상품을 근거로 Gemini 2.5 Flash가 자연어 추천 문장 생성

## 2. 핵심 설계 원칙

작업 전체에 걸쳐 다음 원칙을 지켰다.

- **기존 코드 무수정**: `ProductSearchPort`, `ElasticsearchSearchAdapter`, `ZincSearchSearchAdapter`,
  `Product` 엔티티 등 기존 검색/도메인 코드는 단 한 줄도 수정하지 않았다. 새 기능은 전부
  새 패키지·새 클래스 추가로만 구현했다.
- **Port & Adapter 일관성**: 새로 추가한 `ProductVectorSearchPort` 역시 기존 `ProductSearchPort`와
  동일한 패턴(포트 인터페이스 + 어댑터 구현체)을 따랐고, 두 포트는 서로를 참조하지 않는다.
  병합 책임은 오직 `HybridSearchService` 하나에만 있다.
- **관계형 무결성보다 독립성**: `ProductEmbedding`을 `Product`와 JPA `@OneToOne`으로 엮지 않고
  완전히 분리된 테이블로 두었다. 임베딩 모델을 교체하거나 재계산할 때 기존 `Product` 테이블에
  영향이 가지 않도록 하기 위함이다.
- **Hallucination 방지**: Gemini는 상품을 검색하지 않는다. `HybridSearchService`가 뽑아준
  Top N 상품(이름/브랜드/가격/카테고리)만 프롬프트에 넣고, "목록에 없는 상품을 지어내지 마라"는
  지시를 명시했다.

## 3. 전체 아키텍처

```
                         GET /api/v1/search/rag?keyword=...
                                      │
                          RagRecommendationService
                     ┌────────────────┴────────────────┐
                     ▼                                 ▼
           HybridSearchService                 GeminiClient (generateContent)
           (RRF, k=60, TopK=5)                  "이 상품 목록만 근거로 추천 문장 작성"
             ┌────────┴────────┐
             ▼                 ▼
   ProductSearchPort    ProductVectorSearchPort
   (기존, 무수정)              (신규)
     │        │                 │
     ES(dev) Zinc(prod)    PgVectorAdapter → product_embedding (pgvector, HNSW)

[비동기 임베딩 파이프라인 - 기존 인덱싱과 병렬로 동작]
ProductSavedEvent / ProductUpdatedEvent (기존 이벤트 재사용)
        ├─▶ ElasticsearchEventListener (기존, 무수정)
        └─▶ ProductEmbeddingEventListener (신규, @Async)
                    └─▶ GeminiEmbeddingClient.embedDocument()
                              └─▶ product_embedding 저장
```

## 4. 진행 단계별 요약

| 단계 | 내용 | 핵심 결정 |
|---|---|---|
| 1 | 전체 설계 확정 | 신규 패키지 구조(`vector`, `embedding`, `hybrid`, `rag`)를 `search` 도메인 아래 병렬 배치 |
| 2 | Port/DTO/인터페이스 설계 | `ProductVectorSearchPort`, `ProductEmbedding`, `RankedProductDto` 등 시그니처 확정 |
| 3 | DB 스키마 대응 | Hibernate `hibernate-vector` 모듈로 `float[] ↔ vector(768)` 네이티브 매핑 (별도 컨버터 불필요) |
| 4 | pgvector 인프라 | 로컬 Docker 이미지를 `pgvector/pgvector:pg16`으로 교체, HNSW 인덱스는 `CommandLineRunner`로 앱 기동 시 자동 생성 |
| 5 | 임베딩 저장 | 상품 저장/수정 이벤트에 **비동기 리스너**를 추가(기존 ES 리스너와 동일한 `@Async` 패턴으로 일관성 유지) |
| 6 | Query Embedding | 문서용(`RETRIEVAL_DOCUMENT`)과 질의용(`RETRIEVAL_QUERY`) 임베딩을 분리 - Gemini의 비대칭 최적화를 살리기 위함 |
| 7 | Vector Search | pgvector `<=>` 연산자를 네이티브 쿼리로 실행하는 `PgVectorAdapter` 구현 |
| 8 | Hybrid Search | RRF(`score = Σ 1/(60+rank)`) 병합 로직 구현, 벡터 전용 히트는 `ProductRepository`로 상세정보 보완 |
| 9 | Gemini Client | Gemini 2.5 Flash `generateContent` 호출, 프롬프트에 상품 이름/브랜드/가격/카테고리만 포함 |
| 10 | RAG Service | Hybrid Search 결과(Top 5) → Gemini 추천 문장 조립 |
| 11 | Controller 연결 | 기존 `SearchResultController`와 동일한 스타일(GET, DTO 직접 반환)로 `/api/v1/search/rag` 노출 |
| 12 | 테스트 | RRF 병합 로직, 벡터 리터럴 변환 로직에 대한 Mockito 단위 테스트 작성 |

## 5. 기술적으로 까다로웠던 지점

- **임베딩 정규화**: Gemini Embedding API는 3072차원이 아닌 출력(이번 프로젝트는 768차원)에
  대해 벡터를 자동 정규화해주지 않는다. Cosine similarity가 방향만 비교해야 하는데 정규화가
  안 되어 있으면 크기(magnitude) 때문에 유사도 계산이 왜곡된다. 클라이언트단에서
  L2 정규화를 직접 구현해서 해결했다.
- **네이티브 쿼리 파라미터 바인딩**: `float[]`는 JPA 엔티티 필드에서는 `@JdbcTypeCode(SqlTypes.VECTOR)`로
  깔끔하게 매핑되지만, 네이티브 쿼리의 파라미터로 직접 바인딩할 때는 이 매핑을 타지 않는다.
  `"[0.1,0.2,...]"` 형태의 pgvector 리터럴 문자열로 변환해 `CAST(:param AS vector)`로 캐스팅하는
  방식으로 우회했다.
- **인덱스 자동 생성의 한계**: `ddl-auto: create`는 테이블/컬럼까지는 자동 생성하지만 HNSW
  인덱스는 만들어주지 못한다. 기존 `global/init` 초기화 패턴을 참고해 `CommandLineRunner`로
  앱 기동 시 인덱스를 보장하도록 처리했다.
- **라이브러리 버전 정합성**: 실제 프로젝트의 Spring Boot 버전(3.2.5, Hibernate 6.4.4)에 맞춰
  `hibernate-vector` 모듈 버전을 정확히 맞춰야 했다. Spring Boot 업그레이드 없이 기존 버전
  기반으로 문제를 해결하는 쪽을 택했다.

## 6. 개발 과정에 대한 메모 — AI 에이전트 활용

이번 기능은 JetBrains IDE에 연결된 AI 코딩 에이전트(Claude, MCP 연동)와 함께
**단계별 승인 방식**으로 진행했다. 전체 설계를 한 번에 맡기지 않고, 매 단계마다

1. 에이전트가 실제 프로젝트 코드(`ProductSearchPort`, `docker-compose.yml`, `build.gradle` 등)를
   직접 읽고 현재 구조를 파악하게 한 뒤,
2. 두 가지 이상의 구현 방향(예: Spring Boot 업그레이드 여부, HNSW 인덱스 생성 시점,
   RRF 상수 값, 프롬프트에 포함할 상품 정보 범위)을 제시받아 의사결정하고,
3. 결정된 방향대로 실제 코드를 프로젝트에 바로 반영시키는 방식으로 작업했다.

이 과정에서 중요하게 챙긴 것은 **"기존 아키텍처를 깨지 않는 범위 안에서 새 기능을 통합하는 것"**이었다.
에이전트가 제안한 코드를 그대로 받아들이기보다, 각 단계에서 트레이드오프(성능 vs 단순성,
동기 vs 비동기, 라이브러리 버전 호환성 등)를 확인하고 프로젝트의 기존 컨벤션(이벤트 리스너 패턴,
Repository 프로젝션 방식, Controller 스타일)에 맞춰가도록 방향을 잡았다. AI 도구를
"코드를 대신 써주는 도구"가 아니라 "기존 코드베이스를 빠르게 파악하고 여러 설계안을
비교 검토하는 협업 도구"로 활용하고자 했다.
