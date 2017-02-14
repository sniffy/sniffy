package io.sniffy.util;

import io.sniffy.test.Count;

/**
 * @since 3.1
 */
public class Range {

    public final int min;
    public final int max;

    public Range(int value, int min, int max) {

        if (-1 != value) {
            if (-1 != min || -1 != max) {
                throw new IllegalArgumentException("Ambiguous configuration - parameter value used together with min/max");
            } else {
                this.min = this.max = value;
            }
        } else {
            if (min > max && -1 != max) {
                throw new IllegalArgumentException("Min parameter cannot be larger than max parameter");
            }
            this.min = -1 != min ? min : 0;
            this.max = -1 != max ? max : Integer.MAX_VALUE;
        }

    }

    public static Range parse(Count count) {
        return new Range(count.value(), count.min(), count.max());
    }

}
