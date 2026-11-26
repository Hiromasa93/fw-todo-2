package com.example.todoapp;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

/** Adds CSRF protection to the quick-create API without introducing a login flow. */
@Configuration
public class QuickTodoCsrfConfiguration {
    @Bean
    public FilterRegistrationBean<CsrfFilter> quickTodoCsrfFilter() {
        CsrfFilter filter = new CsrfFilter(new HttpSessionCsrfTokenRepository());
        filter.setRequireCsrfProtectionMatcher(request -> {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            return !"/todos".equals(path) && CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request);
        });
        // GET /todos exposes _csrf to Thymeleaf. Existing endpoints retain their behavior.
        FilterRegistrationBean<CsrfFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/todos", "/api/todo");
        return registration;
    }
}
