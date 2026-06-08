SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- Update 4.0.6-beta to 4.0.7-beta
-- Public room data/protocol support (S1/S2/S3)
-- =====================================================

-- Baked public furniture by room model/layout name (e.g. netcafe)
CREATE TABLE IF NOT EXISTS `public_items` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `room_model` VARCHAR(64) NOT NULL,
    `sprite` VARCHAR(64) NOT NULL,
    `x` INT NOT NULL,
    `y` INT NOT NULL,
    `z` INT NOT NULL DEFAULT 0,
    `rotation` INT NOT NULL DEFAULT 0,
    `length` INT NOT NULL DEFAULT 1,
    `width` INT NOT NULL DEFAULT 1,
    `behaviour` VARCHAR(255) NOT NULL DEFAULT '',
    `top_height` DOUBLE NOT NULL DEFAULT 1,
    `has_dimensions` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_public_items_room_model` (`room_model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed the baked public furniture. Idempotent: clear these two models first.
-- `id` omitted so auto-increment assigns fresh ids (internal handle only).
DELETE FROM `public_items` WHERE `room_model` IN ('netcafe', 'newbie_lobby');
INSERT INTO `public_items`
    (`room_model`, `sprite`, `x`, `y`, `z`, `rotation`, `length`, `width`, `behaviour`, `top_height`, `has_dimensions`)
