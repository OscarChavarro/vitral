package vsdk.toolkit.environment.geometry.volume;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.numerics.FloatingInterval;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayBoundary;
import vsdk.toolkit.environment.geometry.element.RayCrossing;
import vsdk.toolkit.environment.geometry.element.RayCrossingKind;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayCrossingSink;
import vsdk.toolkit.environment.geometry.element.RayCrossingSupport;
import vsdk.toolkit.environment.geometry.element.RayFirstHitStatus;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.RaySolidBoundary;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.geometry.surface.Surface;

/**
Height field: heights h(x, z) on the integer grid 0 <= x < columns,
0 <= z < rows, stored in single precision and interpolated linearly over the
two triangles of each cell, split along the diagonal from (x+1, z) to
(x, z+1). The solid is the part of the bounding box below the surface,
{ min <= p <= max, y < h(x, z) }; the box must lie in the grid domain.
Surface index 0 is the height surface and 1..6 the box faces -x, +x, -y,
+y, -z, +z. Ray queries split the ray at cell edges and diagonals, where the
height difference is linear, so crossings are exact up to rounding.
traceFirstHit is the PovRay block traversal, which reports only the first
surface hit and never box faces. Edits invalidate the traversal blocks until
updateBlocks is called.
*/
public class HeightFieldSolid extends Solid {
    // Tolerances of the PovRay first-hit traversal.
    public static final double INTERSECTION_EPSILON = 1.0e-5;

    public static final double SMALL_TOLERANCE = 0.001;

    public static final double MAX_DISTANCE = 1.0e7;

    private long columns;

    private long rows;

    private final ArrayList<Float> heights = new ArrayList<Float>();

    private Vector3Dd minimum;

    private Vector3Dd maximum;

    private double blockSize;

    private double inverseBlockSize;

    private long blockColumns;

    private long blockRows;

    private final ArrayList<HeightFieldBlock> blocks = new ArrayList<HeightFieldBlock>();

    private boolean blocksValid;

    private static double minValue(double x, double y) { return (x > y) ? y : x; }

    private static double maxValue(double x, double y) { return (x < y) ? y : x; }

    private static int signOf(double x) { return (x > 0.0) ? 1 : ((x == 0.0) ? 0 : -1); }

    private double heightAt(int x, int z) { return (double)heights.get((int)((long)z*columns + x)); }

    private HeightFieldBlock blockAt(int x, int z) {
    if (x < 0 || z < 0 || x >= blockColumns || z >= blockRows) return null;
    return blocks.get((int)((long)x*blockRows + z));
}

    private int intersectPixel(int x, int z, Vector3Dd origin, Vector3Dd direction, HeightFieldTraversal state, double height1, double height2) {
    if (x < 0 || z < 0 || x + 1 >= columns || z + 1 >= rows) {
        return 0;
    }
    Vector3Dd t1V1 = new Vector3Dd();
    Vector3Dd t1V2 = new Vector3Dd();
    Vector3Dd t1V3 = new Vector3Dd();
    Vector3Dd t2V1 = new Vector3Dd();
    Vector3Dd t2V2 = new Vector3Dd();
    Vector3Dd t2V3 = new Vector3Dd();
    Vector3Dd localNormal = new Vector3Dd();
    double pos1;
    double pos2;
    double dot;
    double depth1 = Double.POSITIVE_INFINITY;
    double depth2 = Double.POSITIVE_INFINITY;
    double s;
    double t;
    double y1;
    double y2;
    double y3;
    double y4;
    double maxHeight;
    double minHeight;

    y1 = heightAt(x, z);
    y2 = heightAt(x + 1, z);
    y3 = heightAt(x, z + 1);
    y4 = heightAt(x + 1, z + 1);

    t1V1 = new Vector3Dd((double)x, y1, (double)z);
    t1V2 = new Vector3Dd(1.0, y2 - y1, 0.0);
    t1V3 = new Vector3Dd(0.0, y3 - y1, 1.0);
    t2V1 = new Vector3Dd((double)(x + 1), y4, (double)(z + 1));
    t2V2 = new Vector3Dd(-1.0, y3 - y4, 0.0);
    t2V3 = new Vector3Dd(0.0, y2 - y4, -1.0);

    // First, we check to see if it is even possible for the ray to
    // intersect the triangle.

    if ((maxValue(y1, maxValue(y2, y3)) >= height1) &&
        (minValue(y1, minValue(y2, y3)) <= height2)) {
        localNormal = t1V3.crossProduct(t1V2);
        dot = localNormal.dotProduct(direction);

        if ((dot > INTERSECTION_EPSILON) || (dot < -INTERSECTION_EPSILON)) {
            pos1 = localNormal.dotProduct(t1V1);

            pos2 = localNormal.dotProduct(origin);

            pos1 -= pos2;

            depth1 = pos1 / dot;

            if ((depth1 > SMALL_TOLERANCE) && (depth1 < MAX_DISTANCE)) {
                s = origin.x() + (depth1 * direction.x()) - (double)x;
                t = origin.z() + (depth1 * direction.z()) - (double)z;

                if ((s < -INTERSECTION_EPSILON) || (t < -INTERSECTION_EPSILON) ||
                    ((s + t) > 1.0 + INTERSECTION_EPSILON)) {
                    depth1 = Double.POSITIVE_INFINITY;
                }
            } else {
                depth1 = Double.POSITIVE_INFINITY;
            }
        }
    }

    /**
    First, we check to see if it is even possible for the ray to
    intersect the triangle.
    Rewritten to get around Code Builder FP stack problem.
    Original code:
         if((maxValue(y4,maxValue(y2,y3)) >=
    height1) && (minValue(y4,minValue(y2,y3)) <=
    height2))
    */

    maxHeight = maxValue(y4, maxValue(y2, y3));
    minHeight = minValue(y4, minValue(y2, y3));
    if ((maxHeight >= height1) && (minHeight <= height2)) {
        localNormal = t2V3.crossProduct(t2V2);
        dot = localNormal.dotProduct(direction);

        if ((dot > INTERSECTION_EPSILON) || (dot < -INTERSECTION_EPSILON)) {
            pos1 = localNormal.dotProduct(t2V1);

            pos2 = localNormal.dotProduct(origin);
            pos1 -= pos2;

            depth2 = pos1 / dot;

            if ((depth2 > SMALL_TOLERANCE) && (depth2 < MAX_DISTANCE)) {
                s = origin.x() + (depth2 * direction.x()) - (double)x;
                t = origin.z() + (depth2 * direction.z()) - (double)z;

                if ((s > 1.0 + INTERSECTION_EPSILON) || (t > 1.0 + INTERSECTION_EPSILON) ||
                    ((s + t) < 1.0 - INTERSECTION_EPSILON)) {
                    depth2 = Double.POSITIVE_INFINITY;
                }
            } else {
                depth2 = Double.POSITIVE_INFINITY;
            }
        }
    }

    if ((depth1 >= MAX_DISTANCE) && (depth2 >= MAX_DISTANCE)) {
        return 0;
    }

    state.setDepth(depth2 < depth1 ? depth2 : depth1);
    return 1;
}

