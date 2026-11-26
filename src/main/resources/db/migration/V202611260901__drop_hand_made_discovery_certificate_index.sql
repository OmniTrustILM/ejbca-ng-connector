-- Some installations created this single-column index by hand, and the (discovery_id, id) index makes it redundant.
-- It is dropped in a migration of its own: in the transaction that builds that index, the drop deadlocks with a
-- delete still running on the previous version.
DROP INDEX IF EXISTS discovery_certificate_discovery_id_idx;
