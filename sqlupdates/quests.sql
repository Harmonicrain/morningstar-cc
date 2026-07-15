INSERT INTO `emulator_settings` (`key`, `value`) VALUES ('hotel.quest.room_pulse.interval', '5')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

INSERT INTO emulator_texts (`key`, `value`) VALUES
('commands.keys.cmd_update_quests', 'update_quests;reload_quests'),
('commands.succes.cmd_update_quests.updated', 'Quests reloaded successfully.')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

ALTER TABLE `permissions` ADD `cmd_update_quests` ENUM('0', '1') NOT NULL DEFAULT '0' AFTER `cmd_update_calendar`;

INSERT INTO `emulator_settings` (`key`, `value`) VALUES
('quests.enabled', 'true'),
('quests.daily.enabled', 'true'),
('quests.seasonal.enabled', 'true'),
('quests.progress.offline_queue', 'true'),
('quests.seasonal.start', ''),
('quests.seasonal.offer_id', '0'),
('quests.seasonal.campaign_prefix', 'xmas2013_'),
('hotelview.community.goal.code', 'coins_spent'),
('hotelview.community.goal.enabled', 'true')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

ALTER TABLE `permissions` ADD `cmd_complete_quest` TINYINT(1) NOT NULL DEFAULT '0' AFTER `cmd_update_quests`, ADD `cmd_give_quest` TINYINT(1) NOT NULL DEFAULT '0' AFTER `cmd_complete_quest`, ADD `cmd_reset_quest` TINYINT(1) NOT NULL DEFAULT '0' AFTER `cmd_give_quest`;
INSERT INTO `emulator_texts` (`key`, `value`) VALUES ('commands.keys.cmd_complete_quest', 'complete_quest;completequest'), ('commands.keys.cmd_give_quest', 'give_quest;givequest'), ('commands.keys.cmd_reset_quest', 'reset_quest;resetquest')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
ALTER TABLE `permissions` ADD `cmd_process_community_goals` TINYINT NOT NULL DEFAULT '0' AFTER `cmd_reset_quest`;

INSERT INTO emulator_texts (`key`, `value`) VALUES
('commands.error.cmd_quest.user_not_found', 'User not found: %user%'),
('commands.error.cmd_quest.invalid_quest_id', 'Invalid quest id: %quest_id%'),
('commands.error.cmd_quest.quest_not_found', 'Quest not found: %quest_id%'),
('commands.succes.cmd_give_quest.given', 'Quest %quest_id% given to %user%.'),
('commands.error.cmd_give_quest.failed', 'Quest %quest_id% could not be given to %user%.'),
('commands.succes.cmd_give_quest.queued', 'Quest %quest_id% queued for %user%.'),
('commands.succes.cmd_complete_quest.completed', 'Quest %quest_id% completed for %user%.'),
('commands.succes.cmd_reset_quest.reset', 'Quest progress reset for %user%.'),
('commands.error.cmd_reset_quest.usage', 'Usage: :reset_quest <user> <questId>'),
('commands.error.cmd_complete_quest.usage', 'Usage: :complete_quest <user> <questId>'),
('commands.error.cmd_give_quest.usage', 'Usage: :give_quest <user> <questId>'),
('commands.keys.cmd_process_community_goals', 'processcommunitygoals;processcg'),
('commands.succes.cmd_process_community_goals.processed', 'Expired community goal rewards have been processed.')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);


--
-- Table structure for table `community_goals`
--

