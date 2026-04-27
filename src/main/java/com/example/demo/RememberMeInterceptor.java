package com.example.demo;

import com.example.demo.security.MyUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
public class RememberMeInterceptor implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        // すでにログイン済みなら何もしない
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
            !(SecurityContextHolder.getContext().getAuthentication()
                    instanceof UsernamePasswordAuthenticationToken && 
              SecurityContextHolder.getContext().getAuthentication().getPrincipal().equals("anonymousUser"))) 
              
              {
            return true;
        }

        // Cookie から remember_token を取得
        String rememberToken = null;
        if (request.getCookies() != null) 
        
        
        {
            for (Cookie cookie : request.getCookies()) 
            
            
            {
                if ("remember_token".equals(cookie.getName())) 
                
                
                {
                    rememberToken = cookie.getValue();
                    break;
                }
            }
        }

        if (rememberToken == null || rememberToken.isEmpty()) 

        

        

        {
            return true; // Cookie が無ければ終了
        }

        // DB からユーザーを検索
        User user = userRepository.findByRememberToken(rememberToken);
        if (user == null) 
        
        
        {
            return true;
        }

        // トークンの有効期限チェック
        if (user.getRememberTokenExpiry() == null ||
            user.getRememberTokenExpiry().isBefore(LocalDateTime.now())) 
            
            {
            return true;
        }

        // 自動ログイン処理
        MyUserDetails userDetails = new MyUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(auth);

        return true;
    }
}
