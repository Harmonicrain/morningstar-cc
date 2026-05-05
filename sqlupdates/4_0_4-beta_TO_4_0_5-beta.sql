SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- Update 4.0.4-beta to 4.0.5-beta
-- =====================================================

ALTER TABLE `navigator_publiccats`
    ADD COLUMN IF NOT EXISTS `image_url` VARCHAR(255) NULL DEFAULT NULL AFTER `image`;

ALTER TABLE `users_settings`
    ADD COLUMN IF NOT EXISTS `new_navigator_enabled` ENUM('0', '1') NOT NULL DEFAULT '1' AFTER `ui_flags`;

SET @uniq_cat_room_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'navigator_publics'
      AND INDEX_NAME = 'uniq_cat_room'
);
SET @sql_uniq_cat_room = IF(@uniq_cat_room_exists = 0,
    'ALTER TABLE `navigator_publics` ADD UNIQUE KEY `uniq_cat_room` (`public_cat_id`, `room_id`)',
    'SELECT 1');
PREPARE stmt_uniq_cat_room FROM @sql_uniq_cat_room;
EXECUTE stmt_uniq_cat_room;
DEALLOCATE PREPARE stmt_uniq_cat_room;

INSERT INTO `navigator_publiccats` (`name`, `visible`, `image`, `image_url`, `order_num`)
SELECT 'Public Rooms', '1', '1', 'official_view', COALESCE(MAX(x.`order_num`), 0) + 1
FROM `navigator_publiccats` x
WHERE NOT EXISTS (
    SELECT 1
    FROM `emulator_settings`
    WHERE `key` = 'hotel.navigator.officialroot.categoryid'
);

SET @root_id = (
    SELECT CAST(`value` AS UNSIGNED)
    FROM `emulator_settings`
    WHERE `key` = 'hotel.navigator.officialroot.categoryid'
    LIMIT 1
);
SET @root_id = IFNULL(@root_id, LAST_INSERT_ID());
SET @root_id = IFNULL(@root_id, (
    SELECT `id`
    FROM `navigator_publiccats`
    WHERE `name` = 'Public Rooms'
    ORDER BY `id` DESC
    LIMIT 1
));

INSERT INTO `emulator_settings` (`key`, `value`)
VALUES ('hotel.navigator.officialroot.categoryid', CAST(@root_id AS CHAR))
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

INSERT IGNORE INTO `navigator_publics` (`public_cat_id`, `room_id`, `visible`)
SELECT @root_id, `id`, '1'
FROM `rooms`
WHERE `is_public` = '1';

INSERT IGNORE INTO `navigator_publics` (`public_cat_id`, `room_id`, `visible`)
SELECT CAST(es.`value` AS UNSIGNED), r.`id`, '1'
FROM `rooms` r
JOIN `emulator_settings` es ON es.`key` = 'hotel.navigator.staffpicks.categoryid'
WHERE r.`is_staff_picked` = '1';

UPDATE `users_saved_searches`
SET `search_code` = 'official_view'
WHERE `search_code` = 'official-root';

UPDATE `users_navigator_settings`
SET `caption` = 'Public Rooms'
WHERE `caption` = 'official-root';

UPDATE `users_settings`
SET `new_navigator_enabled` = IF((`ui_flags` & 4) = 4, '1', '0')
WHERE `new_navigator_enabled` IS NULL OR `new_navigator_enabled` NOT IN ('0', '1');

ALTER TABLE `rooms`
    DROP COLUMN `is_public`,
    DROP COLUMN `is_staff_picked`;

SET FOREIGN_KEY_CHECKS = 1;
