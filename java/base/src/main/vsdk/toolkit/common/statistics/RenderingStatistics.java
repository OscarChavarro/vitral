package vsdk.toolkit.common.statistics;

import vsdk.toolkit.common.VSDK;

/**
 * Centralized primitive and intersection counters used by rendering flows.
 */
public final class RenderingStatistics
{
    private static final int primitiveCount[];
    private static final int intersectionCount[];

    static {
        primitiveCount = new int[VSDK.PRIMITIVE_TYPE_COUNT];
        intersectionCount = new int[VSDK.INTERSECTION_TYPE_COUNT];
        resetPrimitiveCounters();
        resetIntersectionCounters();
    }

    private RenderingStatistics()
    {
    }

    public static void resetPrimitiveCounters()
    {
        int i;
        for ( i = 0; i < VSDK.PRIMITIVE_TYPE_COUNT; i++ ) {
            primitiveCount[i] = 0;
        }
    }

    public static void resetIntersectionCounters()
    {
        int i;
        for ( i = 0; i < VSDK.INTERSECTION_TYPE_COUNT; i++ ) {
            intersectionCount[i] = 0;
        }
    }

    public static void accumulatePrimitiveCount(int type, int count)
    {
        primitiveCount[type] += count;
    }
}
