package com.petro.notifications.controllers

import com.petro.notifications.models.events.NotificationChecked
import com.petro.notifications.models.events.NotificationEvent
import io.rsocket.core.RSocketConnector
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveFlux
import org.springframework.security.rsocket.metadata.BearerTokenMetadata
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.util.retry.Retry
import java.net.URI
import java.time.Duration



//todo remove. just for demonstration purposes
@Profile("test")
@RestController
@RequestMapping("/test")
class TestController(private val rSocketRequester: RSocketRequester) {

    @GetMapping(value = ["/notifications"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun feed(): Flux<NotificationEvent>? {
        return rSocketRequester
                .route("notifications")
                .retrieveFlux<NotificationEvent>()
                .log()
    }

    @PostMapping("/send-message/{userId}")
    fun send(@PathVariable userId: String, @RequestBody message: NotificationEvent) {
        rSocketRequester
            .route("notification.action.$userId")
            .data(message)
            .send()
            .subscribe()
    }

    @PostMapping("/send-checked")
    fun sendChecked(@RequestBody message: NotificationChecked) {
        rSocketRequester
            .route("notification.check")
            .data(message)
            .send()
            .subscribe()
    }

    @GetMapping(value = ["/admin-feed/{userId}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun adminFeed(@PathVariable userId: String, @RequestParam token: String): Flux<NotificationEvent> {
        return  RSocketRequester.builder()
                .rsocketConnector { rSocketConnector: RSocketConnector -> rSocketConnector.reconnect(Retry.fixedDelay(2, Duration.ofSeconds(2))) }
                .rsocketStrategies(rSocketRequester.strategies())
                .setupMetadata(token, BearerTokenMetadata.BEARER_AUTHENTICATION_MIME_TYPE)
                .websocket(URI.create("ws://localhost:8090/rsocket"))
                .route("feed.$userId")
                .retrieveFlux<NotificationEvent>()
                .log()
    }

    @GetMapping(value = ["/new-notifications"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun newSocket(@RequestParam token: String): Flux<NotificationEvent> {
        return RSocketRequester.builder()
            .rsocketConnector { rSocketConnector: RSocketConnector -> rSocketConnector.reconnect(Retry.fixedDelay(2, Duration.ofSeconds(2))) }
            .rsocketStrategies(rSocketRequester.strategies())
            .setupMetadata(token, BearerTokenMetadata.BEARER_AUTHENTICATION_MIME_TYPE)
            .websocket(URI.create("ws://localhost:8090/rsocket"))
            .route("notifications")
            .retrieveFlux<NotificationEvent>()
            .log()
    }
}