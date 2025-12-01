-- Cart 테이블에 session_id 컬럼 추가 및 member_id nullable 변경

-- member_id를 nullable로 변경
ALTER TABLE cart MODIFY COLUMN member_id BIGINT NULL;

-- session_id 컬럼 추가
ALTER TABLE cart ADD COLUMN session_id VARCHAR(255) NULL;

-- session_id에 인덱스 추가 (조회 성능 향상)
CREATE INDEX idx_cart_session_id ON cart(session_id);

-- 복합 인덱스 추가 (세션 + 상품옵션으로 중복 체크 성능 향상)
CREATE INDEX idx_cart_session_product ON cart(session_id, product_option_id);

-- 회원 + 상품옵션 복합 인덱스 (회원 장바구니 중복 체크 성능 향상)
CREATE INDEX idx_cart_member_product ON cart(member_id, product_option_id);
