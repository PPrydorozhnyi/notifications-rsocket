package com.petro.notifications.models.events

import java.time.Instant

data class ActiveUser(val userId: String, val lastSeenAt: Instant)