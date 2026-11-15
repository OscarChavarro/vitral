package vsdk.toolkit.environment.geometry.surface;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayBoundary;
import vsdk.toolkit.environment.geometry.element.RayCrossing;
import vsdk.toolkit.environment.geometry.element.RayCrossingKind;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayCrossingSink;
import vsdk.toolkit.environment.geometry.element.RayCrossingSupport;
import vsdk.toolkit.environment.geometry.element.RayFirstHitStatus;
import vsdk.toolkit.environment.geometry.element.RayHit;

/**
Open bicubic Bezier patch rendered as the flat triangles of a subdivision of
its 4 x 4 control net: the PovRay bicubic_patch. Control point (i, j) has
parameters u = i/3 and v = j/3. The geometry is the triangulation chosen by
the subdivision, uSteps, vSteps and flatness, not the exact Bezier surface;
for that see ParametricBiCubicPatch. Being open, the patch has no inside:
ray classification and containment are Unsupported. Ray crossings are
Unclassified and their surface index is the hit's position in intersect
order; typed normals face the u x v side of their triangle. Bounds are the
control net's box, which holds the patch.
*/
public class SubdividedBezierPatch extends Surface {
    // Hits a single ray query can report.
    public static final int MAXIMUM_HITS = 32;

    // Degenerate triangle, derivative and direction threshold.
    public static final double DEGENERACY_EPSILON = 1.0e-10;

    // Depth below which PovRay ignores a hit.
    public static final double POVRAY_MINIMUM_DEPTH = 0.001;

    private BezierPatchSubdivision subdivision;

    private int uSteps;

    private int vSteps;

    private double flatness;

    private Vector3Dd[][] controlPoints = new Vector3Dd[4][4];

    private Vector3Dd boundingCenter;

    private double boundingRadiusSquared;

    // Precomputed grids, (uSteps + 1) rows: grid points with vSteps + 1
    // columns; triangle planes and offsets with 2*(vSteps + 1) columns.
    private final ArrayList<Vector3Dd> gridPoints = new ArrayList<Vector3Dd>();

    private final ArrayList<Vector3Dd> gridPlaneNormals = new ArrayList<Vector3Dd>();

    private final ArrayList<Double> gridPlaneOffsets = new ArrayList<Double>();

    private final ArrayList<Vector3Dd> gridSmoothNormals = new ArrayList<Vector3Dd>();

    private final ArrayList<SubdividedBezierPatchNode> nodes = new ArrayList<SubdividedBezierPatchNode>();

    private static void sphereAround(int count, Vector3Dd[] points, Vector3Dd[] center, double[] radiusSquared) {
    double xc = 0;
    double yc = 0;
    double zc = 0;
    for (int i = 0; i < count; i++) {
        xc += points[i].x();
        yc += points[i].y();
        zc += points[i].z();
    }
    xc /= (double)count;
    yc /= (double)count;
    zc /= (double)count;
    double r0 = 0.0;
    for (int i = 0; i < count; i++) {
        final double x0 = points[i].x() - xc;
        final double y0 = points[i].y() - yc;
        final double z0 = points[i].z() - zc;
        final double r1 = x0 * x0 + y0 * y0 + z0 * z0;
        if (r1 > r0) {
            r0 = r1;
        }
    }
    center[0] = new Vector3Dd(xc, yc, zc);
    radiusSquared[0] = r0;
}

