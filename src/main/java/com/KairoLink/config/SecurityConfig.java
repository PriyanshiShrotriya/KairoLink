package com.KairoLink.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserDetailsService userDetailsService) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/register", "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/favicon.ico", "/login").permitAll()
                        .requestMatchers("/dashboard/rider").hasRole("RIDER")
                        .requestMatchers("/dashboard/driver").hasRole("DRIVER")
                        .requestMatchers("/dashboard/admin").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/rider/**").hasRole("RIDER")
                        .requestMatchers("/driver/bookings/**").hasRole("DRIVER")
                        .requestMatchers("/vehicle/**").hasRole("DRIVER")
                        .requestMatchers("/rides/*/rate").hasAnyRole("RIDER", "DRIVER")
                        .requestMatchers("/rides/**").hasRole("DRIVER")
                        .anyRequest().authenticated())
                .userDetailsService(userDetailsService)
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler((request, response, exception) -> {
                            Authentication authentication =
                                    SecurityContextHolder.getContext().getAuthentication();
                            if (exception instanceof CsrfException) {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                            } else if (authentication != null
                                    && authentication.isAuthenticated()
                                    && !(authentication instanceof AnonymousAuthenticationToken)) {
                                response.sendRedirect(request.getContextPath() + "/dashboard");
                            } else {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                            }
                        }))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessUrl("/")
                        .permitAll());

        return http.build();
    }
}
