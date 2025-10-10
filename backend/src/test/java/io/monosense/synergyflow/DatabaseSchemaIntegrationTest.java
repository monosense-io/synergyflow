package io.monosense.synergyflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test validating Flyway migrations and database schema.
 * Uses Testcontainers with real PostgreSQL 16 container.
 *
 * Tests verify:
 * - AC1: Flyway migrations execute automatically
 * - AC2: ITSM tables created (tickets, incidents, service_requests, ticket_comments, routing_rules)
 * - AC3: PM tables created (issues, sprints, boards, board_columns, issue_links)
 * - AC4: Shared tables created (users, roles, user_roles, teams, approvals, workflow_states, audit_log, outbox)
 * - AC5: Read models created (ticket_card, sla_tracking, related_incidents, queue_row, issue_card, sprint_summary)
 * - AC6: Schema successfully applied to PostgreSQL 16+
 * - AC7: Critical indexes and constraints exist
 */
@SpringBootTest
@Testcontainers
class DatabaseSchemaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("synergyflow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${test.schema:public}")
    private String testSchema;

    @BeforeEach
    void setSearchPath() {
        // Ensure queries in this test operate against the Flyway schema
        jdbcTemplate.execute("SET search_path TO \"" + testSchema + "\"");
    }

    @Test
    void testFlywayMigrationsExecuted() {
        // AC1: Verify migrations executed successfully (updated for Story 2.3)
        Integer migrationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + testSchema + ".flyway_schema_history WHERE success = true AND version IS NOT NULL",
            Integer.class
        );
        assertThat(migrationCount).isEqualTo(17);

        // Verify migration versions in order
        List<String> versions = jdbcTemplate.queryForList(
            "SELECT version FROM " + testSchema + ".flyway_schema_history WHERE success = true AND version IS NOT NULL ORDER BY installed_rank",
            String.class
        );
        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17");
    }

    @Test
    void testItsmTablesCreated() {
        // AC2: Verify ITSM OLTP tables exist
        // Note: After Story 2.1, incidents and service_requests tables removed (SINGLE_TABLE inheritance in tickets table)
        List<String> itsmTables = List.of(
            "tickets", "ticket_comments", "routing_rules"
        );

        for (String tableName : itsmTables) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?",
                Integer.class,
                testSchema,
                tableName
            );
            assertThat(count).as("Table %s should exist", tableName).isEqualTo(1);
        }

        // Verify tickets table has UUIDv7-compatible id column
        String ticketsIdType = jdbcTemplate.queryForObject(
            "SELECT data_type FROM information_schema.columns " +
            "WHERE table_name = 'tickets' AND column_name = 'id'",
            String.class
        );
        assertThat(ticketsIdType).isEqualTo("uuid");

        // Verify tickets table has ticket_type discriminator column for SINGLE_TABLE inheritance (Story 2.1)
        String ticketTypeColumn = jdbcTemplate.queryForObject(
            "SELECT data_type FROM information_schema.columns " +
            "WHERE table_name = 'tickets' AND column_name = 'ticket_type'",
            String.class
        );
        assertThat(ticketTypeColumn).isEqualTo("character varying");
    }

    @Test
    void testPmTablesCreated() {
        // AC3: Verify PM OLTP tables exist
        List<String> pmTables = List.of(
            "issues", "sprints", "boards", "board_columns", "issue_links"
        );

        for (String tableName : pmTables) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?",
                Integer.class,
                testSchema,
                tableName
            );
            assertThat(count).as("Table %s should exist", tableName).isEqualTo(1);
        }
    }

    @Test
    void testSharedTablesCreated() {
        // AC4: Verify shared tables exist
        List<String> sharedTables = List.of(
            "users", "roles", "user_roles", "teams", "approvals",
            "workflow_states", "audit_log", "outbox"
        );

        for (String tableName : sharedTables) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?",
                Integer.class,
                testSchema,
                tableName
            );
            assertThat(count).as("Table %s should exist", tableName).isEqualTo(1);
        }

        // Verify outbox table has BIGSERIAL id for FIFO ordering
        String outboxIdType = jdbcTemplate.queryForObject(
            "SELECT data_type FROM information_schema.columns " +
            "WHERE table_name = 'outbox' AND column_name = 'id'",
            String.class
        );
        assertThat(outboxIdType).isEqualTo("bigint");
    }

    @Test
    void testReadModelsCreated() {
        // AC5: Verify CQRS-lite read models exist
        List<String> readModels = List.of(
            "ticket_card", "sla_tracking", "related_incidents",
            "queue_row", "issue_card", "sprint_summary"
        );

        for (String tableName : readModels) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?",
                Integer.class,
                testSchema,
                tableName
            );
            assertThat(count).as("Read model %s should exist", tableName).isEqualTo(1);
        }

        // Verify read models have version column for idempotent updates
        for (String tableName : readModels) {
            Integer versionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = ? AND column_name = 'version'",
                Integer.class,
                testSchema,
                tableName
            );
            assertThat(versionCount).as("Read model %s should have version column", tableName).isEqualTo(1);
        }
    }

    @Test
    void testCriticalIndexesExist() {
        // AC7: Verify critical indexes for performance
        List<String> criticalIndexes = List.of(
            "idx_tickets_status",
            "idx_issues_status",
            "idx_outbox_unprocessed",
            // Story 2.3: SLA tracking index on due_at for breach detection queries
            "idx_sla_tracking_due_at",
            "idx_ticket_card_sla_due"
        );

        for (String indexName : criticalIndexes) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE indexname = ?",
                Integer.class,
                indexName
            );
            assertThat(count).as("Index %s should exist", indexName).isEqualTo(1);
        }
    }

    @Test
    void testForeignKeyConstraintsExist() {
        // AC7: Verify key foreign key relationships established in V5

        // tickets -> users FK
        Integer ticketsFkCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints " +
            "WHERE table_schema = ? AND table_name = 'tickets' AND constraint_type = 'FOREIGN KEY'",
            Integer.class,
            testSchema
        );
        assertThat(ticketsFkCount).as("tickets should have FK constraints").isGreaterThanOrEqualTo(2);

        // issues -> users, sprints FK
        Integer issuesFkCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints " +
            "WHERE table_schema = ? AND table_name = 'issues' AND constraint_type = 'FOREIGN KEY'",
            Integer.class,
            testSchema
        );
        assertThat(issuesFkCount).as("issues should have FK constraints").isGreaterThanOrEqualTo(3);

        // outbox unique constraint on (aggregate_id, version)
        Integer outboxUniqueCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes " +
            "WHERE tablename = 'outbox' AND indexname = 'idx_outbox_idempotency' AND indexdef LIKE '%UNIQUE%'",
            Integer.class
        );
        assertThat(outboxUniqueCount).as("outbox should have unique constraint on (aggregate_id, version)").isEqualTo(1);
    }

    @Test
    void testOptimisticLockingColumns() {
        // AC7: Verify all OLTP tables have version column for optimistic locking
        // Note: After Story 2.1, incidents and service_requests tables removed (SINGLE_TABLE inheritance)
        // Note: ticket_comments excluded (immutable, no version column needed)
        List<String> olptTables = List.of(
            "tickets", "routing_rules",
            "issues", "sprints", "boards", "board_columns", "issue_links",
            "users", "roles", "user_roles", "teams", "approvals", "workflow_states"
        );

        for (String tableName : olptTables) {
            Integer versionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = ? AND column_name = 'version' AND data_type = 'bigint'",
                Integer.class,
                tableName
            );
            assertThat(versionCount).as("Table %s should have bigint version column", tableName).isEqualTo(1);
        }
    }

    @Test
    void testDatabaseVersion() {
        // AC6: Verify PostgreSQL 16+ is used
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertThat(version).contains("PostgreSQL 16");
    }

    @Test
    void testTableCount() {
        // Verify total table count: 22 domain tables + 1 flyway_schema_history = 23
        // Breakdown: V1=5 ITSM → V6=3 ITSM (after SINGLE_TABLE refactoring), V2=5 PM, V3=8 shared, V4=6 read models
        // V6 drops incidents and service_requests tables (SINGLE_TABLE inheritance in tickets)
        Integer tableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ?",
            Integer.class,
            testSchema
        );
        assertThat(tableCount).isEqualTo(24);
    }

    @Test
    void testFlywayChecksumValidation() {
        // Verify Flyway checksums for migration immutability
        // Checksums ensure migration files haven't been modified after execution
        List<Map<String, Object>> migrations = jdbcTemplate.queryForList(
            "SELECT version, description, checksum, success FROM " + testSchema + ".flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank"
        );

        assertThat(migrations).hasSize(17); // + V15 (team_members), V16 (seed), V17 (routing_rules composite index)

        // Verify all migrations succeeded
        for (Map<String, Object> migration : migrations) {
            assertThat(migration.get("success"))
                .as("Migration %s should have succeeded", migration.get("version"))
                .isEqualTo(true);

            // Verify checksum exists (non-null, indicating file content is tracked)
            assertThat(migration.get("checksum"))
                .as("Migration %s should have checksum for immutability verification", migration.get("version"))
                .isNotNull();
        }

        // Verify migration descriptions match expected names
        assertThat(migrations.get(0).get("description")).isEqualTo("create itsm tables");
        assertThat(migrations.get(1).get("description")).isEqualTo("create pm tables");
        assertThat(migrations.get(2).get("description")).isEqualTo("create shared tables");
        assertThat(migrations.get(3).get("description")).isEqualTo("create read models");
        assertThat(migrations.get(4).get("description")).isEqualTo("add foreign key constraints");
        assertThat(migrations.get(5).get("description")).isEqualTo("refactor tickets single table inheritance");
        assertThat(migrations.get(6).get("description")).isEqualTo("add ticket comments and routing rules");
        assertThat(migrations.get(7).get("description")).isEqualTo("add reopen count to tickets");
        assertThat(migrations.get(8).get("description")).isEqualTo("add ticket performance indexes");
        assertThat(migrations.get(9).get("description")).isEqualTo("modify outbox idempotency index");
        // Story 2.3 additions
        assertThat(migrations.get(10).get("description")).isEqualTo("create sla tracking table");
        assertThat(migrations.get(11).get("description")).isEqualTo("make ticket priority nullable");
        assertThat(migrations.get(12).get("description")).isEqualTo("create teams table");
        assertThat(migrations.get(13).get("description")).isEqualTo("routing rules team column already present");
        assertThat(migrations.get(14).get("description")).isEqualTo("create team members table");
        assertThat(migrations.get(15).get("description")).isEqualTo("seed team memberships");
        assertThat(migrations.get(16).get("description")).isEqualTo("add routing rules composite index");
    }
}
