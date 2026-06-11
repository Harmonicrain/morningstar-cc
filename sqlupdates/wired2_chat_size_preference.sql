-- Wired 2.0 / May 2026 chat: persist the free-flow chat font-size selector.
-- Idempotent for MariaDB 10.2, which has no ADD COLUMN IF NOT EXISTS support.

SET @column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users_settings'
      AND COLUMN_NAME = 'chat_size_preference'
);

SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `users_settings` ADD COLUMN `chat_size_preference` TINYINT(1) NOT NULL DEFAULT 0 AFTER `chat_color`',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
