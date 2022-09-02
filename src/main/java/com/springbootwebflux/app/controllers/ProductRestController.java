package com.springbootwebflux.app.controllers;

import com.springbootwebflux.app.models.entities.Product;
import com.springbootwebflux.app.services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/products")
public class ProductRestController {

    @Autowired
    private ProductService productService;

    private static final Logger log = LoggerFactory.getLogger(ProductRestController.class);

    @GetMapping
    public Flux<Product> index() {
        return productService.findAll()
                .map(product -> {
                    product.setName(product.getName().toUpperCase());
                    return product;
                }).doOnNext(product -> log.info(product.getName()));
    }

    @GetMapping("{id}")
    public Mono<Product> getProductById(@PathVariable String id) {
        //return productService.findById(id);
        Flux<Product> products = productService.findAll();
        return products
                .filter(p -> p.getId().equals(id))
                .next()
                .doOnNext(product -> log.info(product.getName()));
    }
}
