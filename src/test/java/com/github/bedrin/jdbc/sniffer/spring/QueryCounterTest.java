package com.github.bedrin.jdbc.sniffer.spring;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import com.github.bedrin.jdbc.sniffer.WrongNumberOfQueriesError;
import com.github.bedrin.jdbc.sniffer.junit.NoQueriesAllowed;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@NoQueriesAllowed
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = QueryCounterTest.class)
@TestExecutionListeners(QueryCounterListener.class)
public class QueryCounterTest extends BaseTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testNotAllowedQueriesByDefault() {
        executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    public void testSuppressedException() {
        thrown.expect(RuntimeException.class);
        executeStatement();
        throw new RuntimeException();
    }

}