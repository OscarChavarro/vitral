package vsdk.toolkit.render;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.environment.geometry.RayHit;

public final class TraceWorkspace {
    public static final int DEFAULT_MAX_RECURSION_LEVEL = 8;

    final RayHit traversalCandidateHit;
    final RayHit nearestHit;
    final RayHit shadowCandidateHit;
    final RayHit[] reflectionHits;
    final RayHit[] shadingHits;
    final ColorRgb[] reflectionColors;

    public TraceWorkspace()
    {
        this(DEFAULT_MAX_RECURSION_LEVEL);
    }

    public TraceWorkspace(int maxRecursionLevel)
    {
        traversalCandidateHit = new RayHit(RayHit.DETAIL_NONE, false);
        nearestHit = new RayHit(RayHit.DETAIL_NONE, false);
        shadowCandidateHit = new RayHit(RayHit.DETAIL_NONE, false);

        int levels = maxRecursionLevel + 1;
        reflectionHits = new RayHit[levels];
        shadingHits = new RayHit[levels];
        reflectionColors = new ColorRgb[levels];
        for ( int i = 0; i < levels; i++ ) {
            reflectionHits[i] = new RayHit(RayHit.DETAIL_NONE, false);
            shadingHits[i] = new RayHit();
            reflectionColors[i] = new ColorRgb();
        }
    }
}
