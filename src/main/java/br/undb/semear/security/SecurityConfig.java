package br.undb.semear.security;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.undb.semear.config.SemearProperties;
import br.undb.semear.controller.advice.ErroResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Segurança: a View faz login no Supabase Auth e envia o JWT; aqui ele é validado
 * pelas chaves públicas do projeto (JWKS, ES256), pelo emissor e pela audiência.
 * Sessão sem estado: nada é guardado no servidor.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SemearProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtroSeguranca(HttpSecurity http, ObjectMapper json) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(regras -> regras
                .requestMatchers(HttpMethod.GET, "/api/saude").permitAll()
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/publico/contas").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())          // View (index.html, js, css) e Swagger
            .oauth2ResourceServer(oauth -> oauth
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint((req, resp, e) ->
                    escreverErro(resp, json, 401, "NAO_AUTENTICADO", "Faça login para continuar"))
                .accessDeniedHandler((req, resp, e) ->
                    escreverErro(resp, json, 403, "ACESSO_NEGADO", "Você não tem permissão para esta ação")));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SemearProperties propriedades) {
        SemearProperties.Supabase supabase = propriedades.supabase();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(supabase.jwksUri())
                .jwsAlgorithms(algoritmos -> {
                    algoritmos.add(SignatureAlgorithm.ES256);
                    algoritmos.add(SignatureAlgorithm.RS256);
                })
                .build();

        OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, aud -> aud != null && aud.contains(supabase.jwtAudience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(supabase.jwtIssuer()), audiencia));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SemearProperties propriedades) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(propriedades.cors().origens());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", cors);
        return fonte;
    }

    private static void escreverErro(HttpServletResponse resposta, ObjectMapper json,
                                     int status, String codigo, String mensagem) throws IOException {
        resposta.setStatus(status);
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        json.writeValue(resposta.getOutputStream(), ErroResponse.de(status, codigo, mensagem));
    }
}
