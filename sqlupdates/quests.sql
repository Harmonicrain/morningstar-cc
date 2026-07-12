INSERT INTO `emulator_settings` (`key`, `value`) VALUES ('hotel.quest.room_pulse.interval', '5');

INSERT INTO emulator_texts (`key`, `value`) VALUES
('commands.keys.cmd_update_quests', 'update_quests;reload_quests'),
('commands.succes.cmd_update_quests.updated', 'Quests reloaded successfully.');

ALTER TABLE `permissions` ADD `cmd_update_quests` ENUM('0', '1') NOT NULL DEFAULT '0' AFTER `cmd_update_calendar`;


CREATE TABLE `quests` (
  `id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `activity_point_type` int(11) NOT NULL DEFAULT 0,
  `trigger_type` varchar(64) NOT NULL DEFAULT '',
  `target_type` varchar(64) NOT NULL DEFAULT '',
  `target_value` varchar(255) NOT NULL DEFAULT '',
  `image_version` varchar(64) NOT NULL DEFAULT '',
  `reward_currency_amount` int(11) NOT NULL DEFAULT 0,
  `localization_code` varchar(64) NOT NULL DEFAULT '',
  `total_steps` int(11) NOT NULL DEFAULT 1,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  `catalog_page_name` varchar(64) NOT NULL DEFAULT '',
  `chain_code` varchar(64) NOT NULL DEFAULT '',
  `easy` tinyint(1) NOT NULL DEFAULT 1,
  `enabled` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `quests` (`id`, `campaign_id`, `activity_point_type`, `trigger_type`, `target_type`, `target_value`, `image_version`, `reward_currency_amount`, `localization_code`, `total_steps`, `sort_order`, `catalog_page_name`, `chain_code`, `easy`, `enabled`) VALUES
(1, 1, 0, 'ENTER_OTHER_USERS_ROOM', '', '', '', 10, '1761299476975', 2, 0, '', '', 1, 1),
(2, 1, 0, 'FIND_HAND_ITEM', 'hand_item_id', '1', '', 10, '1761579872551', 1, 1, '', '', 1, 1),
(3, 1, 0, 'ROOM_PULSE', 'base_item_id', '6', '', 10, '1761580701585', 6, 2, '', '', 1, 1),
(4, 1, 0, 'FIND_HAND_ITEM', 'hand_item_id', '2', '', 10, '1761581055462', 1, 3, '', '', 1, 1),
(5, 1, 0, 'ROOM_PULSE', 'base_item_id', '6', '', 10, '1761646163810', 6, 4, '', '', 1, 1),
(6, 1, 0, 'SWITCH_ITEM_STATE', 'base_item_id', '7', '', 10, '1761647062487', 1, 5, '', '', 1, 1),
(7, 1, 0, 'ROOM_PULSE', '', '', '', 10, '1761647456344', 1, 6, '', '', 1, 1),
(8, 1, 0, 'CHANGE_MOTTO', '', '', '', 10, '1761648186978', 1, 7, '', '', 1, 1),
(9, 1, 0, 'CHAT_WITH_SOMEONE', '', '', '', 10, '1761648572954', 1, 8, '', '', 1, 1),
(10, 1, 0, 'CHANGE_FIGURE', '', '', '', 10, '1761648741810', 1, 9, '', '', 1, 1),
(11, 1, 0, 'SWITCH_ITEM_STATE', 'base_item_id', '7', '', 10, '1761648878323', 3, 10, '', '', 1, 1),
(12, 1, 1, 'GIVE_RESPECT', '', '', '', 10, '1761650198783', 1, 11, '', '', 1, 1),
(13, 1, 2, 'WEAR_BADGE', 'badge_code', 'XM8', '', 10, '1761654735890', 1, 12, '', '', 1, 1),
(14, 1, 0, 'WAVE', '', '', '', 10, '1761655367136', 2, 13, '', '', 1, 1),
(15, 1, 0, 'DANCE', '', '', '', 10, '1761658240298', 1, 14, '', '', 1, 1),
(16, 1, 0, 'FIND_STUFF', 'base_item_id', '6', '', 10, '1761658512806', 1, 15, '', '', 1, 1),
(17, 1, 0, 'SWITCH_ITEM_STATE', 'base_item_id', '7', '', 10, '1761659793882', 1, 16, '', '', 1, 1),
(18, 1, 0, 'PLACE_ITEM', '', '', '', 10, '1761661887742', 1, 17, '', '', 1, 1),
(19, 1, 0, 'PLACE_ITEM', 'base_item_id', '7', '', 10, '1761662926811', 1, 18, '', '', 1, 1),
(20, 1, 0, 'MOVE_ITEM', 'base_item_id', '6', '', 10, '1761662961113', 1, 19, '', '', 1, 1),
(21, 1, 0, 'ROTATE_ITEM', 'base_item_id', '7', '', 10, '1761663889314', 1, 20, '', '', 1, 1),
(22, 1, 0, 'BUY_FROM_CATALOGUE', 'catalog_item_id', '7', '', 10, '1761664164296', 1, 21, '', '', 1, 1),
(23, 1, 0, 'WALK_OVER_STUFF', 'base_item_id', '10', '', 10, '1761665188454', 1, 22, '', '', 1, 1),
(24, 1, 0, 'CRAFT_PRODUCT', 'craft_reward', '2675', '', 10, '1761666539690', 1, 23, '', '', 1, 1),
(25, 1, 0, 'REQUEST_FRIEND', '', '', '', 10, '1761666799701', 1, 24, '', '', 1, 1),
(26, 1, 0, 'SEND_MESSENGER_MESSAGE', '', '', '', 10, '1761728780398', 1, 25, '', '', 1, 1),
(27, 1, 0, 'SEND_MESSENGER_INVITE', '', '', '', 10, '1761731096129', 1, 26, '', '', 1, 1),
(28, 1, 0, 'FOLLOW_FRIEND', '', '', '', 10, '1761731141046', 1, 27, '', '', 1, 1),
(29, 1, 0, 'SET_RELATIONSHIP_STATUS', 'relation_ship', '2', '', 10, '1761731259486', 1, 28, '', '', 1, 1),
(30, 1, 0, 'TELEPORT', '', '', '', 10, '1761731326196', 1, 29, '', '', 1, 1),
(31, 1, 0, 'PET_RESPECT', '', '', '', 10, '1761731487186', 2, 30, '', '', 1, 1),
(32, 1, 0, 'PET_LEVEL', '', '', '', 10, '1761731524375', 1, 31, '', '', 1, 1),
(33, 1, 0, 'PET_EAT', '', '', '', 10, '1761731597491', 1, 32, '', '', 1, 1),
(34, 1, 0, 'KICK_BALL', 'base_item_id', '4192', '', 10, '1761732734334', 1, 33, '', '', 1, 1),
(35, 1, 0, 'KICK_BALL', 'base_item_id', '1494', '', 10, '1761735280310', 1, 34, '', '', 1, 1),
(36, 1, 0, 'SWIM', '', '', '', 10, '1761735305842', 1, 35, '', '', 1, 1),
(37, 1, 0, 'CREATE_ROOM', '', '', '', 10, '1761825579021', 1, 36, '', '', 1, 1),
(38, 1, 0, 'FRIEND_FURNI_LOCKED', '', '', '', 10, '1761905903471', 1, 37, '', '', 1, 1),
(39, 1, 0, 'GAME_BB_LOCK_TILES', 'tile_count_min', '10', '', 10, '1762184918549', 1, 38, '', '', 1, 1),
(40, 1, 0, 'GAME_BB_LOCK_TILE', '', '', '', 10, '1762263648686', 30, 39, '', '', 1, 1),
(41, 1, 0, 'GAME_BB_LOCK_TILE', '', '', '', 10, '1762263673586', 30, 40, '', '', 1, 1),
(42, 1, 0, 'GAME_WIN_GAME', 'game_type', 'battle_banzai', '', 10, '1762264146566', 1, 41, '', '', 1, 1),
(43, 1, 0, 'GAME_PLAY_GAME', 'game_type', 'battle_banzai', '', 10, '1762355997065', 1, 42, '', '', 1, 1),
(44, 1, 0, 'CHAT_WITH_SOMEONE', '', '', '', 5, '', 1, 43, '', '', 1, 1);

CREATE TABLE `quests_campaigns` (
  `id` int(11) NOT NULL,
  `code` varchar(64) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `quests_campaigns` (`id`, `code`, `enabled`) VALUES
(1, 'xmas25_weekquests', 1);


CREATE TABLE `quests_conditions` (
  `id` int(11) NOT NULL,
  `quest_id` int(11) NOT NULL,
  `condition_type` varchar(64) NOT NULL,
  `condition_key` varchar(64) DEFAULT NULL,
  `condition_value` varchar(255) DEFAULT NULL,
  `condition_extra` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `quests_conditions` (`id`, `quest_id`, `condition_type`, `condition_key`, `condition_value`, `condition_extra`) VALUES
(1, 3, 'FIGURE_PART', 'ha', '1006', NULL),
(2, 10, 'FIGURE_PART', 'ha', '1006', NULL),
(3, 44, 'ROOM_ITEM_STATE', 'base_item_id', '21', '1');

CREATE TABLE `users_quests` (
  `user_id` int(11) NOT NULL,
  `quest_id` int(11) NOT NULL,
  `accepted` tinyint(1) NOT NULL DEFAULT 0,
  `completed_steps` int(11) NOT NULL DEFAULT 0,
  `completed_at` int(11) NOT NULL DEFAULT 0,
  `claimed_at` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
