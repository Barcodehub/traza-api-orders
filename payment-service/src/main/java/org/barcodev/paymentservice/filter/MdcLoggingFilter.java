package org.barcodev.paymentservice.filter;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String sagaId = request.getHeader("X-Saga-Id");
        String userId = request.getHeader("X-User-Id");

        if (sagaId != null) {
            MDC.put("sagaId", sagaId);
        }
        if (userId != null) {
            MDC.put("userId", userId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
