package com.petro.notifications.models.events

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NotificationEvent(
    val successful: Boolean = true,
    val notification: Notification? = null,
    val error: ErrorBody? = null
) {
    companion object {
        fun createError(errorBody: ErrorBody): NotificationEvent =
            NotificationEvent(false, error = errorBody)
    }
}

data class Notification(
    val notificationId: String,
    val notificationType: String,
    val messageKey: String,
    val additionalProperties: Map<String, Any>,
    val createdAt: Instant,
    val checked: Boolean = false
)

fun String.userNotificationTopic(userId: String) = "$this:$userId"

data class NotificationChecked(val notificationIds: Set<String>) // not more than size of the page

data class NotificationCheckedEvent(val userId: String, val notificationChecked: NotificationChecked)