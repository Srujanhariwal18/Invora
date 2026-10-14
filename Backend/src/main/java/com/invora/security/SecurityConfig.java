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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security JWT Configuration.
 * Divided into two dedicated authentication sections:
 *  - Section 1: ADMIN (Strictly restricted to ROLE_ADMIN)
 *  - Section 2: NORMAL USERS (Accessible to ROLE_USER, STAFF, WAREHOUSE_MANAGER, VIEWER, and ADMIN)
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
                // Configure CORS for frontend access
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF for stateless REST APIs using Bearer JWT tokens
                .csrf(AbstractHttpConfigurer::disable)
                // Stateless session management (no server-side session cookies)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Custom 401 (Unauthorized) and 403 (Forbidden) JSON error responses
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // =========================================================================
                        // 1. PUBLIC SECTION (Open to all unauthenticated requests)
                        // =========================================================================
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/health/**", "/error").permitAll()

                        // =========================================================================
                        // 2. ADMIN SECTION (Strictly Admin Access Only: ROLE_ADMIN)
                        // =========================================================================
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/categories/**", "/api/products/**", "/api/suppliers/**",
                                "/api/warehouses/**", "/api/zones/**", "/api/racks/**").hasRole("ADMIN")

                        // =========================================================================
                        // 3. NORMAL USERS SECTION (ROLE_USER, STAFF, WAREHOUSE_MANAGER, VIEWER, ADMIN)
                        // =========================================================================
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // Normal users catalog & inventory queries
                        .requestMatchers(HttpMethod.GET,
                                "/api/categories/**", "/api/products/**", "/api/suppliers/**",
                                "/api/warehouses/**", "/api/zones/**", "/api/racks/**",
                                "/api/inventory/**", "/api/stock/transactions/**",
                                "/api/dashboard/**", "/api/alerts/**",
                                "/api/reports/**")
                                .hasAnyRole("USER", "ADMIN", "WAREHOUSE_MANAGER", "STAFF", "VIEWER")

                        // Stock transactions (Stock In, Stock Out, Transfer)
                        .requestMatchers(HttpMethod.POST, "/api/stock/transactions/**")
                                .hasAnyRole("USER", "ADMIN", "WAREHOUSE_MANAGER", "STAFF")

                        // Warehouse, catalog and alert operations
                        .requestMatchers(HttpMethod.POST,
                                "/api/categories/**", "/api/products/**", "/api/suppliers/**",
                                "/api/warehouses/**", "/api/zones/**", "/api/racks/**",
                                "/api/alerts/**")
                                .hasAnyRole("USER", "ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/categories/**", "/api/products/**", "/api/suppliers/**",
                                "/api/warehouses/**", "/api/zones/**", "/api/racks/**",
                                "/api/alerts/**")
                                .hasAnyRole("USER", "ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/alerts/**")
                                .hasAnyRole("USER", "ADMIN", "WAREHOUSE_MANAGER")

                        // Default: Any remaining endpoint requires authentication
                        .anyRequest().authenticated()
                )
                // Add JWT authentication filter before standard UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:4173",
                "http://127.0.0.1:4173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}