package vn.shoegroup.sd11.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            String accept = request.getHeader("Accept");
            if ((accept != null && accept.contains("application/json")) || request.getRequestURI().startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":true,\"message\":\"Đăng nhập thành công\"}");
                return;
            }
            String path = "/app/";
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) path += "#/admin/overview";
            else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"))) path += "#/admin/overview";
            else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) path += "#/employee/pos";
            else path += "#/store";
            response.sendRedirect(request.getContextPath() + path);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSuccessHandler successHandler) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/", "/app/**", "/img/**", "/uploads/**", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/api/auth/csrf", "/api/auth/register", "/api/store/**").permitAll()
                .requestMatchers("/api/admin/refunds", "/api/admin/refunds/**").hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/pos/**").hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")
                .requestMatchers("/api/auth/me", "/api/account/**", "/api/checkout/**", "/api/orders/**").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/employee/**").hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")
                .requestMatchers("/account/**", "/orders/**", "/checkout/**", "/payment/**").authenticated()
                .anyRequest().permitAll())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler((request, response, exception) -> {
                    String accept = request.getHeader("Accept");
                    if (accept != null && accept.contains("application/json")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Email hoặc mật khẩu không đúng.\"}");
                    } else response.sendRedirect("/login?error");
                })
                .permitAll())
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler((request, response, authentication) -> {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":true,\"message\":\"Đã đăng xuất.\"}");
            }).invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID", "XSRF-TOKEN"))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng đăng nhập.\"}");
                    } else response.sendRedirect("/login");
                })
                .accessDeniedHandler((request, response, exception) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Bạn không có quyền thực hiện thao tác này.\"}");
                    } else response.sendError(HttpServletResponse.SC_FORBIDDEN);
                }))
            .sessionManagement(session -> session.sessionFixation(fixation -> fixation.migrateSession()));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Requested-With", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
