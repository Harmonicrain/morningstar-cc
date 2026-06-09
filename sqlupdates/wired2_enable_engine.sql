-- Wired 2.0: enable the new wired execution engine.
-- The base 4.0.0 seed inserts wired.engine.enabled = '0'; Wired 2.0 requires it on.
-- Idempotent: inserts the key if missing, otherwise forces it to '1'.

INSERT INTO `emulator_settings` (`key`, `value`)
VALUES ('wired.engine.enabled', '1')
ON DUPLICATE KEY UPDATE `value` = '1';
