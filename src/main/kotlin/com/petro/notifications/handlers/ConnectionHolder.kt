package com.petro.notifications.handlers

import org.slf4j.LoggerFactory
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ConnectionHolder(val activeSessions: ConcurrentHashMap<String, RSocketRequester> = ConcurrentHashMap<String, RSocketRequester>()) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun addSession(userId: String, session: RSocketRequester) =
        activeSessions.compute(userId) { _, value -> session.also { value?.dispose() } }
                .also { log.info("Adding session {}", userId) }

    fun removeSession(userId: String) = activeSessions.remove(userId)
            .also { log.info("Connection removed {}", userId) }

}