package com.springbootwebflux.app;

import com.springbootwebflux.app.models.entities.Category;
import com.springbootwebflux.app.models.entities.Product;
import com.springbootwebflux.app.services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

import java.util.Date;

@SpringBootApplication
public class SpringBootWebfluxApplication implements CommandLineRunner {

	@Autowired
	private ProductService productService;

	@Autowired
	private ReactiveMongoTemplate reactiveMongoTemplate;

	private static final Logger log = LoggerFactory.getLogger(SpringBootWebfluxApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebfluxApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		reactiveMongoTemplate.dropCollection("products").subscribe();
		reactiveMongoTemplate.dropCollection("categories").subscribe();

		Category electronic = new Category("Electronic");
		Category sport = new Category("Sport");
		Category computing = new Category("Computing");

		Flux.just(electronic, sport, computing)
				.flatMap(productService::saveCategory)
				.thenMany(
					Flux.just(new Product("TV Panasonic", 121.321, electronic),
							new Product("Sony Camara", 11.321, electronic),
							new Product("Apple Ipod", 122.321, computing),
							new Product("Sony Notebook", 12.321, computing),
							new Product("Bianchi", 2.321, sport),
							new Product("HP Notebook", 4.321, computing),
							new Product("Mica", 131.321, sport),
							new Product("TV Sony", 11232.321, electronic))
							.flatMap(product -> {
								product.setCreateAt(new Date());
								return productService.save(product);
							})
				)
				.subscribe(product -> log.info("Insert: "+product.getId()+" "+" "+product.getName()));
	}
}
