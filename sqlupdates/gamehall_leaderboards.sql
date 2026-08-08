CREATE TABLE IF NOT EXISTS `gamehall_match_results` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `game_type` VARCHAR(32) NOT NULL,
  `room_id` INT(11) NOT NULL DEFAULT 0,
  `station_id` INT(11) NOT NULL DEFAULT 0,
  `player_one_user_id` INT(11) NOT NULL,
  `player_two_user_id` INT(11) NOT NULL,
  `winner_user_id` INT(11) DEFAULT NULL,
  `loser_user_id` INT(11) DEFAULT NULL,
  `result_type` VARCHAR(16) NOT NULL DEFAULT 'WIN',
  `started_at` BIGINT(20) DEFAULT NULL,
  `ended_at` BIGINT(20) NOT NULL,
  `duration_seconds` INT(11) NOT NULL DEFAULT 0,
  `winner_score` INT(11) NOT NULL DEFAULT 0,
  `loser_score` INT(11) NOT NULL DEFAULT 0,
  `metadata` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_gamehall_results_game_ended` (`game_type`, `ended_at`),
  KEY `idx_gamehall_results_winner` (`game_type`, `winner_user_id`, `ended_at`),
  KEY `idx_gamehall_results_loser` (`game_type`, `loser_user_id`, `ended_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `gamehall_leaderboard_stats` (
  `game_type` VARCHAR(32) NOT NULL,
  `period_type` VARCHAR(16) NOT NULL,
  `period_start` BIGINT(20) NOT NULL DEFAULT 0,
  `user_id` INT(11) NOT NULL,
  `points` INT(11) NOT NULL DEFAULT 0,
  `wins` INT(11) NOT NULL DEFAULT 0,
  `losses` INT(11) NOT NULL DEFAULT 0,
  `draws` INT(11) NOT NULL DEFAULT 0,
  `games_played` INT(11) NOT NULL DEFAULT 0,
  `current_streak` INT(11) NOT NULL DEFAULT 0,
  `best_streak` INT(11) NOT NULL DEFAULT 0,
  `last_result_at` BIGINT(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`game_type`, `period_type`, `period_start`, `user_id`),
  KEY `idx_gamehall_stats_rank` (`game_type`, `period_type`, `period_start`, `points`, `wins`, `losses`, `last_result_at`),
  KEY `idx_gamehall_stats_user` (`user_id`, `game_type`, `period_type`, `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
