package com.springbootwebflux.app.models.repository;

import com.springbootwebflux.app.models.entities.Category;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CategoryRepository extends ReactiveMongoRepository<Category, String> {
}