VALUES
('netcafe','k',15,7,0,2,1,1,'can_sit_on_top',1,0),
('netcafe','k',12,12,0,6,1,1,'can_sit_on_top',1,0),
('netcafe','k',15,9,0,2,1,1,'can_sit_on_top',1,0),
('netcafe','k',16,1,1,0,1,1,'can_sit_on_top',1,0),
('netcafe','k',18,10,0,6,1,1,'can_sit_on_top',1,0),
('netcafe','k',18,9,0,6,1,1,'can_sit_on_top',1,0),
('netcafe','k',19,1,1,0,1,1,'can_sit_on_top',1,0),
('netcafe','k',4,10,1,6,1,1,'can_sit_on_top',1,0),
('netcafe','k',4,12,1,6,1,1,'can_sit_on_top',1,0),
('netcafe','k',6,18,1,6,1,1,'can_sit_on_top',1,0),
('netcafe','k',9,9,0,2,1,1,'can_sit_on_top',1,0),
('netcafe','kukat1',13,3,1,0,1,1,'solid',0.001,0),
('netcafe','kukat1',20,3,1,0,1,1,'solid',0.001,0),
('netcafe','kukat1',6,16,1,0,1,1,'solid',0.001,0),
('netcafe','kukat1',8,16,1,0,1,1,'solid',0.001,0),
('netcafe','kukat2',12,3,1,0,1,1,'solid',0.001,0),
('netcafe','kukat2',19,3,1,0,1,1,'solid',0.001,0),
('netcafe','kukat2',5,16,1,0,1,1,'solid',0.001,0),
('netcafe','kukat2',7,16,1,0,1,1,'solid',0.001,0),
('netcafe','kukat3',9,16,1,0,1,1,'solid',0.001,0),
('netcafe','kukat3',9,18,1,0,1,1,'solid',0.001,0),
('netcafe','kukat4',9,17,1,0,1,1,'solid',0.001,0),
('netcafe','kukat4',9,19,1,0,1,1,'solid',0.001,0),
('netcafe','kukat5',6,3,1,0,1,1,'solid',0.001,0),
('netcafe','kukat6',5,3,1,0,1,1,'solid',0.001,0),
('netcafe','kukka',20,23,1,0,1,1,'solid',0.001,0),
('netcafe','kukka2',15,16,1,0,1,1,'solid',0.001,0),
('netcafe','l',12,11,0,6,1,1,'can_sit_on_top',1,0),
('netcafe','l',13,1,1,0,1,1,'can_sit_on_top',1,0),
('netcafe','l',15,8,0,2,1,1,'can_sit_on_top',1,0),
('netcafe','l',18,8,0,6,1,1,'can_sit_on_top',1,0),
('netcafe','l',4,11,1,6,1,1,'can_sit_on_top',1,0),
('netcafe','l',9,11,0,2,1,1,'can_sit_on_top',1,0),
('netcafe','l',9,12,0,2,1,1,'can_sit_on_top',1,0),
('netcafe','m',10,1,1,0,1,1,'can_sit_on_top',1,0),
('netcafe','m',12,10,0,6,1,1,'can_sit_on_top',1,0),
('netcafe','m',12,9,0,6,1,1,'can_sit_on_top',1,0),
('netcafe','m',15,10,0,2,1,1,'can_sit_on_top',1,0),
('netcafe','m',18,7,0,6,1,1,'can_sit_on_top',1,0),
('netcafe','m',4,13,1,6,1,1,'can_sit_on_top',1,0),
('netcafe','m',4,9,1,6,1,1,'can_sit_on_top',1,0),
('netcafe','m',6,21,1,6,1,1,'can_sit_on_top',1,0),
('netcafe','m',9,10,0,2,1,1,'can_sit_on_top',1,0),
('netcafe','shift1',0,5,1,4,1,1,'can_sit_on_top',1,0),
('netcafe','shift1',12,4,0,4,1,1,'can_sit_on_top',1,0),
('netcafe','shift1',19,4,0,4,1,1,'can_sit_on_top',1,0),
('netcafe','shift1',2,5,1,4,1,1,'can_sit_on_top',1,0),
('netcafe','shift1',6,0,1,4,1,1,'can_sit_on_top',1,0),
('netcafe','shift2',1,5,1,4,1,1,'solid',0.001,0),
('netcafe','shift2',13,4,0,4,1,1,'solid',0.001,0),
('netcafe','shift2',20,4,0,4,1,1,'solid',0.001,0),
('netcafe','shift2',3,5,1,4,1,1,'solid',0.001,0),
('netcafe','shift2',7,0,1,4,1,1,'solid',0.001,0),
('netcafe','sofabig1',20,21,1,6,1,1,'can_sit_on_top',1,0),
('netcafe','sofabig2',20,19,1,6,1,1,'can_sit_on_top',1,0),
('netcafe','sofabig2',20,20,1,6,1,1,'can_sit_on_top',1,0),
('netcafe','sofabig3',20,18,1,6,1,1,'can_sit_on_top',1,0),
('netcafe','sofalittle1',18,23,1,0,1,1,'can_sit_on_top',1,0),
('netcafe','sofalittle2',17,23,1,0,1,1,'can_sit_on_top',1,0),
('netcafe','sofalittle3',16,23,1,0,1,1,'can_sit_on_top',1,0),
('netcafe','table1',17,19,1,4,1,1,'solid',0.001,0),
('netcafe','table1',17,20,1,2,1,1,'solid',0.001,0),
('netcafe','table1',17,21,1,0,1,1,'solid',0.001,0),
('netcafe','table2',16,10,0,0,1,1,'solid',0.001,0),
('netcafe','table2',16,7,0,6,1,1,'solid',0.001,0),
('netcafe','table2',16,8,0,4,1,1,'solid',0.001,0),
('netcafe','table2',16,9,0,2,1,1,'solid',0.001,0),
('netcafe','table3',10,10,0,4,1,1,'solid',0.001,0),
('netcafe','table3',10,11,0,2,1,1,'solid',0.001,0),
('netcafe','table3',10,12,0,0,1,1,'solid',0.001,0),
('netcafe','table3',10,9,0,6,1,1,'solid',0.001,0),
('netcafe','tablecorner',11,9,0,0,1,1,'solid',0.001,0),
('netcafe','tablecorner',17,7,0,0,1,1,'solid',0.001,0),
('netcafe','tablecorner',18,19,1,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_lamp',16,0,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_sofa2c',17,0,0,4,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_sofa2b',18,0,0,4,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_sofa2a',19,0,0,4,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_lamp',20,0,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_chair',16,1,0,2,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_lamp',7,2,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_lamp',11,2,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_chair',16,2,0,2,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_pillar',5,3,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_chair',7,3,0,2,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_table1b',9,3,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_sofa1c',11,3,0,6,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_chair',16,3,0,2,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_table2b',19,3,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_table2a',20,3,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_lamp',0,4,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_sofa2c',1,4,0,4,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_sofa2b',2,4,0,4,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_sofa2a',3,4,0,4,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_lamp',4,4,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_table1a',9,4,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_sofa1b',11,4,0,6,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_wall2a',15,4,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_lamp',16,4,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_chair',0,5,0,2,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_chair',7,5,0,2,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_sofa1a',11,5,0,6,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_table2b',2,6,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_table2a',3,6,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_lamp',11,6,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_chair',0,7,0,2,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_lamp',0,8,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_sofa3c',1,8,0,0,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_sofa3b',2,8,0,0,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_sofa3a',3,8,0,0,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_lamp',4,8,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_barchair2',19,8,0,0,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_tablebar',20,8,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_barchair2',21,8,0,0,1,1,'can_sit_on_top',1,0),
('newbie_lobby','crl_pillar2',5,9,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_pillar',9,9,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_desk1a',8,15,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_deski',9,15,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_deskh',10,15,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_deskg',10,16,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_deskf',10,17,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_deske',10,18,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_deske',10,19,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_deske',10,20,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_deske',10,21,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_deske',10,22,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_deske',10,23,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_wallb',7,24,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_deske',10,24,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_walla',7,25,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_desk1b',8,25,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_desk1c',9,25,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_desk1d',10,25,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_lamp2',12,27,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_cabinet2',13,27,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_cabinet1',14,27,0,0,1,1,'solid',0.001,0),
('newbie_lobby','crl_lamp2',15,27,0,0,1,1,'solid',0.001,0);

-- Havana-style fixture bots for public rooms.
ALTER TABLE `bots`
    MODIFY `type` ENUM('generic','visitor_log','bartender','weapons_dealer','banned_bot','public_room') NOT NULL DEFAULT 'generic';

-- Navigator public-rooms category. Resolve the configured official-root category instead of
-- assuming id 1, because NavigatorPublicCategory only marks rooms as public when the category id
-- matches hotel.navigator.officialroot.categoryid.
SET @official_root_public_cat_id = (
    SELECT CAST(`value` AS UNSIGNED)
    FROM `emulator_settings`
    WHERE `key` = 'hotel.navigator.officialroot.categoryid'
    LIMIT 1
);

INSERT INTO `navigator_publiccats` (`id`, `name`, `image`, `image_url`, `visible`, `order_num`)
SELECT @official_root_public_cat_id, 'Public Rooms', '1', 'navigator/navi1.png', '1', 1
WHERE @official_root_public_cat_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `navigator_publiccats` WHERE `id` = @official_root_public_cat_id);

