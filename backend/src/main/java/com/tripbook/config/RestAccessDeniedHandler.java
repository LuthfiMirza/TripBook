package com.tripbook.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripbook.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes the 403 body directly instead of calling response.sendError(403).
 * sendError() triggers Boot's internal ERROR-dispatch forward to /error, which
 * re-enters this entire security filter chain — and JwtAuthenticationFilter
 * (a OncePerRequestFilter) skips its own logic on that ERROR dispatch by
 * default. The re-entered chain then sees no principal, treats the request as
 * anonymous, and hands it to the AuthenticationEntryPoint instead — so the
 * client ends up seeing a 401 for what was correctly evaluated as a 403.
 * Writing the response here bypasses that forward entirely.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(), "Forbidden",
                "You do not have permission to perform this action", request.getRequestURI());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
