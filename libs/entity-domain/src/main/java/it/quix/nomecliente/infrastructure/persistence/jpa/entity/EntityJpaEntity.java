package it.quix.nomecliente.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for entities table.
 */
@Entity
@Table(name = "entities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "create_date", nullable = false)
    private Instant createDate;

    @Column(name = "create_user", nullable = false, length = 128)
    private String createUser;

    @Column(name = "last_update_date")
    private Instant lastUpdateDate;

    @Column(name = "last_update_user", length = 128)
    private String lastUpdateUser;

    @Column(name = "canceled", nullable = false)
    @Builder.Default
    private Boolean canceled = false;
}

