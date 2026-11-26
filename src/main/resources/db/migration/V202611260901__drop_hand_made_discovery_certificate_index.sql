-- Some installations created this single-column index by hand, and the (discovery_id, id) index makes it redundant.
-- It is dropped in a migration of its own: in the transaction that builds that index, the drop deadlocks with a
-- delete still running on the previous version. The name is qualified because Flyway keeps the original search path
-- after the connector's schema, and another component's schema there may hold an index of the same name.
DROP INDEX IF EXISTS "${flyway:defaultSchema}".discovery_certificate_discovery_id_idx;
