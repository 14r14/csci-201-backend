-- Change VKC B10 from MAINTENANCE to OCCUPIED
UPDATE rooms SET current_status = 'OCCUPIED' WHERE building_name = 'VKC' AND room_number = 'B10';

-- Add student to hold the seed reservation (keeps test users alice/bob/dave unaffected)
INSERT INTO users (user_name, password_hash, first_name, last_name, role, courses, social_preferences) VALUES
('eve', '$2a$10$placeholder', 'Eve', 'Santos', 'STUDENT', 'CSCI 201, CSCI 499', 'quiet study');

-- Confirmed reservation for eve so waitlist join is allowed on VKC B10
INSERT INTO reservations (user_id, room_id, start_time, end_time, status) VALUES (
    (SELECT user_id FROM users WHERE user_name = 'eve'),
    (SELECT room_id FROM rooms WHERE building_name = 'VKC' AND room_number = 'B10'),
    DATE_ADD(CURDATE(), INTERVAL 10 HOUR),
    DATE_ADD(CURDATE(), INTERVAL 11 HOUR),
    'CONFIRMED'
);
