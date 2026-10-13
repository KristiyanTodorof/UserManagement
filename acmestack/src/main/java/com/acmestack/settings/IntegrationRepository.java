package com.acmestack.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IntegrationRepository extends JpaRepository<Integration, Long> {
    List<Integration> findAllByOrderByIdAsc();
}