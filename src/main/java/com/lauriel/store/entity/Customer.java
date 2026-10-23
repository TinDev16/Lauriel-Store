package com.lauriel.store.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "customers")
@Getter
@Setter
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true, unique = true)
    private User user;

    @Column(name = "customer_name", length = 150, nullable = false)
    private String customerName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    // MySQL owns this timestamp; Hibernate reads the generated value back.
    @Generated(event = EventType.INSERT)
    @Column(
        name = "created_at",
        columnDefinition = "datetime",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private LocalDateTime createdAt;
}
