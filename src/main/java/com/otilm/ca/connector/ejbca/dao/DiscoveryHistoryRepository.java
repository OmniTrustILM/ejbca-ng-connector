package com.otilm.ca.connector.ejbca.dao;

import com.otilm.ca.connector.ejbca.dao.entity.DiscoveryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscoveryHistoryRepository extends JpaRepository<DiscoveryHistory, Long> {

    Optional<DiscoveryHistory> findById(Long Id);

    Optional<DiscoveryHistory> findByUuid(String uuid);
}