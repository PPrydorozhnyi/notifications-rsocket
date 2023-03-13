package com.petro.notifications.models.events

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ErrorBody(val message: String, val errors: List<ErrorDetail> = listOf())

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDetail(val field: String? = null, val reason: String, val value: Any? = null)