    private int intersectSubBlock(HeightFieldBlock block, Vector3Dd origin, Vector3Dd direction, HeightFieldTraversal state, Vector3Dd start, Vector3Dd end) {
    if (block == null) {
        return 0;
    }
    double y1;
    double y2;
    double sx;
    double sy;
    double sz;
    double ex;
    double ez;
    double f;
    int ix;
    int iz;
    int length;
    int i;

    if (minValue(start.y(), end.y()) > block.getMaximumHeight()) {
        return 0;
    }

    if (maxValue(start.y(), end.y()) < block.getMinimumHeight()) {
        return 0;
    }

    sx = start.x();
    sy = start.y();
    sz = start.z();
    ex = end.x();
    ez = end.z();

    if (state.getXDom()) {
        if (state.getIsdx() >= 0) {
            f = Math.floor(sx) - sx;
            sx = Math.floor(sx);
            sy += state.getMyx() * f;
            sz += state.getMzx() * f;
            ex = Math.floor(ex);
            ix = (int)sx;
        } else {
            f = Math.ceil(sx) - sx;
            sx = Math.ceil(sx);
            sy += state.getMyx() * f;
            sz += state.getMzx() * f;
            ex = Math.ceil(ex);
            ix = (int)sx - 1;
        }

        length = (int)(0.5 + Math.abs(ex - sx));

        if (state.getIsdz() >= 0) {
            f = sz - Math.ceil(sz);
            iz = (int)Math.floor(sz);
        } else {
            f = Math.floor(sz) - sz;
            iz = (int)Math.ceil(sz) - 1;
        }

        if (state.getGdy() >= 0.0) {
            y1 = sy;
            y2 = sy + state.getGdy();
        } else {
            y1 = sy + state.getGdy();
            y2 = sy;
        }

        for (i = 0; i <= length; i++) {
            if (intersectPixel(ix, iz, origin, direction, state, y1, y2) != 0) {
                return 1;
            }
            f += state.getGdz();
            if (f >= 0.0) {
                iz += state.getIsdz();
                if (intersectPixel(ix, iz, origin, direction, state, y1, y2) != 0) {
                    return 1;
                }
                f -= 1.0;
            }
            ix += state.getIsdx();
            y1 += state.getGdy();
            y2 += state.getGdy();
        }
    } else {

        if (state.getIsdz() >= 0) {
            f = Math.floor(sz) - sz;
            sz = Math.floor(sz);
            sy += state.getMyz() * f;
            sx += state.getMxz() * f;
            ez = Math.floor(ez);
            iz = (int)sz;
        } else {
            f = Math.ceil(sz) - sz;
            sz = Math.ceil(sz);
            sy += state.getMyz() * f;
            sx += state.getMxz() * f;
            ez = Math.ceil(ez);
            iz = (int)sz - 1;
        }

        length = (int)(0.5 + Math.abs(ez - sz));

        if (state.getIsdx() >= 0) {
            f = sx - Math.ceil(sx);
            ix = (int)Math.floor(sx);
        } else {
            f = Math.floor(sx) - sx;
            ix = (int)Math.ceil(sx) - 1;
        }

        if (state.getGdy() >= 0.0) {
            y1 = sy;
            y2 = sy + state.getGdy();
        } else {
            y1 = sy + state.getGdy();
            y2 = sy;
        }

        for (i = 0; i <= length; i++) {
            if (intersectPixel(ix, iz, origin, direction, state, y1, y2) != 0) {
                return 1;
            }
            f += state.getGdx();
            if (f >= 0.0) {
                ix += state.getIsdx();
                if (intersectPixel(ix, iz, origin, direction, state, y1, y2) != 0) {
                    return 1;
                }
                f -= 1.0;
            }
            iz += state.getIsdz();
            y1 += state.getGdy();
            y2 += state.getGdy();
        }
    }
    return 0;
}

