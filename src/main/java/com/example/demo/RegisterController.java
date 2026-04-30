package com.example.demo;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Controller
public class RegisterController 
{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterController(UserRepository userRepository,
                              PasswordEncoder passwordEncoder) 
                              
                              {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegisterForm() 
    
    {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String password
    ) 
            
    {
        LocalDateTime now = LocalDateTime.now();

        // ハッシュ化
        String hashed = passwordEncoder.encode(password);

        // ハッシュ化したパスワードを保存
        User user = new User(username, hashed, now, now);
        user.setOnboardingCompleted(false);
        userRepository.save(user);

        return "redirect:/login";
    }
}
