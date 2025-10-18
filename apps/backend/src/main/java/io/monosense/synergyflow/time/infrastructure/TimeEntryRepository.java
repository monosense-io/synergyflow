package io.monosense.synergyflow.time.infrastructure;

import io.monosense.synergyflow.time.domain.TimeEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Repository for time entry persistence using JDBC.
 *
 * <p>Provides CRUD operations for time entries using Spring JDBC
 * with PostgreSQL, following the existing pattern in the codebase.
 *
 * @author monosense
 * @since 0.1.0
 * @version 0.1.0
 */
@Repository
public class TimeEntryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TimeEntryRowMapper rowMapper = new TimeEntryRowMapper();

    public TimeEntryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Saves a time entry to the database.
     *
     * @param timeEntry the time entry to save
     * @return the saved time entry with generated ID
     */
    public TimeEntry save(TimeEntry timeEntry) {
        String sql = """
            INSERT INTO time_entries (
                id, user_id, duration_minutes, description,
                occurred_at, status, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                user_id = EXCLUDED.user_id,
                duration_minutes = EXCLUDED.duration_minutes,
                description = EXCLUDED.description,
                occurred_at = EXCLUDED.occurred_at,
                status = EXCLUDED.status,
                created_at = EXCLUDED.created_at
            """;

        jdbcTemplate.update(sql,
                timeEntry.id(),
                timeEntry.userId(),
                timeEntry.duration().toMinutes(),
                timeEntry.description(),
                Timestamp.from(timeEntry.occurredAt()),
                timeEntry.status().name(),
                Timestamp.from(timeEntry.createdAt())
        );

        return timeEntry;
    }

    /**
     * Finds a time entry by its ID.
     *
     * @param id the time entry ID
     * @return the time entry if found
     */
    public Optional<TimeEntry> findById(String id) {
        String sql = "SELECT * FROM time_entries WHERE id = ?";
        List<TimeEntry> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Finds all time entries for a specific user ordered by creation date (newest first).
     *
     * @param userId the user ID
     * @return list of time entries
     */
    public List<TimeEntry> findByUserIdOrderByCreatedAtDesc(String userId) {
        String sql = """
            SELECT * FROM time_entries
            WHERE user_id = ?
            ORDER BY created_at DESC
            """;
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    /**
     * Finds time entries by status.
     *
     * @param status the time entry status
     * @return list of time entries with the specified status
     */
    public List<TimeEntry> findByStatus(TimeEntry.TimeEntryStatus status) {
        String sql = "SELECT * FROM time_entries WHERE status = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }

    /**
     * Finds time entries that occurred within a time range.
     *
     * @param start the start of the time range (inclusive)
     * @param end the end of the time range (inclusive)
     * @return list of time entries within the range
     */
    public List<TimeEntry> findByOccurredAtBetweenOrderByOccurredAtDesc(Instant start, Instant end) {
        String sql = """
            SELECT * FROM time_entries
            WHERE occurred_at BETWEEN ? AND ?
            ORDER BY occurred_at DESC
            """;
        return jdbcTemplate.query(sql, rowMapper, Timestamp.from(start), Timestamp.from(end));
    }

    /**
     * RowMapper for converting database rows to TimeEntry objects.
     */
    private static class TimeEntryRowMapper implements RowMapper<TimeEntry> {
        @Override
        public TimeEntry mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            return new TimeEntry(
                    rs.getString("id"),
                    rs.getString("user_id"),
                    Duration.ofMinutes(rs.getLong("duration_minutes")),
                    rs.getString("description"),
                    rs.getTimestamp("occurred_at").toInstant(),
                    TimeEntry.TimeEntryStatus.valueOf(rs.getString("status")),
                    rs.getTimestamp("created_at").toInstant()
            );
        }
    }
}