package com.pvc.foodie.security;

import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    log.warn("Unauthorized request: method={}, path={}, reason={}",
        request.getMethod(), request.getRequestURI(), authException.getMessage());

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("""
            {
              "success": false,
              "message": "Unauthorized"
            }
        """);
  }
}
