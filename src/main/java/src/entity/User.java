package src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
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