INSERT INTO `emulator_settings` (`key`, `value`) VALUES
    ('hotel.floorplan.max.widthlength', '100'),
    ('hotel.floorplan.max.totalarea', '10000'),
    ('hotel.floorplan.max.totalarea.builders', '10000')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
