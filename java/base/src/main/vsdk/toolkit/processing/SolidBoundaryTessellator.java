package vsdk.toolkit.processing;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.scene.SceneSolid;

/**
Drawable triangle mesh of the boundary of a body's solid inside a world
region: naive surface nets (Gibson 1998) over a grid of cubic cells whose
side is the region's longest side divided by the resolution. It works for any
body that classifies rays or points, such as quadrics, implicit solids,
height fields and CSG bodies, and asks the body for everything:
- node containment comes from one ray classification per grid line along x
  (one point query per node when the line cannot be classified);
- each grid edge whose ends differ crosses the boundary where the body's
  first hit from the outside end lies, with the body's surface normal;
- each cell holding crossings gets one vertex, the mean of its crossings,
  moved onto the boundary by a ray along the mean normal when that hit stays
  in the cell;
- each crossing edge gives the quad of the four cells around it, facing out.
The mesh is closed where the solid leaves the region. It is an approximation
of the declared resolution, not another definition of the solid: details
thinner than a cell may disappear or join, and a cell with several sheets
gets one vertex. The report says how many crossings and vertices lie on the
boundary found by the body's own ray queries.
*/
public class SolidBoundaryTessellator {
    private static final byte OUTSIDE_NODE = 0;
    private static final byte INSIDE_NODE = 1;
    /** Largest accepted resolution. */
    public static final int MAXIMUM_RESOLUTION = 512;

    private final SceneSolid body;
    private Vector3Dd origin;
    private double cellSize;
    private final long[] nodeCounts = new long[3];
    private final ArrayList<Byte> inside = new ArrayList<Byte>();
    @SuppressWarnings("unchecked")
    private final ArrayList<Integer>[] edgeCrossings = new ArrayList[] {
        new ArrayList<Integer>(), new ArrayList<Integer>(), new ArrayList<Integer>()};
    private final ArrayList<Vector3Dd> crossingPoints = new ArrayList<Vector3Dd>();
    private final ArrayList<Vector3Dd> crossingNormals = new ArrayList<Vector3Dd>();
    private final ArrayList<Integer> cellVertices = new ArrayList<Integer>();
    private final ArrayList<Vector3Dd> vertexPoints = new ArrayList<Vector3Dd>();
    private final ArrayList<Vector3Dd> vertexNormals = new ArrayList<Vector3Dd>();
    private final ArrayList<Integer> triangleIndices = new ArrayList<Integer>();
    private final SolidBoundaryTessellationReport report;

    private SolidBoundaryTessellator(SceneSolid body, SolidBoundaryTessellationReport report)
    {
        this.body = body;
        this.origin = new Vector3Dd();
        this.cellSize = 0;
        this.report = report;
        nodeCounts[0] = nodeCounts[1] = nodeCounts[2] = 0;
    }

    private long nodeIndex(long i, long j, long k)
    {
        return (k*nodeCounts[1] + j)*nodeCounts[0] + i;
    }

    private long cellIndex(long i, long j, long k)
    {
        return (k*(nodeCounts[1] - 1) + j)*(nodeCounts[0] - 1) + i;
    }

    private Vector3Dd nodePoint(long i, long j, long k)
    {
        return new Vector3Dd(origin.x() + i*cellSize, origin.y() + j*cellSize, origin.z() + k*cellSize);
    }

    // The outer layer of nodes lies outside the region and counts as outside, so
    // the mesh closes where the solid leaves the region.
    private boolean isBorderNode(long i, long j, long k)
    {
        return i == 0 || j == 0 || k == 0 || i == nodeCounts[0] - 1 ||
            j == nodeCounts[1] - 1 || k == nodeCounts[2] - 1;
    }

    private void classifyNodesByPoints(long j, long k)
    {
        report.pointClassifiedLines++;
        for (long i = 1; i < nodeCounts[0] - 1; i++) {
            final Vector3Dd p = nodePoint(i, j, k);
            GeometryContainment c = body.classifyPoint(p, 0);
            if (c == GeometryContainment.Unsupported) c = body.classifyPointCertified(p, 0);
            if (c == GeometryContainment.Inside) inside.set((int)nodeIndex(i, j, k), INSIDE_NODE);
            else if (c != GeometryContainment.Outside && c != GeometryContainment.Boundary) report.unknownNodes++;
        }
    }