CREATE TABLE `community_goals` (
  `code` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL DEFAULT '',
  `total_score` int(11) NOT NULL DEFAULT 0,
  `start_timestamp` int(11) NOT NULL DEFAULT 0,
  `duration_seconds` int(11) NOT NULL DEFAULT 604800,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `result_option` tinyint(4) NOT NULL DEFAULT 0,
  `goal_type` enum('contribution','versus_all','versus_winner') NOT NULL DEFAULT 'contribution',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `community_goals`
--

INSERT INTO `community_goals` (`code`, `name`, `total_score`, `start_timestamp`, `duration_seconds`, `enabled`, `result_option`, `goal_type`) VALUES
('coins_spent', 'Moedas gastas no hotel', 0, 1783243440, 604800, 0, 0, 'contribution'),
('ecotron', 'Ecotron', 0, 1783946520, 4080, 1, 0, 'contribution'),
('eng_nor', 'Inglaterra vs Noruega', 0, 1783911540, 2580, 0, 1, 'versus_winner'),
('xmas2015', 'Papai Noel ou Mamãe Noel?', 0, 1783912860, 604800, 0, 0, 'contribution');

-- --------------------------------------------------------

--
-- Table structure for table `community_goal_levels`
--

CREATE TABLE `community_goal_levels` (
  `goal_code` varchar(64) NOT NULL,
  `level` int(11) NOT NULL,
  `score_threshold` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`goal_code`,`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `community_goal_levels`
--

INSERT INTO `community_goal_levels` (`goal_code`, `level`, `score_threshold`) VALUES
('coins_spent', 0, 0),
('coins_spent', 1, 1000),
('coins_spent', 2, 5000),
('coins_spent', 3, 10000),
('ecotron', 0, 0),
('ecotron', 1, 10),
('xmas2015', -3, -300),
('xmas2015', -2, -200),
('xmas2015', -1, -100),
('xmas2015', 0, 0),
('xmas2015', 1, 100),
('xmas2015', 2, 200),
('xmas2015', 3, 300);

-- --------------------------------------------------------

--
-- Table structure for table `community_goal_rewards`
--

CREATE TABLE `community_goal_rewards` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `goal_code` varchar(64) NOT NULL,
  `required_level` int(11) NOT NULL DEFAULT 0,
  `prize_type` enum('badge','item','credits','points') NOT NULL,
  `reward_value` varchar(255) NOT NULL,
  `localized_name` varchar(255) NOT NULL DEFAULT '',
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `required_vote_option` tinyint(4) NOT NULL DEFAULT 0,
  `required_result_option` tinyint(4) NOT NULL DEFAULT 0,
  `minimum_contribution` int(11) NOT NULL DEFAULT 1,
  `reward_timing` enum('on_vote','on_finish') NOT NULL DEFAULT 'on_finish',
  PRIMARY KEY (`id`),
  KEY `idx_goal_enabled_level` (`goal_code`,`enabled`,`required_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `community_goal_rewards`
--

INSERT INTO `community_goal_rewards` (`id`, `goal_code`, `required_level`, `prize_type`, `reward_value`, `localized_name`, `enabled`, `required_vote_option`, `required_result_option`, `minimum_contribution`, `reward_timing`) VALUES
(1, 'eng_nor', 0, 'badge', '26W13', '26W13', 1, 0, 0, 1, 'on_vote'),
(2, 'eng_nor', 0, 'badge', 'W26W1', 'W26W1', 1, 1, 1, 1, 'on_finish'),
(3, 'eng_nor', 0, 'badge', 'W26W2', 'W26W2', 1, 2, 2, 1, 'on_finish'),
(4, 'ecotron', 1, 'badge', 'ECO_WIN', 'ECO_WIN', 1, 0, 0, 1, 'on_finish'),
(5, 'ecotron', 0, 'badge', 'ECO_JOIN', 'ECO_JOIN', 1, 0, 0, 1, 'on_finish');

-- --------------------------------------------------------

--
-- Table structure for table `community_goal_triggers`
--

CREATE TABLE `community_goal_triggers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `goal_code` varchar(64) NOT NULL,
  `trigger_type` enum('catalogue_purchase','credits_spent','points_spent','duckets_spent','ecotron_recycle','concurrent_users') NOT NULL,
  `contribution_mode` enum('event','count','amount') NOT NULL DEFAULT 'amount',
  `catalog_page_id` int(11) NOT NULL DEFAULT 0,
  `catalog_item_id` int(11) NOT NULL DEFAULT 0,
  `base_item_id` int(11) NOT NULL DEFAULT 0,
  `currency_type` int(11) NOT NULL DEFAULT -1,
  `min_amount` int(11) NOT NULL DEFAULT 0,
  `max_amount` int(11) NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `community_goal_triggers_goal_code` (`goal_code`),
  KEY `community_goal_triggers_type` (`trigger_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `community_goal_triggers`
--

INSERT INTO `community_goal_triggers` (`id`, `goal_code`, `trigger_type`, `contribution_mode`, `catalog_page_id`, `catalog_item_id`, `base_item_id`, `currency_type`, `min_amount`, `max_amount`, `enabled`) VALUES
(1, 'ecotron', 'ecotron_recycle', 'event', 0, 0, 0, -1, 0, 0, 1);

-- --------------------------------------------------------

--
-- Table structure for table `quests`
--

CREATE TABLE `quests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `campaign_code` varchar(64) NOT NULL DEFAULT '',
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
  `daily` tinyint(1) NOT NULL DEFAULT 0,
  `seasonal` tinyint(1) NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_chain_code` (`chain_code`),
  KEY `idx_enabled_sort` (`enabled`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `quests`
--

INSERT INTO `quests` (`id`, `campaign_code`, `activity_point_type`, `trigger_type`, `target_type`, `target_value`, `image_version`, `reward_currency_amount`, `localization_code`, `total_steps`, `sort_order`, `catalog_page_name`, `chain_code`, `easy`, `daily`, `seasonal`, `enabled`) VALUES
(1, 'xmas25_weekquests', 0, 'ENTER_OTHER_USERS_ROOM', '', '', '', 10, '1761299476975', 2, 0, '', '1760956224281', 1, 0, 1, 1),
(2, 'xmas25_weekquests', 0, 'FIND_HAND_ITEM', 'hand_item_id', '1', '', 10, '1761579872551', 1, 1, '', '1760956224281', 1, 0, 1, 1),
(3, 'xmas25_weekquests', 0, 'ROOM_PULSE', 'base_item_id', '6', '', 10, '1761580701585', 6, 2, '', '1760956224281', 1, 0, 1, 1),
(4, 'xmas25_weekquests', 0, 'FIND_HAND_ITEM', 'hand_item_id', '2', '', 10, '1761581055462', 1, 3, '', '1760956224281', 1, 0, 1, 1),
(5, 'xmas25_weekquests', 0, 'ROOM_PULSE', 'base_item_id', '6', '', 10, '1761646163810', 6, 4, '', '1760956224281', 1, 0, 1, 1),
(6, 'xmas25_weekquests', 0, 'SWITCH_ITEM_STATE', 'base_item_id', '7', '', 10, '1761647062487', 1, 5, '', '1760956224281', 1, 0, 1, 1),
(7, 'xmas25_weekquests', 0, 'ROOM_PULSE', '', '', '', 10, '1761647456344', 1, 6, '', '1760956224281', 1, 0, 1, 1),
(8, 'xmas25_weekquests', 0, 'CHANGE_MOTTO', '', '', '', 10, '1761648186978', 1, 7, '', '1760956224281', 1, 0, 1, 1),
(9, 'xmas25_weekquests', 0, 'CHAT_WITH_SOMEONE', '', '', '', 10, '1761648572954', 1, 8, '', '1760956224281', 1, 0, 1, 1),
(10, 'xmas25_weekquests', 0, 'CHANGE_FIGURE', '', '', '', 10, '1761648741810', 1, 9, '', '1760956224281', 1, 0, 1, 1),
(11, 'xmas25_weekquests', 0, 'SWITCH_ITEM_STATE', 'base_item_id', '7', '', 10, '1761648878323', 3, 10, '', '1760956224281', 1, 0, 1, 1),
(12, 'xmas25_weekquests', 1, 'GIVE_RESPECT', '', '', '', 10, '1761650198783', 1, 11, '', '1760956224281', 1, 0, 1, 1),
(13, 'xmas25_weekquests', 2, 'WEAR_BADGE', 'badge_code', 'XM8', '', 10, '1761654735890', 1, 12, '', '1760956224281', 1, 0, 1, 1),
(14, 'xmas25_weekquests', 0, 'WAVE', '', '', '', 10, '1761655367136', 2, 13, '', '1760956224281', 1, 0, 1, 1),
(15, 'xmas25_weekquests', 0, 'DANCE', '', '', '', 10, '1761658240298', 1, 14, '', '1760956224281', 1, 0, 1, 1),
(16, 'xmas25_weekquests', 0, 'FIND_STUFF', 'base_item_id', '6', '', 10, '1761658512806', 1, 15, '', '1760956224281', 1, 0, 1, 1),
(17, 'xmas25_weekquests', 0, 'SWITCH_ITEM_STATE', 'base_item_id', '7', '', 10, '1761659793882', 1, 16, '', '1760956224281', 1, 0, 1, 1),
(18, 'xmas25_weekquests', 0, 'PLACE_ITEM', '', '', '', 10, '1761661887742', 1, 17, '', '1760956224281', 1, 0, 1, 1),
(19, 'xmas25_weekquests', 0, 'PLACE_ITEM', 'base_item_id', '7', '', 10, '1761662926811', 1, 18, '', '1760956224281', 1, 0, 1, 1),
(20, 'xmas25_weekquests', 0, 'MOVE_ITEM', 'base_item_id', '6', '', 10, '1761662961113', 1, 19, '', '1760956224281', 1, 0, 1, 1),
(21, 'xmas25_weekquests', 0, 'ROTATE_ITEM', 'base_item_id', '7', '', 10, '1761663889314', 1, 20, '', '1760956224281', 1, 0, 1, 1),
(22, 'xmas25_weekquests', 0, 'BUY_FROM_CATALOGUE', 'catalog_item_id', '7', '', 10, '1761664164296', 1, 21, '', '1760956224281', 1, 0, 1, 1),
(23, 'xmas25_weekquests', 0, 'WALK_OVER_STUFF', 'base_item_id', '10', '', 10, '1761665188454', 1, 22, '', '1760956224281', 1, 0, 1, 1),
(24, 'xmas25_weekquests', 0, 'CRAFT_PRODUCT', 'craft_reward', '2675', '', 10, '1761666539690', 1, 23, '', '1760956224281', 1, 0, 1, 1),
(25, 'xmas25_weekquests', 0, 'REQUEST_FRIEND', '', '', '', 10, '1761666799701', 1, 24, '', '1760956224281', 1, 0, 1, 1),
(26, 'xmas25_weekquests', 0, 'SEND_MESSENGER_MESSAGE', '', '', '', 10, '1761728780398', 1, 25, '', '1760956224281', 1, 0, 1, 1),
(27, 'xmas25_weekquests', 0, 'SEND_MESSENGER_INVITE', '', '', '', 10, '1761731096129', 1, 26, '', '1760956224281', 1, 0, 1, 1),
(28, 'xmas25_weekquests', 0, 'FOLLOW_FRIEND', '', '', '', 10, '1761731141046', 1, 27, '', '1760956224281', 1, 0, 1, 1),
(29, 'xmas25_weekquests', 0, 'SET_RELATIONSHIP_STATUS', 'relation_ship', '2', '', 10, '1761731259486', 1, 28, '', '1760956224281', 1, 0, 1, 1),
(30, 'xmas25_weekquests', 0, 'TELEPORT', '', '', '', 10, '1761731326196', 1, 29, '', '1760956224281', 1, 0, 1, 1),
(31, 'xmas25_weekquests', 0, 'PET_RESPECT', '', '', '', 10, '1761731487186', 2, 30, '', '1760956224281', 1, 0, 1, 1),
(32, 'xmas25_weekquests', 0, 'PET_LEVEL', '', '', '', 10, '1761731524375', 1, 31, '', '1760956224281', 1, 0, 1, 1),
(33, 'xmas25_weekquests', 0, 'PET_EAT', '', '', '', 10, '1761731597491', 1, 32, '', '1760956224281', 1, 0, 1, 1),
(34, 'xmas25_weekquests', 0, 'KICK_BALL', 'base_item_id', '4192', '', 10, '1761732734334', 1, 33, '', '1760956224281', 1, 0, 1, 1),
(35, 'xmas25_weekquests', 0, 'KICK_BALL', 'base_item_id', '1494', '', 10, '1761735280310', 1, 34, '', '1760956224281', 1, 0, 1, 1),
(36, 'xmas25_weekquests', 0, 'SWIM', 'base_item_id', '1750', '', 10, '1761735305842', 10, 35, '', '1760956224281', 1, 0, 1, 1),
(37, 'xmas25_weekquests', 0, 'CREATE_ROOM', '', '', '', 10, '1761825579021', 1, 36, '', '1760956224281', 1, 0, 1, 1),
(38, 'xmas25_weekquests', 0, 'FRIEND_FURNI_LOCKED', '', '', '', 10, '1761905903471', 1, 37, '', '1760956224281', 1, 0, 1, 1),
(39, 'xmas25_weekquests', 0, 'GAME_BB_LOCK_TILES', 'tile_count_min', '10', '', 10, '1762184918549', 1, 38, '', '1760956224281', 1, 0, 1, 1),
(40, 'xmas25_weekquests', 0, 'GAME_BB_LOCK_TILE', '', '', '', 10, '1762263648686', 30, 39, '', '1760956224281', 1, 0, 1, 1),
(41, 'xmas25_weekquests', 0, 'GAME_BB_LOCK_TILE', '', '', '', 10, '1762263673586', 30, 40, '', '1760956224281', 1, 0, 1, 1),
(42, 'xmas25_weekquests', 0, 'GAME_WIN_GAME', 'game_type', 'battle_banzai', '', 10, '1762264146566', 1, 41, '', '1760956224281', 1, 0, 1, 1),
(43, 'xmas25_weekquests', 0, 'GAME_PLAY_GAME', 'game_type', 'battle_banzai', '', 10, '1762355997065', 1, 42, '', '1760956224281', 1, 0, 1, 1),
(44, 'xmas25_weekquests', 0, 'CHAT_WITH_SOMEONE', '', '', '', 5, '', 1, 43, '', '1760956224281', 1, 0, 1, 1),
(45, 'daily', 0, 'WEAR_BADGE', '', '', '', 10, 'BADGE1', 1, 0, '', 'daily', 1, 1, 0, 1),
(46, 'daily', 0, 'GAME_BB_LOCK_TILE', '', '', '', 10, 'BBLOCKTILE1', 10, 1, '', 'daily', 1, 1, 0, 1),
(47, 'daily', 0, 'GAME_BB_LOCK_TILE', '', '', '', 15, 'BBLOCKTILE2', 50, 2, '', 'daily', 0, 1, 0, 1),
(48, 'daily', 0, 'GAME_BB_LOCK_TILES', 'tile_count_min', '50', '', 15, 'BBLOCKTILE3', 1, 3, '', 'daily', 0, 1, 0, 1),
(49, 'daily', 0, 'CHANGE_FIGURE', '', '', '', 10, 'CHANGELOOK1', 3, 4, '', 'daily', 1, 1, 0, 1),
(50, 'daily', 0, 'CHANGE_FIGURE', '', '', '', 10, 'CHANGELOOK2', 3, 5, '', 'daily', 1, 1, 0, 1),
(51, 'daily', 0, 'DANCE', '', '', '', 10, 'DANCE', 1, 6, '', 'daily', 1, 1, 0, 1),
(52, 'daily', 0, 'ENTER_OTHER_USERS_ROOM', '', '', '', 10, 'EXPLORE', 10, 7, '', 'daily', 1, 1, 0, 1),
(53, 'daily', 0, 'REQUEST_FRIEND', '', '', '', 10, 'FRIEND1', 3, 8, '', 'daily', 1, 1, 0, 1),
(54, 'daily', 0, 'REQUEST_FRIEND', '', '', '', 10, 'FRIEND2', 3, 9, '', 'daily', 1, 1, 0, 1),
(55, 'daily', 0, 'GIVE_RESPECT', '', '', '', 10, 'RESPECT1', 1, 10, '', 'daily', 1, 1, 0, 1),
(56, 'daily', 0, 'GIVE_RESPECT', '', '', '', 10, 'RESPECT2', 1, 11, '', 'daily', 1, 1, 0, 1),
(57, 'daily', 0, 'CHAT_WITH_SOMEONE', '', '', '', 10, 'SAYPICKUPLINES', 5, 12, '', 'daily', 1, 1, 0, 1),
(58, 'daily', 0, 'PET_RESPECT', '', '', '', 10, 'SCRATCH1', 1, 13, '', 'daily', 1, 1, 0, 1),
(59, 'daily', 0, 'SWIM', '', '', '', 10, 'SWIM1', 1, 14, '', 'daily', 1, 1, 0, 1),
(60, 'daily', 0, 'SWIM', '', '', '', 10, 'SWIM2', 10, 15, '', 'daily', 1, 1, 0, 1),
(61, 'daily', 0, 'SWIM', '', '', '', 15, 'SWIM3', 30, 16, '', 'daily', 0, 1, 0, 1),
(62, 'daily', 0, 'SWIM', '', '', '', 20, 'SWIM4', 50, 17, '', 'daily', 0, 1, 0, 1),
(63, 'daily', 0, 'GAME_WIN_GAME', 'game_type', 'battle_banzai', '', 20, 'WINBB', 1, 18, '', 'daily', 0, 1, 0, 1),
(64, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1769;1770;1771;1772', '', 10, 'FINDBBQ', 1, 19, '', 'daily', 1, 1, 0, 1),
(65, 'daily', 0, 'KICK_BALL', 'base_item_id', '1756', '', 10, 'FINDBEACHBALL', 1, 20, '', 'daily', 1, 1, 0, 1),
(66, 'daily', 0, 'FIND_STUFF', 'pet_type', '4;24', '', 10, 'FINDBEAR', 1, 21, '', 'daily', 1, 1, 0, 1),
(67, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1724;1733;1745', '', 10, 'FINDBOAT', 1, 22, '', 'daily', 1, 1, 0, 1),
(68, 'daily', 0, 'FIND_HAND_ITEM', 'hand_item_id', '3', '', 10, 'FINDCARROT', 1, 23, '', 'daily', 1, 1, 0, 1),
(69, 'daily', 0, 'FIND_STUFF', 'pet_type', '1;28', '', 10, 'FINDCAT', 1, 24, '', 'daily', 1, 1, 0, 1),
(70, 'daily', 0, 'FIND_STUFF', 'pet_type', '10', '', 10, 'FINDCHICK', 1, 25, '', 'daily', 1, 1, 0, 1),
(71, 'daily', 0, 'FIND_STUFF', 'pet_type', '2', '', 10, 'FINDCROC', 1, 26, '', 'daily', 1, 1, 0, 1),
(72, 'daily', 0, 'FIND_STUFF', 'base_item_id', '7185;7186;7187', '', 10, 'FINDCSTL', 1, 27, '', 'daily', 1, 1, 0, 1),
(73, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1932', '', 10, 'FINDDJ', 1, 28, '', 'daily', 1, 1, 0, 1),
(74, 'daily', 0, 'FIND_STUFF', 'pet_type', '0;29', '', 10, 'FINDDOG', 1, 29, '', 'daily', 1, 1, 0, 1),
(75, 'daily', 0, 'FIND_STUFF', 'pet_type', '12', '', 10, 'FINDDRAGON', 1, 30, '', 'daily', 1, 1, 0, 1),
(76, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1755', '', 10, 'FINDFIN', 1, 31, '', 'daily', 1, 1, 0, 1),
(77, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1778', '', 10, 'FINDICEBOX', 1, 32, '', 'daily', 1, 1, 0, 1),
(78, 'daily', 0, 'FIND_HAND_ITEM', 'hand_item_id', '4', '', 10, 'FINDICECREAM', 1, 33, '', 'daily', 1, 1, 0, 1),
(79, 'daily', 0, 'WALK_OVER_STUFF', 'base_item_id', '0', '', 10, 'FINDJUNGLE', 10, 34, '', 'daily', 1, 1, 0, 1),
(80, 'daily', 0, 'FIND_STUFF', 'pet_type', '6', '', 10, 'FINDLION', 1, 35, '', 'daily', 1, 1, 0, 1),
(81, 'daily', 0, 'FIND_STUFF', 'base_item_id', '3649', '', 10, 'FINDMOVIESCREEN', 1, 36, '', 'daily', 1, 1, 0, 1),
(82, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1721;1730;1742', '', 10, 'FINDMTTRSS', 1, 37, '', 'daily', 1, 1, 0, 1),
(83, 'daily', 0, 'FIND_HAND_ITEM', 'hand_item_id', '38', '', 10, 'FINDORANGE', 1, 38, '', 'daily', 1, 1, 0, 1),
(84, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1804', '', 10, 'FINDPARASOL', 1, 39, '', 'daily', 1, 1, 0, 1),
(85, 'daily', 0, 'FIND_STUFF', 'base_item_id', '2025', '', 10, 'FINDPICNICBLANKET', 1, 40, '', 'daily', 1, 1, 0, 1),
(86, 'daily', 0, 'FIND_STUFF', 'pet_type', '5;30', '', 10, 'FINDPIG', 1, 41, '', 'daily', 1, 1, 0, 1),
(87, 'daily', 0, 'FIND_STUFF', 'base_item_id', '4097', '', 10, 'FINDRAIL', 1, 42, '', 'daily', 1, 1, 0, 1),
(88, 'daily', 0, 'FIND_STUFF', 'base_item_id', '4098', '', 10, 'FINDRAMP', 1, 43, '', 'daily', 1, 1, 0, 1),
(89, 'daily', 0, 'FIND_STUFF', 'pet_type', '11', '', 10, 'FINDRAREFROG', 1, 44, '', 'daily', 1, 1, 0, 1),
(90, 'daily', 0, 'FIND_STUFF', 'pet_type', '7', '', 10, 'FINDRHINO', 1, 45, '', 'daily', 1, 1, 0, 1),
(91, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1666;1667', '', 10, 'FINDRUNWAYCHAIR', 1, 46, '', 'daily', 1, 1, 0, 1),
(92, 'daily', 0, 'FIND_STUFF', 'base_item_id', '8306;8307;8308;8309;8310', '', 10, 'FINDSHELL', 1, 47, '', 'daily', 1, 1, 0, 1),
(93, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1754', '', 10, 'FINDSHOWER', 1, 48, '', 'daily', 1, 1, 0, 1),
(94, 'daily', 0, 'FIND_STUFF', 'pet_type', '8', '', 10, 'FINDSPIDER', 1, 49, '', 'daily', 1, 1, 0, 1),
(95, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1807', '', 10, 'FINDSTATUE', 1, 50, '', 'daily', 1, 1, 0, 1),
(96, 'daily', 0, 'FIND_STUFF', 'pet_type', '3;25', '', 10, 'FINDTERRIER', 1, 51, '', 'daily', 1, 1, 0, 1),
(97, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1933', '', 10, 'FINDTRAY', 1, 52, '', 'daily', 1, 1, 0, 1),
(98, 'daily', 0, 'FIND_STUFF', 'base_item_id', '1803', '', 10, 'FINDWATERFALL', 1, 53, '', 'daily', 1, 1, 0, 1),
(99, 'daily', 0, 'WALK_OVER_STUFF', 'base_item_id', '1515', '', 10, 'SKATING1', 1, 55, '', 'daily', 1, 1, 0, 1),
(100, 'daily', 0, 'WALK_OVER_STUFF', 'base_item_id', '4097;4098', '', 15, 'SKATING2', 40, 56, '', 'daily', 0, 1, 0, 1),
(101, 'daily', 0, 'WALK_OVER_STUFF', 'base_item_id', '4078', '', 15, 'WALKONSIDEWALK', 30, 57, '', 'daily', 0, 1, 0, 1),
(102, 'xmas2013_1', 1, 'CHAT_WITH_SOMEONE', '', '', '', 10, 'the_doors', 1, 1, '', 'xmas2013_1', 1, 0, 1, 1),
(103, 'xmas2013_2', 1, 'WALK_OVER_STUFF', '', '', '', 10, 'the_keys', 1, 2, '', 'xmas2013_2', 1, 0, 1, 1),
(104, 'xmas2013_3', 1, 'SWITCH_ITEM_STATE', '', '', '', 10, 'hole_torch', 1, 3, '', 'xmas2013_3', 1, 0, 1, 1),
(105, 'xmas2013_4', 1, 'SWIM', '', '', '', 10, 'dive', 1, 4, '', 'xmas2013_4', 1, 0, 1, 1),
(106, 'xmas2013_5', 1, 'SWITCH_ITEM_STATE', '', '', '', 10, 'fireplace', 1, 5, '', 'xmas2013_5', 1, 0, 1, 1),
(107, 'xmas2013_6', 1, 'CHANGE_FIGURE', '', '', '', 10, 'slippers', 1, 6, '', 'xmas2013_6', 1, 0, 1, 1),
(108, 'xmas2013_7', 1, 'CHANGE_FIGURE', '', '', '', 10, 'trendset', 1, 7, '', 'xmas2013_7', 1, 0, 1, 1),
(109, 'xmas2013_8', 1, 'CHAT_WITH_SOMEONE', '', '', '', 10, 'penguin', 1, 8, '', 'xmas2013_8', 1, 0, 1, 1),
(110, 'xmas2013_9', 1, 'FIND_STUFF', '', '', '', 10, 'satellite', 1, 9, '', 'xmas2013_9', 1, 0, 1, 1),
(111, 'xmas2013_10', 1, 'ENTER_OTHER_USERS_ROOM', '', '', '', 10, 'friends', 1, 10, '', 'xmas2013_10', 1, 0, 1, 1),
(112, 'xmas2013_11', 1, 'FIND_HAND_ITEM', '', '', '', 10, 'drinks', 10, 11, '', 'xmas2013_11', 1, 0, 1, 1),
(113, 'xmas2013_12', 1, 'WAVE', '', '', '', 10, 'snow', 10, 12, '', 'xmas2013_12', 1, 0, 1, 1),
(114, 'xmas2013_13', 1, 'DANCE', '', '', '', 10, 'dance', 1, 13, '', 'xmas2013_13', 1, 0, 1, 1),
(115, 'xmas2013_14', 1, 'FIND_HAND_ITEM', '', '', '', 10, 'moredrink', 1, 14, '', 'xmas2013_14', 1, 0, 1, 1),
(116, 'xmas2013_15', 1, 'SWIM', '', '', '', 10, '1386093252572', 1, 15, '', 'xmas2013_15', 1, 0, 1, 1),
(117, 'xmas2013_16', 1, 'ROOM_PULSE', '', '', '', 10, 'barrel', 1, 16, '', 'xmas2013_16', 1, 0, 1, 1),
(118, 'xmas2013_17', 1, 'SWITCH_ITEM_STATE', '', '', '', 10, 'tree', 1, 17, '', 'xmas2013_17', 1, 0, 1, 1),
(119, 'xmas2013_18', 1, 'CHAT_WITH_SOMEONE', '', '', '', 10, 'carol', 50, 18, '', 'xmas2013_18', 1, 0, 1, 1),
(120, 'xmas2013_19', 1, 'CHAT_WITH_SOMEONE', '', '', '', 10, 'guess', 5, 19, '', 'xmas2013_19', 1, 0, 1, 1),
(121, 'xmas2013_20', 1, 'FIND_HAND_ITEM', '', '', '', 10, 'carrots', 10, 20, '', 'xmas2013_20', 1, 0, 1, 1),
(122, 'xmas2013_22', 1, 'ROOM_PULSE', '', '', '', 10, 'santa', 1, 21, '', 'xmas2013_22', 1, 0, 1, 1),
(123, 'xmas2013_23', 1, 'CHAT_WITH_SOMEONE', '', '', '', 10, 'turkey', 1, 22, '', 'xmas2013_23', 1, 0, 1, 1),
(124, 'xmas2013_24', 1, 'SWITCH_ITEM_STATE', '', '', '', 10, 'snowman', 1, 23, '', 'xmas2013_24', 1, 0, 1, 1),
(125, 'xmas2013_25', 1, 'CHAT_WITH_SOMEONE', '', '', '', 10, 'merry', 12, 24, '', 'xmas2013_25', 1, 0, 1, 1),
(126, 'social', 0, 'CHAT_WITH_SOMEONE', '', '', '', 0, 'CHATWITHSOMEONE', 25, 1, '', 'social', 0, 0, 0, 1),
(127, 'social', 0, 'DANCE', '', '', '', 0, 'DANCE', 30, 2, '', 'social', 0, 0, 0, 1),
(128, 'social', 0, 'ENTER_OTHER_USERS_ROOM', '', '', '', 0, 'ENTEROTHERSROOM', 5, 3, '', 'social', 0, 0, 0, 1),
(129, 'social', 0, 'GIVE_RESPECT', '', '', '', 0, 'GIVERESPECT', 3, 4, '', 'social', 0, 0, 0, 1),
(130, 'social', 0, 'POST_IT_OTHER_USERS_ROOM', '', '', '', 0, 'POSTITOTHERUSERSROOM', 1, 5, '', 'social', 0, 0, 0, 1),
(131, 'social', 0, 'REQUEST_FRIEND', '', '', '', 0, 'REQUESTFRIEND', 1, 6, '', 'social', 0, 0, 0, 1),
(132, 'social', 0, 'WAVE', '', '', '', 0, 'WAVE', 1, 7, '', 'social', 0, 0, 0, 1),
(133, 'explore', 0, 'FIND_STUFF', 'base_item_id', '1467', '', 0, 'FINDBBGATE', 1, 0, '', '', 1, 0, 0, 1),
(134, 'explore', 0, 'FIND_STUFF', 'base_item_id', '1477', '', 0, 'FINDBBTELEPORT', 1, 1, '', '', 1, 0, 0, 1),
(135, 'explore', 0, 'FIND_STUFF', 'base_item_id', '1487', '', 0, 'FINDBBTILE', 1, 2, '', '', 1, 0, 0, 1),
(136, 'explore', 0, 'FIND_STUFF', 'base_item_id', '1743', '', 0, 'FINDBEETLE', 1, 3, '', '', 1, 0, 0, 1),
(137, 'explore', 0, 'FIND_STUFF', 'base_item_id', '1941', '', 0, 'FINDDISCOBALL', 1, 4, '', '', 1, 0, 0, 1),
(138, 'explore', 0, 'FIND_STUFF', 'base_item_id', '1514', '', 0, 'FINDFREEZEEXITTILE', 1, 5, '', '', 1, 0, 0, 1),
(139, 'explore', 0, 'FIND_STUFF', 'base_item_id', '1502', '', 0, 'FINDFREEZEGATE', 1, 6, '', '', 1, 0, 0, 1),
(140, 'explore', 0, 'FIND_STUFF', 'base_item_id', '1506', '', 0, 'FINDFREEZESCOREBOARD', 1, 7, '', '', 1, 0, 0, 1),
(141, 'explore', 0, 'FIND_STUFF', 'base_item_id', '4786', '', 0, 'FINDJUKEBOX', 1, 8, '', '', 1, 0, 0, 1),
(142, 'explore', 0, 'FIND_STUFF', 'base_item_id', '1752', '', 0, 'FINDLIFEGUARDTOWER', 1, 9, '', '', 1, 0, 0, 1),
(143, 'explore', 0, 'FIND_STUFF', 'base_item_id', '1936', '', 0, 'FINDNEONFLOOR', 1, 10, '', '', 1, 0, 0, 1),
(144, 'explore', 0, 'FIND_STUFF', 'base_item_id', '1716', '', 0, 'FINDSURFBOARD', 1, 11, '', '', 1, 0, 0, 1),
(145, 'explore', 0, 'FIND_STUFF', 'base_item_id', '3888', '', 0, 'FINDTAGPOLE', 1, 12, '', '', 1, 0, 0, 1),
(146, 'explore', 0, 'WALK_OVER_STUFF', 'base_item_id', '3156', '', 0, 'ICESKATE', 25, 13, '', '', 1, 0, 0, 1),
(147, 'explore', 0, 'WALK_OVER_STUFF', 'base_item_id', '1515', '', 0, 'ROLLERSKATE', 25, 14, '', '', 1, 0, 0, 1),
(148, 'explore', 0, 'SWIM', 'base_item_id', '1750', '', 0, 'SWIM', 25, 15, '', '', 1, 0, 0, 1);

-- --------------------------------------------------------

--
-- Table structure for table `quests_conditions`
--

CREATE TABLE `quests_conditions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `quest_id` int(11) NOT NULL,
  `condition_type` varchar(64) NOT NULL,
  `condition_key` varchar(64) DEFAULT NULL,
  `condition_value` varchar(255) DEFAULT NULL,
  `condition_extra` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `quest_id` (`quest_id`),
  KEY `condition_type` (`condition_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `quests_conditions`
--

INSERT INTO `quests_conditions` (`id`, `quest_id`, `condition_type`, `condition_key`, `condition_value`, `condition_extra`) VALUES
(1, 3, 'FIGURE_PART', 'ha', '1006', NULL),
(2, 10, 'FIGURE_PART', 'ha', '1006', NULL),
(3, 44, 'ROOM_ITEM_STATE', 'base_item_id', '21', '1'),
(4, 126, 'ROOM_USERS_MIN', NULL, '2', NULL),
(5, 132, 'ROOM_USERS_MIN', NULL, '2', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `users_community_goals`
--

CREATE TABLE `users_community_goals` (
  `user_id` int(11) NOT NULL,
  `goal_code` varchar(64) NOT NULL,
  `contribution_score` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`user_id`,`goal_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users_community_goal_rewards`
--

CREATE TABLE `users_community_goal_rewards` (
  `user_id` int(11) NOT NULL,
  `goal_code` varchar(64) NOT NULL,
  `prize_id` int(11) NOT NULL,
  `claimed_at` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`user_id`,`goal_code`,`prize_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users_quests`
--

CREATE TABLE `users_quests` (
  `user_id` int(11) NOT NULL,
  `quest_id` int(11) NOT NULL,
  `accepted` tinyint(1) NOT NULL DEFAULT 0,
  `completed_steps` int(11) NOT NULL DEFAULT 0,
  `completed_at` int(11) NOT NULL DEFAULT 0,
  `claimed_at` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`user_id`,`quest_id`),
  KEY `idx_quest_id` (`quest_id`),
  KEY `idx_user_completed` (`user_id`,`completed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users_quests_queue`
--

CREATE TABLE `users_quests_queue` (
  `user_id` int(11) NOT NULL,
  `quest_id` int(11) NOT NULL,
  `amount` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`user_id`,`quest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


INSERT INTO community_goals
(code, name, total_score, start_timestamp, duration_seconds, enabled, result_option, goal_type)
VALUES
('concurrent_users', 'Concurrent Users', 0, 1783912860, 604800, 1, 0, 'contribution');

INSERT INTO community_goal_levels
(goal_code, level, score_threshold)
VALUES
('concurrent_users', 0, 0),
('concurrent_users', 1, 100);

INSERT INTO community_goal_triggers
(goal_code, trigger_type, contribution_mode, enabled)
VALUES
('concurrent_users', 'concurrent_users', 'event', 1);
