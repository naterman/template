-- 1. Create the Users
INSERT INTO users (username, email)
VALUES ('alice_smith', 'alice@example.com'),
    ('bob_jones', 'bob@example.com');
-- 2. Create the Settings
-- Assuming this is a fresh database, Alice is user_id 1 and Bob is user_id 2
INSERT INTO settings (user_id, theme, notifications_enabled)
VALUES (1, 'dark', FALSE),
    (2, 'light', TRUE);
-- 3. Create the Projects
-- We will assign 6 projects to Alice (user 1) and 4 projects to Bob (user 2)
INSERT INTO projects (user_id, name, description)
VALUES (
        1,
        'Website Redesign',
        'Overhaul the main landing page for better conversion.'
    ),
    (
        1,
        'Mobile App v2',
        'Implement the new feature set for iOS and Android.'
    ),
    (
        1,
        'Marketing Campaign',
        'Prepare assets for the Q3 social media push.'
    ),
    (
        1,
        'Database Migration',
        'Migrate legacy MySQL databases to PostgreSQL.'
    ),
    (
        1,
        'Analytics Dashboard',
        'Build an internal tool for tracking sales metrics.'
    ),
    (
        1,
        'Q4 Planning',
        'Draft departmental goals for the end of the year.'
    ),
    (
        2,
        'Customer Portal',
        'Develop a self-service portal for enterprise clients.'
    ),
    (
        2,
        'API Integration',
        'Connect the backend to a third-party payment gateway.'
    ),
    (
        2,
        'Security Audit',
        'Resolve vulnerabilities identified in the annual penetration test.'
    ),
    (
        2,
        'Brand Guidelines',
        'Update company logo and color palette documentation.'
    );