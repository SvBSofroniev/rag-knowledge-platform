package src.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    private String username;

    private String email;

    private String passwordHash;

    private String role;

    private boolean isEnabled = true;

    private boolean isAccountNonLocked = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}