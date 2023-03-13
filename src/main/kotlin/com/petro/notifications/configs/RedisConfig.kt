package com.petro.notifications.configs

import com.fasterxml.jackson.databind.ObjectMapper
import com.petro.notifications.models.events.ActiveUser
import com.petro.notifications.models.events.NotificationCheckedEvent
import com.petro.notifications.models.events.NotificationEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    fun notificationRedisTemplate(
        factory: ReactiveRedisConnectionFactory,
        objectMapper: ObjectMapper
    ): ReactiveRedisTemplate<String, NotificationEvent> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(objectMapper, NotificationEvent::class.java)

        val context = RedisSerializationContext.newSerializationContext<String, NotificationEvent>()
            .hashKey(keySerializer)
            .key(keySerializer)
            .hashValue(valueSerializer)
            .value(valueSerializer)
            .build()

        return ReactiveRedisTemplate(factory, context)
    }

    @Bean
    fun notificationCheckedRedisTemplate(
        factory: ReactiveRedisConnectionFactory,
        objectMapper: ObjectMapper
    ): ReactiveRedisTemplate<String, NotificationCheckedEvent> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(objectMapper, NotificationCheckedEvent::class.java)

        val context = RedisSerializationContext.newSerializationContext<String, NotificationCheckedEvent>()
            .hashKey(keySerializer)
            .key(keySerializer)
            .hashValue(valueSerializer)
            .value(valueSerializer)
            .build()

        return ReactiveRedisTemplate(factory, context)
    }

    @Bean
    fun activeUserRedisTemplate(
        factory: ReactiveRedisConnectionFactory,
        objectMapper: ObjectMapper
    ): ReactiveRedisTemplate<String, ActiveUser> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(objectMapper, ActiveUser::class.java)

        val context = RedisSerializationContext.newSerializationContext<String, ActiveUser>()
            .hashKey(keySerializer)
            .key(keySerializer)
            .hashValue(valueSerializer)
            .value(valueSerializer)
            .build()

        return ReactiveRedisTemplate(factory, context)
    }

    @Bean
    fun container(factory: ReactiveRedisConnectionFactory): ReactiveRedisMessageListenerContainer =
        ReactiveRedisMessageListenerContainer(factory)

}