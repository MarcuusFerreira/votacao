package br.com.marcusferreira.voting;

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class AbstractIntegrationTest {

    // Singleton container pattern: this base class is extended by multiple concrete
    // integration test classes. Using @Testcontainers/@Container would make JUnit's
    // extension start/stop this same static container instance once per subclass,
    // and Testcontainers does not reliably support restarting an already-stopped
    // container. Starting it once here (reused for the whole JVM, reaped by Ryuk on
    // exit) avoids that.
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Each distinct test context keeps its own pool open against the shared container
        // (max_connections=100), so tests use Hikari's default size instead of the production one.
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 10);
    }
}
