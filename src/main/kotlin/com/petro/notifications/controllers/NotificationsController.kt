package com.petro.notifications.controllers

import com.petro.notifications.handlers.ConnectionHolder
import com.petro.notifications.models.events.ErrorBody
import com.petro.notifications.models.events.NotificationChecked
import com.petro.notifications.models.events.NotificationEvent
import com.petro.notifications.services.CommunicationService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.annotation.ConnectMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Controller
@CrossOrigin("*")
@MessageMapping
class NotificationsController(val connectionHolder: ConnectionHolder, val communicationService: CommunicationService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @ConnectMapping
    fun onConnect(requester: RSocketRequester, @AuthenticationPrincipal token: Jwt) {
        val userId = token.subject

        requester
            .rsocket()
            ?.onClose()
            ?.doOnError { log.error("Connection closed with error", it) }
            ?.doOnSuccess { log.debug("Connection closed") }
            ?.doFinally { connectionHolder.removeSession(userId) }
            ?.subscribe()
            .also { connectionHolder.addSession(userId, requester) }
    }

    @MessageMapping("notifications")
    fun allNewsByCategory(@AuthenticationPrincipal token: Jwt): Flux<NotificationEvent> {
        log.info("RSocket, getting all notifications by current userId: {}!", token.subject)
        return communicationService.subscribeToNotifications(token.subject)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MessageMapping("feed.{userId}")
    fun customFeed(@DestinationVariable userId: String): Flux<NotificationEvent> {
        log.info("admin is getting all notifications by userId: {}!", userId)
        return communicationService.subscribeToNotifications(userId)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MessageMapping("notification.action.{userId}")
    fun notificationAction(@DestinationVariable userId: String, message: NotificationEvent): Mono<Void> {
        log.info("Publishing notification: {}!", message)
        communicationService.publishNotification(userId, message)
        return Mono.empty()
    }

    @MessageMapping("notification.check")
    fun notificationCheck(message: NotificationChecked, @AuthenticationPrincipal token: Jwt): Mono<Void> {
        log.info("Publishing notification: {}!", message)
        communicationService.publishNotificationCheck(token.subject, message)
        return Mono.empty()
    }

    //todo change messages
    @MessageExceptionHandler
    fun handleException(e: Exception): Mono<NotificationEvent> {
        log.error("Rsocket general error", e)
        return Mono.just(NotificationEvent.createError(ErrorBody("Internal error")))
    }

    @MessageExceptionHandler
    fun handleException(e: org.springframework.security.access.AccessDeniedException): Mono<NotificationEvent> {
        log.error("Rsocket access denied error", e)
        return Mono.just(NotificationEvent.createError(ErrorBody("Access Denied to this resource")))
    }
}