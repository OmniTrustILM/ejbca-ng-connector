package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.connector.discovery.DiscoveryRequestDto;
import com.otilm.api.model.core.discovery.DiscoveryStatus;
import com.otilm.ca.connector.ejbca.dao.DiscoveryHistoryRepository;
import com.otilm.ca.connector.ejbca.dao.entity.DiscoveryHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiscoveryHistoryServiceImplTest {

    @Mock
    DiscoveryHistoryRepository discoveryHistoryRepository;

    DiscoveryHistoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DiscoveryHistoryServiceImpl();
        service.setDiscoveryHistoryRepository(discoveryHistoryRepository);
    }

    @Test
    void addHistory_savesEntityWithCorrectFields() {
        DiscoveryRequestDto request = new DiscoveryRequestDto();
        request.setName("test-discovery");

        // repository.save returns what it receives (or a new instance)
        given(discoveryHistoryRepository.save(any(DiscoveryHistory.class)))
                .willAnswer(inv -> inv.getArgument(0));

        DiscoveryHistory result = service.addHistory(request);

        assertNotNull(result);
        assertEquals("test-discovery", result.getName());
        assertEquals(DiscoveryStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getUuid());
        assertFalse(result.getUuid().isEmpty());

        verify(discoveryHistoryRepository).save(result);
    }

    @Test
    void getHistoryById_present_returnsEntity() throws NotFoundException {
        DiscoveryHistory history = new DiscoveryHistory();
        history.setId(1L);
        history.setName("found");
        given(discoveryHistoryRepository.findById(1L)).willReturn(Optional.of(history));

        DiscoveryHistory result = service.getHistoryById(1L);

        assertNotNull(result);
        assertEquals("found", result.getName());
    }

    @Test
    void getHistoryById_absent_throwsNotFoundException() {
        given(discoveryHistoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getHistoryById(99L));
    }

    @Test
    void getHistoryByUuid_present_returnsEntity() throws NotFoundException {
        String uuid = "abc-123";
        DiscoveryHistory history = new DiscoveryHistory();
        history.setUuid(uuid);
        given(discoveryHistoryRepository.findByUuid(uuid)).willReturn(Optional.of(history));

        DiscoveryHistory result = service.getHistoryByUuid(uuid);

        assertEquals(uuid, result.getUuid());
    }

    @Test
    void getHistoryByUuid_absent_throwsNotFoundException() {
        given(discoveryHistoryRepository.findByUuid("missing")).willReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getHistoryByUuid("missing"));
    }

    @Test
    void setHistory_delegatesToRepository() {
        DiscoveryHistory history = new DiscoveryHistory();
        history.setName("to-update");

        service.setHistory(history);

        verify(discoveryHistoryRepository).save(history);
    }

    @Test
    void deleteHistory_delegatesToRepository() {
        DiscoveryHistory history = new DiscoveryHistory();
        history.setName("to-delete");

        service.deleteHistory(history);

        verify(discoveryHistoryRepository).delete(history);
    }
}
