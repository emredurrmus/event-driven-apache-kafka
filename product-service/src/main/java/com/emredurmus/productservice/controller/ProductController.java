package com.emredurmus.productservice.controller;

import com.emredurmus.productservice.model.CreateProductModel;
import com.emredurmus.productservice.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    ProductService productService;


    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody CreateProductModel productModel) {
        String productId = productService.createProduct(productModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }



}
