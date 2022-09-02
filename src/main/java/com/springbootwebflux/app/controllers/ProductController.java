package com.springbootwebflux.app.controllers;

import com.springbootwebflux.app.models.entities.Category;
import com.springbootwebflux.app.models.entities.Product;
import com.springbootwebflux.app.services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    @Value("${config.uploads.path}")
    private  String path;

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @ModelAttribute("categories")
    public Flux<Category> categories() {
        return productService.findAllCategories();
    }

    @GetMapping("uploads/img/{namePhoto:.+}")
    public Mono<ResponseEntity<Resource>> viewPhoto(@PathVariable String namePhoto) throws MalformedURLException {
        Path route = Paths.get(path).resolve(namePhoto).toAbsolutePath();
        Resource image = new UrlResource(route.toUri());

        return Mono.just(
                ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getFilename() + "\"")
                        .body(image)
        );
    }

    @GetMapping("view/{id}")
    public Mono<String> view(Model model, @PathVariable String id) {
        return productService.findById(id)
                .doOnNext(product -> {
                    model.addAttribute("product", product);
                    model.addAttribute("title", "Product Detail");
                })
                .switchIfEmpty(Mono.just(new Product()))
                .flatMap(product -> {
                    if(product.getId() == null) return Mono.error(new InterruptedException("The product does not exist"));
                    return Mono.just(product);
                })
                .then(Mono.just("view"))
                .onErrorResume(ex -> Mono.just("redirect:/list?error=the+product+does+not+exist"));
    }

    @GetMapping({"list", "/"})
    public Mono<String> list(Model model) {
        Flux<Product> products = productService.findAllWithName();

        products.subscribe(product -> log.info(product.getName()));

        model.addAttribute("products", products);
        model.addAttribute("title", "product list");
        return Mono.just("products");
    }

    @GetMapping("form")
    public Mono<String> create(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("title", "form product");
        model.addAttribute("button", "Create");
        return Mono.just("form");
    }

    @GetMapping("form/{id}")
    public Mono<String> edit(@PathVariable String id, Model model) {
        Mono<Product> productMono = productService.findById(id)
                .doOnNext(product -> log.info(product.getName()))
                        .defaultIfEmpty(new Product());

        model.addAttribute("title", "Edit product");
        model.addAttribute("product", productMono);
        model.addAttribute("button", "Edit");

        return Mono.just("form");
    }

    @GetMapping("form-v2/{id}")
    public Mono<String> editV2(@PathVariable String id, Model model) {
        return productService.findById(id)
                .doOnNext(product -> {
                    log.info(product.getName());
                    model.addAttribute("title", "Edit product");
                    model.addAttribute("product", product);
                    model.addAttribute("button", "Edit");
                })
                .defaultIfEmpty(new Product())
                .flatMap(product -> {
                    if(product.getId() == null) return Mono.error(new InterruptedException("The product does not exist"));
                    return Mono.just(product);
                })
                .then(Mono.just("form"))
                .onErrorResume(ex -> Mono.just("redirect:/list?error=the+product+does+not+exist"));
    }

    @PostMapping("form")
    public Mono<String> save(@Valid Product product, BindingResult result, Model model, @RequestPart FilePart file) {
        if(result.hasErrors()) {
            model.addAttribute("title", "Errors");
            model.addAttribute("button", "Save");
            return Mono.just("form");
        } else {
            return productService.findCategoryById(product.getCategory().getId())
                    .flatMap(category -> {
                        if(product.getCreateAt() == null) product.setCreateAt(new Date());
                        if(!file.filename().isEmpty()) {
                            product.setPhoto(UUID.randomUUID().toString() + "-" + file.filename()
                                    .replace(" ", "")
                                    .replace(":", "")
                                    .replace("\\", "")
                            );
                        }

                        product.setCategory(category);
                        return productService.save(product);
                    })
                    .doOnNext(p -> log.info("saved product"))
                    .flatMap(p -> {
                        if(!file.filename().isEmpty()) return file.transferTo(new File(path + p.getPhoto()));
                        return Mono.empty();
                    })
                    .thenReturn("redirect:/list?success=success");
        }
    }

    @GetMapping("delete/{id}")
    public Mono<String> delete(@PathVariable String id) {
        return productService.findById(id)
                .defaultIfEmpty(new Product())
                .flatMap(product -> {
                    if(product.getId() == null) return Mono.error(new InterruptedException("The product does not exist"));
                    return Mono.just(product);
                })
                .flatMap(productService::delete)
                .then(Mono.just("redirect:/list?success=success"))
                .onErrorResume(ex -> Mono.just("redirect:/list?error=the+product+does+not+exist"));
    }

    @GetMapping("list-data-driver")
    public String listDataDriver(Model model) {
        Flux<Product> products = productService.findAllWithName()
                .delayElements(Duration.ofSeconds(1));

        products.subscribe(product -> log.info(product.getName()));

        model.addAttribute("products", new ReactiveDataDriverContextVariable(products, 2));
        model.addAttribute("title", "product list");
        return "products";
    }

    @GetMapping("list-full")
    public String listFull(Model model) {
        Flux<Product> products = productService.findAllWithName()
                .repeat(2000);

        model.addAttribute("products", products);
        model.addAttribute("title", "product list");
        return "products";
    }

    @GetMapping("list-chunked")
    public String listChunked(Model model) {
        Flux<Product> products = productService.findAllWithName()
                .repeat(2000);

        model.addAttribute("products", products);
        model.addAttribute("title", "product list");
        return "products-chunked";
    }
}
