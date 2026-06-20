package com.otilm.ca.connector.ejbca.dao;

import com.otilm.ca.connector.ejbca.dao.entity.AuthorityInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorityInstanceRepository extends JpaRepository<AuthorityInstance, Long> {

    Optional<AuthorityInstance> findByName(String name);

    Optional<AuthorityInstance> findByUuid(String uuid);
}
