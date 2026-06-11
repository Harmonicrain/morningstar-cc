-- Wired 2.0 advanced source settings: generic per-item persistence of
-- furniSourceTypes/userSourceTypes (JSON {"f":[...],"u":[...]}, empty = defaults).
ALTER TABLE `items` ADD COLUMN `wired_sources` VARCHAR(512) NOT NULL DEFAULT '';
