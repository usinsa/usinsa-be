package com.usinsa.backend.domain.search.elastic.event;

import com.usinsa.backend.domain.product.entity.Product;

public record ProductUpdatedEvent(Product product){
}
