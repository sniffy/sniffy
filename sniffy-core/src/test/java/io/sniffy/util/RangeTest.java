package io.sniffy.util;

import io.sniffy.Expectation;
import io.sniffy.test.Count;
import org.junit.Test;

import static org.junit.Assert.*;

public class RangeTest {

    @Test(expected = IllegalArgumentException.class)
    public void testAmbiguousConfiguration1() {
        new Range(1,2,3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAmbiguousConfiguration2() {
        new Range(-1,4,3);
    }

    @Test(expected = IllegalArgumentException.class)
    @Expectation(value = 2, count = @Count(3))
    public void testParseExpectationWithAmbiguousConfiguration1() throws NoSuchMethodException {
        Range.parse(RangeTest.class.getMethod("testParseExpectationWithAmbiguousConfiguration1").getAnnotation(Expectation.class));
    }

    @Test(expected = IllegalArgumentException.class)
    @Expectation(atLeast = 2, count = @Count(min = 3))
    public void testParseExpectationWithAmbiguousConfiguration2() throws NoSuchMethodException {
        Range.parse(RangeTest.class.getMethod("testParseExpectationWithAmbiguousConfiguration1").getAnnotation(Expectation.class));
    }

    @Test(expected = IllegalArgumentException.class)
    @Expectation(atMost = 2, count = @Count(max = 3))
    public void testParseExpectationWithAmbiguousConfiguration3() throws NoSuchMethodException {
        Range.parse(RangeTest.class.getMethod("testParseExpectationWithAmbiguousConfiguration1").getAnnotation(Expectation.class));
    }

    @Test(expected = IllegalArgumentException.class)
    @Expectation(count = @Count(max = 3))
    public void testParseExpectation() throws NoSuchMethodException {
        Range range = Range.parse(RangeTest.class.getMethod("testParseExpectationWithAmbiguousConfiguration1").getAnnotation(Expectation.class));
        assertEquals(3, range.max);
    }


}