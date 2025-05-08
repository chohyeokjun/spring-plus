package org.example.expert.config.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8"); // 한글이 깨지지 않음
        response.setStatus(401);

        ErrorMessage errorMessage = new ErrorMessage(401, "Unauthorized", "인증이 필요한 요청입니다.");

        String jsonResponse = objectMapper.writeValueAsString(errorMessage);
        response.getWriter().write(jsonResponse);
    }
}
