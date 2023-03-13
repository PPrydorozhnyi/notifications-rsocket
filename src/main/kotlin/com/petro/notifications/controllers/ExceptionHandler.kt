package com.petro.notifications.controllers

import com.petro.notifications.models.events.ErrorBody
import com.petro.notifications.models.events.NotificationEvent
import io.rsocket.exceptions.RejectedSetupException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import reactor.core.publisher.Mono

@RestControllerAdvice
class ExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleHttpException(e: RejectedSetupException): NotificationEvent {
        log.error("Setup exception", e)
        return NotificationEvent.createError(errorBody = ErrorBody("Rejected setup"))
    }

    @ExceptionHandler
    fun handleHttpException(e: Exception): Mono<NotificationEvent> {
        log.error("General exception", e)
        return Mono.just(NotificationEvent.createError(errorBody = ErrorBody("Internal error")))
    }

}