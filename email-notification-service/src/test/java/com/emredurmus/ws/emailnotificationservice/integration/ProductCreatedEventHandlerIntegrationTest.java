package com.emredurmus.ws.emailnotificationservice.integration;

import com.emredurmus.ws.core.event.ProductCreatedEvent;
import com.emredurmus.ws.emailnotificationservice.configuration.KafkaTestConfig;
import com.emredurmus.ws.emailnotificationservice.data.entity.ProcessedEvent;
import com.emredurmus.ws.emailnotificationservice.data.repository.ProcessedEventRepository;
import com.emredurmus.ws.emailnotificationservice.handler.ProductCreatedEventHandler;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@EmbeddedKafka
@Import(KafkaTestConfig.class)
@SpringBootTest(properties = "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class ProductCreatedEventHandlerIntegrationTest {

    @MockBean
    ProcessedEventRepository processedEventRepository;

    @MockBean
    RestTemplate restTemplate;

    @Autowired
    @Qualifier("testKafkaTemplate")
    KafkaTemplate<String, Object> kafkaTemplate;

    @SpyBean
    ProductCreatedEventHandler productCreatedEventHandler;

    @Test
    public void testProductCreatedEventHandler_OnProductCreated_HandlesEvent() throws Exception {
        // Arrange
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();
        productCreatedEvent.setPrice(new BigDecimal("10"));
        productCreatedEvent.setProductId(UUID.randomUUID().toString());
        productCreatedEvent.setQuantity(10);
        productCreatedEvent.setTitle("Test Product");

        String messageId = UUID.randomUUID().toString();
        String messageKey = productCreatedEvent.getProductId();

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>("product-created-topic", messageKey, productCreatedEvent);
        producerRecord.headers().add("messageId", messageId.getBytes());
        producerRecord.headers().add(KafkaHeaders.RECEIVED_KEY, messageKey.getBytes());

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setProductId(productCreatedEvent.getProductId());
        when(processedEventRepository.findByMessageId(anyString())).thenReturn(processedEvent);
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(null);

        String responseBody = "{\"key\": \"value\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> entity = new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

        when(restTemplate.exchange(
                any(String.class),
                any(HttpMethod.class),
                isNull(), eq(String.class)))
                .thenReturn(entity);

        // Act
        kafkaTemplate.send(producerRecord).get();

        // Assert
        ArgumentCaptor<String> messageIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ProductCreatedEvent> productCreatedEventCaptor = ArgumentCaptor.forClass(ProductCreatedEvent.class);
        verify(productCreatedEventHandler, timeout(5000).times(1)).handle(productCreatedEventCaptor.capture(),
                messageIdCaptor.capture(),
                messageKeyCaptor.capture());

        assertEquals(messageId, messageIdCaptor.getValue());
        assertEquals(messageKey, messageKeyCaptor.getValue());
        assertEquals(processedEvent.getProductId(), productCreatedEventCaptor.getValue().getProductId());
    }


}
