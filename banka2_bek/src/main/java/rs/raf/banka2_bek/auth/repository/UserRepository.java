package rs.raf.banka2_bek.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.banka2_bek.auth.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}