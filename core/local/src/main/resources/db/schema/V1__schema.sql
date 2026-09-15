-- 1. Create the base 'users' table first
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
-- 2. Create 'settings' (One-to-One relationship with users)
CREATE TABLE settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER UNIQUE NOT NULL,
    theme TEXT DEFAULT 'light',
    notifications_enabled INTEGER DEFAULT 1,
    CONSTRAINT fk_user_settings FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
-- 3. Create 'projects' (One-to-Many relationship with users)
CREATE TABLE projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_projects FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);