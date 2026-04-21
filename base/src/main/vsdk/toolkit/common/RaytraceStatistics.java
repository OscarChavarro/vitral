package vsdk.toolkit.common;

import java.util.concurrent.atomic.LongAdder;

public final class RaytraceStatistics {
    private static final boolean ENABLED = Boolean.getBoolean("vsdk.raytrace.stats");

    private static final LongAdder primaryRays = new LongAdder();
    private static final LongAdder shadowRays = new LongAdder();
    private static final LongAdder reflectionRays = new LongAdder();
    private static final LongAdder sceneTraversals = new LongAdder();
    private static final LongAdder objectIntersectionTests = new LongAdder();
    private static final LongAdder rayWithTCalls = new LongAdder();
    private static final LongAdder rayHitInstances = new LongAdder();
    private static final LongAdder hitInfoClones = new LongAdder();
    private static final LongAdder geometryDetailComputations = new LongAdder();

    private RaytraceStatistics() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void recordPrimaryRay() {
        if (ENABLED) {
            primaryRays.increment();
        }
    }

    public static void recordShadowRay() {
        if (ENABLED) {
            shadowRays.increment();
        }
    }

    public static void recordReflectionRay() {
        if (ENABLED) {
            reflectionRays.increment();
        }
    }

    public static void recordSceneTraversal() {
        if (ENABLED) {
            sceneTraversals.increment();
        }
    }

    public static void recordObjectIntersectionTest() {
        if (ENABLED) {
            objectIntersectionTests.increment();
        }
    }

    public static void recordRayWithT() {
        if (ENABLED) {
            rayWithTCalls.increment();
        }
    }

    public static void recordRayHitInstance() {
        if (ENABLED) {
            rayHitInstances.increment();
        }
    }

    public static void recordHitInfoClone() {
        if (ENABLED) {
            hitInfoClones.increment();
        }
    }

    public static void recordGeometryDetailComputation() {
        if (ENABLED) {
            geometryDetailComputations.increment();
        }
    }

    public static void printSummary() {
        if (!ENABLED) {
            return;
        }

        long primaryRaysSnapshot = primaryRays.sum();
        long shadowRaysSnapshot = shadowRays.sum();
        long reflectionRaysSnapshot = reflectionRays.sum();
        long sceneTraversalsSnapshot = sceneTraversals.sum();
        long objectIntersectionTestsSnapshot = objectIntersectionTests.sum();
        long rayWithTCallsSnapshot = rayWithTCalls.sum();
        long rayHitInstancesSnapshot = rayHitInstances.sum();
        long hitInfoClonesSnapshot = hitInfoClones.sum();
        long geometryDetailComputationsSnapshot = geometryDetailComputations.sum();
        long totalRays =
            primaryRaysSnapshot + shadowRaysSnapshot + reflectionRaysSnapshot;

        System.out.println("Ray statistics:");
        System.out.println("  Primary rays: " + primaryRaysSnapshot);
        System.out.println("  Shadow rays: " + shadowRaysSnapshot);
        System.out.println("  Reflection rays: " + reflectionRaysSnapshot);
        System.out.println("  Total rays cast: " + totalRays);
        System.out.println("  Scene traversals: " + sceneTraversalsSnapshot);
        System.out.println("  Object intersection tests: " + objectIntersectionTestsSnapshot);
        System.out.println("  Ray.withT calls: " + rayWithTCallsSnapshot);
        System.out.println("  RayHit instances: " + rayHitInstancesSnapshot);
        System.out.println("  Hit info clones: " + hitInfoClonesSnapshot);
        System.out.println("  Geometry detail computations: " + geometryDetailComputationsSnapshot);
        if (sceneTraversalsSnapshot > 0) {
            System.out.println(
                "  Avg. object tests / traversal: " +
                ((double) objectIntersectionTestsSnapshot / (double) sceneTraversalsSnapshot)
            );
        }
    }
}
