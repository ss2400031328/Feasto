package com.klef.sdp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.klef.sdp.entity.Role;
import com.klef.sdp.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByRole(Role role);

    List<User> findByRegion(String region);

    List<User> findByRegionIgnoreCase(String region);
}