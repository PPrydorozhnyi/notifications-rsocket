package com.petro.notifications.configs

import io.rsocket.core.RSocketConnector
import io.rsocket.metadata.WellKnownMimeType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.security.rsocket.metadata.BearerTokenAuthenticationEncoder
import org.springframework.security.rsocket.metadata.BearerTokenMetadata
import org.springframework.util.MimeTypeUtils
import reactor.util.retry.Retry
import java.net.URI
import java.time.Duration

// signed with ES512
const val token =
"eyJhbGciOiJFUzUxMiJ9.eyJzdWIiOiJ1c2VyMyIsInNjb3BlIjoiQURNSU4iLCJpYXQiOjE2Nzg3MjM0ODAsImlzcyI6ImxvY2FsLXBldHJvIiwiZXhwIjoxNjc4NzMwNjgwfQ.ATGjv66VqINTAx62kZo9jAZVS7H3_zi6j3peo8RiCxA-QNFTOSR-mWgFV7NB32tZ56bhOCN4OqYjxZivNZa0iiVmAU9a5GxWY-xsrANZeO5hIHVIMfvlGnPe1XK2CvQTIpGkvjCLbsST_k2I3SrajNEWPquuW2sN0HmsPwS0GkfOe5Ui"

//todo remove when real JS client is ready. just for demonstration purposes
@Configuration
class ClientConfig {

    // js way https://github.com/rsocket/rsocket-js/issues/88
    @Bean
    fun getRsocketRequester(strategies: RSocketStrategies): RSocketRequester {

        val authenticationMimeType =
            MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string)

        //build you bearer token
        val bearerMetadata = BearerTokenMetadata(token)

        // add bearer encoder, so you will be able to build auth header properly
        val extendedStrategies = strategies.mutate().encoder(BearerTokenAuthenticationEncoder()).build()

        return RSocketRequester.builder()
            .rsocketConnector { rSocketConnector: RSocketConnector ->
                rSocketConnector.reconnect(
                    Retry.fixedDelay(
                        2,
                        Duration.ofSeconds(2)
                    )
                )
            }
            // pass updated strategy to rsocket builder
            .rsocketStrategies(extendedStrategies)
            // pass your bearer token with up-to-date auth mime type
            .setupMetadata(bearerMetadata, authenticationMimeType)
            .websocket(URI.create("ws://localhost:8090/rsocket"))
    }

}