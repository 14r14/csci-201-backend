-- MySQL / MariaDB (InnoDB)

CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    courses TEXT,
    social_preferences TEXT,
    last_login_timestamp TIMESTAMP(6) NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_user_name UNIQUE (user_name)
) ENGINE=InnoDB;

CREATE TABLE rooms (
    room_id BIGINT NOT NULL AUTO_INCREMENT,
    building_name VARCHAR(255) NOT NULL,
    room_number VARCHAR(64) NOT NULL,
    capacity INT NOT NULL,
    feature_list TEXT,
    map_location VARCHAR(512) NULL,
    current_status VARCHAR(32) NOT NULL,
    average_rating DOUBLE NOT NULL DEFAULT 0,
    ratings_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (room_id),
    CONSTRAINT uk_rooms_building_room UNIQUE (building_name, room_number)
) ENGINE=InnoDB;

CREATE TABLE reservations (
    reservation_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    start_time TIMESTAMP(6) NOT NULL,
    end_time TIMESTAMP(6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (reservation_id),
    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_reservations_room FOREIGN KEY (room_id) REFERENCES rooms (room_id)
) ENGINE=InnoDB;

CREATE INDEX idx_reservations_user ON reservations (user_id);
CREATE INDEX idx_reservations_room ON reservations (room_id);

CREATE TABLE waitlist (
    waitlist_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    requested_time_slot VARCHAR(512) NOT NULL,
    queue_position INT NOT NULL,
    created_timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (waitlist_id),
    CONSTRAINT fk_waitlist_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_waitlist_room FOREIGN KEY (room_id) REFERENCES rooms (room_id),
    CONSTRAINT uk_waitlist_user_room_slot UNIQUE (user_id, room_id, requested_time_slot)
) ENGINE=InnoDB;

CREATE INDEX idx_waitlist_user ON waitlist (user_id);
CREATE INDEX idx_waitlist_room ON waitlist (room_id);

CREATE TABLE reviews (
    review_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (review_id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_reviews_room FOREIGN KEY (room_id) REFERENCES rooms (room_id)
) ENGINE=InnoDB;

CREATE INDEX idx_reviews_user ON reviews (user_id);
CREATE INDEX idx_reviews_room ON reviews (room_id);

CREATE TABLE user_matches (
    match_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    matched_user_id BIGINT NOT NULL,
    shared_courses TEXT,
    compatibility_score DOUBLE NOT NULL,
    created_timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (match_id),
    CONSTRAINT fk_user_matches_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_matches_matched FOREIGN KEY (matched_user_id) REFERENCES users (user_id),
    CONSTRAINT chk_user_matches_distinct CHECK (user_id <> matched_user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_user_matches_user ON user_matches (user_id);
CREATE INDEX idx_user_matches_matched ON user_matches (matched_user_id);
