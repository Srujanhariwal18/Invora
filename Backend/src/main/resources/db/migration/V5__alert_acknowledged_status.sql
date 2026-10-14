ALTER TABLE alerts DROP CONSTRAINT IF EXISTS chk_alerts_status;
ALTER TABLE alerts ADD CONSTRAINT chk_alerts_status
    CHECK (status IN ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED'));
