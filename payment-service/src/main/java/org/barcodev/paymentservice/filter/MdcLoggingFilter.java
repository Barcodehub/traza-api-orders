package org.barcodev.paymentservice.filter;

import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    public MdcLoggingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String sagaId = request.getHeader("X-Saga-Id");
        String userId = request.getHeader("X-User-Id");

        if (sagaId != null) {
            MDC.put("sagaId", sagaId);
            if (tracer.currentSpan() != null) {
                tracer.currentSpan().tag("sagaId", sagaId);
            }
        }
        if (userId != null) {
            MDC.put("userId", userId);
            if (tracer.currentSpan() != null) {
                tracer.currentSpan().tag("userId", userId);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
