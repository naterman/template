package org.example.repository;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class RlsDataSource implements DataSource {

    // 1. ScopedValue replaces Reactor's ContextView/ThreadLocals for Virtual
    // Threads
    public static final ScopedValue<Integer> USER_ID = ScopedValue.newInstance();

    private final DataSource delegate;

    public RlsDataSource(DataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = delegate.getConnection();
        return prepareConnection(conn);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = delegate.getConnection(username, password);
        return prepareConnection(conn);
    }

    private Connection prepareConnection(Connection conn) throws SQLException {
        // 2. Read the userId implicitly from the ScopedValue context
        if (USER_ID.isBound()) {
            Integer userId = USER_ID.get();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT set_config('app.current_user_id', ?, false)")) {
                stmt.setString(1, userId.toString());
                stmt.execute();
            }
            return wrapConnection(conn);
        }
        return conn;
    }

    private Connection wrapConnection(Connection originalConn) {
        // 3. Dynamic proxy intercepts close() to sanitize the connection for the pooler
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        // 4. Reset the context BEFORE returning the connection to PgBouncer
                        try (originalConn) {
                            // 4. Reset the context BEFORE returning the connection to PgBouncer
                            try (PreparedStatement stmt = originalConn
                                    .prepareStatement("SELECT set_config('app.current_user_id', '', false)")) {
                                stmt.execute();
                            } catch (SQLException e) {
                                System.err.println("Failed to clear RLS context: " + e.getMessage());
                            }
                        }
                        return null;
                    }
                    return method.invoke(originalConn, args);
                });
    }

    // --- Standard DataSource Delegation Methods ---
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }
    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
