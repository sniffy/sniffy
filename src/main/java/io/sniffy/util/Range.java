package io.sniffy.util;

import io.sniffy.test.Count;
import io.sniffy.Expectation;

public class Range {

    public final int value;
    public final int min;
    public final int max;

    public Range(int value, int min, int max) {

        if (-1 != value) {
            if (-1 != min || -1 != max) {
                throw new IllegalArgumentException("Ambiguous configuration - parameter value used together with min/max");
            } else {
                this.min = this.max = this.value = value;
            }
        } else {
            if (min > max) {
                throw new IllegalArgumentException("Min parameter cannot be larger than max parameter");
            } else if (max == min) {
                this.min = this.max = this.value = max;
            } else {
                this.value = value;
                this.min = min;
                this.max = max;
            }
        }

    }

    public static Range parse(Count count) {
        return new Range(count.value(), count.min(), count.max());
    }

    public static Range parse(Expectation expectation) {

        int value = expectation.value();
        int min = expectation.atLeast();
        int max = expectation.atMost();

        Count count = expectation.count();

        if (-1 != value && (-1 != min || -1 != max)) {
            throw new IllegalArgumentException("Ambiguous configuration - parameter value used together with atLeast/atMost");
        }

        if (-1 != count.value()) {
            if (-1 != value) {
                throw new IllegalArgumentException("Ambiguous configuration - parameter value used together with count");
            } else {
                value = count.value();
            }
        }

        if (-1 != count.min()) {
            if (-1 != min) {
                throw new IllegalArgumentException("Ambiguous configuration - parameter min used together with count");
            } else {
                min = count.min();
            }
        }

        if (-1 != count.max()) {
            if (-1 != max) {
                throw new IllegalArgumentException("Ambiguous configuration - parameter max used together with count");
            } else {
                max = count.max();
            }
        }

        return new Range(value, min, max);

    }

}
