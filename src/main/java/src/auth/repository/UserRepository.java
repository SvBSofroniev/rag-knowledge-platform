package src.auth.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import src.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmailAndIdNot(
            String email,
            UUID id
    );

    boolean existsByUsernameAndIdNot(
            String username,
            UUID id
    );

    @Query("""
        SELECT u
        FROM User u
        WHERE u.id <> :currentUserId
          AND (
                LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY u.username ASC
        """)
    List<User> searchUsers(
            @Param("query") String query,
            @Param("currentUserId") UUID currentUserId,
            Pageable pageable
    );
}