    private int intersectHfNode(Vector3Dd origin, Vector3Dd direction, HeightFieldTraversal state, Vector3Dd start, Vector3Dd end) {
    // cpp swaps pointers to two Vector3Dd values; Java swaps one-element holders.
    Vector3Dd[] curr;
    Vector3Dd[] next;
    Vector3Dd[] temp;
    Vector3Dd[] temp1 = {new Vector3Dd()};
    Vector3Dd[] temp2 = {new Vector3Dd()};
    double sx;
    double sy;
    double sz;
    double ex;
    double ey;
    double ez;
    double x;
    double y;
    double z;
    double tnear;
    double tfar;
    double blockSize;
    double invBlkSize;
    int ix;
    int iz;
    int xSize;
    int zSize;
    int length;
    int i;

    x = sx = start.x();
    y = sy = start.y();
    z = sz = start.z();
    ex = end.x();
    ey = end.y();
    ez = end.z();

    blockSize = this.blockSize;
    invBlkSize = this.inverseBlockSize;

    xSize = Math.abs((int)(ex * invBlkSize) - (int)(sx * invBlkSize));
    zSize = Math.abs((int)(ez * invBlkSize) - (int)(sz * invBlkSize));
    length = xSize + zSize;

    curr = temp1;
    next = temp2;
    curr[0] = new Vector3Dd(x, y, z);
    double t;

    if (state.getXDom()) {
        if (state.getIsdx() >= 0) {
            ix = (int)Math.floor(sx * invBlkSize);
            tnear = blockSize * (ix + 1) - sx;

            if (state.getIsdz() >= 0) {
                iz = (int)Math.floor(sz * invBlkSize);
                tfar = state.getGdx() * (blockSize * (iz + 1) - sz);
            } else {
                iz = (int)Math.ceil(sz * invBlkSize) - 1;
                tfar = state.getGdx() * (sz - blockSize * (iz));
            }
            for (i = 0; i < length; i++) {
                if (tnear < tfar) {
                    t = tnear;
                    x = sx + t;
                    y = sy + state.getMyx() * t;
                    z = sz + state.getMzx() * t;
                    next[0] = new Vector3Dd(x, y, z);
                    if (intersectSubBlock(blockAt(ix, iz), origin, direction, state, curr[0], next[0]) != 0) {
                        return 1;
                    }
                    temp = curr;
                    curr = next;
                    next = temp;
                    ix++;
                    tnear = blockSize * (ix + 1) - sx;
                } else {
                    t = tfar;
                    x = sx + t;
                    y = sy + state.getMyx() * t;
                    z = sz + state.getMzx() * t;
                    next[0] = new Vector3Dd(x, y, z);
                    if (intersectSubBlock(blockAt(ix, iz), origin, direction, state, curr[0], next[0]) != 0) {
                        return 1;
                    }
                    temp = curr;
                    curr = next;
                    next = temp;
                    iz += state.getIsdz();
                    if (state.getIsdz() >= 0) {
                        tfar = state.getGdx() * (blockSize * (iz + 1) - sz);
                    } else {
                        tfar = state.getGdx() * (sz - blockSize * (iz));
                    }
                }
            }
        } else {
            ix = (int)Math.ceil(sx * invBlkSize) - 1;
            tnear = sx - blockSize * (ix);

            if (state.getIsdz() >= 0) {
                iz = (int)Math.floor(sz * invBlkSize);
                tfar = state.getGdx() * (blockSize * (iz + 1) - sz);
            } else {
                iz = (int)Math.ceil(sz * invBlkSize) - 1;
                tfar = state.getGdx() * (sz - blockSize * (iz));
            }

            for (i = 0; i < length; i++) {
                if (tnear < tfar) {
                    t = tnear;
                    x = sx - t;
                    y = sy - state.getMyx() * t;
                    z = sz - state.getMzx() * t;
                    next[0] = new Vector3Dd(x, y, z);
                    if (intersectSubBlock(blockAt(ix, iz), origin, direction, state, curr[0], next[0]) != 0) {
                        return 1;
                    }
                    temp = curr;
                    curr = next;
                    next = temp;
                    ix--;
                    tnear = sx - blockSize * (ix);
                } else {
                    t = tfar;
                    x = sx - t;
                    y = sy - state.getMyx() * t;
                    z = sz - state.getMzx() * t;
                    next[0] = new Vector3Dd(x, y, z);
                    if (intersectSubBlock(blockAt(ix, iz), origin, direction, state, curr[0], next[0]) != 0) {
                        return 1;
                    }
                    temp = curr;
                    curr = next;
                    next = temp;
                    iz += state.getIsdz();
                    if (state.getIsdz() >= 0) {
                        tfar = state.getGdx() * (blockSize * (iz + 1) - sz);
                    } else {
                        tfar = state.getGdx() * (sz - blockSize * (iz));
                    }
                }
            }
        }
    } else {
        if (state.getIsdz() >= 0) {
            iz = (int)Math.floor(sz * invBlkSize);
            tnear = blockSize * (iz + 1) - sz;

            if (state.getIsdx() >= 0) {
                ix = (int)Math.floor(sx * invBlkSize);
                tfar = state.getGdz() * (blockSize * (ix + 1) - sx);
            } else {
                ix = (int)Math.ceil(sx * invBlkSize) - 1;
                tfar = state.getGdz() * (sx - blockSize * (ix));
            }
            for (i = 0; i < length; i++) {
                if (tnear < tfar) {
                    t = tnear;
                    z = sz + t;
                    y = sy + state.getMyz() * t;
                    x = sx + state.getMxz() * t;
                    next[0] = new Vector3Dd(x, y, z);
                    if (intersectSubBlock(blockAt(ix, iz), origin, direction, state, curr[0], next[0]) != 0) {
                        return 1;
                    }
                    temp = curr;
                    curr = next;
                    next = temp;
                    iz++;
                    tnear = blockSize * (iz + 1) - sz;
                } else {
                    t = tfar;
                    z = sz + t;
                    y = sy + state.getMyz() * t;
                    x = sx + state.getMxz() * t;
                    next[0] = new Vector3Dd(x, y, z);
                    if (intersectSubBlock(blockAt(ix, iz), origin, direction, state, curr[0], next[0]) != 0) {
                        return 1;
                    }
                    temp = curr;
                    curr = next;
                    next = temp;
                    ix += state.getIsdx();
                    if (state.getIsdx() >= 0) {
                        tfar = state.getGdz() * (blockSize * (ix + 1) - sx);
                    } else {
                        tfar = state.getGdz() * (sx - blockSize * (ix));
                    }
                }
            }
        } else {
            iz = (int)Math.ceil(sz * invBlkSize) - 1;
            tnear = sz - blockSize * (iz);

            if (state.getIsdx() >= 0) {
                ix = (int)Math.floor(sx * invBlkSize);
                tfar = state.getGdz() * (blockSize * (ix + 1) - sx);
            } else {
                ix = (int)Math.ceil(sx * invBlkSize) - 1;
                tfar = state.getGdz() * (sx - blockSize * (ix));
            }
            for (i = 0; i < length; i++) {
                if (tnear < tfar) {
                    t = tnear;
                    z = sz - t;
                    y = sy - state.getMyz() * t;
                    x = sx - state.getMxz() * t;
                    next[0] = new Vector3Dd(x, y, z);
                    if (intersectSubBlock(blockAt(ix, iz), origin, direction, state, curr[0], next[0]) != 0) {
                        return 1;
                    }
                    temp = curr;
                    curr = next;
                    next = temp;
                    iz--;
                    tnear = sz - blockSize * iz;
                } else {
                    t = tfar;
                    z = sz - t;
                    y = sy - state.getMyz() * t;
                    x = sx - state.getMxz() * t;
                    next[0] = new Vector3Dd(x, y, z);
                    if (intersectSubBlock(blockAt(ix, iz), origin, direction, state, curr[0], next[0]) != 0) {
                        return 1;
                    }
                    temp = curr;
                    curr = next;
                    next = temp;
                    ix += state.getIsdx();
                    if (state.getIsdx() >= 0) {
                        tfar = state.getGdz() * (blockSize * (ix + 1) - sx);
                    } else {
                        tfar = state.getGdz() * (sx - blockSize * (ix));
                    }
                }
            }
        }
    }
    next[0] = new Vector3Dd(ex, ey, ez);
    if (state.getIsdx() >= 0) {
        ix = (int)Math.floor(ex * invBlkSize);
    } else {
        ix = (int)Math.ceil(ex * invBlkSize) - 1;
    }
    if (state.getIsdz() >= 0) {
        iz = (int)Math.floor(ez * invBlkSize);
    } else {
        iz = (int)Math.ceil(ez * invBlkSize) - 1;
    }
    if (intersectSubBlock(blockAt(ix, iz), origin, direction, state, curr[0], next[0]) != 0) {
        return 1;
    }
    return 0;
}

