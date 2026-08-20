-- Inventory uniqueness for correlation matching (vendor/product/version per asset).
CREATE UNIQUE INDEX IF NOT EXISTS uq_asset_software_asset_product_version
    ON asset_software (asset_id, vendor, product, COALESCE(version, ''));

CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications (status);
CREATE INDEX IF NOT EXISTS idx_notifications_org ON notifications (organization_id);
