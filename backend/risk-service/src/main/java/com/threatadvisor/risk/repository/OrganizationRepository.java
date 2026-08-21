package com.threatadvisor.risk.repository;

import com.threatadvisor.risk.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
