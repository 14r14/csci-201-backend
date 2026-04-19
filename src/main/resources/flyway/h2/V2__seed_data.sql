-- Seed users
INSERT INTO users (user_name, password_hash, first_name, last_name, role, courses, social_preferences) VALUES
('alice',   '$2a$10$placeholder', 'Alice',   'Kim',     'STUDENT',    'CSCI 201, CSCI 270', 'quiet study'),
('bob',     '$2a$10$placeholder', 'Bob',     'Park',    'STUDENT',    'CSCI 201, CSCI 350', 'group work'),
('carol',   '$2a$10$placeholder', 'Carol',   'Lee',     'INSTRUCTOR', 'CSCI 201',           NULL),
('dave',    '$2a$10$placeholder', 'Dave',    'Nguyen',  'STUDENT',    'CSCI 270, CSCI 350', 'flexible'),
('admin1',  '$2a$10$placeholder', 'Admin',   'User',    'ADMIN',      NULL,                 NULL);

-- Seed rooms
INSERT INTO rooms (building_name, room_number, capacity, feature_list, map_location, current_status) VALUES
('SAL',  '101', 20,  'whiteboard,projector',        '34.0206,-118.2897', 'AVAILABLE'),
('SAL',  '102', 10,  'whiteboard',                  '34.0206,-118.2897', 'AVAILABLE'),
('GFS',  '201', 30,  'whiteboard,projector,tv',     '34.0201,-118.2891', 'AVAILABLE'),
('GFS',  '106', 6,   'whiteboard',                  '34.0201,-118.2891', 'AVAILABLE'),
('VKC',  '301', 50,  'projector,lecture_seating',   '34.0213,-118.2852', 'AVAILABLE'),
('VKC',  'B10', 8,   'whiteboard,tv',               '34.0213,-118.2852', 'MAINTENANCE');
