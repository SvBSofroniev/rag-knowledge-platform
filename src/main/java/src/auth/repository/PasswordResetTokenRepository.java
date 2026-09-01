package src.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import src.auth.entity.PasswordResetToken;
import src.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(
            String tokenHash
    );

    void deleteAllByUser(
            User user
    );
}