package vsdk.toolkit.environment.geometry.element;

import java.util.ArrayList;

/** Caller-owned, reusable per worker/query; never share during concurrent queries. */
public class GeometryQueryWorkspace {
    private final RaySolidClassification solid;
    private final GeometryBounds bounds;
    private final RayHit hit;
    private final ArrayList<RaySolidClassification> scratch;
    private long scratchDepth;

    public GeometryQueryWorkspace()
    {
        solid = new RaySolidClassification(); bounds = new GeometryBounds(); hit = new RayHit();
        scratch = new ArrayList<>(); scratchDepth = 0;
    }
    public RaySolidClassification solidResult() { return solid; }
    public GeometryBounds boundsResult() { return bounds; }
    public RayHit hitResult() { return hit; }
    /** Acquired references remain stable across pool growth; release in reverse order. */
    public RaySolidClassification acquireSolidScratch()
    {
        if (scratchDepth == scratch.size()) {
            RaySolidClassification value = new RaySolidClassification();
            if (!scratch.add(value)) return null;
        }
        RaySolidClassification result = scratch.get((int)(scratchDepth++));
        result.clear();
        return result;
    }
    public boolean releaseSolidScratch(RaySolidClassification value)
    {
        if (scratchDepth == 0 || value != scratch.get((int)(scratchDepth - 1))) return false;
        value.clear();
        scratchDepth--;
        return true;
    }
    public void clear()
    {
        solid.clear(); bounds.setUnsupported(); hit.clear();
        for (long i = 0; i < scratchDepth; i++) scratch.get((int)i).clear();
        scratchDepth = 0;
    }
}
