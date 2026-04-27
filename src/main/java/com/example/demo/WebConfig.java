package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RememberMeInterceptor rememberMeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) 

    
    {
        registry.addInterceptor(rememberMeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/login/**",
                        "/register",
                        "/register/**",
                        "/css/**",
                        "/js/**",
                        "/h2-console/**",   // H2 Console を使う場合は除外推奨
                        "/error"            // エラー画面は除外しておくと安全
                );
    }
}
