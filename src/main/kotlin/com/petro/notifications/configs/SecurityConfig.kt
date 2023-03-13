package com.petro.notifications.configs

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.proc.SingleKeyJWSKeySelector
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.nimbusds.jwt.proc.JWTProcessor
import io.jsonwebtoken.io.Decoders
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity.AuthorizePayloadsSpec
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.util.pattern.PathPatternRouteMatcher
import reactor.core.publisher.Mono
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

const val ROLE_PREFIX = "ROLE_"

@Configuration
@EnableWebFluxSecurity
@EnableRSocketSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {

    @Bean
    @Throws(Exception::class)
    fun filterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http.cors().and().csrf().disable()
            .authorizeExchange()
            .pathMatchers(HttpMethod.POST, "/test/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/actuator/**", "/test/**").permitAll()
            .anyExchange().authenticated()
            .and()
            .build()

    @Bean
    fun rsocketInterceptor(rSocketSecurity: RSocketSecurity, reactiveJwtDecoder: ReactiveJwtDecoder):
            PayloadSocketAcceptorInterceptor =
        rSocketSecurity.authorizePayload { authorize: AuthorizePayloadsSpec ->
            authorize.setup().authenticated()
                .anyRequest().authenticated()
                .anyExchange().authenticated()
        }
            .jwt { it.authenticationManager(jwtReactiveAuthenticationManager(reactiveJwtDecoder)) }
            .build()

    @Bean
    fun reactiveJwtDecoder(
        @Value("\${token.issuer}") issuer: String,
        @Value("\${token.public-key}") publicKeyString: String
    ): ReactiveJwtDecoder {
        val publicKeyBytes = Decoders.BASE64.decode(publicKeyString)

        //todo check EdDSA for performance reason. not supported for now
        val keyFactory = KeyFactory.getInstance("EC") // ECDSA using P-521 and SHA-512

        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes))

        return NimbusReactiveJwtDecoder(processor(publicKey))
            .apply {
                val withIssuer = JwtValidators.createDefaultWithIssuer(issuer)
                setJwtValidator(withIssuer)
            }
    }

    private fun processor(publicKey: PublicKey): Converter<JWT?, Mono<JWTClaimsSet?>> {
        val jwsKeySelector: JWSKeySelector<SecurityContext?> = SingleKeyJWSKeySelector(JWSAlgorithm.ES512, publicKey)
        val jwtProcessor = DefaultJWTProcessor<SecurityContext?>()
        jwtProcessor.jwsKeySelector = jwsKeySelector
        // Spring Security validates the claim set independent of Nimbus
        jwtProcessor.setJWTClaimsSetVerifier { claims: JWTClaimsSet?, context: SecurityContext? -> }
        return Converter { jwt: JWT? ->
            Mono.just(
                createClaimsSet(jwtProcessor, jwt, null)
            )
        }
    }

    private fun <C : SecurityContext?> createClaimsSet(
        jwtProcessor: JWTProcessor<C>,
        parsedToken: JWT?, context: C
    ): JWTClaimsSet {
        return try {
            jwtProcessor.process(parsedToken, context)
        } catch (ex: BadJOSEException) {
            throw BadJwtException("Failed to validate the token", ex)
        } catch (ex: JOSEException) {
            throw JwtException("Failed to validate the token", ex)
        }
    }

    @Bean
    fun messageHandler(strategies: RSocketStrategies): RSocketMessageHandler =
        RSocketMessageHandler()
            .apply {
                argumentResolverConfigurer.addCustomResolver(
                    AuthenticationPrincipalArgumentResolver()
                )
                routeMatcher = PathPatternRouteMatcher()
                rSocketStrategies = strategies
            }

    private fun jwtReactiveAuthenticationManager(reactiveJwtDecoder: ReactiveJwtDecoder) =
        JwtReactiveAuthenticationManager(reactiveJwtDecoder)
            .apply {

                val jwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
                    .apply { setAuthorityPrefix(ROLE_PREFIX) }
                val authenticationConverter = JwtAuthenticationConverter()
                    .apply { setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter) }

                setJwtAuthenticationConverter(
                    ReactiveJwtAuthenticationConverterAdapter(
                        authenticationConverter
                    )
                )
            }
}