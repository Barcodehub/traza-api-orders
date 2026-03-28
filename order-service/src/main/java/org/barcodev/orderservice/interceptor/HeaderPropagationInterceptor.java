package org.barcodev.orderservice.interceptor;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HeaderPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String sagaId = MDC.get("sagaId");
        String userId = MDC.get("userId");

        if (sagaId != null) {
            request.getHeaders().add("X-Saga-Id", sagaId);
        }
        if (userId != null) {
            request.getHeaders().add("X-User-Id", userId);
        }

        return execution.execute(request, body);
    }
}