    private static void netSphere(Vector3Dd[][] net, Vector3Dd[] center, double[] radiusSquared) {
    double xc = 0;
    double yc = 0;
    double zc = 0;
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            xc += net[i][j].x();
            yc += net[i][j].y();
            zc += net[i][j].z();
        }
    }
    xc /= 16.0;
    yc /= 16.0;
    zc /= 16.0;
    double r0 = 0.0;
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            final double x0 = net[i][j].x() - xc;
            final double y0 = net[i][j].y() - yc;
            final double z0 = net[i][j].z() - zc;
            final double r1 = x0 * x0 + y0 * y0 + z0 * z0;
            if (r1 > r0) {
                r0 = r1;
            }
        }
    }
    center[0] = new Vector3Dd(xc, yc, zc);
    radiusSquared[0] = r0;
}

    private static boolean trianglePlane(Vector3Dd v1, Vector3Dd v2, Vector3Dd v3, Vector3Dd[] normal, double[] offset) {
    final Vector3Dd edge1 = v1.subtract(v2);
    final Vector3Dd edge2 = v3.subtract(v2);
    normal[0] = edge1.crossProduct(edge2);
    final double length = normal[0].length();
    if (length < DEGENERACY_EPSILON) {
        normal[0] = new Vector3Dd(1.0, 0.0, 0.0);
        offset[0] = -1.0 * v1.x();
        return false;
    }
    normal[0] = new Vector3Dd(normal[0].x() / length, normal[0].y() / length, normal[0].z() / length);
    offset[0] = normal[0].dotProduct(v1);
    offset[0] = 0.0 - offset[0];
    return true;
}

    private static double planeDistance(Vector3Dd p, Vector3Dd n, double d) {
    double temp1 = p.dotProduct(n);
    temp1 += d;
    final double temp2 = n.length();
    if (Math.abs(temp2) < DEGENERACY_EPSILON) {
        return 0;
    }
    return temp1 / temp2;
}

    private static boolean rayMeetsSphere(Vector3Dd origin, Vector3Dd direction, Vector3Dd center, double radiusSquared) {
    final double x = center.x() - origin.x();
    final double y = center.y() - origin.y();
    final double z = center.z() - origin.z();
    final double dist1 = x * x + y * y + z * z;
    if (dist1 < radiusSquared) {
        return true;
    }
    final double projection = x * direction.x() + y * direction.y() + z * direction.z();
    if (projection <= 0.0) {
        return false;
    }
    final double directionLengthSquared = direction.dotProduct(direction);
    if (directionLengthSquared <= DEGENERACY_EPSILON) {
        return false;
    }
    return dist1 - (projection * projection) / directionLengthSquared < radiusSquared;
}

    private static void splitLeftRight(Vector3Dd[][] net, Vector3Dd[][] left, Vector3Dd[][] right) {
    Vector3Dd[] temp1 = new Vector3Dd[4];
    Vector3Dd[] temp2 = new Vector3Dd[4];
    for (int i = 0; i < 4; i++) {
        temp1[0] = net[i][0];
        temp1[1] = net[i][0].midpoint(net[i][1]);
        final Vector3Dd half = net[i][1].midpoint(net[i][2]);
        temp1[2] = temp1[1].midpoint(half);
        temp2[2] = net[i][2].midpoint(net[i][3]);
        temp2[1] = half.midpoint(temp2[2]);
        temp1[3] = temp1[2].midpoint(temp2[1]);
        temp2[0] = temp1[3];
        temp2[3] = net[i][3];
        for (int j = 0; j < 4; j++) {
            left[i][j] = temp1[j];
            right[i][j] = temp2[j];
        }
    }
}

    private static void splitUpDown(Vector3Dd[][] net, Vector3Dd[][] top, Vector3Dd[][] bottom) {
    Vector3Dd[] temp1 = new Vector3Dd[4];
    Vector3Dd[] temp2 = new Vector3Dd[4];
    for (int i = 0; i < 4; i++) {
        temp1[0] = net[0][i];
        temp1[1] = net[0][i].midpoint(net[1][i]);
        final Vector3Dd half = net[1][i].midpoint(net[2][i]);
        temp1[2] = temp1[1].midpoint(half);
        temp2[2] = net[2][i].midpoint(net[3][i]);
        temp2[1] = half.midpoint(temp2[2]);
        temp1[3] = temp1[2].midpoint(temp2[1]);
        temp2[0] = temp1[3];
        temp2[3] = net[3][i];
        for (int j = 0; j < 4; j++) {
            bottom[j][i] = temp1[j];
            top[j][i] = temp2[j];
        }
    }
}

    private static double netFlatness(Vector3Dd[][] net) {
    Vector3Dd[] vertices = new Vector3Dd[3];
    vertices[0] = net[0][0];
    vertices[1] = net[0][3];
    double temp1 = vertices[0].subtract(vertices[1]).length();
    if (Math.abs(temp1) < DEGENERACY_EPSILON) {
        vertices[1] = net[3][3];
        temp1 = vertices[0].subtract(vertices[1]).length();
        if (Math.abs(temp1) < DEGENERACY_EPSILON) {
            return -1.0;
        }
        vertices[2] = net[3][0];
        temp1 = vertices[0].subtract(vertices[1]).length();
        if (Math.abs(temp1) < DEGENERACY_EPSILON) {
            return -1.0;
        }
        temp1 = vertices[1].subtract(vertices[2]).length();
        if (Math.abs(temp1) < DEGENERACY_EPSILON) {
            return -1.0;
        }
    } else {
        vertices[2] = net[3][0];
        temp1 = vertices[0].subtract(vertices[1]).length();
        if (Math.abs(temp1) < DEGENERACY_EPSILON) {
            vertices[2] = net[3][3];
            temp1 = vertices[0].subtract(vertices[2]).length();
            if (Math.abs(temp1) < DEGENERACY_EPSILON) {
                return -1.0;
            }
            temp1 = vertices[1].subtract(vertices[2]).length();
            if (Math.abs(temp1) < DEGENERACY_EPSILON) {
                return -1.0;
            }
        } else {
            temp1 = vertices[1].subtract(vertices[2]).length();
            if (Math.abs(temp1) < DEGENERACY_EPSILON) {
                return -1.0;
            }
        }
    }
    Vector3Dd[] n = {new Vector3Dd()};
    double[] d = {0};
    if (trianglePlane(vertices[0], vertices[1], vertices[2], n, d)) {
        double dist = 0.0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                temp1 = Math.abs(planeDistance(net[i][j], n[0], d[0]));
                if (temp1 > dist) {
                    dist = temp1;
                }
            }
        }
        return dist;
    }
    return -1.0;
}

    private boolean isFlatEnough(Vector3Dd[][] net) {
    final double dist = netFlatness(net);
    if (dist < 0.0) {
        return false;
    }
    return dist < flatness;
}

    private boolean intersectTriangle(Vector3Dd origin, Vector3Dd direction, double minimumDepth, boolean orientNormal, Vector3Dd v1, Vector3Dd v2, Vector3Dd v3, Vector3Dd n, double d, Vector3Dd n1, Vector3Dd n2, Vector3Dd n3, double[] depth, Vector3Dd[] point, Vector3Dd[] normal) {
    double s = direction.dotProduct(n);
    if (s == 0.0) {
        return false;
    }
    double t = origin.dotProduct(n);
    depth[0] = 0.0 - (d + t) / s;
    if (depth[0] < minimumDepth) {
        return false;
    }
    final Vector3Dd ip = direction.multiply(depth[0]).add(origin);
    double x = Math.abs(n.x());
    double y = Math.abs(n.y());
    double z = Math.abs(n.z());
    double x1, y1, x2, y2, x3, y3;
    if (x > y) {
        if (x > z) {
            x1 = v1.y() - ip.y(); y1 = v1.z() - ip.z();
            x2 = v2.y() - ip.y(); y2 = v2.z() - ip.z();
            x3 = v3.y() - ip.y(); y3 = v3.z() - ip.z();
        } else {
            x1 = v1.x() - ip.x(); y1 = v1.y() - ip.y();
            x2 = v2.x() - ip.x(); y2 = v2.y() - ip.y();
            x3 = v3.x() - ip.x(); y3 = v3.y() - ip.y();
        }
    } else if (y > z) {
        x1 = v1.x() - ip.x(); y1 = v1.z() - ip.z();
        x2 = v2.x() - ip.x(); y2 = v2.z() - ip.z();
        x3 = v3.x() - ip.x(); y3 = v3.z() - ip.z();
    } else {
        x1 = v1.x() - ip.x(); y1 = v1.y() - ip.y();
        x2 = v2.x() - ip.x(); y2 = v2.y() - ip.y();
        x3 = v3.x() - ip.x(); y3 = v3.y() - ip.y();
    }
    int crossings = 0;
    int signHolder = (y1 < 0) ? -1 : 1;
    int nextSign = (y2 < 0) ? -1 : 1;
    if (signHolder != nextSign) {
        if (x1 > 0.0) {
            if (x2 > 0.0) {
                crossings++;
            } else if ((x1 - y1 * (x2 - x1) / (y2 - y1)) > 0.0) {
                crossings++;
            }
        } else if (x2 > 0.0) {
            if ((x1 - y1 * (x2 - x1) / (y2 - y1)) > 0.0) {
                crossings++;
            }
        }
        signHolder = nextSign;
    }
    nextSign = (y3 < 0) ? -1 : 1;
    if (signHolder != nextSign) {
        if (x2 > 0.0) {
            if (x3 > 0.0) {
                crossings++;
            } else if ((x2 - y2 * (x3 - x2) / (y3 - y2)) > 0.0) {
                crossings++;
            }
        } else if (x3 > 0.0) {
            if ((x2 - y2 * (x3 - x2) / (y3 - y2)) > 0.0) {
                crossings++;
            }
        }
        signHolder = nextSign;
    }
    nextSign = (y1 < 0) ? -1 : 1;
    if (signHolder != nextSign) {
        if (x3 > 0.0) {
            if (x1 > 0.0) {
                crossings++;
            } else if ((x3 - y3 * (x1 - x3) / (y1 - y3)) > 0.0) {
                crossings++;
            }
        } else if (x1 > 0.0) {
            if ((x3 - y3 * (x1 - x3) / (y1 - y3)) > 0.0) {
                crossings++;
            }
        }
    }
    if (crossings != 1) {
        return false;
    }
    point[0] = ip;
    if (subdivision != BezierPatchSubdivision.SmoothGrid) {
        normal[0] = n;
        return true;
    }
    // Normal interpolated from the corner normals n1, n2, n3.
    Vector3Dd tempV1 = v2.subtract(v3).normalizedFast();
    Vector3Dd tempV2 = v1.subtract(v3);
    final double proj = tempV2.dotProduct(tempV1);
    tempV1 = tempV1.multiply(proj);
    Vector3Dd perp = tempV1.subtract(tempV2).normalizedFast();
    double mu = tempV2.dotProduct(perp);
    mu = -1.0 / mu;
    perp = perp.multiply(mu);
    tempV1 = ip.subtract(v1);
    s = tempV1.dotProduct(perp);
    if (s < DEGENERACY_EPSILON) {
        normal[0] = n1;
        if (orientNormal && normal[0].dotProduct(n) < 0) normal[0] = normal[0].multiply(-1.0);
        return true;
    }
    tempV1 = v3.subtract(v2);
    x = Math.abs(tempV1.x());
    y = Math.abs(tempV1.y());
    z = Math.abs(tempV1.z());
    if (x > y) {
        if (x > z) {
            t = (tempV1.x() / s + v1.x() - v2.x()) / tempV1.x();
        } else {
            t = (tempV1.z() / s + v1.z() - v2.z()) / tempV1.z();
        }
    } else if (y > z) {
        t = (tempV1.y() / s + v1.y() - v2.y()) / tempV1.y();
    } else {
        t = (tempV1.z() / s + v1.z() - v2.z()) / tempV1.z();
    }
    tempV1 = n2.subtract(n1).multiply(s).add(n1);
    tempV2 = n3.subtract(n1).multiply(s).add(n1);
    normal[0] = tempV2.subtract(tempV1).multiply(t).add(tempV1).normalizedFast();
    if (orientNormal) {
        // The interpolation can divide by zero; then use the triangle normal[0].
        if (!Double.isFinite(normal[0].x()) || !Double.isFinite(normal[0].y()) ||
            !Double.isFinite(normal[0].z())) normal[0] = n;
        if (normal[0].dotProduct(n) < 0) normal[0] = normal[0].multiply(-1.0);
    }
    return true;
}

    private long buildTree(Vector3Dd[][] net, int depth) {
    final long index = nodes.size();
    nodes.add(new SubdividedBezierPatchNode());
    Vector3Dd[] center = {new Vector3Dd()};
    double[] radiusSquared = {0};
    netSphere(net, center, radiusSquared);
    nodes.get((int)index).center = center[0];
    nodes.get((int)index).radiusSquared = radiusSquared[0];
    Vector3Dd[][] lowerLeft = new Vector3Dd[4][4];
    Vector3Dd[][] lowerRight = new Vector3Dd[4][4];
    Vector3Dd[][] upperLeft = new Vector3Dd[4][4];
    Vector3Dd[][] upperRight = new Vector3Dd[4][4];
    long[] children = {-1, -1, -1, -1};
    int count = 0;
    if (isFlatEnough(net) || (depth >= uSteps && depth >= vSteps)) {
        nodes.get((int)index).leaf = true;
        nodes.get((int)index).corners[0] = net[0][0];
        nodes.get((int)index).corners[1] = net[0][3];
        nodes.get((int)index).corners[2] = net[3][3];
        nodes.get((int)index).corners[3] = net[3][0];
        return index;
    }
    if (depth >= uSteps) {
        splitUpDown(net, lowerLeft, upperLeft);
        children[0] = buildTree(lowerLeft, depth + 1);
        children[1] = buildTree(upperLeft, depth + 1);
        count = 2;
    } else if (depth >= vSteps) {
        splitLeftRight(net, lowerLeft, lowerRight);
        children[0] = buildTree(lowerLeft, depth + 1);
        children[1] = buildTree(lowerRight, depth + 1);
        count = 2;
    } else {
        splitLeftRight(net, lowerLeft, lowerRight);
        splitUpDown(lowerLeft, lowerLeft, upperLeft);
        splitUpDown(lowerRight, lowerRight, upperRight);
        children[0] = buildTree(lowerLeft, depth + 1);
        children[1] = buildTree(upperLeft, depth + 1);
        children[2] = buildTree(lowerRight, depth + 1);
        children[3] = buildTree(upperRight, depth + 1);
        count = 4;
    }
    nodes.get((int)index).leaf = false;
    nodes.get((int)index).childCount = count;
    for (int i = 0; i < 4; i++) nodes.get((int)index).children[i] = children[i];
    return index;
}

    private void precompute() {
    Vector3Dd[] netPoints = new Vector3Dd[16];
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            netPoints[4 * i + j] = controlPoints[i][j];
        }
    }
    Vector3Dd[] boundingCenterH = {boundingCenter};
    double[] boundingRadiusSquaredH = {boundingRadiusSquared};
    sphereAround(16, netPoints, boundingCenterH, boundingRadiusSquaredH);
    boundingCenter = boundingCenterH[0];
    boundingRadiusSquared = boundingRadiusSquaredH[0];
    if (subdivision == BezierPatchSubdivision.UniformGrid ||
        subdivision == BezierPatchSubdivision.Adaptive) {
        return;
    }
    if (subdivision == BezierPatchSubdivision.AdaptiveTree) {
        buildTree(controlPoints, 0);
        return;
    }
    final double deltaU = 1.0 / (double)uSteps;
    final double deltaV = 1.0 / (double)vSteps;
    for (long i = 0; i < (long)(uSteps + 1)*(vSteps + 1); i++) gridPoints.add(new Vector3Dd());
    for (long i = 0; i < (long)(uSteps + 1)*2*(vSteps + 1); i++) {
        gridPlaneNormals.add(new Vector3Dd());
        gridPlaneOffsets.add(0.0);
    }
    // The PovRay fill order, kept because later writes overwrite earlier
    // ones with differently rounded parameters.
    for (int i = 0; i <= uSteps; i++) {
        final double u = (double)i / (double)uSteps;
        for (int j = 0; j < vSteps; j++) {
            gridPoints.set((int)(gridIndex(i, j)), evaluate(u, (double)j / (double)vSteps));
        }
    }
    for (int i = 0; i < uSteps; i++) {
        final double u = (double)i / (double)uSteps;
        for (int j = 0; j < vSteps; j++) {
            final double v = (double)j / (double)vSteps;
            final Vector3Dd v0 = evaluate(u, v);
            final Vector3Dd v1 = evaluate(u + deltaU, v);
            final Vector3Dd v2 = evaluate(u, v + deltaV);
            final Vector3Dd v3 = evaluate(u + deltaU, v + deltaV);
            gridPoints.set((int)(gridIndex(i, j)), v0);
            gridPoints.set((int)(gridIndex(i + 1, j)), v1);
            gridPoints.set((int)(gridIndex(i, j + 1)), v2);
            gridPoints.set((int)(gridIndex(i + 1, j + 1)), v3);
            Vector3Dd[] n = {new Vector3Dd()};
            double[] d = {0};
            if (trianglePlane(v0, v2, v1, n, d)) {
                gridPlaneNormals.set((int)(planeIndex(i, 2 * j)), n[0]);
                gridPlaneOffsets.set((int)(planeIndex(i, 2 * j)), d[0]);
            } else {
                gridPlaneNormals.set((int)(planeIndex(i, 2 * j)), new Vector3Dd(0.0, 0.0, 0.0));
                gridPlaneOffsets.set((int)(planeIndex(i, 2 * j)), 0.0);
            }
            if (trianglePlane(v1, v2, v3, n, d)) {
                gridPlaneNormals.set((int)(planeIndex(i, 2 * j + 1)), n[0]);
                gridPlaneOffsets.set((int)(planeIndex(i, 2 * j + 1)), d[0]);
            } else {
                gridPlaneNormals.set((int)(planeIndex(i, 2 * j + 1)), new Vector3Dd(0.0, 0.0, 0.0));
                gridPlaneOffsets.set((int)(planeIndex(i, 2 * j + 1)), 0.0);
            }
        }
    }
    if (subdivision == BezierPatchSubdivision.SmoothGrid) {
        for (int i = 0; i <= uSteps; i++) {
            final double u = (double)i / (double)uSteps;
            for (int j = 0; j <= vSteps; j++) {
                gridSmoothNormals.add(evaluateNormal(u, (double)j / (double)vSteps));
            }
        }
    }
}

    private long gridIndex(int i, int j) { return (long)i*(vSteps + 1) + j; }

    private long planeIndex(int i, int j) { return (long)i*2*(vSteps + 1) + j; }

    // The hit sink of every strategy; returns false once the hit budget is spent.
    private static final class HitList {
        public int used;
        public int maximum;
        public int count;
        public double[] depths;
        public Vector3Dd[] points;
        public Vector3Dd[] normals;
        public boolean orientNormals;
        public boolean add(double depth, Vector3Dd point, Vector3Dd normal)
        {
            if (used + count >= maximum) return false;
            depths[count] = depth;
            points[count] = point;
            normals[count] = normal;
            count++;
            return true;
        }
        public boolean isFull() { return used + count >= maximum; }
    }

    private int intersectWith(Vector3Dd origin, Vector3Dd direction, double minimumDepth, boolean orient, int alreadyUsed, int maximumHits, double[] depths, Vector3Dd[] points, Vector3Dd[] normals) {
    HitList hits = new HitList();
    hits.used = alreadyUsed;
    hits.maximum = maximumHits;
    hits.count = 0;
    hits.depths = depths;
    hits.points = points;
    hits.normals = normals;
    hits.orientNormals = orient;
    switch (subdivision) {
    case UniformGrid:
        return intersectUniform(origin, direction, minimumDepth, hits);
    case PrecomputedGrid:
        return intersectGrid(origin, direction, minimumDepth, hits);
    case Adaptive:
        subdivide(origin, direction, minimumDepth, controlPoints, 0, hits);
        return hits.count;
    case AdaptiveTree:
        if (!nodes.isEmpty()) walkTree(origin, direction, minimumDepth, 0, hits);
        return hits.count;
    case SmoothGrid:
        return intersectSmooth(origin, direction, minimumDepth, hits);
    }
    return 0;
}

    private int intersectUniform(Vector3Dd origin, Vector3Dd direction, double minimumDepth, HitList hits) {
    if (!rayMeetsSphere(origin, direction, boundingCenter, boundingRadiusSquared)) {
        return 0;
    }
    final double deltaU = 1.0 / (double)uSteps;
    final double deltaV = 1.0 / (double)vSteps;
    for (int i = 0; i < uSteps; i++) {
        final double u = (double)i / (double)uSteps;
        for (int j = 0; j < vSteps; j++) {
            final double v = (double)j / (double)vSteps;
            final Vector3Dd v0 = evaluate(u, v);
            final Vector3Dd v1 = evaluate(u + deltaU, v);
            final Vector3Dd v2 = evaluate(u, v + deltaV);
            final Vector3Dd v3 = evaluate(u + deltaU, v + deltaV);
            Vector3Dd[] n = {new Vector3Dd()}, ip = {new Vector3Dd()}, normal = {new Vector3Dd()};
            double[] d = {0}, depth = {0};
            if (trianglePlane(v0, v2, v1, n, d) && intersectTriangle(origin, direction, minimumDepth, hits.orientNormals,
                    v0, v2, v1, n[0], d[0], null, null, null, depth, ip, normal)) {
                hits.add(depth[0], ip[0], normal[0]);
                if (hits.isFull()) return hits.count;
            }
            if (trianglePlane(v1, v2, v3, n, d) && intersectTriangle(origin, direction, minimumDepth, hits.orientNormals,
                    v1, v2, v3, n[0], d[0], null, null, null, depth, ip, normal)) {
                hits.add(depth[0], ip[0], normal[0]);
                if (hits.isFull()) return hits.count;
            }
        }
    }
    return hits.count;
}

    private int intersectGrid(Vector3Dd origin, Vector3Dd direction, double minimumDepth, HitList hits) {
    if (!rayMeetsSphere(origin, direction, boundingCenter, boundingRadiusSquared)) {
        return 0;
    }
    for (int i = 0; i < uSteps; i++) {
        for (int j = 0; j < vSteps; j++) {
            Vector3Dd[] v = new Vector3Dd[4];
            v[0] = gridPoints.get((int)(gridIndex(i, j)));
            v[1] = gridPoints.get((int)(gridIndex(i + 1, j)));
            v[2] = gridPoints.get((int)(gridIndex(i, j + 1)));
            v[3] = gridPoints.get((int)(gridIndex(i + 1, j + 1)));
            Vector3Dd[] center = {new Vector3Dd()};
            double[] radiusSquared = {0};
            sphereAround(4, v, center, radiusSquared);
            if (!rayMeetsSphere(origin, direction, center[0], radiusSquared[0])) {
                continue;
            }
            Vector3Dd[] ip = {new Vector3Dd()}, normal = {new Vector3Dd()};
            double[] depth = {0};
            Vector3Dd n = gridPlaneNormals.get((int)(planeIndex(i, 2 * j)));
            if (!(n.x() == 0.0 && n.y() == 0.0 && n.z() == 0.0) &&
                intersectTriangle(origin, direction, minimumDepth, hits.orientNormals, v[0], v[2], v[1], n,
                    gridPlaneOffsets.get((int)(planeIndex(i, 2 * j))), null, null, null, depth, ip, normal)) {
                hits.add(depth[0], ip[0], normal[0]);
                if (hits.isFull()) return hits.count;
            }
            n = gridPlaneNormals.get((int)(planeIndex(i, 2 * j + 1)));
            if (n.x() == 0.0 && n.y() == 0.0 && n.z() == 0.0) {
                continue;
            }
            if (intersectTriangle(origin, direction, minimumDepth, hits.orientNormals, v[1], v[2], v[3], n,
                    gridPlaneOffsets.get((int)(planeIndex(i, 2 * j + 1))), null, null, null, depth, ip, normal)) {
                hits.add(depth[0], ip[0], normal[0]);
                if (hits.isFull()) return hits.count;
            }
        }
    }
    return hits.count;
}

    private int intersectSmooth(Vector3Dd origin, Vector3Dd direction, double minimumDepth, HitList hits) {
    if (!rayMeetsSphere(origin, direction, boundingCenter, boundingRadiusSquared)) {
        return 0;
    }
    for (int i = 0; i < uSteps; i++) {
        for (int j = 0; j < vSteps; j++) {
            final Vector3Dd v0 = gridPoints.get((int)(gridIndex(i, j)));
            final Vector3Dd v1 = gridPoints.get((int)(gridIndex(i + 1, j)));
            final Vector3Dd v2 = gridPoints.get((int)(gridIndex(i, j + 1)));
            final Vector3Dd v3 = gridPoints.get((int)(gridIndex(i + 1, j + 1)));
            Vector3Dd n0 = gridSmoothNormals.get((int)(gridIndex(i, j)));
            Vector3Dd n1 = gridSmoothNormals.get((int)(gridIndex(i + 1, j)));
            Vector3Dd n2 = gridSmoothNormals.get((int)(gridIndex(i, j + 1)));
            Vector3Dd n3 = gridSmoothNormals.get((int)(gridIndex(i + 1, j + 1)));
            Vector3Dd[] ip = {new Vector3Dd()}, normal = {new Vector3Dd()};
            double[] depth = {0};
            Vector3Dd n = gridPlaneNormals.get((int)(planeIndex(i, 2 * j)));
            if (!(n.x() == 0.0 && n.y() == 0.0 && n.z() == 0.0)) {
                // Corner normals turned to the side of the triangle normal.
                if (n0.dotProduct(n) < 0) n0 = n0.multiply(-1.0);
                if (n1.dotProduct(n) < 0) n1 = n1.multiply(-1.0);
                if (n2.dotProduct(n) < 0) n2 = n2.multiply(-1.0);
                if (intersectTriangle(origin, direction, minimumDepth, hits.orientNormals, v0, v2, v1, n,
                        gridPlaneOffsets.get((int)(planeIndex(i, 2 * j))), n0, n2, n1, depth, ip, normal)) {
                    hits.add(depth[0], ip[0], normal[0]);
                    if (hits.isFull()) return hits.count;
                }
            }
            n = gridPlaneNormals.get((int)(planeIndex(i, 2 * j + 1)));
            if (n.x() == 0.0 && n.y() == 0.0 && n.z() == 0.0) {
                continue;
            }
            // PovRay replaces, rather than turns, these corner normals.
            if (n1.dotProduct(n) > 0) n1 = n0.multiply(-1.0);
            if (n2.dotProduct(n) > 0) n2 = n1.multiply(-1.0);
            if (n3.dotProduct(n) > 0) n3 = n2.multiply(-1.0);
            if (intersectTriangle(origin, direction, minimumDepth, hits.orientNormals, v1, v2, v3, n,
                    gridPlaneOffsets.get((int)(planeIndex(i, 2 * j + 1))), n1, n2, n3, depth, ip, normal)) {
                hits.add(depth[0], ip[0], normal[0]);
                if (hits.isFull()) return hits.count;
            }
        }
    }
    return hits.count;
}

    private void subdivide(Vector3Dd origin, Vector3Dd direction, double minimumDepth, Vector3Dd[][] net, int depth, HitList hits) {
    if (hits.used >= hits.maximum) {
        return;
    }
    Vector3Dd[] center = {new Vector3Dd()};
    double[] radiusSquared = {0};
    netSphere(net, center, radiusSquared);
    if (!rayMeetsSphere(origin, direction, center[0], radiusSquared[0])) {
        return;
    }
    // PovRay tests a flat piece and still subdivides it.
    if (isFlatEnough(net) && !hits.isFull()) {
        intersectPiece(origin, direction, minimumDepth, net[0][0], net[0][3], net[3][3], net[3][0], hits);
    }
    Vector3Dd[][] lowerLeft = new Vector3Dd[4][4];
    Vector3Dd[][] lowerRight = new Vector3Dd[4][4];
    Vector3Dd[][] upperLeft = new Vector3Dd[4][4];
    Vector3Dd[][] upperRight = new Vector3Dd[4][4];
    if (depth >= uSteps) {
        if (depth >= vSteps) {
            if (!hits.isFull()) {
                intersectPiece(origin, direction, minimumDepth, net[0][0], net[0][3], net[3][3], net[3][0], hits);
            }
        } else {
            splitUpDown(net, lowerLeft, upperLeft);
            subdivide(origin, direction, minimumDepth, lowerLeft, depth + 1, hits);
            subdivide(origin, direction, minimumDepth, upperLeft, depth + 1, hits);
        }
    } else if (depth >= vSteps) {
        splitLeftRight(net, lowerLeft, lowerRight);
        subdivide(origin, direction, minimumDepth, lowerLeft, depth + 1, hits);
        subdivide(origin, direction, minimumDepth, lowerRight, depth + 1, hits);
    } else {
        splitLeftRight(net, lowerLeft, lowerRight);
        splitUpDown(lowerLeft, lowerLeft, upperLeft);
        splitUpDown(lowerRight, lowerRight, upperRight);
        subdivide(origin, direction, minimumDepth, lowerLeft, depth + 1, hits);
        subdivide(origin, direction, minimumDepth, upperLeft, depth + 1, hits);
        subdivide(origin, direction, minimumDepth, lowerRight, depth + 1, hits);
        subdivide(origin, direction, minimumDepth, upperRight, depth + 1, hits);
    }
}

    private void intersectPiece(Vector3Dd origin, Vector3Dd direction, double minimumDepth, Vector3Dd c0, Vector3Dd c1, Vector3Dd c2, Vector3Dd c3, HitList hits) {
    Vector3Dd[] n = {new Vector3Dd()}, ip = {new Vector3Dd()}, normal = {new Vector3Dd()};
    double[] d = {0}, depth = {0};
    if (trianglePlane(c0, c1, c2, n, d) && intersectTriangle(origin, direction, minimumDepth, hits.orientNormals,
            c0, c1, c2, n[0], d[0], null, null, null, depth, ip, normal)) {
        hits.add(depth[0], ip[0], normal[0]);
    }
    if (hits.isFull()) {
        return;
    }
    if (trianglePlane(c0, c2, c3, n, d) && intersectTriangle(origin, direction, minimumDepth, hits.orientNormals,
            c0, c2, c3, n[0], d[0], null, null, null, depth, ip, normal)) {
        hits.add(depth[0], ip[0], normal[0]);
    }
}

    private void walkTree(Vector3Dd origin, Vector3Dd direction, double minimumDepth, long index, HitList hits) {
    if (hits.used >= hits.maximum) {
        return;
    }
    final SubdividedBezierPatchNode node = nodes.get((int)(index));
    if (!rayMeetsSphere(origin, direction, node.center, node.radiusSquared)) {
        return;
    }
    if (!node.leaf) {
        for (int i = 0; i < node.childCount; i++) {
            walkTree(origin, direction, minimumDepth, node.children[i], hits);
        }
        return;
    }
    intersectPiece(origin, direction, minimumDepth, node.corners[0], node.corners[1],
        node.corners[2], node.corners[3], hits);
}

    private RayFirstHitStatus collectHits(Ray ray, RayCrossingQuery query, int[] count, double[] depths, Vector3Dd[] normals) {
    count[0] = 0;
    final Vector3Dd o = ray.getOrigin();
    final Vector3Dd d = ray.getDirection();
    if (!Double.isFinite(o.x()) || !Double.isFinite(o.y()) || !Double.isFinite(o.z()) ||
        !Double.isFinite(d.x()) || !Double.isFinite(d.y()) || !Double.isFinite(d.z()) ||
        (d.x() == 0 && d.y() == 0 && d.z() == 0) || Double.isNaN(query.minT) ||
        Double.isNaN(query.maxT) || query.minT > query.maxT) return RayFirstHitStatus.InvalidInput;
    // Hits behind the origin are not all reported, so the query must start
    // at or after it.
    if (!(query.minT >= 0) || query.minimumChordSquared != 0 ||
        query.minimumExteriorApproach != Double.NEGATIVE_INFINITY) return RayFirstHitStatus.Unsupported;
    Vector3Dd[] points = new Vector3Dd[MAXIMUM_HITS];
    for (int i = 0; i < MAXIMUM_HITS; i++) points[i] = new Vector3Dd();
    final int found = intersectOriented(o, d, query.minT, 0, MAXIMUM_HITS, depths, points, normals);
    // A full budget may have hidden further hits.
    if (found >= MAXIMUM_HITS) return RayFirstHitStatus.Indeterminate;
    for (int i = 0; i < found; i++) {
        if (!Double.isFinite(depths[i])) return RayFirstHitStatus.Indeterminate;
    }
    count[0] = found;
    return RayFirstHitStatus.Hit;
}

    // Throws java.IllegalArgumentException for steps below 1 (or, for the
    // adaptive subdivisions, below 0) or a non-finite control point.
    public SubdividedBezierPatch(BezierPatchSubdivision subdivisionValue, int uStepsValue,
        int vStepsValue, double flatnessValue, Vector3Dd[][] net) {
        this.subdivision = subdivisionValue;
        this.uSteps = uStepsValue;
        this.vSteps = vStepsValue;
        this.flatness = flatnessValue;
        this.boundingCenter = new Vector3Dd();
        this.boundingRadiusSquared = 0;
    final boolean adaptive = subdivision == BezierPatchSubdivision.Adaptive ||
        subdivision == BezierPatchSubdivision.AdaptiveTree;
    if (uSteps < (adaptive ? 0 : 1) || vSteps < (adaptive ? 0 : 1)) {
        throw new IllegalArgumentException("SubdividedBezierPatch: invalid subdivision steps");
    }
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            final Vector3Dd p = net[i][j];
            if (!Double.isFinite(p.x()) || !Double.isFinite(p.y()) || !Double.isFinite(p.z())) {
                throw new IllegalArgumentException("SubdividedBezierPatch: control points must be finite");
            }
            controlPoints[i][j] = p;
        }
    }
    precompute();
}

    public SubdividedBezierPatch(SubdividedBezierPatch other) {
        super();
        assign(other);
    }

    /** cpp copy assignment. */
    public SubdividedBezierPatch assign(SubdividedBezierPatch other) {
    if (this != other) {
        subdivision = other.subdivision;
        uSteps = other.uSteps;
        vSteps = other.vSteps;
        flatness = other.flatness;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) controlPoints[i][j] = other.controlPoints[i][j];
        }
        boundingCenter = other.boundingCenter;
        boundingRadiusSquared = other.boundingRadiusSquared;
        gridPoints.clear(); gridPoints.addAll(other.gridPoints);
        gridPlaneNormals.clear(); gridPlaneNormals.addAll(other.gridPlaneNormals);
        gridPlaneOffsets.clear(); gridPlaneOffsets.addAll(other.gridPlaneOffsets);
        gridSmoothNormals.clear(); gridSmoothNormals.addAll(other.gridSmoothNormals);
        nodes.clear();
        for (int i = 0; i < other.nodes.size(); i++) nodes.add(new SubdividedBezierPatchNode(other.nodes.get(i)));
    }
    return this;
}

    public BezierPatchSubdivision getSubdivision() { return subdivision; }

    public int getUSteps() { return uSteps; }

    public int getVSteps() { return vSteps; }

    public double getFlatness() { return flatness; }

    public Vector3Dd getControlPoint(int i, int j) { return controlPoints[i][j]; }

    // Point of the Bezier surface at (u, v) in [0, 1]^2.
    public Vector3Dd evaluate(double u, double v) {
    final double u2 = u * u;
    final double u3 = u * u2;
    final double v2 = v * v;
    final double v3 = v * v2;
    final double uu1 = 1.0 - u;
    final double uu2 = uu1 * uu1;
    final double uu3 = uu1 * uu2;
    final double vv1 = 1.0 - v;
    final double vv2 = vv1 * vv1;
    final double vv3 = vv1 * vv2;
    double[][] t = new double[4][4];
    t[0][0] = uu3 * vv3;
    t[0][1] = 3.0 * uu3 * v * vv2;
    t[0][2] = 3.0 * uu3 * v2 * vv1;
    t[0][3] = uu3 * v3;
    t[1][0] = 3.0 * u * uu2 * vv3;
    t[1][1] = 9.0 * u * uu2 * v * vv2;
    t[1][2] = 9.0 * u * uu2 * v2 * vv1;
    t[1][3] = 3.0 * u * uu2 * v3;
    t[2][0] = 3.0 * u2 * uu1 * vv3;
    t[2][1] = 9.0 * u2 * uu1 * v * vv2;
    t[2][2] = 9.0 * u2 * uu1 * v2 * vv1;
    t[2][3] = 3.0 * u2 * uu1 * v3;
    t[3][0] = u3 * vv3;
    t[3][1] = 3.0 * u3 * v * vv2;
    t[3][2] = 3.0 * u3 * v2 * vv1;
    t[3][3] = u3 * v3;
    double rx = 0.0;
    double ry = 0.0;
    double rz = 0.0;
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            rx += t[i][j] * controlPoints[i][j].x();
            ry += t[i][j] * controlPoints[i][j].y();
            rz += t[i][j] * controlPoints[i][j].z();
        }
    }
    return new Vector3Dd(rx, ry, rz);
}

    // Cross product of the unit u and v derivatives, or (1, 0, 0) where
    // either derivative vanishes.
    public Vector3Dd evaluateNormal(double u, double v) {
    final double u2 = u * u;
    final double u3 = u * u2;
    final double v2 = v * v;
    final double v3 = v * v2;
    double[][] t = new double[4][4];
    t[0][0] = 3.0 * (v3 - 3.0 * v2 + 3.0 * v - 1.0) * (u2 - 2.0 * u + 1.0);
    t[0][1] = 9.0 * v * (v2 - 2.0 * v + 1.0) * (u2 - 2.0 * u + 1.0);
    t[0][2] = 9.0 * v2 * (v - 1.0) * (u2 - 2.0 * u + 1.0);
    t[0][3] = 3.0 * v3 * (u2 - 2.0 * u + 1.0);
    t[1][0] = 3.0 * (v3 - 3.0 * v2 + 3.0 * v - 1) * (3.0 * u2 - 4.0 * u + 1);
    t[1][1] = 9.0 * v * (v2 - 2.0 * v + 1.0) * (3.0 * u2 - 4.0 * u + 1.0);
    t[1][2] = 9.0 * v2 * (v - 1.0) * (3.0 * u2 - 4.0 * u + 1.0);
    t[1][3] = 3.0 * v3 * (3.0 * u2 - 4.0 * u + 1.0);
    t[2][0] = 3.0 * u * (v3 - 3.0 * v2 + 3.0 * v - 1.0) * (3.0 * u - 2.0);
    t[2][1] = 9.0 * u * v * (v2 - 2.0 * v + 1.0) * (3.0 * u - 2.0);
    t[2][2] = 9.0 * u * v2 * (v - 1.0) * (3.0 * u - 2.0);
    t[2][3] = 3.0 * u * v3 * (3.0 * u - 2.0);
    t[3][0] = 3.0 * u2 * (v3 - 3.0 * v2 + 3.0 * v - 1.0);
    t[3][1] = 9.0 * u2 * v * (v2 - 2.0 * v + 1.0);
    t[3][2] = 9.0 * u2 * v2 * (v - 1.0);
    t[3][3] = 3.0 * u2 * v3;
    double ux = 0.0;
    double uy = 0.0;
    double uz = 0.0;
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            ux += t[i][j] * controlPoints[i][j].x();
            uy += t[i][j] * controlPoints[i][j].y();
            uz += t[i][j] * controlPoints[i][j].z();
        }
    }
    double temp = ux * ux + uy * uy + uz * uz;
    if (temp < DEGENERACY_EPSILON) {
        return new Vector3Dd(1.0, 0.0, 0.0);
    }
    temp = Math.sqrt(temp);
    final Vector3Dd uVec = new Vector3Dd(ux / temp, uy / temp, uz / temp);

    t[0][0] = 3.0 * (v2 - 2.0 * v + 1.0) * (u3 - 3.0 * u2 + 3.0 * u - 1.0);
    t[0][1] = 3.0 * (3.0 * v2 - 4.0 * v + 1.0) * (u3 - 3.0 * u2 + 3.0 * u - 1.0);
    t[0][2] = 3.0 * v * (3.0 * v - 2.0) * (u3 - 3.0 * u2 + 3.0 * u - 1.0);
    t[0][3] = 3.0 * v2 * (u3 - 3.0 * u2 + 3.0 * u - 1.0);
    t[1][0] = 9.0 * u * (v2 - 2.0 * v + 1.0) * (u2 - 2.0 * u + 1.0);
    t[1][1] = 9.0 * u * (3.0 * v2 - 4.0 * v + 1.0) * (u2 - 2.0 * u + 1.0);
    t[1][2] = 9.0 * u * v * (3.0 * v - 2.0) * (u2 - 2.0 * u + 1.0);
    t[1][3] = 9.0 * u * v2 * (u2 - 2.0 * u + 1.0);
    t[2][0] = 9.0 * u2 * (v2 - 2.0 * v + 1.0) * (u - 1.0);
    t[2][1] = 9.0 * u2 * (3.0 * v2 - 4.0 * v + 1.0) * (u - 1.0);
    t[2][2] = 9.0 * u2 * v * (3.0 * v - 2.0) * (u - 1.0);
    t[2][3] = 9.0 * u2 * v2 * (u - 1.0);
    t[3][0] = 3.0 * u3 * (v2 - 2.0 * v + 1.0);
    t[3][1] = 3.0 * u3 * (3.0 * v2 - 4.0 * v + 1.0);
    t[3][2] = 3.0 * u3 * v * (3.0 * v - 2.0);
    t[3][3] = 3.0 * u3 * v2;
    double vx = 0.0;
    double vy = 0.0;
    double vz = 0.0;
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            vx += t[i][j] * controlPoints[i][j].x();
            vy += t[i][j] * controlPoints[i][j].y();
            vz += t[i][j] * controlPoints[i][j].z();
        }
    }
    temp = vx * vx + vy * vy + vz * vz;
    if (temp < DEGENERACY_EPSILON) {
        return new Vector3Dd(1.0, 0.0, 0.0);
    }
    temp = Math.sqrt(temp);
    final Vector3Dd vVec = new Vector3Dd(vx / temp, vy / temp, vz / temp);
    return uVec.crossProduct(vVec);
}

    // PovRay hits along origin + depth*direction with depth >= minimumDepth,
    // in discovery order, each with its point and unit triangle (or, for
    // SmoothGrid, interpolated) normal. alreadyUsed hits of a shared budget of
    // maximumHits are already spent; returns how many were added, at most
    // maximumHits - alreadyUsed, written from index 0 of the arrays.
    public int intersect(Vector3Dd origin, Vector3Dd direction, double minimumDepth, int alreadyUsed, int maximumHits, double[] depths, Vector3Dd[] points, Vector3Dd[] normals) {
    return intersectWith(origin, direction, minimumDepth, false, alreadyUsed, maximumHits, depths,
        points, normals);
}

    // As intersect, with every normal turned to the u x v side of its
    // triangle; the PovRay SmoothGrid interpolation can leave it reversed or,
    // dividing by zero, not finite, and then the triangle normal is used.
    public int intersectOriented(Vector3Dd origin, Vector3Dd direction, double minimumDepth, int alreadyUsed, int maximumHits, double[] depths, Vector3Dd[] points, Vector3Dd[] normals) {
    return intersectWith(origin, direction, minimumDepth, true, alreadyUsed, maximumHits, depths,
        points, normals);
}

    @Override
    public Geometry copyGeometry() { return new SubdividedBezierPatch(this); }

    @Override
    public void getBounds(GeometryBounds result) {
    Vector3Dd low = controlPoints[0][0];
    Vector3Dd high = controlPoints[0][0];
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            final Vector3Dd p = controlPoints[i][j];
            low = new Vector3Dd(Math.min(low.x(), p.x()), Math.min(low.y(), p.y()),
                Math.min(low.z(), p.z()));
            high = new Vector3Dd(Math.max(high.x(), p.x()), Math.max(high.y(), p.y()),
                Math.max(high.z(), p.z()));
        }
    }
    if (!result.setFinite(low, high)) result.setIndeterminate();
}

    @Override
    public RayFirstHitStatus queryFirstHit(Ray ray, RayCrossingQuery query, RayHit hit, RayBoundary boundary) {
    if (hit != null) hit.clear();
    if (boundary != null) boundary.assign(new RayBoundary());
    double[] depths = new double[MAXIMUM_HITS];
    Vector3Dd[] normals = new Vector3Dd[MAXIMUM_HITS];
    for (int i = 0; i < MAXIMUM_HITS; i++) normals[i] = new Vector3Dd();
    int[] countH = {0};
    final RayFirstHitStatus status = collectHits(ray, query, countH, depths, normals);
    final int count = countH[0];
    if (status != RayFirstHitStatus.Hit) return status;
    int best = -1;
    for (int i = 0; i < count; i++) {
        if (depths[i] <= query.maxT && (best < 0 || depths[i] < depths[best])) best = i;
    }
    if (best < 0) return RayFirstHitStatus.Miss;
    if (boundary != null) boundary.assign(new RayBoundary(depths[best], this, false, best));
    if (hit != null) {
        hit.setHitDistance(depths[best]);
        if (hit.shouldStoreRay()) hit.setRay(ray.withT(depths[best]));
        if (hit.needsAnySurfaceData()) {
            hit.beginDetailEvaluation();
            if (hit.needsPoint()) {
                hit.point = ray.getOrigin().add(ray.getDirection().multiply(depths[best]));
                hit.markDetailAvailable(RayHit.DETAIL_POINT);
            }
            if (hit.needsNormal()) {
                hit.normal = normals[best];
                hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
            }
        }
    }
    return RayFirstHitStatus.Hit;
}

    @Override
    public RayFirstHitStatus querySurfaceDetail(Ray ray, RayBoundary boundary, RayHit hit) {
    if (hit != null) hit.clear();
    if (hit == null || boundary.geometry != this || !Double.isFinite(boundary.t) ||
        boundary.surfaceIndex < 0 || boundary.surfaceIndex >= MAXIMUM_HITS)
        return RayFirstHitStatus.InvalidInput;
    if (boundary.getInstanceCount() != 0) return RayFirstHitStatus.Unsupported;
    double[] depths = new double[MAXIMUM_HITS];
    Vector3Dd[] normals = new Vector3Dd[MAXIMUM_HITS];
    for (int i = 0; i < MAXIMUM_HITS; i++) normals[i] = new Vector3Dd();
    int[] countH = {0};
    final RayFirstHitStatus status = collectHits(ray,
        new RayCrossingQuery(0, Double.POSITIVE_INFINITY), countH, depths, normals);
    final int count = countH[0];
    if (status != RayFirstHitStatus.Hit) return status;
    final int index = (int)boundary.surfaceIndex;
    if (index >= count || depths[index] != boundary.t) return RayFirstHitStatus.Indeterminate;
    hit.setHitDistance(boundary.t);
    if (hit.shouldStoreRay()) hit.setRay(ray.withT(boundary.t));
    hit.beginDetailEvaluation();
    if (hit.needsPoint()) {
        hit.point = ray.getOrigin().add(ray.getDirection().multiply(boundary.t));
        hit.markDetailAvailable(RayHit.DETAIL_POINT);
    }
    if (hit.needsNormal()) {
        hit.normal = boundary.normalReversed ? normals[index].multiply(-1.0) : normals[index];
        hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
    }
    return RayFirstHitStatus.Hit;
}

    @Override
    public RayCrossingSupport emitRayCrossings(Ray ray, RayCrossingQuery query, RayCrossingSink sink) {
    double[] depths = new double[MAXIMUM_HITS];
    Vector3Dd[] normals = new Vector3Dd[MAXIMUM_HITS];
    for (int i = 0; i < MAXIMUM_HITS; i++) normals[i] = new Vector3Dd();
    int[] countH = {0};
    final RayFirstHitStatus status = collectHits(ray, query, countH, depths, normals);
    final int count = countH[0];
    if (status == RayFirstHitStatus.InvalidInput) return RayCrossingSupport.InvalidInput;
    if (status == RayFirstHitStatus.Unsupported) return RayCrossingSupport.Unsupported;
    if (status != RayFirstHitStatus.Hit) return RayCrossingSupport.Indeterminate;
    int[] order = new int[MAXIMUM_HITS];
    int sorted = 0;
    for (int i = 0; i < count; i++) {
        if (depths[i] > query.maxT) continue;
        int at = sorted;
        while (at > 0 && depths[order[at - 1]] > depths[i]) {
            order[at] = order[at - 1];
            at--;
        }
        order[at] = i;
        sorted++;
    }
    for (int i = 0; i < sorted; i++) {
        RayCrossing event = new RayCrossing(depths[order[i]], this, RayCrossingKind.Unclassified);
        event.surfaceIndex = order[i];
        sink.crossing(event);
    }
    return RayCrossingSupport.Supported;
}

    // Like PovRay, ignores hits nearer than POVRAY_MINIMUM_DEPTH: the
    // overlapping pieces of an adaptive subdivision would shadow themselves.
    @Override
    public boolean doIntersectionFirstHit(Ray ray, RayHit hit) {
    return queryFirstHit(ray, new RayCrossingQuery(POVRAY_MINIMUM_DEPTH, Double.POSITIVE_INFINITY), hit) ==
        RayFirstHitStatus.Hit;
}

    @Override
    public void doExtraInformation(Ray ray, double t, RayHit hit) {
    if (hit == null) return;
    hit.beginDetailEvaluation();
    if (hit.needsPoint()) {
        hit.point = ray.getOrigin().add(ray.getDirection().multiply(t));
        hit.markDetailAvailable(RayHit.DETAIL_POINT);
    }
    if (!hit.needsNormal()) return;
    double[] depths = new double[MAXIMUM_HITS];
    Vector3Dd[] normals = new Vector3Dd[MAXIMUM_HITS];
    for (int i = 0; i < MAXIMUM_HITS; i++) normals[i] = new Vector3Dd();
    int[] countH = {0};
    if (collectHits(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), countH, depths, normals) !=
        RayFirstHitStatus.Hit) return;
    final int count = countH[0];
    // The caller's ray may be rebuilt from the hit, so match the nearest depth.
    int best = -1;
    for (int i = 0; i < count; i++) {
        if (best < 0 || Math.abs(depths[i] - t) < Math.abs(depths[best] - t)) best = i;
    }
    if (best >= 0 && Math.abs(depths[best] - t) <= 1.0e-6*(1.0 + Math.abs(t))) {
        hit.normal = normals[best];
        hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
    }
}

    @Override
    public double[] getMinMax() {
    GeometryBounds bounds = new GeometryBounds();
    getBounds(bounds);
    if (bounds.getKind() == GeometryBounds.Kind.Finite)
        return new double[]{bounds.getMinimum().x(), bounds.getMinimum().y(), bounds.getMinimum().z(),
            bounds.getMaximum().x(), bounds.getMaximum().y(), bounds.getMaximum().z()};
    return new double[]{-1e308,-1e308,-1e308,1e308,1e308,1e308};
}
}
