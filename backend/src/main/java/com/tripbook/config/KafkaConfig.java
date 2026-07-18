package com.tripbook.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    public NewTopic bookingEventsTopic() {
        return TopicBuilder.name("booking-events").partitions(3).replicas(1).build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler((ConsumerRecord<?, ?> record, Exception exception) -> {
            String payload;
            try {
                payload = objectMapper.writeValueAsString(record.value());
            } catch (Exception serializationException) {
                payload = String.valueOf(record.value());
            }
            jdbcTemplate.update(
                    "INSERT INTO failed_events (payload, error_message) VALUES (?, ?)",
                    payload,
                    exception.getMessage());
            log.error("Stored failed Kafka event from topic {} offset {}", record.topic(), record.offset(), exception);
        }, new FixedBackOff(500L, 3L)));
        return factory;
    }

    @Bean
    public JsonDeserializer<Object> jsonDeserializer() {
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>();
        deserializer.addTrustedPackages("com.tripbook.event");
        return deserializer;
    }
}
