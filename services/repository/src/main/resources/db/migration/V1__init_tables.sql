-- 1. Create the base 'users' table first
CREATE TABLE users (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- 2. Create 'settings' (One-to-One relationship with users)
CREATE TABLE settings (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INT UNIQUE NOT NULL,
    theme VARCHAR(20) DEFAULT 'light',
    notifications_enabled BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_user_settings FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
-- 3. Create 'projects' (One-to-Many relationship with users)
CREATE TABLE projects (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_projects FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);