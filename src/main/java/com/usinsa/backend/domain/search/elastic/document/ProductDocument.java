package com.usinsa.backend.domain.search.elastic.document;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private Long id;
    private String name;
    private String brandName;
    private String categoryName;
    private Long price;
    private int likeCount;
    private int clickCount;
}