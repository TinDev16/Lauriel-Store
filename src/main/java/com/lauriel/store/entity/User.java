package com.lauriel.store.entity;

import com.lauriel.store.entity.enums.UserStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 150, nullable = false)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 150, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // MySQL owns this timestamp; Hibernate reads the generated value back.
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", columnDefinition = "datetime", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

}
