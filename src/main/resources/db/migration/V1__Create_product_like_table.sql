CREATE TABLE product_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_member_product UNIQUE (member_id, product_id),
    CONSTRAINT fk_product_like_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_like_product FOREIGN KEY (product_id) REFERENCES product(product_id) ON DELETE CASCADE
);

CREATE INDEX idx_product_like_member ON product_like(member_id);
CREATE INDEX idx_product_like_product ON product_like(product_id);
