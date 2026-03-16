package com.planora.backend.model.user;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "tb_roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Getter
    private String name;

    public enum Values {
        BASIC(1L, "user"),
        ADMIN(2L, "admin");

        @Getter
        private final Long id;
        @Getter
        private final String description;

        Values(Long id, String description) {
            this.id = id;
            this.description = description;
        }

    }

}
