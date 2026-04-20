package vsdk.toolkit.common;

public final class RaytraceProfiling {
    private static final boolean ENABLED = Boolean.getBoolean("vsdk.raytrace.stats");

    private static long primaryRays;
    private static long shadowRays;
    private static long reflectionRays;
    private static long sceneTraversals;
    private static long objectIntersectionTests;
    private static long rayWithTCalls;
    private static long rayHitInstances;
    private static long hitInfoClones;
    private static long geometryDetailComputations;

    private RaytraceProfiling() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void recordPrimaryRay() {
        if (ENABLED) {
            primaryRays++;
        }
    }

    public static void recordShadowRay() {
        if (ENABLED) {
            shadowRays++;
        }
    }

    public static void recordReflectionRay() {
        if (ENABLED) {
            reflectionRays++;
        }
    }

    public static void recordSceneTraversal() {
        if (ENABLED) {
            sceneTraversals++;
        }
    }

    public static void recordObjectIntersectionTest() {
        if (ENABLED) {
            objectIntersectionTests++;
        }
    }

    public static void recordRayWithT() {
        if (ENABLED) {
            rayWithTCalls++;
        }
    }

    public static void recordRayHitInstance() {
        if (ENABLED) {
            rayHitInstances++;
        }
    }

    public static void recordHitInfoClone() {
        if (ENABLED) {
            hitInfoClones++;
        }
    }

    public static void recordGeometryDetailComputation() {
        if (ENABLED) {
            geometryDetailComputations++;
        }
    }

    public static void printSummary() {
        if (!ENABLED) {
            return;
        }

        long totalRays = primaryRays + shadowRays + reflectionRays;

        System.out.println("Ray statistics:");
        System.out.println("  Primary rays: " + primaryRays);
        System.out.println("  Shadow rays: " + shadowRays);
        System.out.println("  Reflection rays: " + reflectionRays);
        System.out.println("  Total rays cast: " + totalRays);
        System.out.println("  Scene traversals: " + sceneTraversals);
        System.out.println("  Object intersection tests: " + objectIntersectionTests);
        System.out.println("  Ray.withT calls: " + rayWithTCalls);
        System.out.println("  RayHit instances: " + rayHitInstances);
        System.out.println("  Hit info clones: " + hitInfoClones);
        System.out.println("  Geometry detail computations: " + geometryDetailComputations);
        if (sceneTraversals > 0) {
            System.out.println(
                "  Avg. object tests / traversal: " +
                ((double) objectIntersectionTests / (double) sceneTraversals)
            );
        }
    }
}
