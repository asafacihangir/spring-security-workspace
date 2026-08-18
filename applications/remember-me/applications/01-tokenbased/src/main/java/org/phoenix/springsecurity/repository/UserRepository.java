package org.phoenix.springsecurity.repository;

import org.phoenix.springsecurity.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

    User findByEmail(String email);

}
