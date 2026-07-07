
ALTER TABLE `achievements` ADD `category_id` INT(11) NOT NULL AFTER `category`;
ALTER TABLE `achievements` ADD `badge_has_level` tinyint(1) NOT NULL DEFAULT 1 AFTER `progress_needed`;
ALTER TABLE `achievements` ADD `order_num` INT(11) NOT NULL DEFAULT '0' AFTER `badge_has_level`;

CREATE TABLE `achievements_categories` (
  `id` int(11) NOT NULL,
  `name` varchar(55) NOT NULL,
  `order_num` smallint(6) NOT NULL DEFAULT 0,
  `visible` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


ALTER TABLE achievements_categories
    ADD PRIMARY KEY (id);

ALTER TABLE achievements_categories
    MODIFY id int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE achievements
    ADD INDEX idx_achievements_category_id (category_id);

ALTER TABLE achievements
    ADD CONSTRAINT fk_achievements_category
        FOREIGN KEY (category_id)
        REFERENCES achievements_categories(id);

INSERT INTO achievements_categories (name, order_num, visible)
SELECT DISTINCT category, 0, 1
FROM achievements
WHERE category IS NOT NULL
  AND category != ''
  AND category NOT IN (
      SELECT name FROM achievements_categories
  );

UPDATE achievements a
JOIN achievements_categories c ON c.name = a.category
SET a.category_id = c.id
WHERE a.category_id = 0 OR a.category_id IS NULL;