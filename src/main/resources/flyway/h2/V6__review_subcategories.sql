-- V6: subcategory rating columns on reviews + per-subcategory averages on rooms

ALTER TABLE reviews
    ADD COLUMN noise_rating       INT CHECK (noise_rating       BETWEEN 1 AND 5),
    ADD COLUMN cleanliness_rating INT CHECK (cleanliness_rating BETWEEN 1 AND 5),
    ADD COLUMN amenities_rating   INT CHECK (amenities_rating   BETWEEN 1 AND 5);

ALTER TABLE rooms
    ADD COLUMN avg_noise_rating       DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN avg_cleanliness_rating DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN avg_amenities_rating   DOUBLE NOT NULL DEFAULT 0;
