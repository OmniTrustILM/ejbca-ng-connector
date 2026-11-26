CREATE INDEX IF NOT EXISTS discovery_certificate_discovery_id_id_idx ON discovery_certificate (discovery_id, id);

-- Some installations created this single-column index by hand; the index above covers what it served.
DROP INDEX IF EXISTS discovery_certificate_discovery_id_idx;
