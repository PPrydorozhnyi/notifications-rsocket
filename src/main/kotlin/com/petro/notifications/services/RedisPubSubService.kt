package com.petro.notifications.services

import com.petro.notifications.models.*
import com.petro.notifications.models.events.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class RedisPubSubService(
    @Value("\${spring.data.redis.notifications-topic}") private val notificationsTopic: String,
    private val notificationRedisTemplate: ReactiveRedisTemplate<String, NotificationEvent>,
    @Value("\${spring.data.redis.notification-checked-topic}") private val notificationCheckedTopic: String,
    private val notificationCheckedRedisTemplate: ReactiveRedisTemplate<String, NotificationCheckedEvent>,
    @Value("\${spring.data.redis.active-user-topic}") private val activeUserTopic: String,
    private val activeUserRedisTemplate: ReactiveRedisTemplate<String, ActiveUser>,
    private val container: ReactiveRedisMessageListenerContainer
) : CommunicationService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun subscribeToNotifications(userId: String): Flux<NotificationEvent> =
        container.receive(
            listOf(ChannelTopic(notificationsTopic.userNotificationTopic(userId))),
            notificationRedisTemplate.serializationContext.keySerializationPair,
            notificationRedisTemplate.serializationContext.valueSerializationPair
        )
            .map { it.message }

    override fun publishNotification(userId: String, event: NotificationEvent) {
        notificationRedisTemplate.convertAndSend(
            notificationsTopic.userNotificationTopic(userId),
            event
        )
            .doOnSuccess { log.info("Notification event published {} {}", userId, event) }
            .doOnError { log.error("Error publishing notification event for user {}", userId, it) }
            .subscribe()
    }

    override fun publishNotificationCheck(userId: String, event: NotificationChecked) {
        notificationCheckedRedisTemplate.convertAndSend(notificationCheckedTopic, NotificationCheckedEvent(userId, event))
            .doOnSuccess { log.info("Notification checked event published {}", event) }
            .doOnError { log.error("Error publishing notification checked event for user {}", userId, it) }
            .subscribe()
    }

    override fun publishActiveUser(event: ActiveUser) {
        activeUserRedisTemplate.convertAndSend(activeUserTopic, event)
            .doOnSuccess { log.info("Active user published {}", event) }
            .doOnError { log.error("Error publishing active user {}", event.userId, it) }
            .subscribe()
    }
}