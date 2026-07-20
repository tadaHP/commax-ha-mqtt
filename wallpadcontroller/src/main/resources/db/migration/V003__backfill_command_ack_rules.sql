-- Repairs databases that ran V002 before the initial Commax seed was inserted.
-- The updates are idempotent and also complete any previously incomplete ACK policy.
UPDATE command_mapping_rule SET ack_pattern = 'B1 01 {index}', ack_mask = 'FF FF FF' WHERE id = 1;
UPDATE command_mapping_rule SET ack_pattern = 'B1 00 {index}', ack_mask = 'FF FF FF' WHERE id = 2;
UPDATE command_mapping_rule SET ack_pattern = 'A2 01 {index}', ack_mask = 'FF FF FF' WHERE id = 3;
UPDATE command_mapping_rule SET ack_pattern = 'A2 00 {index}', ack_mask = 'FF FF FF' WHERE id = 4;
UPDATE command_mapping_rule SET ack_pattern = '84 81 {index}', ack_mask = 'FF FF FF' WHERE id = 5;
UPDATE command_mapping_rule SET ack_pattern = '84 80 {index}', ack_mask = 'FF FF FF' WHERE id = 6;
UPDATE command_mapping_rule SET ack_pattern = '84 00 {index}', ack_mask = 'FF 00 FF' WHERE id = 7;
UPDATE command_mapping_rule SET ack_pattern = 'F8 00 {index}', ack_mask = 'FF 00 FF' WHERE id IN (8, 9, 10, 11, 12, 14, 15);
UPDATE command_mapping_rule SET ack_pattern = '91', ack_mask = 'FF' WHERE id = 13;
UPDATE command_mapping_rule SET ack_pattern = NULL, ack_mask = NULL WHERE id IN (16, 17, 18, 19, 20);
UPDATE command_mapping_rule
SET ack_pattern = '23', ack_mask = 'FF', ack_timeout_ms = 500, retry_delay_ms = 0, max_retries = 0
WHERE id = 21;
