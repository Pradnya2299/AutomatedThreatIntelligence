package com.threatadvisor.correlation.repository;

import com.threatadvisor.correlation.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
