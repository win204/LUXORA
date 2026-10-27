package com.luxora.commerce.liquibase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:luxora-liquibase-test;MODE=MSSQLServer;DATABASE_TO_UPPER=false;DEFAULT_NULL_ORDERING=HIGH",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.enabled=true",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml"
})
class LiquibaseIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void liquibaseChangelogsApplySuccessfullyAndInitializeTables() {
        // Verify DATABASECHANGELOG table was created and populated
        Integer changeLogCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM DATABASECHANGELOG", Integer.class);
        assertThat(changeLogCount).isNotNull().isGreaterThan(0);

        // Verify core tables were created by Liquibase
        Integer brandsCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM brands", Integer.class);
        assertThat(brandsCount).isNotNull().isGreaterThanOrEqualTo(0);

        Integer rolesCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM roles", Integer.class);
        assertThat(rolesCount).isNotNull().isGreaterThanOrEqualTo(2);

        Integer promotionsCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM promotions", Integer.class);
        assertThat(promotionsCount).isNotNull().isGreaterThanOrEqualTo(0);

        Integer reviewsCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM reviews", Integer.class);
        assertThat(reviewsCount).isNotNull().isGreaterThanOrEqualTo(0);

        Integer reviewImagesCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM review_images", Integer.class);
        assertThat(reviewImagesCount).isNotNull().isGreaterThanOrEqualTo(0);

        Integer loyaltyAccountsCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM loyalty_accounts", Integer.class);
        assertThat(loyaltyAccountsCount).isNotNull().isGreaterThanOrEqualTo(0);

        Integer loyaltyTxCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM loyalty_transactions", Integer.class);
        assertThat(loyaltyTxCount).isNotNull().isGreaterThanOrEqualTo(0);

        Integer bannersCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM banners", Integer.class);
        assertThat(bannersCount).isNotNull().isGreaterThanOrEqualTo(0);
    }
}
