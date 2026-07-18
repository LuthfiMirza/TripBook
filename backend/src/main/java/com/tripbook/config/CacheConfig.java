package com.tripbook.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Cache lives in Redis, never an in-memory cache manager: with two
        // backend JVMs behind nginx, local caches would let one instance serve
        // stale seat/room availability after another instance processes a booking.
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(
                        "flightSearch", defaultConfig.entryTtl(Duration.ofMinutes(5)),
                        "hotelSearch", defaultConfig.entryTtl(Duration.ofMinutes(5)),
                        "flightDetail", defaultConfig.entryTtl(Duration.ofMinutes(2)),
                        "hotelDetail", defaultConfig.entryTtl(Duration.ofMinutes(2))))
                .build();
    }
}
