package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

    // username（主キー）で検索
    User findByUsername(String username);

    // RememberMe 用
    User findByRememberToken(String rememberToken);
}