INSERT INTO `navigator_publiccats` (`name`, `image`, `image_url`, `visible`, `order_num`)
SELECT 'Public Rooms', '1', 'navigator/navi1.png', '1', 1
WHERE @official_root_public_cat_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM `navigator_publiccats` WHERE `name` = 'Public Rooms');

SET @official_root_public_cat_id = IFNULL(@official_root_public_cat_id, (
    SELECT `id`
    FROM `navigator_publiccats`
    WHERE `name` = 'Public Rooms'
    ORDER BY `id` DESC
    LIMIT 1
));

INSERT INTO `emulator_settings` (`key`, `value`)
VALUES ('hotel.navigator.officialroot.categoryid', CAST(@official_root_public_cat_id AS CHAR))
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

-- =====================================================
-- Public rooms themselves (owned by user 1). Auto-increment ids; dependents
-- linked by the freshly assigned room id (resolved by model), never hardcoded.
-- Every step guarded with NOT EXISTS so re-running the migration is a no-op.
-- =====================================================

-- Net Cafe
INSERT INTO `rooms`
    (`owner_id`, `owner_name`, `name`, `description`, `model`, `state`, `users_max`, `category`, `override_model`, `allow_walkthrough`, `roller_speed`, `trade_mode`)
SELECT 1, (SELECT `username` FROM `users` WHERE `id` = 1 LIMIT 1),
    'Net Cafe', 'Official public Net Cafe', 'netcafe', 'open', 50, 15, 1, 1, 4, 2