    private boolean classifyNodes()
    {
        final long total = nodeCounts[0]*nodeCounts[1]*nodeCounts[2];
        inside.clear();
        inside.ensureCapacity((int)total);
        for (long n = 0; n < total; n++) inside.add(OUTSIDE_NODE);
        final double length = (nodeCounts[0] - 1)*cellSize;
        RaySolidClassification classification = new RaySolidClassification();
        for (long k = 1; k < nodeCounts[2] - 1; k++) {
            for (long j = 1; j < nodeCounts[1] - 1; j++) {
                final Ray line = Ray.fromAffine(nodePoint(0, j, k), new Vector3Dd(1, 0, 0));
                final RaySolidQueryStatus status =
                    body.classifyRay(line, new RayCrossingQuery(0, length), classification);
                if (status != RaySolidQueryStatus.Supported ||
                    classification.insideSegments.size() != classification.boundaries.size() + 1) {
                    classifyNodesByPoints(j, k);
                    continue;
                }
                int segment = 0;
                for (long i = 1; i < nodeCounts[0] - 1; i++) {
                    final double t = i*cellSize;
                    while (segment < classification.boundaries.size() &&
                        classification.boundaries.get(segment).t < t) segment++;
                    if (classification.insideSegments.get(segment).booleanValue())
                        inside.set((int)nodeIndex(i, j, k), INSIDE_NODE);
                }
            }
        }
        return true;
    }

    /** `point` and `normal` are one-element output holders. */
    private boolean firstHit(Vector3Dd from, Vector3Dd direction, double reach,
        Vector3Dd[] point, Vector3Dd[] normal)
    {
        final Ray ray = new Ray(from, direction);
        RayHit candidate = new RayHit();
        candidate.setStoreRay(false);
        candidate.resetForDistanceOnly();
        if (!body.doIntersectionFirstHit(ray, candidate)) return false;
        final double distance = candidate.getHitDistance();
        if (!(distance >= 0) || distance > reach) return false;
        final Ray hitRay = ray.withT(distance);
        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
        hit.setRay(hitRay);
        body.doExtraInformation(hitRay, distance, hit);
        point[0] = from.add(ray.getDirection().multiply(distance));
        normal[0] = hit.normal;
        final double size = normal[0].length();
        if (!(size > 0) || !Double.isFinite(size)) return false;
        normal[0] = normal[0].multiply(1.0/size);
        return true;
    }

    // Crossing on the edge from node (i, j, k) to its neighbour along the axis.
    private int crossing(int axis, long i, long j, long k)
    {
        final long edge = nodeIndex(i, j, k);
        if (edgeCrossings[axis].get((int)edge) >= 0) return edgeCrossings[axis].get((int)edge);
        long ni = i, nj = j, nk = k;
        if (axis == 0) ni++; else if (axis == 1) nj++; else nk++;
        final boolean firstInside = inside.get((int)edge) == INSIDE_NODE;
        final Vector3Dd a = nodePoint(i, j, k), b = nodePoint(ni, nj, nk);
        final Vector3Dd outer = firstInside ? b : a, inner = firstInside ? a : b;
        final boolean outerIsBorder = firstInside ? isBorderNode(ni, nj, nk) : isBorderNode(i, j, k);
        final Vector3Dd inward = inner.subtract(outer).multiply(1.0/cellSize);
        Vector3Dd[] point = {new Vector3Dd()}, normal = {new Vector3Dd()};
        if (firstHit(outer, inward, cellSize*(1 + 1e-9), point, normal)) {
            if (normal[0].dotProduct(inward) > 0) normal[0] = normal[0].multiply(-1);
            report.exactCrossings++;
        }
        else if (outerIsBorder) {
            // The solid continues beyond the region: close it at the inner node.
            point[0] = inner;
            normal[0] = inward.multiply(-1);
            report.regionCrossings++;
        }
        else {
            point[0] = a.add(b).multiply(0.5);
            normal[0] = inward.multiply(-1);
            report.midpointCrossings++;
        }
        final int index = crossingPoints.size();
        crossingPoints.add(point[0]);
        crossingNormals.add(normal[0]);
        edgeCrossings[axis].set((int)edge, index);
        return index;
    }