    private boolean isInsideGrid(double x, double z) {
    return x >= 0 && z >= 0 && x <= (double)(columns - 1) && z <= (double)(rows - 1);
}

    private double heightDifference(Vector3Dd origin, Vector3Dd direction, double t, long cellX, long cellZ, boolean firstTriangle) {
    final double x = origin.x() + direction.x()*t;
    final double y = origin.y() + direction.y()*t;
    final double z = origin.z() + direction.z()*t;
    final double fx = x - (double)cellX;
    final double fz = z - (double)cellZ;
    if (firstTriangle) {
        final double y1 = getHeight(cellX, cellZ);
        return y - (y1 + fx*(getHeight(cellX + 1, cellZ) - y1) + fz*(getHeight(cellX, cellZ + 1) - y1));
    }
    final double y4 = getHeight(cellX + 1, cellZ + 1);
    return y - (y4 + (1.0 - fx)*(getHeight(cellX, cellZ + 1) - y4) +
        (1.0 - fz)*(getHeight(cellX + 1, cellZ) - y4));
}

    private void heightRange(double lowX, double highX, double lowZ, double highZ, double[] lowest, double[] highest) {
    long x0 = (long)Math.floor(lowX), x1 = (long)Math.ceil(highX);
    long z0 = (long)Math.floor(lowZ), z1 = (long)Math.ceil(highZ);
    if (x0 < 0) x0 = 0;
    if (z0 < 0) z0 = 0;
    if (x1 > columns - 1) x1 = columns - 1;
    if (z1 > rows - 1) z1 = rows - 1;
    lowest[0] = Double.POSITIVE_INFINITY;
    highest[0] = Double.NEGATIVE_INFINITY;
    for (long z = z0; z <= z1; z++) {
        for (long x = x0; x <= x1; x++) {
            final double h = getHeight(x, z);
            if (h < lowest[0]) lowest[0] = h;
            if (h > highest[0]) highest[0] = h;
        }
    }
}

    private long nearestSurface(Vector3Dd point) {
    long best = 0;
    double distance = Double.POSITIVE_INFINITY;
    final double h = evaluateHeight(point.x(), point.z());
    if (Double.isFinite(h)) distance = Math.abs(point.y() - h);
    final double[] faces = {point.x() - minimum.x(), maximum.x() - point.x(),
        point.y() - minimum.y(), maximum.y() - point.y(), point.z() - minimum.z(), maximum.z() - point.z()};
    for (int i = 0; i < 6; i++) {
        if (Math.abs(faces[i]) < distance) { distance = Math.abs(faces[i]); best = i + 1; }
    }
    return best;
}

    private Vector3Dd surfaceNormal(Vector3Dd point, long surfaceIndex) {
    switch ((int)surfaceIndex) {
    case 1: return new Vector3Dd(-1, 0, 0);
    case 2: return new Vector3Dd(1, 0, 0);
    case 3: return new Vector3Dd(0, -1, 0);
    case 4: return new Vector3Dd(0, 1, 0);
    case 5: return new Vector3Dd(0, 0, -1);
    case 6: return new Vector3Dd(0, 0, 1);
    default: break;
    }
    Vector3Dd[] first = {new Vector3Dd()}, second = {new Vector3Dd()};
    getSurfaceTangents(point, first, second);
    return second[0].crossProduct(first[0]);
}

    private GeometryContainment classifyRegion(Vector3Dd center, Vector3Dd halfWidths, double tolerance) {
    if (!isValid()) return GeometryContainment.Indeterminate;
    if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
    final FloatingInterval x = FloatingInterval.around(center.x(), halfWidths.x());
    final FloatingInterval y = FloatingInterval.around(center.y(), halfWidths.y());
    final FloatingInterval z = FloatingInterval.around(center.z(), halfWidths.z());
    if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment.InvalidInput;
    if (x.getUpper() < minimum.x() || x.getLower() > maximum.x() ||
        y.getUpper() < minimum.y() || y.getLower() > maximum.y() ||
        z.getUpper() < minimum.z() || z.getLower() > maximum.z()) return GeometryContainment.Outside;
    double[] lowestH = {0}, highestH = {0};
    heightRange(x.getLower(), x.getUpper(), z.getLower(), z.getUpper(), lowestH, highestH);
    final double lowest = lowestH[0], highest = highestH[0];
    // The interpolated surface lies between the heights of its cell corners.
    if (y.getLower() > highest) return GeometryContainment.Outside;
    if (x.getLower() > minimum.x() && x.getUpper() < maximum.x() &&
        y.getLower() > minimum.y() && y.getUpper() < maximum.y() &&
        z.getLower() > minimum.z() && z.getUpper() < maximum.z() && y.getUpper() < lowest)
        return GeometryContainment.Inside;
    if (!(tolerance > 0) || !(center.x() > minimum.x()) || !(center.x() < maximum.x()) ||
        !(center.z() > minimum.z()) || !(center.z() < maximum.z())) return GeometryContainment.Indeterminate;
    // A surface point straight above or below the centre within the ball.
    long cx = (long)Math.floor(center.x());
    long cz = (long)Math.floor(center.z());
    if (cx > columns - 2) cx = columns - 2;
    if (cz > rows - 2) cz = rows - 2;
    final FloatingInterval fx = FloatingInterval.exact(center.x()).subtract(FloatingInterval.exact((double)cx));
    final FloatingInterval fz = FloatingInterval.exact(center.z()).subtract(FloatingInterval.exact((double)cz));
    FloatingInterval h = new FloatingInterval();
    if (fx.add(fz).getUpper() <= 1.0) {
        final FloatingInterval y1 = FloatingInterval.exact(getHeight(cx, cz));
        h = y1.add(fx.multiply(FloatingInterval.exact(getHeight(cx + 1, cz)).subtract(y1))).add(
            fz.multiply(FloatingInterval.exact(getHeight(cx, cz + 1)).subtract(y1)));
    } else if (fx.add(fz).getLower() > 1.0) {
        final FloatingInterval y4 = FloatingInterval.exact(getHeight(cx + 1, cz + 1));
        final FloatingInterval one = FloatingInterval.exact(1.0);
        h = y4.add(one.subtract(fx).multiply(FloatingInterval.exact(getHeight(cx, cz + 1)).subtract(y4))).add(
            one.subtract(fz).multiply(FloatingInterval.exact(getHeight(cx + 1, cz)).subtract(y4)));
    } else {
        return GeometryContainment.Indeterminate;
    }
    if (!h.isValid() || !(h.getLower() > minimum.y()) || !(h.getUpper() < maximum.y()))
        return GeometryContainment.Indeterminate;
    final FloatingInterval offset = h.subtract(FloatingInterval.exact(center.y()));
    if (offset.isValid() && Math.max(Math.abs(offset.getLower()),
            Math.abs(offset.getUpper())) < tolerance) return GeometryContainment.Boundary;
    return GeometryContainment.Indeterminate;
}

