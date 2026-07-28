package src.auth.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import src.auth.jwt.JwtService;
import src.auth.repository.UserRepository;
import src.entity.User;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER =
            "Authorization";

    private static final String BEARER_PREFIX =
            "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null ||
                !authHeader.startsWith(BEARER_PREFIX)) {

            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader
                .substring(BEARER_PREFIX.length())
                .trim();

        if (jwt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticateRequest(jwt, request);

        } catch (Exception exception) {
            /*
             * JWT parsing and validation errors must not escape this
             * filter as generic server errors.
             *
             * The request remains unauthenticated. If the endpoint is
             * protected, RestAuthenticationEntryPoint returns the JSON 401.
             */
            SecurityContextHolder.clearContext();

            log.debug(
                    "JWT authentication failed for request path: {}",
                    request.getRequestURI()
            );
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(
            String jwt,
            HttpServletRequest request
    ) {
        if (SecurityContextHolder
                .getContext()
                .getAuthentication() != null) {
            return;
        }

        String email = jwtService.extractEmail(jwt);

        if (email == null || email.isBlank()) {
            return;
        }

        User user = userRepository
                .findByEmail(email)
                .orElse(null);

        /*
         * The JWT may belong to a user who has since been deleted.
         * In that case, leave the request unauthenticated.
         */
        if (user == null) {
            return;
        }

        if (!user.isEnabled() ||
                !user.isAccountNonLocked()) {
            return;
        }

        if (!jwtService.isTokenValid(jwt, user)) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }
}