    private int cellVertex(long i, long j, long k)
    {
        final long cell = cellIndex(i, j, k);
        if (cellVertices.get((int)cell) >= 0) return cellVertices.get((int)cell);
        Vector3Dd sum = new Vector3Dd(0, 0, 0), normalSum = new Vector3Dd(0, 0, 0);
        int count = 0;
        for (int axis = 0; axis < 3; axis++) {
            for (int a = 0; a < 2; a++) {
                for (int b = 0; b < 2; b++) {
                    long ei = i, ej = j, ek = k;
                    if (axis == 0) { ej += a; ek += b; }
                    else if (axis == 1) { ek += a; ei += b; }
                    else { ei += a; ej += b; }
                    long fi = ei, fj = ej, fk = ek;
                    if (axis == 0) fi++; else if (axis == 1) fj++; else fk++;
                    if (inside.get((int)nodeIndex(ei, ej, ek)).byteValue() ==
                        inside.get((int)nodeIndex(fi, fj, fk)).byteValue()) continue;
                    final int c = crossing(axis, ei, ej, ek);
                    sum = sum.add(crossingPoints.get(c));
                    normalSum = normalSum.add(crossingNormals.get(c));
                    count++;
                }
            }
        }
        Vector3Dd point = sum.multiply(1.0/count);
        Vector3Dd normal = normalSum;
        final double size = normal.length();
        boolean projected = false;
        if (size > 1e-12) {
            normal = normal.multiply(1.0/size);
            final double reach = cellSize*Math.sqrt(3.0);
            final Vector3Dd from = point.add(normal.multiply(reach/2));
            Vector3Dd[] hitPoint = {new Vector3Dd()}, hitNormal = {new Vector3Dd()};
            if (firstHit(from, normal.multiply(-1), reach, hitPoint, hitNormal)) {
                final Vector3Dd low = nodePoint(i, j, k);
                final double margin = 1e-9*cellSize;
                if (hitPoint[0].x() >= low.x() - margin && hitPoint[0].x() <= low.x() + cellSize + margin &&
                    hitPoint[0].y() >= low.y() - margin && hitPoint[0].y() <= low.y() + cellSize + margin &&
                    hitPoint[0].z() >= low.z() - margin && hitPoint[0].z() <= low.z() + cellSize + margin) {
                    if (hitNormal[0].dotProduct(normal) < 0) hitNormal[0] = hitNormal[0].multiply(-1);
                    point = hitPoint[0];
                    normal = hitNormal[0];
                    projected = true;
                }
            }
        }
        else normal = new Vector3Dd(0, 0, 1);
        if (projected) report.boundaryVertices++;
        else report.cellVertices++;
        final int index = vertexPoints.size();
        vertexPoints.add(point);
        vertexNormals.add(normal);
        cellVertices.set((int)cell, index);
        return index;
    }

    // Every edge along the axis whose ends differ gives the quad of the four cells
    // around it, wound to face from the inside end to the outside end.
    private void emitQuads(int axis)
    {
        final int b = (axis + 1) % 3, c = (axis + 2) % 3;
        long[] index = new long[3];
        for (index[2] = 0; index[2] < nodeCounts[2]; index[2]++) {
            for (index[1] = 0; index[1] < nodeCounts[1]; index[1]++) {
                for (index[0] = 0; index[0] < nodeCounts[0]; index[0]++) {
                    if (index[axis] + 1 >= nodeCounts[axis]) continue;
                    long[] next = {index[0], index[1], index[2]};
                    next[axis]++;
                    final boolean here = inside.get((int)nodeIndex(index[0], index[1], index[2])) == INSIDE_NODE;
                    final boolean there = inside.get((int)nodeIndex(next[0], next[1], next[2])) == INSIDE_NODE;
                    if (here == there) continue;
                    // Border nodes are outside, so both ends are interior across b and c.
                    int[] quad = new int[4];
                    final int[][] offsets = {{-1, -1}, {0, -1}, {0, 0}, {-1, 0}};
                    for (int q = 0; q < 4; q++) {
                        long[] cell = {index[0], index[1], index[2]};
                        cell[b] += offsets[q][0];
                        cell[c] += offsets[q][1];
                        quad[q] = cellVertex(cell[0], cell[1], cell[2]);
                    }
                    // The (b, c) cycle above turns around +axis.
                    if (here) {
                        triangleIndices.add(quad[0]); triangleIndices.add(quad[1]); triangleIndices.add(quad[2]);
                        triangleIndices.add(quad[0]); triangleIndices.add(quad[2]); triangleIndices.add(quad[3]);
                    }
                    else {
                        triangleIndices.add(quad[0]); triangleIndices.add(quad[2]); triangleIndices.add(quad[1]);
                        triangleIndices.add(quad[0]); triangleIndices.add(quad[3]); triangleIndices.add(quad[2]);
                    }
                }
            }
        }
    }

