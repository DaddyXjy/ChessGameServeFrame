package frame.util;

import java.util.ArrayList;
import java.util.List;

import io.netty.util.internal.ThreadLocalRandom;

public class RandomUtil {
    public static int ramdom(int max) {
        return ThreadLocalRandom.current().nextInt(max + 1);
    }

    public static int ramdom(int min, int max) {
        if (min > max) {
            min = max + min;
            max = min - max;
            min = min - max;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static float ramdom(float max) {
        return (float) ThreadLocalRandom.current().nextDouble(0.0, max);
    }

    public static long ramdom(long min, long max) {
        if (min > max) {
            min = max + min;
            max = min - max;
            min = min - max;
        }
        return ThreadLocalRandom.current().nextLong(min, max);
    }

    public static float ramdom(float min, float max) {
        if (min > max) {
            min = max + min;
            max = min - max;
            min = min - max;
        }
        return (float) ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static double ramdom(double max) {
        return ThreadLocalRandom.current().nextDouble(0.0, max);
    }

    public static double ramdom(double min, double max) {
        if (min > max) {
            min = max + min;
            max = min - max;
            min = min - max;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static boolean ramdom() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    public static List<Integer> random(int min, int max, int num) {
        if (max - min < num) {
            return null;
        }
        List<Integer> r = new ArrayList<>();
        while (r.size() < num) {
            int p = ramdom(min, max);
            if (!r.contains(p)) {
                r.add(p);
            }
        }
        return r;
    }

}