    // Heights start at zero. A grid needs at least 2 x 2 points.
    public HeightFieldSolid(long columnCount, long rowCount, Vector3Dd minimumCorner, Vector3Dd maximumCorner) {
        this.columns = columnCount < 2 ? 2 : columnCount;
        this.rows = rowCount < 2 ? 2 : rowCount;
        this.minimum = minimumCorner;
        this.maximum = maximumCorner;
        this.blockSize = 0;
        this.inverseBlockSize = 0;
        this.blockColumns = 0;
        this.blockRows = 0;
        this.blocksValid = false;
    heights.ensureCapacity((int)(columns*rows));
    for (long i = 0; i < columns*rows; i++) heights.add(0.0f);
}

    public HeightFieldSolid(HeightFieldSolid other) {
        super();
        this.columns = other.columns;
        this.rows = other.rows;
        heights.addAll(other.heights);
        this.minimum = other.minimum;
        this.maximum = other.maximum;
        this.blockSize = other.blockSize;
        this.inverseBlockSize = other.inverseBlockSize;
        this.blockColumns = other.blockColumns;
        this.blockRows = other.blockRows;
        for (int i = 0; i < other.blocks.size(); i++) blocks.add(new HeightFieldBlock(other.blocks.get(i)));
        this.blocksValid = other.blocksValid;
}

    /** cpp copy assignment. */
    public HeightFieldSolid assign(HeightFieldSolid other) {
    if (this != other) {
        columns = other.columns;
        rows = other.rows;
        heights.clear(); heights.addAll(other.heights);
        minimum = other.minimum;
        maximum = other.maximum;
        blockSize = other.blockSize;
        inverseBlockSize = other.inverseBlockSize;
        blockColumns = other.blockColumns;
        blockRows = other.blockRows;
        blocks.clear();
        for (int i = 0; i < other.blocks.size(); i++) blocks.add(new HeightFieldBlock(other.blocks.get(i)));
        blocksValid = other.blocksValid;
    }
    return this;
}

    public long getColumnCount() { return columns; }

    public long getRowCount() { return rows; }

    public double getHeight(long x, long z) {
    return (double)heights.get((int)(z*columns + x));
}

    public void setHeight(long x, long z, double height) {
    heights.set((int)(z*columns + x), (float)height);
    blocksValid = false;
}

    public Vector3Dd getMinimum() { return minimum; }

    public Vector3Dd getMaximum() { return maximum; }

    public void setBounds(Vector3Dd minimumCorner, Vector3Dd maximumCorner) {
    minimum = minimumCorner;
    maximum = maximumCorner;
    blocksValid = false;
}

    // Grid, finite heights and an ordered box inside the grid domain.
    public boolean isValid() {
    final double[] values = {minimum.x(), minimum.y(), minimum.z(), maximum.x(), maximum.y(), maximum.z()};
    for (int i = 0; i < 6; i++) if (!Double.isFinite(values[i])) return false;
    return minimum.x() < maximum.x() && minimum.y() < maximum.y() && minimum.z() < maximum.z() &&
        minimum.x() >= 0 && minimum.z() >= 0 && maximum.x() <= (double)(columns - 1) &&
        maximum.z() <= (double)(rows - 1);
}

    // Rebuilds the traversal blocks; heights below the box are counted at its
    // floor.
    public void updateBlocks() {
    final int maxX = (int)(columns - 1);
    final int maxZ = (int)(rows - 1);
    final double size = (double)maxValue(maxX, maxZ);
    blockSize = Math.ceil(Math.sqrt(size + 1.0));
    inverseBlockSize = 1.0 / blockSize;
    blockColumns = (long)Math.ceil((maxX + 1.0) * inverseBlockSize);
    blockRows = (long)Math.ceil((maxZ + 1.0) * inverseBlockSize);
    blocks.clear();
    for (long i = 0; i < blockColumns*blockRows; i++) blocks.add(new HeightFieldBlock());
    final int n = (int)blockSize;
    // Adjacent blocks share their border points.
    for (int j = 0; j < blockRows; j++) {
        for (int j2 = 0; (j2 <= n) && (j * n + j2 <= maxZ); j2++) {
            final int z = j * n + j2;
            for (int i = 0; i < blockColumns; i++) {
                for (int i2 = 0; (i2 <= n) && (i * n + i2 <= maxX); i2++) {
                    final int x = i * n + i2;
                    double height = heightAt(x, z);
                    if (height < minimum.y()) {
                        height = minimum.y();
                    }
                    blocks.get((int)((long)i*blockRows + j)).include(height);
                }
            }
        }
    }
    blocksValid = true;
}

