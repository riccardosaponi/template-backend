package it.quix.nomecliente.infrastructure.persistence.jdbc.adapter;

import it.quix.nomecliente.domain.ddd.entity.Entity;
import it.quix.nomecliente.domain.port.out.EntityRepositoryOut;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JDBC adapter implementing EntityRepositoryOut using NamedParameterJdbcTemplate.
 * All queries use SQL text blocks and named parameters — no ORM.
 */
@Component
@RequiredArgsConstructor
public class EntityRepositoryAdapter implements EntityRepositoryOut {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Entity save(Entity entity) {
        String sql = """
                INSERT INTO entities (id, code, description, create_date, create_user, canceled)
                VALUES (:id, :code, :description, :createDate, :createUser, :canceled)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", entity.getId())
                .addValue("code", entity.getCode())
                .addValue("description", entity.getDescription())
                .addValue("createDate", Timestamp.from(entity.getCreateDate()))
                .addValue("createUser", entity.getCreateUser())
                .addValue("canceled", entity.getCanceled());
        jdbcTemplate.update(sql, params);
        return entity;
    }

    @Override
    public Optional<Entity> findById(UUID id) {
        String sql = """
                SELECT id, code, description, create_date, create_user,
                       last_update_date, last_update_user, canceled
                FROM entities
                WHERE id = :id
                """;
        List<Entity> results = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("id", id),
                this::mapRow);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Page<Entity> findAll(Pageable pageable) {
        String orderBy = buildOrderBy(pageable);
        String sql = """
                SELECT id, code, description, create_date, create_user,
                       last_update_date, last_update_user, canceled
                FROM entities
                WHERE canceled = false
                ORDER BY %s
                LIMIT :limit OFFSET :offset
                """.formatted(orderBy);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());
        List<Entity> content = jdbcTemplate.query(sql, params, this::mapRow);

        String countSql = "SELECT COUNT(*) FROM entities WHERE canceled = false";
        Long total = jdbcTemplate.queryForObject(countSql, new MapSqlParameterSource(), Long.class);

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Entity update(Entity entity) {
        String sql = """
                UPDATE entities
                SET code             = :code,
                    description      = :description,
                    last_update_date = :lastUpdateDate,
                    last_update_user = :lastUpdateUser,
                    canceled         = :canceled
                WHERE id       = :id
                  AND canceled = false
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("code", entity.getCode())
                .addValue("description", entity.getDescription())
                .addValue("lastUpdateDate", entity.getLastUpdateDate() != null
                        ? Timestamp.from(entity.getLastUpdateDate()) : null)
                .addValue("lastUpdateUser", entity.getLastUpdateUser())
                .addValue("canceled", entity.getCanceled())
                .addValue("id", entity.getId());
        jdbcTemplate.update(sql, params);
        return entity;
    }

    @Override
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM entities WHERE id = :id AND canceled = false";
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("id", id), Long.class);
        return count != null && count > 0;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastUpdateDate = rs.getTimestamp("last_update_date");
        return Entity.builder()
                .id(rs.getObject("id", UUID.class))
                .code(rs.getString("code"))
                .description(rs.getString("description"))
                .createDate(rs.getTimestamp("create_date").toInstant())
                .createUser(rs.getString("create_user"))
                .lastUpdateDate(lastUpdateDate != null ? lastUpdateDate.toInstant() : null)
                .lastUpdateUser(rs.getString("last_update_user"))
                .canceled(rs.getBoolean("canceled"))
                .build();
    }

    /**
     * Builds a safe ORDER BY clause from Pageable sort.
     * Only allow-listed column names are accepted to prevent SQL injection.
     */
    private String buildOrderBy(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return "create_date DESC";
        }
        return pageable.getSort().stream()
                .map(order -> toColumnName(order.getProperty()) + " " + order.getDirection().name())
                .collect(Collectors.joining(", "));
    }

    private String toColumnName(String field) {
        return switch (field) {
            case "code"        -> "code";
            case "description" -> "description";
            case "createDate"  -> "create_date";
            case "createUser"  -> "create_user";
            default            -> "create_date";
        };
    }
}
