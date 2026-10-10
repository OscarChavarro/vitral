//= References:                                                             =
//= [WAYN1990] Knapp Wayne. "Ray with Bicubic Patch Intersection Problem",  =
//=            Ray Tracing News, volume 3, number 3, july 13 1990.          =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics, princi-   =
//=            ples and practice" - second edition, Addison Wesley, 1992.   =
//= [POVR1993] POV-Ray 1.0/2.x "bezier.c" bicubic_patch implementation, as  =
//=            ported to C++ in the povCpp project (ParametricPatch.cpp,    =
//=            ParametricBiCubicIntersection.cpp).                          =

package vsdk.toolkit.environment.geometry.surface;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;

/**
Ray / bicubic patch intersection support for `ParametricBiCubicPatch`.

This follows the "subdivision tree" strategy (POV-Ray bicubic_patch type 1/3,
as replicated on povCpp) discussed in [WAYN1990]: the patch is converted to
its Bezier control net, the net is recursively split by de Casteljau
subdivision until every sub-patch is flat enough, and a tree of bounding
spheres around the sub-patch control nets is built. Each ray walks the tree,
discarding sub-trees whose bounding sphere is missed, and leaf sub-patches are
tested as two triangles built from their corners.

Differences with respect to the povCpp version, aimed at rendering quality:
<UL>
  <LI> All leaves are at the same subdivision depth, so neighboring leaves
  share their corner points and the triangle approximation has no cracks
  (T-junctions) that would let rays leak through the surface.
  <LI> The flatness criterion measures the distance from the control net to
  the bilinear interpolation of the sub-patch corners. That bounds both the
  out of plane bending (as in povCpp) and the in plane curvature of the
  sub-patch borders.
  <LI> The (u, v) parameters of the triangle hit are refined with a few Newton
  iterations on the exact surface, so the reported point, normal, tangent and
  texture coordinates are the ones of the true bicubic surface.
</UL>

Instances are immutable after construction, so they can be shared by
multiple raytracing threads.
*/
final class _ParametricBiCubicPatchIntersector
{
    /// Inverse of the Bezier basis matrix [FOLE1992].11.28, used to convert
    /// a power basis coefficient matrix into a Bezier control net.
    private static final double[][] INVERSE_BEZIER_MATRIX = {
        {0.0, 0.0,       0.0,       1.0},
        {0.0, 0.0,       1.0 / 3.0, 1.0},
        {0.0, 1.0 / 3.0, 2.0 / 3.0, 1.0},
        {1.0, 1.0,       1.0,       1.0}
    };

    private static final double DEFAULT_RELATIVE_FLATNESS = 1.0e-3;
    private static final int MIN_SUBDIVISION_DEPTH = 2;
    private static final int MAX_SUBDIVISION_DEPTH = 7;
    private static final int MAX_NEWTON_ITERATIONS = 6;
    private static final double NEWTON_TOLERANCE = 1.0e-12;
    private static final double BARYCENTRIC_TOLERANCE = 1.0e-9;

    /**
    Node in the bounding sphere hierarchy. Interior nodes have `children`,
    leaves have the four corner points of the sub-patch and its parametric
    range.
    */
    private static final class PatchNode
    {
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final double radiusSquared;
        private final PatchNode[] children;
        private final double[] corners;
        private final double uMin;
        private final double uMax;
        private final double vMin;
        private final double vMax;

        private PatchNode(double[] net, PatchNode[] children,
                          double u0, double u1, double v0, double v1)
        {
            double[] sphere = boundingSphere(net);
            this.centerX = sphere[0];
            this.centerY = sphere[1];
            this.centerZ = sphere[2];
            this.radiusSquared = sphere[3];
            this.children = children;
            this.uMin = u0;
            this.uMax = u1;
            this.vMin = v0;
            this.vMax = v1;
            if ( children == null ) {
                // Corners (u0, v0), (u0, v1), (u1, v1) and (u1, v0)
                corners = new double[12];
                copyPoint(net, 0, 0, corners, 0);
                copyPoint(net, 0, 3, corners, 1);
                copyPoint(net, 3, 3, corners, 2);
                copyPoint(net, 3, 0, corners, 3);
            }
            else {
                corners = null;
            }
        }
    }

    /**
    Result of a ray/patch query, in patch parameter space.
    */
    static final class PatchHit
    {
        double t;
        double u;
        double v;
        double pointX;
        double pointY;
        double pointZ;
        double triangleNx;
        double triangleNy;
        double triangleNz;

