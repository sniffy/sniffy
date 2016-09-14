package io.sniffy.util;

import io.sniffy.sql.SqlExpectation;
import io.sniffy.test.Count;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RangeTest {

    @Test(expected = IllegalArgumentException.class)
    public void testAmbiguousConfiguration1() {
        new Range(1,2,3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAmbiguousConfiguration2() {
        new Range(-1,4,3);
    }

    @Test
    @SqlExpectation(count = @Count(max = 3))
    public void testParseExpectation() throws NoSuchMethodException {
        Range range = Range.parse(RangeTest.class.getMethod("testParseExpectation").getAnnotation(SqlExpectation.class).count());
        assertEquals(3, range.max);
    }


}