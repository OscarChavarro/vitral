package vsdk.toolkit.processing;

/** Resolution and accuracy of a SolidBoundaryTessellator mesh. */
public class SolidBoundaryTessellationReport {
    /** Side of the cubic cells of the sampling grid, in world units. */
    public double cellSize;
    /** Grid nodes along x, y and z. */
    public final long[] nodeCounts = new long[3];
    /**
    Grid lines whose ray classification failed; their nodes were
    classified one by one.
    */
    public long pointClassifiedLines;
    /** Nodes whose containment stayed unknown; they count as outside. */
    public long unknownNodes;
    /**
    Edge crossings found by a ray hit on the body; the others are the edge
    midpoints.
    */
    public long exactCrossings;
    public long midpointCrossings;
    /**
    Mesh vertices moved onto the body's boundary by a ray hit; the others
    stay at the mean of their cell's crossings.
    */
    public long boundaryVertices;
    public long cellVertices;
    /**
    Crossings on the region's sides, where the mesh closes a solid that
    continues beyond the region.
    */
    public long regionCrossings;
    public long triangles;

    public SolidBoundaryTessellationReport()
    {
        clear();
    }

    public void clear()
    {
        cellSize = 0;
        nodeCounts[0] = nodeCounts[1] = nodeCounts[2] = 0;
        pointClassifiedLines = 0;
        unknownNodes = 0;
        exactCrossings = 0;
        midpointCrossings = 0;
        boundaryVertices = 0;
        cellVertices = 0;
        regionCrossings = 0;
        triangles = 0;
    }
}