        private PatchHit()
        {
            t = Double.MAX_VALUE;
        }
    }

    /// Bezier control net, stored as 16 points of 3 coordinates, with point
    /// (i, j) at index 3*(4*i + j). Index i follows the s (u) parameter and
    /// index j the t (v) parameter of `ParametricBiCubicPatch.evaluate`.
    private final double[] bezierNet;
    private final PatchNode root;
    private final int subdivisionDepth;

    /**
    Builds the intersection structures for the patch with the given power
    basis coefficient matrices (`M * G * Mt` for each coordinate).
    @param coefficientMatrixX x coordinate coefficients
    @param coefficientMatrixY y coordinate coefficients
    @param coefficientMatrixZ z coordinate coefficients
    */
    _ParametricBiCubicPatchIntersector(Matrix4x4d coefficientMatrixX,
                                        Matrix4x4d coefficientMatrixY,
                                        Matrix4x4d coefficientMatrixZ)
    {
        bezierNet = new double[48];
        storeBezierCoordinate(coefficientMatrixX, 0);
        storeBezierCoordinate(coefficientMatrixY, 1);
        storeBezierCoordinate(coefficientMatrixZ, 2);

        double[] rootSphere = boundingSphere(bezierNet);
        double tolerance =
            DEFAULT_RELATIVE_FLATNESS * Math.sqrt(rootSphere[3]);
        if ( tolerance < VSDK.EPSILON * VSDK.EPSILON ) {
            tolerance = VSDK.EPSILON * VSDK.EPSILON;
        }
        int depth = requiredSubdivisionDepth(bezierNet, 0, tolerance);
        if ( depth < MIN_SUBDIVISION_DEPTH ) {
            depth = MIN_SUBDIVISION_DEPTH;
        }
        subdivisionDepth = depth;
        root = buildTree(bezierNet, 0, 0.0, 1.0, 0.0, 1.0);
    }

    /**
    @return the number of de Casteljau subdivision levels used for the
    leaves of the bounding sphere tree
    */
    int getSubdivisionDepth()
    {
        return subdivisionDepth;
    }

    /**
    @param i control point index on the s (u) direction, in [0, 3]
    @param j control point index on the t (v) direction, in [0, 3]
    @return the Bezier control point (i, j) equivalent to current patch
    */
    Vector3Dd getBezierControlPoint(int i, int j)
    {
        int index = 3 * (4 * i + j);
        return new Vector3Dd(bezierNet[index], bezierNet[index + 1],
            bezierNet[index + 2]);
    }

    /**
    Bounding box of the Bezier control net. By the convex hull property it
    contains the whole patch.
    @return min-max array as specified by `Geometry.getMinMax`
    */
    double[] getMinMax()
    {
        double[] minMax = {
            Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
            -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE
        };
        for ( int k = 0; k < 16; k++ ) {
            for ( int c = 0; c < 3; c++ ) {
                double value = bezierNet[3 * k + c];
                if ( value < minMax[c] ) {
                    minMax[c] = value;
                }
                if ( value > minMax[c + 3] ) {
                    minMax[c + 3] = value;
                }
            }
        }
        return minMax;
    }

    /**
    Finds the nearest intersection of the ray with the patch.
    @param ray ray to test, its direction does not need to be normalized
    @return nearest hit with positive ray parameter, or null if the ray
    misses the patch
    */
    PatchHit intersect(Ray ray)
    {
        Vector3Dd origin = ray.getOrigin();
        Vector3Dd direction = ray.getDirection();
        double[] o = { origin.x(), origin.y(), origin.z() };
        double[] d = { direction.x(), direction.y(), direction.z() };
        double directionLengthSquared = d[0]*d[0] + d[1]*d[1] + d[2]*d[2];

        if ( directionLengthSquared <= 0.0 ) {
            return null;
        }

        PatchHit best = new PatchHit();
        walkTree(root, o, d, directionLengthSquared, best);
        if ( best.t == Double.MAX_VALUE ) {
            return null;
        }
        return best;
    }

