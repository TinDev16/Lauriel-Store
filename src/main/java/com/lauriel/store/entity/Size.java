package com.lauriel.store.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sizes")
@Getter
@Setter
public class Size {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "size_id")
    private Long sizeId;

    @Column(name = "size_name", length = 20, nullable = false, unique = true)
    private String sizeName;

}
