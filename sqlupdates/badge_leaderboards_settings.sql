INSERT INTO `emulator_settings` (`key`, `value`) VALUES
    ('leaderboards.enabled', '1'),
    ('leaderboards.cache.seconds', '600'),
    ('leaderboards.rank.max', '2'),
    ('leaderboards.entries.max', '500')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);


ALTER TABLE `users_badges`
    ADD INDEX IF NOT EXISTS `idx_badge_code_user_id` (`badge_code`, `user_id`),
    ADD INDEX IF NOT EXISTS `idx_user_id_badge_code` (`user_id`, `badge_code`);

ALTER TABLE `users`
    ADD INDEX IF NOT EXISTS `idx_rank_id` (`rank`, `id`);

ALTER TABLE `users_settings`
    DROP INDEX IF EXISTS `achievement_score`,
    ADD INDEX IF NOT EXISTS `idx_achievement_score_user_id`
        (`achievement_score`, `user_id`);

CREATE TABLE IF NOT EXISTS `badge_owner_counts` (
    `badge_code` VARCHAR(32) NOT NULL,
    `owner_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `rarity` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`badge_code`),
    INDEX `idx_rarity` (`rarity`)
);

CREATE TABLE IF NOT EXISTS `user_badge_rarity_counts` (
    `user_id` INT NOT NULL,
    `rarity` TINYINT UNSIGNED NOT NULL,
    `badge_count` INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`user_id`, `rarity`),
    INDEX `idx_rarity_score` (`rarity`, `badge_count`, `user_id`)
);

INSERT INTO `badge_owner_counts` (`badge_code`, `owner_count`, `rarity`)
SELECT
    ub.`badge_code`,
    COUNT(DISTINCT ub.`user_id`) AS `owner_count`,
    CASE
        WHEN COUNT(DISTINCT ub.`user_id`) BETWEEN 51 AND 200 THEN 1
        WHEN COUNT(DISTINCT ub.`user_id`) BETWEEN 11 AND 50 THEN 2
        WHEN COUNT(DISTINCT ub.`user_id`) BETWEEN 6 AND 10 THEN 3
        WHEN COUNT(DISTINCT ub.`user_id`) BETWEEN 4 AND 5 THEN 4
        WHEN COUNT(DISTINCT ub.`user_id`) BETWEEN 2 AND 3 THEN 5
        WHEN COUNT(DISTINCT ub.`user_id`) = 1 THEN 6
        ELSE 0
    END AS `rarity`
FROM `users_badges` ub
INNER JOIN `users` u
    ON u.`id` = ub.`user_id`
WHERE u.`rank` <= COALESCE(
    (
        SELECT CAST(es.`value` AS UNSIGNED)
        FROM `emulator_settings` es
        WHERE es.`key` = 'leaderboards.rank.max'
        LIMIT 1
    ),
    2
)
GROUP BY ub.`badge_code`
ON DUPLICATE KEY UPDATE
    `owner_count` = VALUES(`owner_count`),
    `rarity` = VALUES(`rarity`);

INSERT INTO `user_badge_rarity_counts` (`user_id`, `rarity`, `badge_count`)
SELECT
    ub.`user_id`,
    owners.`rarity`,
    COUNT(DISTINCT ub.`badge_code`) AS `badge_count`
FROM `users_badges` ub
INNER JOIN `badge_owner_counts` owners
    ON owners.`badge_code` = ub.`badge_code`
INNER JOIN `users` u
    ON u.`id` = ub.`user_id`
WHERE u.`rank` <= COALESCE(
    (
        SELECT CAST(es.`value` AS UNSIGNED)
        FROM `emulator_settings` es
        WHERE es.`key` = 'leaderboards.rank.max'
        LIMIT 1
    ),
    2
)
GROUP BY ub.`user_id`, owners.`rarity`
ON DUPLICATE KEY UPDATE
    `badge_count` = VALUES(`badge_count`);

ALTER TABLE `permissions` ADD COLUMN IF NOT EXISTS `cmd_update_badge_leaderboards` ENUM('0', '1') NOT NULL DEFAULT '0';

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.keys.cmd_update_badge_leaderboards', 'update_badge_leaderboards'),
    ('commands.description.cmd_update_badge_leaderboards', ':update_badge_leaderboards'),
    ('commands.success.cmd_update_badge_leaderboards.started', 'The badge leaderboard aggregate rebuild has started in the background.'),
    ('commands.success.cmd_update_badge_leaderboards.completed', 'Badge leaderboard aggregates rebuilt in %duration% ms.'),
    ('commands.error.cmd_update_badge_leaderboards.running', 'A badge leaderboard aggregate rebuild is already running.'),
    ('commands.error.cmd_update_badge_leaderboards', 'Failed to rebuild badge leaderboard aggregates.')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