    public boolean hasCurrentBlocks() { return blocksValid; }

    // Interpolated height, NaN outside the grid.
    public double evaluateHeight(double x, double z) {
    if (!isInsideGrid(x, z)) return Double.NaN;
    long cx = (long)Math.floor(x);
    long cz = (long)Math.floor(z);
    if (cx > columns - 2) cx = columns - 2;
    if (cz > rows - 2) cz = rows - 2;
    final double fx = x - (double)cx;
    final double fz = z - (double)cz;
    if (fx + fz <= 1.0) {
        final double y1 = getHeight(cx, cz);
        return y1 + fx*(getHeight(cx + 1, cz) - y1) + fz*(getHeight(cx, cz + 1) - y1);
    }
    final double y4 = getHeight(cx + 1, cz + 1);
    return y4 + (1.0 - fx)*(getHeight(cx, cz + 1) - y4) + (1.0 - fz)*(getHeight(cx + 1, cz) - y4);
}

    // Edge vectors of the triangle under the point, in the PovRay order; their
    // cross product second x first is the upward surface normal.
    public void getSurfaceTangents(Vector3Dd point, Vector3Dd[] first, Vector3Dd[] second) {
    int px = (int)point.x();
    int pz = (int)point.z();
    if (px < 0) px = 0;
    if (pz < 0) pz = 0;
    if (px > columns - 2) px = (int)(columns - 2);
    if (pz > rows - 2) pz = (int)(rows - 2);
    final double x = point.x() - (double)px;
    final double z = point.z() - (double)pz;
    if ((x + z) <= 1) {
        final double y1 = heightAt(px, pz);
        final double y2 = heightAt(px + 1, pz);
        final double y3 = heightAt(px, pz + 1);
        first[0] = new Vector3Dd(1.0, y2 - y1, 0.0);
        second[0] = new Vector3Dd(0.0, y3 - y1, 1.0);
    } else {
        final double y1 = heightAt(px + 1, pz + 1);
        final double y2 = heightAt(px, pz + 1);
        final double y3 = heightAt(px + 1, pz);
        first[0] = new Vector3Dd(-1.0, y2 - y1, 0.0);
        second[0] = new Vector3Dd(0.0, y3 - y1, -1.0);
    }
}

    // PovRay first surface hit along origin + depth*direction, depth beyond
    // SMALL_TOLERANCE; requires current blocks. Like PovRay height fields, the
    // box must stay at least one unit inside the grid on x and z
    // (1 <= x <= columns - 2, likewise z); otherwise hits may be missed. It
    // can also report hits on the cells just outside the box. The typed ray
    // queries have neither limitation.
    public HeightFieldTraceResult traceFirstHit(Vector3Dd origin, Vector3Dd direction, double[] depth) {
    depth[0] = 0.0;
    if (!blocksValid) {
        return HeightFieldTraceResult.MissedBounds;
    }
    double[] depth1H = {0.0};
    double[] depth2H = {Double.POSITIVE_INFINITY};
    boolean[] faceOverlap = {false};
    if (Box.intersectBounds(origin, direction, minimum, maximum, INTERSECTION_EPSILON,
            false, depth1H, depth2H, faceOverlap) != RayFirstHitStatus.Hit) {
        return HeightFieldTraceResult.MissedBounds;
    }
    double depth1 = depth1H[0];
    double depth2 = depth2H[0];
    if ((depth1 < SMALL_TOLERANCE) || (depth1 > MAX_DISTANCE)) {
        if ((depth2 < SMALL_TOLERANCE) || (depth2 > MAX_DISTANCE)) {
            return HeightFieldTraceResult.MissedBounds;
        }
        depth1 = depth2;
    } else if ((depth2 < SMALL_TOLERANCE) || (depth2 > MAX_DISTANCE)) {
        depth2 = depth1;
    }
    if (depth1 == depth2) {
        depth1 = 0.0;
    }
    final Vector3Dd temp1 = direction.multiply(depth1).add(origin);
    final Vector3Dd temp2 = direction.multiply(depth2).add(origin);
    double mzx;
    double myx;
    if (Math.abs(direction.x()) > INTERSECTION_EPSILON) {
        mzx = direction.z() / direction.x();
        myx = direction.y() / direction.x();
    } else {
        mzx = direction.z() / INTERSECTION_EPSILON;
        myx = direction.y() / INTERSECTION_EPSILON;
    }
    double mxz;
    double myz;
    if (Math.abs(direction.z()) > INTERSECTION_EPSILON) {
        mxz = direction.x() / direction.z();
        myz = direction.y() / direction.z();
    } else {
        mxz = direction.x() / INTERSECTION_EPSILON;
        myz = direction.y() / INTERSECTION_EPSILON;
    }
    final int isdx = signOf(direction.x());
    final int isdz = signOf(direction.z());
    final boolean xDom = Math.abs(direction.x()) >= Math.abs(direction.z());
    final double gdx = Math.abs(mxz);
    final double gdz = Math.abs(mzx);
    final double gdy = xDom ? myx * (double)isdx : myz * (double)isdz;
    HeightFieldTraversal state = new HeightFieldTraversal(isdx, isdz, xDom, gdx, gdy, gdz, myx, mxz, mzx, myz);
    if (intersectHfNode(origin, direction, state, temp1, temp2) != 0) {
        depth[0] = state.getDepth();
        return HeightFieldTraceResult.Hit;
    }
    return HeightFieldTraceResult.MissedSurface;
}

    @Override
    public Geometry copyGeometry() { return new HeightFieldSolid(this); }

    @Override
    public void getBounds(GeometryBounds result) {
    if (!isValid()) { result.setIndeterminate(); return; }
    double[] lowestH = {0}, highestH = {0};
    heightRange(minimum.x(), maximum.x(), minimum.z(), maximum.z(), lowestH, highestH);
    final double lowest = lowestH[0], highest = highestH[0];
    final double top = Math.min(maximum.y(), highest);
    if (!Double.isFinite(top)) { result.setIndeterminate(); return; }
    if (!(top > minimum.y())) { result.setEmpty(); return; }
    if (!result.setFinite(minimum, new Vector3Dd(maximum.x(), top, maximum.z()))) result.setIndeterminate();
}

