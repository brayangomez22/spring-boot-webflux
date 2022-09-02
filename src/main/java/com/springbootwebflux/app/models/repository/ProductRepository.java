package com.springbootwebflux.app.models.repository;

import com.springbootwebflux.app.models.entities.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ProductRepository extends ReactiveMongoRepository<Product, String> {
}
