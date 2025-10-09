package io.monosense.synergyflow.eventing.internal;

import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.Priority;
import io.monosense.synergyflow.itsm.internal.domain.TicketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Verifies AC2: outbox write participates in the SAME transaction as domain write
 * and does not persist on rollback.
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransactionalSemanticsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TicketService ticketService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UUID testUserId;

    @BeforeEach
    void createTestUser() {
        testUserId = UUID.randomUUID();
        String uniqueUsername = "txnuser-" + testUserId.toString().substring(0, 8);
        String uniqueEmail = uniqueUsername + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, is_active, version) VALUES (?, ?, ?, ?, ?, ?)",
                testUserId, uniqueUsername, uniqueEmail, "Txn User", true, 1
        );
    }

    @Test
    void outboxRowIsNotPersistedOnRollback() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        try {
            template.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(@NonNull TransactionStatus status) {
                    // Domain write + publish
                    CreateTicketCommand command = new CreateTicketCommand(
                            "Rollback Ticket",
                            "Test rollback scenario",
                            TicketType.INCIDENT,
                            Priority.MEDIUM,
                            null,
                            testUserId
                    );
                    ticketService.createTicket(command, testUserId);
                    // Force rollback after publish
                    status.setRollbackOnly();
                }
            });
        } catch (Exception e) {
            fail("Transaction template should not throw: %s", e);
        }

        // After rollback, outbox must be empty
        assertThat(outboxRepository.findAll()).isEmpty();
    }
}