    @Override
    public GeometryContainment classifyPoint(Vector3Dd point, double tolerance) {
    if (!Double.isFinite(tolerance) || tolerance < 0) return GeometryContainment.InvalidInput;
    if (tolerance != 0) return classifyPointCertified(point, tolerance);
    if (!isValid()) return GeometryContainment.Indeterminate;
    if (point.x() < minimum.x() || point.x() > maximum.x() || point.y() < minimum.y() ||
        point.y() > maximum.y() || point.z() < minimum.z() || point.z() > maximum.z())
        return GeometryContainment.Outside;
    final double h = evaluateHeight(point.x(), point.z());
    if (!Double.isFinite(h)) return GeometryContainment.Indeterminate;
    if (point.y() > h) return GeometryContainment.Outside;
    if (point.y() < h && point.x() > minimum.x() && point.x() < maximum.x() && point.y() > minimum.y() &&
        point.y() < maximum.y() && point.z() > minimum.z() && point.z() < maximum.z())
        return GeometryContainment.Inside;
    return GeometryContainment.Boundary;
}

    @Override
    public GeometryContainment classifyPointCertified(Vector3Dd point, double tolerance) {
    if (!Double.isFinite(tolerance) || tolerance < 0 || !Double.isFinite(point.x()) ||
        !Double.isFinite(point.y()) || !Double.isFinite(point.z()))
        return GeometryContainment.InvalidInput;
    return classifyRegion(point, new Vector3Dd(tolerance, tolerance, tolerance), tolerance);
}

    @Override
    public GeometryContainment classifyBoxCertified(Vector3Dd center, Vector3Dd halfWidths) {
    final GeometryContainment result = classifyRegion(center, halfWidths, -1.0);
    return result == GeometryContainment.Boundary ? GeometryContainment.Indeterminate : result;
}

    // cpp lambda `addLines` of classifyRay, capturing cuts, entry and exit.
    private static void addLines(ArrayList<Double> cuts, double entry, double exit, double start, double slope) {
        if (slope == 0) return;
        final double a = start + slope*entry, b = start + slope*exit;
        final double low = Math.min(a, b), high = Math.max(a, b);
        for (double k = Math.ceil(low); k <= high; k += 1.0) {
            final double t = (k - start)/slope;
            if (t > entry && t < exit) cuts.add(t);
        }
    }

    @Override
    public RaySolidQueryStatus classifyRay(Ray ray, RayCrossingQuery query, RaySolidClassification result) {
    result.clear();
    final Vector3Dd o = ray.getOrigin();
    final Vector3Dd d = ray.getDirection();
    if (!Double.isFinite(o.x()) || !Double.isFinite(o.y()) || !Double.isFinite(o.z()) ||
        !Double.isFinite(d.x()) || !Double.isFinite(d.y()) || !Double.isFinite(d.z()) ||
        (d.x() == 0 && d.y() == 0 && d.z() == 0) || Double.isNaN(query.minT) ||
        Double.isNaN(query.maxT) || query.minT >= query.maxT) return RaySolidQueryStatus.InvalidInput;
    if (query.minimumChordSquared != 0 || query.minimumExteriorApproach != Double.NEGATIVE_INFINITY)
        return RaySolidQueryStatus.Unsupported;
    if (!isValid()) return RaySolidQueryStatus.Indeterminate;
    double[] entryH = {query.minT}, exitH = {query.maxT};
    boolean[] overlapH = {false};
    final RayFirstHitStatus clip = Box.intersectBounds(o, d, minimum, maximum, 0.0, true,
        entryH, exitH, overlapH);
    final double entry = entryH[0], exit = exitH[0];
    final boolean overlap = overlapH[0];
    if (clip == RayFirstHitStatus.InvalidInput) return RaySolidQueryStatus.InvalidInput;
    if (clip == RayFirstHitStatus.Miss || (clip == RayFirstHitStatus.Hit && !overlap && !(exit > entry))) {
        result.insideSegments.add(false);
        return RaySolidQueryStatus.Supported;
    }
    if (clip != RayFirstHitStatus.Hit || overlap || !Double.isFinite(entry) ||
        !Double.isFinite(exit)) return RaySolidQueryStatus.Indeterminate;

    // Cuts at cell edges and diagonals, where the surface plane changes.
    ArrayList<Double> cuts = new ArrayList<Double>();
    cuts.add(entry);
    cuts.add(exit);
    addLines(cuts, entry, exit, o.x(), d.x());
    addLines(cuts, entry, exit, o.z(), d.z());
    addLines(cuts, entry, exit, o.x() + o.z(), d.x() + d.z());
    cuts.sort(null);

    ArrayList<Double> starts = new ArrayList<Double>();
    ArrayList<Boolean> inside = new ArrayList<Boolean>();
    if (entry > query.minT) { starts.add(query.minT); inside.add(false); }
    for (long i = 0; i + 1 < cuts.size(); i++) {
        final double ta = cuts.get((int)(i)), tb = cuts.get((int)(i + 1));
        if (!(tb > ta)) continue;
        final double mid = 0.5*(ta + tb);
        long cx = (long)Math.floor(o.x() + d.x()*mid);
        long cz = (long)Math.floor(o.z() + d.z()*mid);
        if (cx < 0) cx = 0;
        if (cz < 0) cz = 0;
        if (cx > columns - 2) cx = columns - 2;
        if (cz > rows - 2) cz = rows - 2;
        final boolean first = (o.x() + d.x()*mid - (double)cx) + (o.z() + d.z()*mid - (double)cz) <= 1.0;
        final double ga = heightDifference(o, d, ta, cx, cz, first);
        final double gb = heightDifference(o, d, tb, cx, cz, first);
        if (!Double.isFinite(ga) || !Double.isFinite(gb)) {
            result.clear(); return RaySolidQueryStatus.Indeterminate;
        }
        if ((ga < 0 && gb > 0) || (ga > 0 && gb < 0)) {
            // One crossing of the plane inside this piece.
            final double root = ta + (tb - ta)*(ga/(ga - gb));
            starts.add(ta); inside.add(ga < 0);
            if (root > ta && root < tb) { starts.add(root); inside.add(gb < 0); }
            continue;
        }
        final double gm = heightDifference(o, d, mid, cx, cz, first);
        if (gm == 0 || !Double.isFinite(gm)) {
            result.clear(); return RaySolidQueryStatus.Indeterminate;
        }
        starts.add(ta); inside.add(gm < 0);
    }
    if (exit < query.maxT) { starts.add(exit); inside.add(false); }
    if (starts.isEmpty()) { result.insideSegments.add(false); return RaySolidQueryStatus.Supported; }
    boolean state = inside.get((int)(0));
    result.insideSegments.add(state);
    for (long i = 1; i < starts.size(); i++) {
        if (inside.get((int)(i)) == state) continue;
        state = inside.get((int)(i));
        final double t = starts.get((int)(i));
        long surface = 0;
        if (t == entry || t == exit) surface = nearestSurface(o.add(d.multiply(t)));
        if (!result.boundaries.add(new RaySolidBoundary(t, this, false, surface)) ||
            !result.insideSegments.add(state)) {
            result.clear(); return RaySolidQueryStatus.Indeterminate;
        }
    }
    return RaySolidQueryStatus.Supported;
}

