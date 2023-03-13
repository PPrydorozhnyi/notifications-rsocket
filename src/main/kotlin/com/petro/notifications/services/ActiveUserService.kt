package com.petro.notifications.services

import com.petro.notifications.handlers.ConnectionHolder
import com.petro.notifications.models.events.ActiveUser
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ActiveUserService(
    private val connectionHolder: ConnectionHolder,
    private val communicationService: CommunicationService
) {

    @Scheduled(cron = "\${app.user.update.cron}")
    fun updateActiveUsers() {
        connectionHolder.activeSessions.keys
            .forEach { communicationService.publishActiveUser(ActiveUser(it, Instant.now()))}
    }
}