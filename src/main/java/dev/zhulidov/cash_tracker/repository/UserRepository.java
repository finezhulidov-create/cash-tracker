package dev.zhulidov.cash_tracker.repository;

import dev.zhulidov.cash_tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
   Optional<User> getUserById(Long id);
}