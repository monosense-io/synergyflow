package io.monosense.synergyflow.eventing.readmodel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketCardRepositoryImpl Unit Tests")
class TicketCardRepositoryImplTest {

    @Mock
    EntityManager entityManager;

    @Mock
    Query query;

    private TicketCardRepositoryImpl repoWith(EntityManager em) throws Exception {
        TicketCardRepositoryImpl impl = new TicketCardRepositoryImpl();
        Field f = TicketCardRepositoryImpl.class.getDeclaredField("entityManager");
        f.setAccessible(true);
        f.set(impl, em);
        return impl;
    }

    @Test
    @DisplayName("findAgentWithFewestTickets maps UUID result directly")
    void mapsUuidResult() throws Exception {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("teamId"), any())).thenReturn(query);
        UUID expected = UUID.randomUUID();
        when(query.getSingleResult()).thenReturn(expected);

        TicketCardRepositoryImpl impl = repoWith(entityManager);
        Optional<UUID> out = impl.findAgentWithFewestTickets(UUID.randomUUID());
        assertThat(out).contains(expected);
    }

    @Test
    @DisplayName("findAgentWithFewestTickets parses String UUID result")
    void parsesStringUuidResult() throws Exception {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("teamId"), any())).thenReturn(query);
        UUID expected = UUID.randomUUID();
        when(query.getSingleResult()).thenReturn(expected.toString());

        TicketCardRepositoryImpl impl = repoWith(entityManager);
        Optional<UUID> out = impl.findAgentWithFewestTickets(UUID.randomUUID());
        assertThat(out).contains(expected);
    }

    @Test
    @DisplayName("findAgentWithFewestTickets returns empty when result is null")
    void returnsEmptyWhenNull() throws Exception {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("teamId"), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(null);

        TicketCardRepositoryImpl impl = repoWith(entityManager);
        Optional<UUID> out = impl.findAgentWithFewestTickets(UUID.randomUUID());
        assertThat(out).isEmpty();
    }
}

