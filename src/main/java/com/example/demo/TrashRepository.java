package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TrashRepository extends JpaRepository<Trash, Long> {

    // ユーザーごとの削除一覧取得
    List<Trash> findByUsername(String username);
    List<Trash> findByDeletedAtLessThanEqual(LocalDateTime cutoff);
}
