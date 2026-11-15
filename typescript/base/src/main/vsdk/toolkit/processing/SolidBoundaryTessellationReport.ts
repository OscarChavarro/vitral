// Resolution and accuracy of a SolidBoundaryTessellator mesh.
export class SolidBoundaryTessellationReport {
    // Side of the cubic cells of the sampling grid, in world units.
    public cellSize = 0;
    // Grid nodes along x, y and z.
    public readonly nodeCounts: number[] = [0, 0, 0];
    // Grid lines whose ray classification failed; their nodes were
    // classified one by one.
    public pointClassifiedLines = 0;
    // Nodes whose containment stayed unknown; they count as outside.
    public unknownNodes = 0;
    // Edge crossings found by a ray hit on the body; the others are the edge
    // midpoints.
    public exactCrossings = 0;
    public midpointCrossings = 0;
    // Mesh vertices moved onto the body's boundary by a ray hit; the others
    // stay at the mean of their cell's crossings.
    public boundaryVertices = 0;
    public cellVertices = 0;
    // Crossings on the region's sides, where the mesh closes a solid that
    // continues beyond the region.
    public regionCrossings = 0;
    public triangles = 0;

    public constructor() {
        this.clear();
    }

    public clear(): void {
        this.cellSize = 0;
        this.nodeCounts[0] = this.nodeCounts[1] = this.nodeCounts[2] = 0;
        this.pointClassifiedLines = 0;
        this.unknownNodes = 0;
        this.exactCrossings = 0;
        this.midpointCrossings = 0;
        this.boundaryVertices = 0;
        this.cellVertices = 0;
        this.regionCrossings = 0;
        this.triangles = 0;
    }
}
