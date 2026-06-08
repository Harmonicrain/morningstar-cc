SET NAMES utf8mb4;

-- Add separate command permission/texts for client furnidata reload.
ALTER TABLE `permissions`
    ADD `cmd_update_furnidata` ENUM('0', '1') NOT NULL DEFAULT '0' AFTER `cmd_update_items`;

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.keys.cmd_update_furnidata', 'update_furnidata;reload_furnidata'),
    ('commands.description.cmd_update_furnidata', ':update_furnidata'),
    ('commands.succes.cmd_update_furnidata', 'Furnidata reload broadcast sent!');
