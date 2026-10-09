package com.invora.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration for Invora.
 *
 * CSRF RATIONALE:
 * CSRF (Cross-Site Request Forgery) protection is intentionally disabled via {@code csrf(AbstractHttpConfigurer::disable)}.
 * This REST API is strictly stateless ({@link SessionCreationPolicy#STATELESS}) and relies exclusively on Bearer JWTs
 * transmitted via HTTP Authorization headers rather than browser session cookies. Because web browsers do not automatically
 * attach Bearer tokens to cross-site requests, cross-site request forgery attacks are fundamentally prevented by design.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DelegatedAuthenticationEntryPoint authenticationEntryPoint;
    private final DelegatedAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          DelegatedAuthenticationEntryPoint authenticationEntryPoint,
                          DelegatedAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF as justified above for stateless Bearer token REST APIs
                .csrf(AbstractHttpConfigurer::disable)
                // Stateless session management (no HttpSession or server-side cookies)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Custom 401 and 403 JSON responses matching Invora ErrorResponse schema
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // Role-based URL authorization
                .authorizeHttpRequests(auth -> auth
                        // 1. Explicitly public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/health/**", "/error").permitAll()

                        // 2. Authenticated user profile
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // 3. Sensitive audit log querying strictly restricted to ADMIN
                        .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")

                        // 4. Stock movement operations (ADMIN, WAREHOUSE_MANAGER, STAFF)
                        .requestMatchers(HttpMethod.POST, "/api/stock/transactions/**")
                                .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "STAFF")

                        // 5. Warehouse, Catalog & Alert management write operations (ADMIN, WAREHOUSE_MANAGER)
                        .requestMatchers(HttpMethod.POST,
                                "/api/categories/**", "/api/products/**", "/api/suppliers/**",
                                "/api/warehouses/**", "/api/zones/**", "/api/racks/**",
                                "/api/alerts/**")
                                .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/categories/**", "/api/products/**", "/api/suppliers/**",
                                "/api/warehouses/**", "/api/zones/**", "/api/racks/**",
                                "/api/alerts/**")
                                .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/alerts/**")
                                .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/categories/**", "/api/products/**", "/api/suppliers/**",
                                "/api/warehouses/**", "/api/zones/**", "/api/racks/**")
                                .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // 6. Read-only queries and reports for authenticated roles (ADMIN, WAREHOUSE_MANAGER, STAFF, VIEWER)
                        .requestMatchers(HttpMethod.GET,
                                "/api/categories/**", "/api/products/**", "/api/suppliers/**",
                                "/api/warehouses/**", "/api/zones/**", "/api/racks/**",
                                "/api/inventory/**", "/api/stock/transactions/**",
                                "/api/dashboard/**", "/api/alerts/**",
                                "/api/reports/**")
                                .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "STAFF", "VIEWER")

                        // 7. Deny any unauthenticated access to remaining APIs by default
                        .anyRequest().authenticated()
                )
                // Add JWT filter before standard UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
