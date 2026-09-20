package com.KairoLink.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

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
                        .requestMatchers("/rider/**").hasRole("RIDER")
                        .requestMatchers("/driver/bookings/**").hasRole("DRIVER")
                        .requestMatchers("/vehicle/**").hasRole("DRIVER")
                        .requestMatchers("/rides/**").hasRole("DRIVER")
                        .anyRequest().authenticated())
                .userDetailsService(userDetailsService)
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessUrl("/")
                        .permitAll());

        return http.build();
    }
}
