package com.emredurmus.productservice.service;

import com.emredurmus.productservice.model.CreateProductModel;
import com.emredurmus.productservice.service.events.ProductCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ProductServiceImpl implements ProductService {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    private static final String CREATE_PRODUCT_TOPIC = "product-created-topic";


    public ProductServiceImpl(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    @Override
    public String createProduct(CreateProductModel productModel) throws Exception {
        String productId = UUID.randomUUID().toString();

        // TODO: Save product to database

        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(
                productId,
                productModel.getTitle(),
                productModel.getPrice(),
                productModel.getQuantity()
        );

        LOGGER.info("Before publishing product created event");

        SendResult<String, ProductCreatedEvent> result = kafkaTemplate.send(CREATE_PRODUCT_TOPIC, productId, productCreatedEvent).get();

        LOGGER.info("Partitions" + result.getRecordMetadata().partition());
        LOGGER.info("Topic" + result.getRecordMetadata().topic());
        LOGGER.info("Offset" + result.getRecordMetadata().offset());


        LOGGER.info("Returning product id");

        return productId;
    }


}
