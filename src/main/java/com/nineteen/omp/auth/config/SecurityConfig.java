package com.nineteen.omp.auth.config;

import com.nineteen.omp.auth.filter.JwtAuthenticationFilter;
import com.nineteen.omp.auth.filter.JwtFilter;
import com.nineteen.omp.auth.handler.CustomAuthenticationSuccessHandler;
import com.nineteen.omp.auth.jwt.JwtHeaderHandler;
import com.nineteen.omp.auth.jwt.JwtProvider;
import com.nineteen.omp.user.repository.UserRepository;
import com.nineteen.omp.user.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final UserDetailsServiceImpl userDetailsService;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      AuthenticationConfiguration authenticationConfiguration,
      JwtProvider jwtProvider,
      JwtHeaderHandler jwtHeaderHandler,
      UserRepository userRepository,
      CustomAuthenticationSuccessHandler successHandler
  ) throws Exception {

    AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();
    JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
        authenticationManager, successHandler);

    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/", "/api/v1/users/login", "/api/v1/users/signup",
                "/api/v1/profile", "/api/v1/test/sleep", "/favicon.ico"
            )
            .permitAll()
            .anyRequest().authenticated()
        )
        .addFilter(jwtAuthenticationFilter) // 로그인 필터 추가
        .addFilterAfter(new JwtFilter(jwtProvider, jwtHeaderHandler, userRepository),
            JwtAuthenticationFilter.class); // JWT 인증 필터 추가
    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return new ProviderManager(authProvider);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }


}
