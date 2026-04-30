package com.example.demo.security;

import com.example.demo.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class MyUserDetails implements UserDetails {

    private final User user;

    public MyUserDetails(User user) 

    

    {
        this.user = user;
    }

    public User getUser() 

    

    {
        return this.user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() 
    {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() 

    
    {
        return user.getPassword();
    }

    @Override
    public String getUsername() 

    
    {
        return user.getUsername();
    }

    // アカウント状態はすべて有効扱い
    @Override
    public boolean isAccountNonExpired() 
    
    {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() 

    
    {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() 

    
    {
        return true;
    }

    @Override
    public boolean isEnabled() 

    
    {
        return true;
    }
}
