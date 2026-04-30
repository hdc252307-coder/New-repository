package com.example.demo.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RememberMeObservationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RememberMeObservationInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!log.isDebugEnabled()) {
            return true;
        }
        if (!request.getRequestURI().startsWith("/tasks")
                && !request.getRequestURI().startsWith("/calendar")
                && !request.getRequestURI().startsWith("/schedule")) {
            return true;
        }

        boolean hasRememberCookie = false;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("remember_token".equals(c.getName())) {
                    hasRememberCookie = true;
                    break;
                }
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String authType = "none";
        String principal = "anonymous";
        if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {
            principal = String.valueOf(auth.getName());
            authType = (auth instanceof RememberMeAuthenticationToken) ? "remember_me" : "session";
        }

        log.debug("remember-observe uri={} hasRememberCookie={} authType={} principal={}",
                request.getRequestURI(), hasRememberCookie, authType, principal);
        return true;
    }
}