    /**
    Evaluates the patch and its first partial derivatives.
    @param u parameter in the s direction
    @param v parameter in the t direction
    @param out receives point (0..2), dS/du (3..5) and dS/dv (6..8)
    */
    void evaluate(double u, double v, double[] out)
    {
        double[] bu = new double[4];
        double[] dbu = new double[4];
        double[] bv = new double[4];
        double[] dbv = new double[4];

        bernstein(u, bu, dbu);
        bernstein(v, bv, dbv);
        for ( int k = 0; k < 9; k++ ) {
            out[k] = 0.0;
        }
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                int index = 3 * (4 * i + j);
                double w = bu[i] * bv[j];
                double wu = dbu[i] * bv[j];
                double wv = bu[i] * dbv[j];
                for ( int c = 0; c < 3; c++ ) {
                    double p = bezierNet[index + c];
                    out[c] += w * p;
                    out[3 + c] += wu * p;
                    out[6 + c] += wv * p;
                }
            }
        }
    }

    //= Construction ======================================================

    /**
    Computes B = Minv * C * Minv^t, where C is a power basis coefficient
    matrix and Minv the inverse Bezier basis matrix, and stores B into the
    given coordinate of the Bezier net.
    */
    private void storeBezierCoordinate(Matrix4x4d coefficients, int coordinate)
    {
        double[][] temp = new double[4][4];
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                double accum = 0.0;
                for ( int k = 0; k < 4; k++ ) {
                    accum += INVERSE_BEZIER_MATRIX[i][k] * coefficients.get(k, j);
                }
                temp[i][j] = accum;
            }
        }
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                double accum = 0.0;
                for ( int k = 0; k < 4; k++ ) {
                    accum += temp[i][k] * INVERSE_BEZIER_MATRIX[j][k];
                }
                bezierNet[3 * (4 * i + j) + coordinate] = accum;
            }
        }
    }

    private static int requiredSubdivisionDepth(double[] net, int depth,
                                                double tolerance)
    {
        if ( depth >= MAX_SUBDIVISION_DEPTH ||
             subPatchFlatness(net) < tolerance ) {
            return depth;
        }
        double[][] quarters = splitInFour(net);
        int maxDepth = depth;
        for ( double[] quarter : quarters ) {
            int childDepth =
                requiredSubdivisionDepth(quarter, depth + 1, tolerance);
            if ( childDepth > maxDepth ) {
                maxDepth = childDepth;
                if ( maxDepth >= MAX_SUBDIVISION_DEPTH ) {
                    break;
                }
            }
        }
        return maxDepth;
    }

    /**
    Port of povCpp `parametricTreeBuilder`, with a uniform leaf depth.
    */
    private PatchNode buildTree(double[] net, int depth,
                                double u0, double u1, double v0, double v1)
    {
        if ( depth >= subdivisionDepth ) {
            return new PatchNode(net, null, u0, u1, v0, v1);
        }
        double um = (u0 + u1) / 2.0;
        double vm = (v0 + v1) / 2.0;
        double[][] quarters = splitInFour(net);
        PatchNode[] children = new PatchNode[4];
        children[0] = buildTree(quarters[0], depth + 1, u0, um, v0, vm);
        children[1] = buildTree(quarters[1], depth + 1, u0, um, vm, v1);
        children[2] = buildTree(quarters[2], depth + 1, um, u1, v0, vm);
        children[3] = buildTree(quarters[3], depth + 1, um, u1, vm, v1);
        return new PatchNode(net, children, u0, u1, v0, v1);
    }

    /**
    @return the four quarters of the net, in order (low u, low v),
    (low u, high v), (high u, low v) and (high u, high v)
    */
    private static double[][] splitInFour(double[] net)
    {
        double[] lowU = new double[48];
        double[] highU = new double[48];
        splitU(net, lowU, highU);

        double[][] quarters = new double[4][48];
        splitV(lowU, quarters[0], quarters[1]);
        splitV(highU, quarters[2], quarters[3]);
        return quarters;
    }

    /**
    Splits the net at u = 1/2 (povCpp `parametricSplitUpDown`).
    */
    private static void splitU(double[] net, double[] low, double[] high)
    {
        for ( int j = 0; j < 4; j++ ) {
            for ( int c = 0; c < 3; c++ ) {
                deCasteljau(net[3*(0+j)+c], net[3*(4+j)+c],
                    net[3*(8+j)+c], net[3*(12+j)+c],
                    low, high, 3*j + c, 12);
            }
        }
    }

    /**
    Splits the net at v = 1/2 (povCpp `parametricSplitLeftRight`).
    */
    private static void splitV(double[] net, double[] low, double[] high)
    {
        for ( int i = 0; i < 4; i++ ) {
            for ( int c = 0; c < 3; c++ ) {
                int base = 12*i + c;
                deCasteljau(net[base], net[base+3], net[base+6], net[base+9],
                    low, high, base, 3);
            }
        }
    }

    private static void deCasteljau(double p0, double p1, double p2, double p3,
                                    double[] low, double[] high,
                                    int start, int stride)
    {
        double p01 = (p0 + p1) / 2.0;
        double p12 = (p1 + p2) / 2.0;
        double p23 = (p2 + p3) / 2.0;
        double p012 = (p01 + p12) / 2.0;
        double p123 = (p12 + p23) / 2.0;
        double middle = (p012 + p123) / 2.0;

        low[start] = p0;
        low[start + stride] = p01;
        low[start + 2*stride] = p012;
        low[start + 3*stride] = middle;
        high[start] = middle;
        high[start + stride] = p123;
        high[start + 2*stride] = p23;
        high[start + 3*stride] = p3;
    }

    /**
    Maximum distance between the control net and the bilinear patch spanned
    by its four corners, plus the deviation of that bilinear patch with
    respect to its two triangles (half the corner twist). When this value is
    small, the two corner triangles approximate the sub-patch.
    */
    private static double subPatchFlatness(double[] net)
    {
        double maxDistanceSquared = 0.0;
        for ( int i = 0; i < 4; i++ ) {
            double a = i / 3.0;
            for ( int j = 0; j < 4; j++ ) {
                double b = j / 3.0;
                double distanceSquared = 0.0;
                for ( int c = 0; c < 3; c++ ) {
                    double bilinear =
                        (1-a)*(1-b)*net[c] + (1-a)*b*net[9+c] +
                        a*(1-b)*net[36+c] + a*b*net[45+c];
                    double delta = net[3*(4*i + j) + c] - bilinear;
                    distanceSquared += delta * delta;
                }
                if ( distanceSquared > maxDistanceSquared ) {
                    maxDistanceSquared = distanceSquared;
                }
            }
        }
        double twistSquared = 0.0;
        for ( int c = 0; c < 3; c++ ) {
            double twist = (net[c] - net[9+c] - net[36+c] + net[45+c]) / 4.0;
            twistSquared += twist * twist;
        }
        return Math.sqrt(maxDistanceSquared) + Math.sqrt(twistSquared);
    }

    /**
    Port of povCpp `parametricBoundingSphere`.
    @return center (0..2) and squared radius (3) of a sphere containing the
    16 control points of the net
    */
    private static double[] boundingSphere(double[] net)
    {
        double xc = 0.0;
        double yc = 0.0;
        double zc = 0.0;
        for ( int k = 0; k < 16; k++ ) {
            xc += net[3*k];
            yc += net[3*k + 1];
            zc += net[3*k + 2];
        }
        xc /= 16.0;
        yc /= 16.0;
        zc /= 16.0;
        double r0 = 0.0;
        for ( int k = 0; k < 16; k++ ) {
            double x0 = net[3*k] - xc;
            double y0 = net[3*k + 1] - yc;
            double z0 = net[3*k + 2] - zc;
            double r1 = x0*x0 + y0*y0 + z0*z0;
            if ( r1 > r0 ) {
                r0 = r1;
            }
        }
        // Small inflation so that numerically flat leaves keep a usable
        // bounding volume
        double inflation = 1.0e-9 * (1.0 + Math.abs(xc) + Math.abs(yc) +
            Math.abs(zc));
        double radius = Math.sqrt(r0) + inflation;
        return new double[] { xc, yc, zc, radius * radius };
    }

    private static void copyPoint(double[] net, int i, int j,
                                  double[] target, int targetIndex)
    {
        int source = 3 * (4 * i + j);
        target[3*targetIndex] = net[source];
        target[3*targetIndex + 1] = net[source + 1];
        target[3*targetIndex + 2] = net[source + 2];
    }

    //= Ray queries =======================================================

    /**
    Port of povCpp `parametricTreeWalker`, keeping only the nearest hit and
    pruning sub-trees farther than the current nearest hit.
    */
    private void walkTree(PatchNode node, double[] o, double[] d,
                          double directionLengthSquared, PatchHit best)
    {
        if ( !sphericalBoundsCheck(node, o, d, directionLengthSquared,
                 best.t) ) {
            return;
        }
        if ( node.children != null ) {
            for ( PatchNode child : node.children ) {
                walkTree(child, o, d, directionLengthSquared, best);
            }
            return;
        }

        // Triangulate this sub-patch, then check for intersections in the
        // triangles (0, 1, 2) and (0, 2, 3)
        double[] c = node.corners;
        double[] result = new double[3];
        if ( intersectTriangle(o, d, c, 0, 1, 2, result) &&
             result[0] < best.t ) {
            double u = node.uMin + result[2] * (node.uMax - node.uMin);
            double v = node.vMin + (result[1] + result[2]) * (node.vMax - node.vMin);
            acceptHit(node, o, d, c, 0, 1, 2, result[0], u, v, best);
        }
        if ( intersectTriangle(o, d, c, 0, 2, 3, result) &&
             result[0] < best.t ) {
            double u = node.uMin + (result[1] + result[2]) * (node.uMax - node.uMin);
            double v = node.vMin + result[1] * (node.vMax - node.vMin);
            acceptHit(node, o, d, c, 0, 2, 3, result[0], u, v, best);
        }
    }

    /**
    Port of povCpp `sphericalBoundsCheck`, extended to reject spheres whose
    entry point lies beyond the nearest hit already found.
    */
    private static boolean sphericalBoundsCheck(PatchNode node, double[] o,
        double[] d, double directionLengthSquared, double maxT)
    {
        double x = node.centerX - o[0];
        double y = node.centerY - o[1];
        double z = node.centerZ - o[2];
        double distanceSquared = x*x + y*y + z*z;
        if ( distanceSquared < node.radiusSquared ) {
            // Ray starts inside sphere - assume it intersects
            return true;
        }
        double projection = x*d[0] + y*d[1] + z*d[2];
        if ( projection <= 0.0 ) {
            return false;
        }
        double closestDistanceSquared =
            distanceSquared - (projection * projection) / directionLengthSquared;
        if ( closestDistanceSquared >= node.radiusSquared ) {
            return false;
        }
        double halfChord = Math.sqrt(
            (node.radiusSquared - closestDistanceSquared) / directionLengthSquared);
        double entryT = projection / directionLengthSquared - halfChord;
        return entryT < maxT;
    }

    /**
    Moller-Trumbore ray/triangle test over the leaf corners a, b and c.
    @param result receives t (0) and the barycentric coordinates of the hit
    with respect to b (1) and c (2)
    @return true if the ray hits the triangle at a positive distance
    */
    private static boolean intersectTriangle(double[] o, double[] d,
        double[] corners, int a, int b, int c, double[] result)
    {
        double ax = corners[3*a];
        double ay = corners[3*a + 1];
        double az = corners[3*a + 2];
        double e1x = corners[3*b] - ax;
        double e1y = corners[3*b + 1] - ay;
        double e1z = corners[3*b + 2] - az;
        double e2x = corners[3*c] - ax;
        double e2y = corners[3*c + 1] - ay;
        double e2z = corners[3*c + 2] - az;

        double px = d[1]*e2z - d[2]*e2y;
        double py = d[2]*e2x - d[0]*e2z;
        double pz = d[0]*e2y - d[1]*e2x;
        double determinant = e1x*px + e1y*py + e1z*pz;
        double scale = Math.abs(e1x) + Math.abs(e1y) + Math.abs(e1z) +
            Math.abs(e2x) + Math.abs(e2y) + Math.abs(e2z);
        if ( Math.abs(determinant) <= 1.0e-14 * scale * scale ) {
            // Degenerate triangle or ray parallel to its plane
            return false;
        }
        double inverse = 1.0 / determinant;

        double sx = o[0] - ax;
        double sy = o[1] - ay;
        double sz = o[2] - az;
        double b1 = (sx*px + sy*py + sz*pz) * inverse;
        if ( b1 < -BARYCENTRIC_TOLERANCE || b1 > 1.0 + BARYCENTRIC_TOLERANCE ) {
            return false;
        }
        double qx = sy*e1z - sz*e1y;
        double qy = sz*e1x - sx*e1z;
        double qz = sx*e1y - sy*e1x;
        double b2 = (d[0]*qx + d[1]*qy + d[2]*qz) * inverse;
        if ( b2 < -BARYCENTRIC_TOLERANCE ||
             b1 + b2 > 1.0 + BARYCENTRIC_TOLERANCE ) {
            return false;
        }
        double t = (e2x*qx + e2y*qy + e2z*qz) * inverse;
        if ( t <= VSDK.EPSILON ) {
            return false;
        }
        result[0] = t;
        result[1] = Math.max(0.0, b1);
        result[2] = Math.max(0.0, b2);
        return true;
    }

    /**
    Stores a leaf triangle hit, refining its parameters on the exact
    surface when the Newton iteration converges near the leaf.
    */
    private void acceptHit(PatchNode node, double[] o, double[] d,
        double[] corners, int a, int b, int c,
        double t, double u, double v, PatchHit best)
    {
        double[] refined = { u, v, t };
        double du = node.uMax - node.uMin;
        double dv = node.vMax - node.vMin;
        if ( refineOnSurface(o, d, refined) &&
             refined[0] >= node.uMin - du && refined[0] <= node.uMax + du &&
             refined[1] >= node.vMin - dv && refined[1] <= node.vMax + dv ) {
            if ( refined[2] <= VSDK.EPSILON ) {
                // The exact surface root is the ray origin itself (i.e. a
                // shadow or reflected ray leaving the patch): the triangle
                // hit is only an approximation artifact and must be ignored
                return;
            }
            u = refined[0];
            v = refined[1];
            t = refined[2];
        }
        if ( t >= best.t ) {
            return;
        }

        best.t = t;
        best.u = clamp01(u);
        best.v = clamp01(v);
        best.pointX = o[0] + t * d[0];
        best.pointY = o[1] + t * d[1];
        best.pointZ = o[2] + t * d[2];

        double e1x = corners[3*b] - corners[3*a];
        double e1y = corners[3*b + 1] - corners[3*a + 1];
        double e1z = corners[3*b + 2] - corners[3*a + 2];
        double e2x = corners[3*c] - corners[3*a];
        double e2y = corners[3*c + 1] - corners[3*a + 1];
        double e2z = corners[3*c + 2] - corners[3*a + 2];
        best.triangleNx = e1y*e2z - e1z*e2y;
        best.triangleNy = e1z*e2x - e1x*e2z;
        best.triangleNz = e1x*e2y - e1y*e2x;
    }

    /**
    Newton iteration over F(u, v, t) = S(u, v) - (o + t * d) = 0.
    @param uvt initial guess as input, solution as output
    @return true if the iteration converged
    */
    private boolean refineOnSurface(double[] o, double[] d, double[] uvt)
    {
        double[] s = new double[9];
        for ( int iteration = 0; iteration < MAX_NEWTON_ITERATIONS;
              iteration++ ) {
            evaluate(uvt[0], uvt[1], s);
            double fx = s[0] - (o[0] + uvt[2] * d[0]);
            double fy = s[1] - (o[1] + uvt[2] * d[1]);
            double fz = s[2] - (o[2] + uvt[2] * d[2]);

            // Jacobian columns: dS/du, dS/dv, -d
            double determinant = determinant3x3(
                s[3], s[6], -d[0],
                s[4], s[7], -d[1],
                s[5], s[8], -d[2]);
            if ( Math.abs(determinant) < 1.0e-30 ) {
                return false;
            }
            double deltaU = determinant3x3(
                fx, s[6], -d[0],
                fy, s[7], -d[1],
                fz, s[8], -d[2]) / determinant;
            double deltaV = determinant3x3(
                s[3], fx, -d[0],
                s[4], fy, -d[1],
                s[5], fz, -d[2]) / determinant;
            double deltaT = determinant3x3(
                s[3], s[6], fx,
                s[4], s[7], fy,
                s[5], s[8], fz) / determinant;
            uvt[0] -= deltaU;
            uvt[1] -= deltaV;
            uvt[2] -= deltaT;
            if ( Double.isNaN(uvt[0]) || Double.isNaN(uvt[1]) ||
                 Double.isNaN(uvt[2]) ) {
                return false;
            }
            if ( Math.abs(deltaU) + Math.abs(deltaV) < NEWTON_TOLERANCE ) {
                return true;
            }
        }
        return false;
    }

    private static double determinant3x3(double a, double b, double c,
                                         double d, double e, double f,
                                         double g, double h, double i)
    {
        return a * (e*i - f*h) - b * (d*i - f*g) + c * (d*h - e*g);
    }

    private static void bernstein(double t, double[] b, double[] db)
    {
        double s = 1.0 - t;
        b[0] = s * s * s;
        b[1] = 3.0 * t * s * s;
        b[2] = 3.0 * t * t * s;
        b[3] = t * t * t;
        db[0] = -3.0 * s * s;
        db[1] = 3.0 * s * s - 6.0 * t * s;
        db[2] = 6.0 * t * s - 3.0 * t * t;
        db[3] = 3.0 * t * t;
    }

    private static double clamp01(double value)
    {
        if ( value < 0.0 ) {
            return 0.0;
        }
        if ( value > 1.0 ) {
            return 1.0;
        }
        return value;
    }
}
