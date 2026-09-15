package org.example.local;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.sqlite.SQLiteDataSource;

public class Service {

    private final SQLiteDataSource dataSource;

    public Service(String url) {
        this.dataSource = new SQLiteDataSource();
        this.dataSource.setUrl(url);
    }

    public void executeQuery(Consumer<DSLContext> queryAction) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // BEGIN transaction

            try {
                DSLContext ctx = DSL.using(conn, SQLDialect.SQLITE);
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
