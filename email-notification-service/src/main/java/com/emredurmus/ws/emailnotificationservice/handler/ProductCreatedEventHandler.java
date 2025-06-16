package com.emredurmus.ws.emailnotificationservice.handler;

import com.emredurmus.ws.core.event.ProductCreatedEvent;
import com.emredurmus.ws.emailnotificationservice.data.entity.ProcessedEvent;
import com.emredurmus.ws.emailnotificationservice.data.repository.ProcessedEventRepository;
import com.emredurmus.ws.emailnotificationservice.exception.NotRetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@KafkaListener(topics = "product-created-topic", groupId = "product-created-events")
public class ProductCreatedEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final ProcessedEventRepository processedEventRepository;

    public ProductCreatedEventHandler(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaHandler
    public void handle(@Payload ProductCreatedEvent productCreatedEvent,
                       @Header("messageId") String messageId,
                       @Header(KafkaHeaders.RECEIVED_KEY)  String messageKey) {
        // Check if the message has already been processed
        ProcessedEvent existingProcessedEvent = processedEventRepository.findByMessageId(messageId);

        if (existingProcessedEvent != null) {
            LOGGER.info("ProductCreatedEvent: {} with product id: {} has already been processed", productCreatedEvent.getTitle(), productCreatedEvent.getProductId());
            return;
        }

        LOGGER.info("ProductCreatedEvent: {}", productCreatedEvent.getTitle() + "with product id: " + productCreatedEvent.getProductId());
        try {
            ProcessedEvent processedEvent = new ProcessedEvent(messageId, productCreatedEvent.getProductId());
            processedEventRepository.save(processedEvent);
        } catch (DataIntegrityViolationException e) {
            throw new NotRetryableException(e.getMessage());
        }
    }
}
