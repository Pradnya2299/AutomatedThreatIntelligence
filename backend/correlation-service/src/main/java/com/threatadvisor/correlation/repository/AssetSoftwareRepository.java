package com.threatadvisor.correlation.repository;

import com.threatadvisor.correlation.domain.AssetSoftware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssetSoftwareRepository extends JpaRepository<AssetSoftware, UUID> {

    @Query(value = """
            SELECT s.*
            FROM asset_software s
            JOIN assets a ON a.id = s.asset_id
            WHERE regexp_replace(lower(s.vendor), '[^a-z0-9]', '', 'g') = :vendorKey
              AND regexp_replace(lower(s.product), '[^a-z0-9]', '', 'g') = :productKey
              AND s.installation_status = 'INSTALLED'
              AND a.status = 'ACTIVE'
            """, nativeQuery = true)
    List<AssetSoftware> findInstalledByNormalizedVendorProduct(
            @Param("vendorKey") String vendorKey,
            @Param("productKey") String productKey);
}
