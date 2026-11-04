package com.omnisolutions.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisolutions.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Seguridad basada en sesión (cookie JSESSIONID) con protección CSRF por cookie XSRF-TOKEN.
 * - /api/** responde JSON 401/403 en lugar de redirigir.
 * - Las páginas HTML privadas redirigen a /index.html si no hay sesión.
 * - Las reglas de rol y de propiedad se validan en la capa de servicio.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final RequestMatcher API = request ->
            request.getRequestURI().startsWith(request.getContextPath() + "/api/");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UsuarioService usuarioService,
                                           ObjectMapper objectMapper) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.svg", "/error").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/index.html")
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("usuario")
                .passwordParameter("contrasena")
                .successHandler((req, res, auth) ->
                        escribirJson(res, objectMapper, HttpStatus.OK, usuarioService.perfilDe(auth.getName())))
                .failureHandler((req, res, ex) ->
                        escribirJson(res, objectMapper, HttpStatus.UNAUTHORIZED,
                                Map.of("status", 401, "mensaje", "Usuario o contraseña incorrectos")))
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
                .deleteCookies("JSESSIONID")
                .permitAll())
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor((req, res, e) ->
                        escribirJson(res, objectMapper, HttpStatus.UNAUTHORIZED,
                                Map.of("status", 401, "mensaje", "Debés iniciar sesión")), API)
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/index.html"), AnyRequestMatcher.INSTANCE)
                .accessDeniedHandler((req, res, e) ->
                        escribirJson(res, objectMapper, HttpStatus.FORBIDDEN, Map.of("status", 403,
                                "mensaje", e instanceof CsrfException
                                        ? "La sesión de seguridad expiró. Recargá la página e intentá de nuevo."
                                        : "No tenés permisos para realizar esta acción"))))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'self'; "
              + "frame-ancestors 'none'; form-action 'self'")));
        return http.build();
    }

    private static void escribirJson(HttpServletResponse res, ObjectMapper mapper, HttpStatus status, Object body)
            throws IOException {
        res.setStatus(status.value());
        res.setCharacterEncoding("UTF-8");
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), body);
    }

    /** Fuerza la generación del token CSRF para que la cookie XSRF-TOKEN llegue al navegador. */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
