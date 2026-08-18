package org.phoenix.springsecurity.service;

import org.phoenix.springsecurity.domain.Role;
import org.phoenix.springsecurity.domain.User;
import org.phoenix.springsecurity.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public int createUser(User user) {
        if (user.getId() != null) {
            throw new IllegalArgumentException("user.getId() must be null when creating a new user");
        }
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new DuplicateEmailException("Email address is already in use.");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        return userRepository.save(user).getId();
    }

}
