package io.monosense.synergyflow.eventing.internal.worker;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
class UserLookup {
    private final JdbcTemplate jdbc;

    UserLookup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Optional<Map<String, String>> nameEmailById(UUID userId) {
        try {
            return Optional.ofNullable(jdbc.query(
                    "SELECT full_name, email FROM users WHERE id = ?",
                    ps -> ps.setObject(1, userId),
                    rs -> rs.next() ? Map.of(
                            "name", rs.getString("full_name"),
                            "email", rs.getString("email")
                    ) : null
            ));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}

