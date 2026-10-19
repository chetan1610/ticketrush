package com.ticketrush.notification;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Only other TicketRush services (which know the shared token) may call /internal/**. */
@Configuration
public class InternalTokenInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private final String internalToken;

    public InternalTokenInterceptor(@Value("${ticketrush.internal-token}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/internal/**");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!internalToken.equals(request.getHeader("X-Internal-Token"))) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "internal endpoint");
            return false;
        }
        return true;
    }
}
