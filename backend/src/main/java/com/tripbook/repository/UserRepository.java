package com.tripbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripbook.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
