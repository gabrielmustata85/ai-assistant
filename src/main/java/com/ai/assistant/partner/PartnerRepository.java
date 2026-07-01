package com.ai.assistant.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnerRepository extends JpaRepository<Partner, Long> {
    List<Partner> findByCompanyIdOrderByNameAsc(Long companyId);
    Optional<Partner> findFirstByCompanyIdAndCuiIgnoreCase(Long companyId, String cui);
    Optional<Partner> findFirstByCompanyIdAndNameIgnoreCase(Long companyId, String name);
}
