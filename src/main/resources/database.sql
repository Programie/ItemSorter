CREATE TABLE IF NOT EXISTS `links`
(
    `id`     INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `uuid`   TEXT,
    `player` TEXT COLLATE NOCASE,
    `name`   TEXT COLLATE NOCASE,
    `type`   TEXT COLLATE NOCASE,
    `order`  INTEGER,
    `world`  TEXT,
    `x`      INTEGER,
    `y`      INTEGER,
    `z`      INTEGER
);

CREATE UNIQUE INDEX `location` ON `links` (`world`, `x`, `y`, `z`);
CREATE INDEX `uuid` ON `links` (`uuid`);
CREATE INDEX `nameType` ON `links` (`name`, `type`);
CREATE INDEX `order` ON `links` (`order`);