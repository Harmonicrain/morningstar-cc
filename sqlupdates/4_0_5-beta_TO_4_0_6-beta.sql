SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- Update 4.0.5-beta to 4.0.6-beta
-- =====================================================

ALTER TABLE `permissions`
    ADD COLUMN IF NOT EXISTS `acc_public_pick` ENUM('0','1') NOT NULL DEFAULT '0' AFTER `acc_staff_pick`;

ALTER TABLE `permissions`
    ADD COLUMN IF NOT EXISTS `cmd_room_poll` ENUM('0','1') NOT NULL DEFAULT '0' AFTER `cmd_roommute`;

DROP TEMPORARY TABLE IF EXISTS `tmp_official_view_navigator_settings`;

CREATE TEMPORARY TABLE `tmp_official_view_navigator_settings` AS
SELECT public_rooms.`user_id`,
       MAX(public_rooms.`list_type`) AS `list_type`,
       MAX(public_rooms.`display`) AS `display`
FROM `users_navigator_settings` public_rooms
LEFT JOIN `users_navigator_settings` official
    ON official.`user_id` = public_rooms.`user_id`
    AND official.`caption` = 'official_view'
WHERE public_rooms.`caption` = 'Public Rooms'
  AND official.`user_id` IS NULL
GROUP BY public_rooms.`user_id`;

INSERT INTO `users_navigator_settings` (`user_id`, `caption`, `list_type`, `display`)
SELECT `user_id`, 'official_view', `list_type`, `display`
FROM `tmp_official_view_navigator_settings`;

DELETE FROM `users_navigator_settings`
WHERE `caption` = 'Public Rooms';

DROP TEMPORARY TABLE IF EXISTS `tmp_official_view_navigator_settings`;

INSERT IGNORE INTO `emulator_settings` (`key`, `value`) VALUES
('navigator.legacy.search.ad.enabled', '0'),
('navigator.legacy.search.ad.room_id', '0'),
('navigator.legacy.search.ad.title', ''),
('navigator.legacy.search.ad.description', ''),
('navigator.legacy.search.ad.image', '');

UPDATE `emulator_texts`
SET `value` = TRIM(BOTH ';' FROM REPLACE(REPLACE(CONCAT(';', `value`, ';'), ';roompoll;', ';'), ';room_poll;', ';'))
WHERE `key` = 'commands.keys.cmd_word_quiz'
  AND CONCAT(';', `value`, ';') REGEXP ';roompoll;|;room_poll;';

INSERT IGNORE INTO `emulator_texts` (`key`, `value`) VALUES
('commands.keys.cmd_room_poll', 'roompoll;room_poll'),
('commands.description.cmd_room_poll', ':roompoll <seconds> <question>;<choice1>;<choice2>;...');

SET FOREIGN_KEY_CHECKS = 1;
