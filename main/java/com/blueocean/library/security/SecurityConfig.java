package com.blueocean.library.security;

import com.blueocean.library.repository.AccountRepository;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(AccountRepository repo) {
    return username -> {
      var a =
          repo.findByUsername(username)
              .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
      return User.withUsername(a.username)
          .password(a.passwordHash)
          .roles(a.role)
          .disabled(!a.enabled)
          .build();
    };
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/", "/index.html", "/app.js", "/style.css", "/favicon.svg", "/api/csrf", "/api/demo-accounts", "/login", "/error")
                    .permitAll()
                    // Mọi người đã đăng nhập: thông tin phiên của mình
                    .requestMatchers("/api/me")
                    .authenticated()
                    // Độc giả: lịch sử mượn của chính mình
                    .requestMatchers("/api/me/**")
                    .hasRole("READER")
                    // Quản trị viên: tài khoản, phân quyền, thống kê
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    // Thủ thư và độc giả: tra cứu sách, danh mục
                    .requestMatchers(HttpMethod.GET, "/api/books/**", "/api/catalog/**")
                    .hasAnyRole("LIBRARIAN", "READER")
                    // Thủ thư: quản lý sách, độc giả, mượn/trả, quá hạn, tiền phạt
                    .requestMatchers(
                        "/api/books/**",
                        "/api/copies/**",
                        "/api/catalog/**",
                        "/api/readers/**",
                        "/api/loans/**",
                        "/api/fines/**")
                    .hasRole("LIBRARIAN")
                    .anyRequest()
                    .denyAll())
        .formLogin(
            f ->
                f.successHandler((q, s, a) -> s.setStatus(204))
                    .failureHandler((q, s, e) -> s.sendError(401)))
        .logout(l -> l.logoutSuccessHandler((q, s, a) -> s.setStatus(204)))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((q, s, x) -> s.sendError(401))
                    .accessDeniedHandler((q, s, x) -> s.sendError(403)));
    return http.build();
  }
}
