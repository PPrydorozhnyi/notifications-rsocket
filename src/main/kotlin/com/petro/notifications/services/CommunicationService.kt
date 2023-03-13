package com.petro.notifications.services

import com.petro.notifications.models.events.ActiveUser
import com.petro.notifications.models.events.NotificationChecked
import com.petro.notifications.models.events.NotificationEvent
import reactor.core.publisher.Flux

interface CommunicationService {

    fun subscribeToNotifications(userId: String) : Flux<NotificationEvent>

    fun publishNotification(userId: String, event: NotificationEvent)

    fun publishNotificationCheck(userId: String, event: NotificationChecked)

    fun publishActiveUser(event: ActiveUser)
}