package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByPhone(String phone);

    @Query("SELECT u FROM UserEntity u WHERE " +
        "(:keyword IS NULL OR :keyword = '' OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "ORDER BY u.createdAt DESC")
    List<UserEntity> searchByKeyword(@Param("keyword") String keyword);
}

