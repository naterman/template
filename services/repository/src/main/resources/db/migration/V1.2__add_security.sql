ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
CREATE POLICY user_isolation_policy ON projects FOR ALL USING (
    user_id = NULLIF(current_setting('app.current_user_id', true), '')::INT
);