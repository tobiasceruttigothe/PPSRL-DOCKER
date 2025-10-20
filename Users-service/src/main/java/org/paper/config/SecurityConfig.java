package org.paper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Deshabilitar CSRF que no es necesario para una API REST
                .authorizeHttpRequests(auth -> auth
                        // Permitir acceso público al endpoint de health y a todos los de actuator
                        .requestMatchers("/actuator/**").permitAll()

                        // Permitir acceso público a la documentación de la API (Swagger UI y api-docs)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Para el resto de los endpoints, puedes decidir si requerir autenticación o permitirlos
                        // Por ahora, para que funcione como antes, permitimos todos los demás.
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}