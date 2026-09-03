package com.usinsa.backend.support;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;

/**
 * 테스트 전용 H2 Dialect.
 *
 * 운영 환경(PostgreSQL + pgvector)에서는 {@code ProductEmbedding.embedding} 필드가
 * {@code @JdbcTypeCode(SqlTypes.VECTOR)}로 매핑되어 pgvector의 vector(768) 컬럼으로 생성된다.
 * 하지만 테스트에서 사용하는 H2는 VECTOR SQL 타입(코드 10000)에 대한 DDL 타입 매핑이
 * DdlTypeRegistry에 전혀 등록되어 있지 않아, 컨텍스트 로딩 시 Hibernate가 스키마를 생성하지
 * 못하고 "no type mapping for ... code: 10000 (VECTOR)" 예외로 전체 @SpringBootTest가 깨진다.
 *
 * Cart/Auth 등 임베딩과 무관한 테스트에서도 애플리케이션 전체 컨텍스트를 로드하는 이상
 * 이 매핑 문제를 피할 수 없으므로, 테스트 전용으로 VECTOR 타입 코드에 대한 DdlType을
 * H2가 이해할 수 있는 ARRAY로 직접 등록해 우회한다.
 * (운영 소스 코드의 엔티티/Dialect 설정은 그대로 둔 채 테스트 설정에서만 사용됨)
 */
public class TestH2VectorDialect extends H2Dialect {

    @Override
    protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.registerColumnTypes(typeContributions, serviceRegistry);

        typeContributions.getTypeConfiguration()
                .getDdlTypeRegistry()
                .addDescriptor(new DdlTypeImpl(SqlTypes.VECTOR, "blob", this));
    }
}