    @Override
    public RayCrossingSupport emitRayCrossings(Ray ray, RayCrossingQuery query, RayCrossingSink sink) {
    RaySolidClassification classification = new RaySolidClassification();
    final RaySolidQueryStatus status = classifyRay(ray, query, classification);
    if (status == RaySolidQueryStatus.InvalidInput) return RayCrossingSupport.InvalidInput;
    if (status == RaySolidQueryStatus.Unsupported) return RayCrossingSupport.Unsupported;
    if (status != RaySolidQueryStatus.Supported) return RayCrossingSupport.Indeterminate;
    for (long i = 0; i < classification.boundaries.size(); i++) {
        sink.crossing(new RayCrossing(classification.boundaries.get((int)(i)).t, this,
            classification.insideSegments.get((int)(i + 1)) ? RayCrossingKind.Entering : RayCrossingKind.Leaving));
    }
    return RayCrossingSupport.Supported;
}

    @Override
    public RayFirstHitStatus queryFirstHit(Ray ray, RayCrossingQuery query, RayHit hit, RayBoundary boundary) {
    if (hit != null) hit.clear();
    if (boundary != null) boundary.assign(new RayBoundary());
    if (!Double.isNaN(query.minT) && query.minT == query.maxT) return RayFirstHitStatus.Unsupported;
    RaySolidClassification classification = new RaySolidClassification();
    final RaySolidQueryStatus status = classifyRay(ray, query, classification);
    if (status == RaySolidQueryStatus.InvalidInput) return RayFirstHitStatus.InvalidInput;
    if (status == RaySolidQueryStatus.Unsupported) return RayFirstHitStatus.Unsupported;
    if (status != RaySolidQueryStatus.Supported) return RayFirstHitStatus.Indeterminate;
    if (classification.boundaries.isEmpty()) return RayFirstHitStatus.Miss;
    final RayBoundary first = classification.boundaries.get((int)(0));
    if (boundary != null) boundary.assign(first);
    if (hit != null) {
        hit.setHitDistance(first.t);
        if (hit.shouldStoreRay()) hit.setRay(ray.withT(first.t));
        if (hit.needsAnySurfaceData()) {
            hit.beginDetailEvaluation();
            final Vector3Dd point = ray.getOrigin().add(ray.getDirection().multiply(first.t));
            if (hit.needsPoint()) { hit.point = point; hit.markDetailAvailable(RayHit.DETAIL_POINT); }
            if (hit.needsNormal()) {
                final Vector3Dd normal = surfaceNormal(point, first.surfaceIndex);
                final double length = normal.length();
                if (Double.isFinite(length) && length > 0) {
                    hit.normal = normal.multiply(1.0/length);
                    hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
                }
            }
        }
    }
    return RayFirstHitStatus.Hit;
}

    @Override
    public RayFirstHitStatus querySurfaceDetail(Ray ray, RayBoundary boundary, RayHit hit) {
    if (hit != null) hit.clear();
    if (hit == null || boundary.geometry != this || !Double.isFinite(boundary.t) ||
        boundary.surfaceIndex < 0 || boundary.surfaceIndex > 6)
        return RayFirstHitStatus.InvalidInput;
    if (boundary.getInstanceCount() != 0) return RayFirstHitStatus.Unsupported;
    hit.setHitDistance(boundary.t);
    if (hit.shouldStoreRay()) hit.setRay(ray.withT(boundary.t));
    hit.beginDetailEvaluation();
    final Vector3Dd point = ray.getOrigin().add(ray.getDirection().multiply(boundary.t));
    if (hit.needsPoint()) { hit.point = point; hit.markDetailAvailable(RayHit.DETAIL_POINT); }
    if (hit.needsNormal()) {
        final Vector3Dd normal = surfaceNormal(point, boundary.surfaceIndex);
        final double length = normal.length();
        if (Double.isFinite(length) && length > 0) {
            hit.normal = normal.multiply((boundary.normalReversed ? -1.0 : 1.0)/length);
            hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
        }
    }
    return RayFirstHitStatus.Hit;
}

    @Override
    public boolean doIntersectionFirstHit(Ray ray, RayHit hit) {
    return queryFirstHit(ray, new RayCrossingQuery(1e-9, Double.POSITIVE_INFINITY), hit) ==
        RayFirstHitStatus.Hit;
}

    @Override
    public void doExtraInformation(Ray ray, double t, RayHit hit) {
    if (hit == null) return;
    hit.beginDetailEvaluation();
    final Vector3Dd point = ray.getOrigin().add(ray.getDirection().multiply(t));
    if (hit.needsPoint()) { hit.point = point; hit.markDetailAvailable(RayHit.DETAIL_POINT); }
    if (hit.needsNormal()) {
        final Vector3Dd normal = surfaceNormal(point, nearestSurface(point));
        final double length = normal.length();
        if (Double.isFinite(length) && length > 0) {
            hit.normal = normal.multiply(1.0/length);
            hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
        }
    }
}

    @Override
    public int doContainmentTest(Vector3Dd point, double tolerance) {
    // Legacy integer adapter over the typed contract.
    final GeometryContainment result = classifyPoint(point, tolerance);
    if (result == GeometryContainment.Inside) return INSIDE;
    if (result == GeometryContainment.Boundary) return LIMIT;
    if (result == GeometryContainment.Outside) return OUTSIDE;
    throw new IllegalStateException("Height field containment cannot be represented by the legacy integer result");
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
