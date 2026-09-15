package org.example.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.postgresql.ds.PGSimpleDataSource;

public class Service {

    public static final ScopedValue<Integer> USER_ID = ScopedValue.newInstance();
    private final PGSimpleDataSource dataSource;

    public Service(String url, String user, String password) {
        this.dataSource = new PGSimpleDataSource();
        // this.dataSource.setUrl("jdbc:postgresql://cloud-pooler-url:5432/my_database");
        this.dataSource.setUrl(url);
        this.dataSource.setUser(user);
        this.dataSource.setPassword(password);

    }

    public void executeQuery(Consumer<DSLContext> queryAction) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // BEGIN transaction

            try {
                if (USER_ID.isBound()) {
                    try (PreparedStatement stmt = conn
                            .prepareStatement("SELECT set_config('app.current_user_id', ?, true)")) {
                        stmt.setString(1, USER_ID.get().toString());
                        stmt.execute();
                    }
                }

                DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
                queryAction.accept(ctx);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Query execution failed", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }
}