    private TriangleMesh buildMesh()
    {
        TriangleMesh mesh = new TriangleMesh();
        final int vertexCount = vertexPoints.size();
        final int triangleCount = triangleIndices.size()/3;
        mesh.initVertexPositionsArray(vertexCount);
        mesh.initVertexNormalsArray();
        double[] positions = mesh.getVertexPositions();
        double[] normals = mesh.getVertexNormals();
        for (int v = 0; v < vertexCount; v++) {
            positions[v*3] = vertexPoints.get(v).x();
            positions[v*3 + 1] = vertexPoints.get(v).y();
            positions[v*3 + 2] = vertexPoints.get(v).z();
            normals[v*3] = vertexNormals.get(v).x();
            normals[v*3 + 1] = vertexNormals.get(v).y();
            normals[v*3 + 2] = vertexNormals.get(v).z();
        }
        mesh.initTriangleArrays(triangleCount);
        int[] indices = mesh.getTriangleIndexes();
        double[] faceNormals = mesh.getTriangleNormals();
        for (int t = 0; t < triangleCount; t++) {
            final int v0 = triangleIndices.get(t*3), v1 = triangleIndices.get(t*3 + 1), v2 = triangleIndices.get(t*3 + 2);
            indices[t*3] = v0;
            indices[t*3 + 1] = v1;
            indices[t*3 + 2] = v2;
            Vector3Dd n = vertexPoints.get(v1).subtract(vertexPoints.get(v0)).crossProduct(
                vertexPoints.get(v2).subtract(vertexPoints.get(v0)));
            final double size = n.length();
            n = size > 0 ? n.multiply(1.0/size) : vertexNormals.get(v0);
            faceNormals[t*3] = n.x();
            faceNormals[t*3 + 1] = n.y();
            faceNormals[t*3 + 2] = n.z();
        }
        return mesh;
    }

    /**
    Returns a new mesh in world coordinates, or null when the region is not
    finite and nonempty or the resolution is outside 1..MAXIMUM_RESOLUTION.
    An empty mesh means no boundary was found.
    */
    public static TriangleMesh tessellate(SceneSolid body, GeometryBounds region,
        int resolution, SolidBoundaryTessellationReport report)
    {
        report.clear();
        if (region.getKind() != GeometryBounds.Kind.Finite) return null;
        if (resolution < 1 || resolution > MAXIMUM_RESOLUTION) return null;
        final Vector3Dd low = region.getMinimum(), high = region.getMaximum();
        final double[] extent = {high.x() - low.x(), high.y() - low.y(), high.z() - low.z()};
        double longest = 0;
        for (int a = 0; a < 3; a++) {
            if (!Double.isFinite(extent[a]) || extent[a] < 0) return null;
            if (extent[a] > longest) longest = extent[a];
        }
        if (!(longest > 0)) return null;

        SolidBoundaryTessellator tessellator = new SolidBoundaryTessellator(body, report);
        tessellator.cellSize = longest/resolution;
        // Cells covering the region, plus one layer of outside nodes on each side.
        for (int a = 0; a < 3; a++) {
            long cells = (long)Math.ceil(extent[a]/tessellator.cellSize - 1e-9);
            if (cells < 1) cells = 1;
            tessellator.nodeCounts[a] = cells + 3;
            report.nodeCounts[a] = tessellator.nodeCounts[a];
        }
        report.cellSize = tessellator.cellSize;
        tessellator.origin = new Vector3Dd(low.x() - tessellator.cellSize, low.y() - tessellator.cellSize,
            low.z() - tessellator.cellSize);
        tessellator.classifyNodes();

        final long nodes = tessellator.nodeCounts[0]*tessellator.nodeCounts[1]*tessellator.nodeCounts[2];
        for (int a = 0; a < 3; a++) {
            tessellator.edgeCrossings[a].ensureCapacity((int)nodes);
            for (long n = 0; n < nodes; n++) tessellator.edgeCrossings[a].add(-1);
        }
        final long cells = (tessellator.nodeCounts[0] - 1)*(tessellator.nodeCounts[1] - 1)*
            (tessellator.nodeCounts[2] - 1);
        tessellator.cellVertices.ensureCapacity((int)cells);
        for (long c = 0; c < cells; c++) tessellator.cellVertices.add(-1);
        for (int a = 0; a < 3; a++) tessellator.emitQuads(a);
        report.triangles = tessellator.triangleIndices.size()/3;
        return tessellator.buildMesh();
    }
}
