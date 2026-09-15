package org.example.repository;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.example.repository.postgres.generated.Tables;
import org.example.repository.postgres.generated.tables.daos.ProjectsDao;
import org.example.repository.postgres.generated.tables.records.ProjectsRecord;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

public class ServiceTest {

    private static PostgreSQLContainer<?> container;

    private Service service;

    @BeforeAll
    public static void beforeAll() throws IOException {
        container = new PostgreSQLContainer<>("postgres:18");
        container.start();

        Enumeration<URL> urls = ServiceTest.class.getClassLoader().getResources("db/migration");
        String[] locations = Stream.of(urls).flatMap(e -> Collections.list(e).stream()).map(URL::getPath)
                .map(value -> "filesystem:" + value).toArray(String[]::new);

        Flyway.configure().dataSource(container.getJdbcUrl(), // Host URL
                container.getUsername(), // Username
                container.getPassword()) // Password
                .locations(locations) // Migration
                .load() // Load
                .migrate(); // Run Migration
    }

    @AfterAll
    public static void afterAll() {
        container.close();
    }

    @BeforeEach
    public void beforeEach() {
        service = new Service(container.getJdbcUrl(), "app_user", "secret123");
    }

    @Test
    public void testValidContainer() {
        assertNotNull(container);
    }

    @Test
    public void testContainerPath() {
        String containerCompose = System.getProperty("containers.postgres", null);
        assertThat(containerCompose).isNotEmpty();
    }

    @Test
    public void testUsersTable() {
        service.executeQuery(ctx -> {
            var userId = ctx.select(Tables.Users.Id).from(Tables.Users)
                    .where(Tables.Users.Email.eq("alice@example.com")).fetchOneInto(Integer.class);

            assertEquals(1, userId);
        });
    }

    @Test
    public void testAuthorizedUser() {
        ScopedValue.where(Service.USER_ID, 1).run(() -> {
            service.executeQuery(ctx -> {
                var results = ctx.select(Tables.Projects.Name).from(Tables.Projects).fetchInto(String.class);

                assertEquals(6, results.size());
                results.forEach(IO::println);
            });
        });
    }

    @Test
    public void testUnauthorizedUser() {
        service.executeQuery(ctx -> {
            var results = ctx.select(Tables.Projects.Name).from(Tables.Projects).fetchInto(String.class);
            assertEquals(0, results.size());

        });
    }

    @Test
    public void testCreateNewRecord() {
        ScopedValue.where(Service.USER_ID, 1).run(() -> {
            service.executeQuery(ctx -> {
                var project = new ProjectsRecord();
                project.setUserId(1);
                project.setName("NEW_PROJECT");
                var results = ctx.insertInto(Tables.Projects).set(project).execute();

                assertEquals(1, results);

                var projectList = ctx.fetchCount(Tables.Projects);
                assertEquals(7, projectList);
            });
        });
    }

    @Test
    public void testProjectDao_AuthorizedUser() {
        ScopedValue.where(Service.USER_ID, 1).run(() -> {
            service.executeQuery(ctx -> {

                var dao = new ProjectsDao(ctx.configuration());

                var results = dao.fetchByUserId(1);
                assertEquals(6, results.size());
                results.forEach(IO::println);
            });
        });
    }

    @Test
    public void testProjectDao_AuthorizedUser_RestrictedResults() {
        ScopedValue.where(Service.USER_ID, 1).run(() -> {
            service.executeQuery(ctx -> {

                var dao = new ProjectsDao(ctx.configuration());

                var results = dao.fetchByUserId(2);
                assertEquals(0, results.size());
            });
        });
    }
}
