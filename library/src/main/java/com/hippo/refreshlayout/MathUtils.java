package com.hippo.refreshlayout;

final class MathUtils {
    private MathUtils() {}

    /**
     * Returns the input value x clamped to the range [min, max].
     */
    public static float clamp(float x, float min, float max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }
}