WHERE NOT EXISTS (SELECT 1 FROM `rooms` WHERE `model` = 'netcafe' AND `owner_id` = 1);

INSERT INTO `room_models_custom` (`id`, `name`, `door_x`, `door_y`, `door_dir`, `heightmap`)
SELECT r.`id`, 'netcafe', 22, 12, 2, 'xxxxx1111xxxxxxxxxxx1xxxx\r\nxxxxx1111111111111111xxxx\r\nxxxxx1111111111111111xxxx\r\nxxxxx1111111111111111xxxx\r\nxxxxxxxx0000000000000xxxx\r\n111111100000000000000xxxx\r\n111111100000000000000xxxx\r\n111111100000000000000xxxx\r\nxxxx11100000000000000xxxx\r\nx1xx11100000000000000xxxx\r\nx1xx11100000000000000xxxx\r\nx1xx111000000000000000000\r\nx1xx111000000000000000000\r\nxxxx111000000000000000000\r\nxxxx11100000000000000xxxx\r\nxxxx1110000000xx11111xxxx\r\nxxxxx111110000x111111xxxx\r\nxxxxxx111100001111111xxxx\r\nxxxxxx111100001111111xxxx\r\nxxxxxx111100001111111xxxx\r\nxxxxxx111100001111111xxxx\r\nxxxxxx111100001111111xxxx\r\nxxxxxx111100001111111xxxx\r\nxxxxx1111100001111111xxxx\r\n'
FROM `rooms` r
WHERE r.`model` = 'netcafe' AND r.`owner_id` = 1
  AND NOT EXISTS (SELECT 1 FROM `room_models_custom` c WHERE c.`id` = r.`id`);

INSERT INTO `navigator_publics` (`public_cat_id`, `room_id`, `visible`)
SELECT @official_root_public_cat_id, r.`id`, '1'
FROM `rooms` r
WHERE r.`model` = 'netcafe' AND r.`owner_id` = 1
  AND NOT EXISTS (SELECT 1 FROM `navigator_publics` np WHERE np.`room_id` = r.`id`);

-- Newbie Lobby
INSERT INTO `rooms`
    (`owner_id`, `owner_name`, `name`, `description`, `model`, `state`, `users_max`, `category`, `override_model`, `allow_walkthrough`, `roller_speed`, `trade_mode`)
SELECT 1, (SELECT `username` FROM `users` WHERE `id` = 1 LIMIT 1),
    'Newbie Lobby', 'Newbie Lobby.', 'newbie_lobby', 'open', 50, 15, 1, 1, 4, 2
WHERE NOT EXISTS (SELECT 1 FROM `rooms` WHERE `model` = 'newbie_lobby' AND `owner_id` = 1);

INSERT INTO `room_models_custom` (`id`, `name`, `door_x`, `door_y`, `door_dir`, `heightmap`)
SELECT r.`id`, 'newbie_lobby', 2, 11, 2, 'xxxxxxxxxxxxxxxx000000\r\nxxxxx0xxxxxxxxxx000000\r\nxxxxx00000000xxx000000\r\nxxxxx000000000xx000000\r\n0000000000000000000000\r\n0000000000000000000000\r\n0000000000000000000000\r\n0000000000000000000000\r\n0000000000000000000000\r\nxxxxx000000000000000xx\r\nxxxxx000000000000000xx\r\nx0000000000000000000xx\r\nx0000000000000000xxxxx\r\nxxxxxx00000000000xxxxx\r\nxxxxxxx0000000000xxxxx\r\nxxxxxxxx000000000xxxxx\r\nxxxxxxxx000000000xxxxx\r\nxxxxxxxx000000000xxxxx\r\nxxxxxxxx000000000xxxxx\r\nxxxxxxxx000000000xxxxx\r\nxxxxxxxx000000000xxxxx\r\nxxxxxx00000000000xxxxx\r\nxxxxxx00000000000xxxxx\r\nxxxxxx00000000000xxxxx\r\nxxxxxx00000000000xxxxx\r\nxxxxxx00000000000xxxxx\r\nxxxxx000000000000xxxxx\r\nxxxxx000000000000xxxxx'
FROM `rooms` r
WHERE r.`model` = 'newbie_lobby' AND r.`owner_id` = 1
  AND NOT EXISTS (SELECT 1 FROM `room_models_custom` c WHERE c.`id` = r.`id`);

