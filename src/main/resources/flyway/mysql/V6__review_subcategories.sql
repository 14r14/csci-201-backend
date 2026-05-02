-- V6: subcategory rating columns on reviews + per-subcategory averages on rooms

ALTER TABLE reviews
    ADD COLUMN noise_rating       INT NULL,
    ADD COLUMN cleanliness_rating INT NULL,
    ADD COLUMN amenities_rating   INT NULL;

ALTER TABLE rooms
    ADD COLUMN avg_noise_rating       DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN avg_cleanliness_rating DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN avg_amenities_rating   DOUBLE NOT NULL DEFAULT 0;

-- MySQL 8 doesn't allow CHECK referencing column in ALTER ADD in older syntax,
-- enforce 1-5 range via application layer validation instead.
