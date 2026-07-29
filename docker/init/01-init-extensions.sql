-- pgvector 확장 활성화
-- 이 스크립트는 Postgres 컨테이너가 "빈 볼륨"으로 최초 기동될 때만 자동 실행된다.
-- 이미 db_data 볼륨에 데이터가 있는 기존 환경이라면, 컨테이너 접속 후 아래 명령을 수동으로 한 번 실행해야 한다:
--   docker exec -it usinsa-postgres psql -U postgres -d usinsa -c "CREATE EXTENSION IF NOT EXISTS vector;"

CREATE EXTENSION IF NOT EXISTS vector;
