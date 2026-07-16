package io.sniffy.test.junit.jupiter;

import io.sniffy.sql.SqlExpectation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@ExtendWith(SniffyExtension.class)
public class SniffyExtensionTest {

    @BeforeEach
    public void beforeEach() throws Exception {
        query();
    }

    @AfterEach
    public void afterEach() throws Exception {
        query();
    }

    @Test
    @SqlExpectation(count = @io.sniffy.test.Count(3))
    public void countsBeforeTestAndAfterEachQueries() throws Exception {
        query();
    }

    @RepeatedTest(2)
    @SqlExpectation(count = @io.sniffy.test.Count(3))
    public void repeatedInvocationsAreIsolated() throws Exception {
        query();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @SqlExpectation(count = @io.sniffy.test.Count(min = 2, max = 3))
    public void parameterizedInvocationsAreIsolated(boolean execute) throws Exception {
        if (execute) query();
    }

    @Nested
    public class NestedTests {
        @Test
        @ThreeQueries
        public void supportsNestedAndComposedAnnotations() throws Exception {
            query();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @SqlExpectation(count = @io.sniffy.test.Count(3))
    public @interface ThreeQueries {
    }

    static void query() throws Exception {
        Class.forName("org.h2.Driver");
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
        try {
            Statement statement = connection.createStatement();
            try {
                statement.execute("SELECT 1");
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
    }
}