INSERT INTO `navigator_publics` (`public_cat_id`, `room_id`, `visible`)
SELECT @official_root_public_cat_id, r.`id`, '1'
FROM `rooms` r
WHERE r.`model` = 'newbie_lobby' AND r.`owner_id` = 1
  AND NOT EXISTS (SELECT 1 FROM `navigator_publics` np WHERE np.`room_id` = r.`id`);

-- =====================================================
-- Public-room bot data
-- =====================================================
CREATE TABLE IF NOT EXISTS `bot_public_room_data` (
    `bot_id` INT NOT NULL,
    `walkspace` TEXT NOT NULL,
    `ambient_lines` TEXT NOT NULL,
    `response_lines` TEXT NOT NULL,
    `unrecognised_lines` TEXT NOT NULL,
    PRIMARY KEY (`bot_id`),
    CONSTRAINT `fk_bot_public_room_data_bot`
        FOREIGN KEY (`bot_id`) REFERENCES `bots` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `bot_public_room_drinks` (
    `bot_id` INT NOT NULL,
    `drink_name` VARCHAR(64) NOT NULL,
    `handitem_id` INT NOT NULL,
    PRIMARY KEY (`bot_id`, `drink_name`),
    CONSTRAINT `fk_bot_public_room_drinks_bot`
        FOREIGN KEY (`bot_id`) REFERENCES `bots` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELETE d
FROM `bot_public_room_drinks` d
INNER JOIN `bots` b ON b.`id` = d.`bot_id`
INNER JOIN `rooms` r ON r.`id` = b.`room_id`
WHERE b.`type` = 'public_room'
  AND r.`model` IN ('newbie_lobby', 'netcafe');

DELETE d
FROM `bot_public_room_data` d
INNER JOIN `bots` b ON b.`id` = d.`bot_id`
INNER JOIN `rooms` r ON r.`id` = b.`room_id`
WHERE b.`type` = 'public_room'
  AND r.`model` IN ('newbie_lobby', 'netcafe');

DELETE b
FROM `bots` b
INNER JOIN `rooms` r ON r.`id` = b.`room_id`
WHERE b.`type` = 'public_room'
  AND r.`model` IN ('newbie_lobby', 'netcafe');

INSERT INTO `bots` (`user_id`, `room_id`, `name`, `motto`, `figure`, `gender`, `x`, `y`, `z`, `rot`, `chat_lines`, `chat_auto`, `chat_random`, `chat_delay`, `dance`, `freeroam`, `type`, `effect`, `bubble_id`)
SELECT 0, r.`id`, 'Harry', 'Happy to help', 'hr-831-61.ch-809-62.sh-290-110.hd-180-1.lg-285-64', 'M', 8, 21, 0.0, 2, '', '0', '0', 30, 0, '1', 'public_room', 0, 31
FROM `rooms` r
WHERE r.`model` = 'newbie_lobby';

INSERT INTO `bots` (`user_id`, `room_id`, `name`, `motto`, `figure`, `gender`, `x`, `y`, `z`, `rot`, `chat_lines`, `chat_auto`, `chat_random`, `chat_delay`, `dance`, `freeroam`, `type`, `effect`, `bubble_id`)
SELECT 0, r.`id`, 'Ray', 'Chill out and have a coconut!', 'hr-829-34.lg-281-72.ch-803-62.hd-180-19.sh-295-68', 'M', 1, 11, 0.0, 2, '', '0', '0', 30, 0, '1', 'public_room', 0, 31
FROM `rooms` r
WHERE r.`model` = 'netcafe';

INSERT INTO `bot_public_room_data` (`bot_id`, `walkspace`, `ambient_lines`, `response_lines`, `unrecognised_lines`)
SELECT b.`id`,
       '9,18 9,16 9,17 9,19 9,20 9,21 9,22 9,23 8,18 8,16 8,17 8,19 8,20 8,21 8,22 8,23',
       'Please keep it down people are trying to think!#SHOUT|Only use the Call for help in an emergency!|Want to know more about Habbo Hotel? Ask a Habbo Guide!|Is it me or is something BIG about to happen?|In Trouble? Call for Moderator assistance using the Blue Question Mark!|There''s no such thing as a free lunch or free credits!',
       'Why Hello there! *Shakes Habbo Hand* My name''s Harry.|Hello, Hello, Hello!|Hello and welcome to Habbo Hotel! Enjoy your stay! :)',
       'Why Hello there! *Shakes Habbo Hand* My name''s Harry.|Hello, Hello, Hello!|Hello and welcome to Habbo Hotel! Enjoy your stay! :)'
FROM `bots` b
INNER JOIN `rooms` r ON r.`id` = b.`room_id`
WHERE b.`type` = 'public_room'
  AND b.`name` = 'Harry'
  AND r.`model` = 'newbie_lobby';

INSERT INTO `bot_public_room_data` (`bot_id`, `walkspace`, `ambient_lines`, `response_lines`, `unrecognised_lines`)
SELECT b.`id`,
       '1,9 1,10 1,11 1,12 2,9 2,10 2,11 2,12',
       'Official Fansite are voted by YOU, the Habbo community!|Did you know the Official Fansites are changed every 3 months?|If they aren''t listed once you click the billboard then they aren''t Official!|Once refreshed, visit an Official Fansite!|Click the billboard now to visit our Official Fansites!|Official Fansites have great events, comps and radio shows!',
       'Refreshing!|Here you are, with extra coconut milk, only for you ;)|Here you go, hope you like the umbrella.|You sure are thirsty, huh?|You can only have one at a time!|That''s my name! As in the beams of golden sunshine and not the sunglasses.|Hi my name is what? my name is who? my name is...ray',
       ''
FROM `bots` b
INNER JOIN `rooms` r ON r.`id` = b.`room_id`
WHERE b.`type` = 'public_room'
  AND b.`name` = 'Ray'
  AND r.`model` = 'netcafe';

INSERT INTO `bot_public_room_drinks` (`bot_id`, `drink_name`, `handitem_id`)
SELECT b.`id`, 'Water', 7
FROM `bots` b
INNER JOIN `rooms` r ON r.`id` = b.`room_id`
WHERE b.`type` = 'public_room'
  AND b.`name` = 'Harry'
  AND r.`model` = 'newbie_lobby';

INSERT INTO `bot_public_room_drinks` (`bot_id`, `drink_name`, `handitem_id`)
SELECT b.`id`, 'Cola', 55
FROM `bots` b
INNER JOIN `rooms` r ON r.`id` = b.`room_id`
WHERE b.`type` = 'public_room'
  AND b.`name` = 'Ray'
  AND r.`model` = 'netcafe';

INSERT INTO `bot_public_room_drinks` (`bot_id`, `drink_name`, `handitem_id`)
SELECT b.`id`, 'Coke', 55
FROM `bots` b
INNER JOIN `rooms` r ON r.`id` = b.`room_id`
WHERE b.`type` = 'public_room'
  AND b.`name` = 'Ray'
  AND r.`model` = 'netcafe';

INSERT INTO `bot_public_room_drinks` (`bot_id`, `drink_name`, `handitem_id`)
SELECT b.`id`, 'Coconut Milk', 5
FROM `bots` b
INNER JOIN `rooms` r ON r.`id` = b.`room_id`
WHERE b.`type` = 'public_room'
  AND b.`name` = 'Ray'
  AND r.`model` = 'netcafe';

SET FOREIGN_KEY_CHECKS = 1;
