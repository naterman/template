package org.example.local;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.example.repository.sqlite.generated.Tables;
import org.example.repository.sqlite.generated.tables.daos.ProjectsDao;
import org.example.repository.sqlite.generated.tables.records.ProjectsRecord;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ServiceTest {

    private Service service;

    // JUnit 5 provisions a fresh temporary directory for every test run
    @TempDir
    Path tempDir;

    @BeforeEach
    public void beforeEach() throws Exception {
        File tempDb = tempDir.resolve("test-schema.db").toFile();
        String jdbcUrl = "jdbc:sqlite:" + tempDb.getAbsolutePath() + "?foreign_keys=on";

        Enumeration<URL> schemas = ServiceTest.class.getClassLoader().getResources("db/schema");
        Enumeration<URL> data = ServiceTest.class.getClassLoader().getResources("db/data");
        Enumeration<URL> triggers = ServiceTest.class.getClassLoader().getResources("db/triggers");

        List<Path> paths = Stream.of(schemas, data, triggers).flatMap(e -> Collections.list(e).stream().sorted())
                .map(url -> {
                    try {
                        return url.toURI();
                    } catch (URISyntaxException e1) {
                        return null;
                    }
                }).filter(Objects::nonNull).map(Paths::get).collect(Collectors.toList());

        if (paths != null) {

            try (Connection conn = DriverManager.getConnection(jdbcUrl)) {

                DSLContext ctx = DSL.using(conn, SQLDialect.SQLITE);

                for (Path path : paths) {

                    try (Stream<Path> pathList = Files.walk(path)) {
                        List<Path> sqlFiles = pathList.filter(Files::isRegularFile)
                                .filter(p -> p.toString().endsWith(".sql")).sorted().toList();

                        for (Path sqlFile : sqlFiles) {
                            String sql = Files.readString(sqlFile);

                            // 2. Parse the multi-statement string and execute all statements
                            ctx.parser().parse(sql).executeBatch();
                        }
                    }
                }
            }
        }

        // 3. Initialize the service with the temporary file[cite: 4]
        service = new Service(jdbcUrl);
    }

    @Test
    public void testUsersTable() {
        service.executeQuery(ctx -> {
            var userId = ctx.select(Tables.Users.Id).from(Tables.Users)
                    .where(Tables.Users.Email.eq("alice@example.com")).fetchOneInto(Integer.class);

            assertEquals(2, userId);
        });
    }

    @Test
    public void testFetchAllProjects() {
        service.executeQuery(ctx -> {
            var results = ctx.select(Tables.Projects.Name).from(Tables.Projects).fetchInto(String.class);

            assertEquals(10, results.size());
            results.forEach(System.out::println);
        });
    }

    @Test
    public void testCreateNewRecord() {
        service.executeQuery(ctx -> {
            var project = new ProjectsRecord();
            project.setUserId(1);
            project.setName("NEW_PROJECT");

            var results = ctx.insertInto(Tables.Projects).set(project).execute();
            assertEquals(1, results);

            var projectList = ctx.fetchCount(Tables.Projects);
            assertEquals(11, projectList);
        });
    }

    @Test
    public void testProjectDao() {
        service.executeQuery(ctx -> {
            var dao = new ProjectsDao(ctx.configuration());

            var results = dao.fetchByUserId(1);
            assertEquals(6, results.size());
            results.forEach(System.out::println);
        });
    }
}
