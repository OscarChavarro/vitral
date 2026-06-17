package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.sameCoordinate;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.addUniqueCoordinate;

/**
Structural-shape boolean fallback for axis-aligned cell decompositions: when
both operands are axis-aligned, classifies the uniform cell grid against each
operand and rebuilds the boolean result cell-by-cell. Extracted verbatim from
{@link _PolyhedralBoundedSolidSetOperator} in Stage 7 R2 (one family per class);
pure code motion, no behavior change.
 */
final class _PolyhedralBoundedSolidAxisAlignedCellFallback
    extends _PolyhedralBoundedSolidOperator
{
    private static final class AxisAlignedCellBooleanBuilder
    {
        private final PolyhedralBoundedSolid solid;
        private final ArrayList<Double> xs;
        private final ArrayList<Double> ys;
        private final ArrayList<Double> zs;
        private final HashMap<String, _PolyhedralBoundedSolidVertex> vertices;
        private final HashMap<String, _PolyhedralBoundedSolidEdge> edges;
        private int nextVertexId;
        private int nextFaceId;

        private AxisAlignedCellBooleanBuilder(ArrayList<Double> xs,
                                              ArrayList<Double> ys,
                                              ArrayList<Double> zs)
        {
            this.solid = new PolyhedralBoundedSolid();
            this.xs = xs;
            this.ys = ys;
            this.zs = zs;
            this.vertices =
                new HashMap<String, _PolyhedralBoundedSolidVertex>();
            this.edges = new HashMap<String, _PolyhedralBoundedSolidEdge>();
            this.nextVertexId = 1;
            this.nextFaceId = 1;
        }

        private String vertexKey(int ix, int iy, int iz)
        {
            return ix + ":" + iy + ":" + iz;
        }

        private _PolyhedralBoundedSolidVertex vertexAt(int ix, int iy, int iz)
        {
            String key;
            _PolyhedralBoundedSolidVertex vertex;

            key = vertexKey(ix, iy, iz);
            vertex = vertices.get(key);
            if ( vertex != null ) {
                return vertex;
            }

            vertex = new _PolyhedralBoundedSolidVertex(solid,
                new Vector3Dd(xs.get(ix), ys.get(iy), zs.get(iz)),
                nextVertexId);
            solid.setMaxVertexId(nextVertexId);
            nextVertexId++;
            vertices.put(key, vertex);
            return vertex;
        }

        private String edgeKey(_PolyhedralBoundedSolidVertex a,
                               _PolyhedralBoundedSolidVertex b)
        {
            if ( a.id < b.id ) {
                return a.id + ":" + b.id;
            }
            return b.id + ":" + a.id;
        }

        private void attachEdge(_PolyhedralBoundedSolidHalfEdge he,
                                _PolyhedralBoundedSolidVertex a,
                                _PolyhedralBoundedSolidVertex b)
        {
            String key;
            _PolyhedralBoundedSolidEdge edge;

            key = edgeKey(a, b);
            edge = edges.get(key);
            if ( edge == null ) {
                edge = new _PolyhedralBoundedSolidEdge(solid);
                edge.rightHalf = he;
                edges.put(key, edge);
            }
            else if ( edge.leftHalf == null ) {
                edge.leftHalf = he;
            }
            else if ( edge.rightHalf == null ) {
                edge.rightHalf = he;
            }
            he.parentEdge = edge;
        }

        private void addQuad(int[][] corners)
        {
            _PolyhedralBoundedSolidFace face;
            _PolyhedralBoundedSolidLoop loop;
            _PolyhedralBoundedSolidHalfEdge[] halfEdges;
            _PolyhedralBoundedSolidVertex[] faceVertices;
            int i;

            face = new _PolyhedralBoundedSolidFace(solid, nextFaceId);
            solid.setMaxFaceId(nextFaceId);
            nextFaceId++;
            loop = new _PolyhedralBoundedSolidLoop(face);
            halfEdges = new _PolyhedralBoundedSolidHalfEdge[corners.length];
            faceVertices =
                new _PolyhedralBoundedSolidVertex[corners.length];

            for ( i = 0; i < corners.length; i++ ) {
                faceVertices[i] = vertexAt(corners[i][0], corners[i][1],
                    corners[i][2]);
                halfEdges[i] = new _PolyhedralBoundedSolidHalfEdge(
                    faceVertices[i], loop, solid);
                loop.halfEdgesList.add(halfEdges[i]);
                if ( faceVertices[i].emanatingHalfEdge == null ) {
                    faceVertices[i].emanatingHalfEdge = halfEdges[i];
                }
            }
            loop.boundaryStartHalfEdge = halfEdges[0];

            for ( i = 0; i < halfEdges.length; i++ ) {
                attachEdge(halfEdges[i], faceVertices[i],
                    faceVertices[(i + 1) % faceVertices.length]);
            }
        }

        private PolyhedralBoundedSolid result()
        {
            return solid;
        }
    }

    private static boolean isAxisAlignedEdge(_PolyhedralBoundedSolidEdge edge)
    {
        Vector3Dd a;
        Vector3Dd b;
        int changingAxes;

        if ( edge == null || edge.rightHalf == null || edge.leftHalf == null ) {
            return false;
        }
        a = edge.rightHalf.startingVertex.position;
        b = edge.leftHalf.startingVertex.position;
        changingAxes = 0;
        if ( !sameCoordinate(a.x(), b.x()) ) {
            changingAxes++;
        }
        if ( !sameCoordinate(a.y(), b.y()) ) {
            changingAxes++;
        }
        if ( !sameCoordinate(a.z(), b.z()) ) {
            changingAxes++;
        }
        return changingAxes <= 1;
    }

    private static boolean isAxisAlignedSolid(PolyhedralBoundedSolid solid)
    {
        int i;

        if ( solid == null || solid.getEdgesList().size() <= 0 ) {
            return false;
        }
        for ( i = 0; i < solid.getEdgesList().size(); i++ ) {
            if ( !isAxisAlignedEdge(solid.getEdgesList().get(i)) ) {
                return false;
            }
        }
        return true;
    }

    private static int classifyPointForAxisAlignedFallback(
        PolyhedralBoundedSolid solid,
        Vector3Dd point)
    {
        Ray ray;
        ArrayList<Double> distances;
        int i;
        int hits;
        double eps;

        if ( solid == null || solid.getPolygonsList().size() <= 0 ) {
            return Geometry.OUTSIDE;
        }

        eps = numericContext.bigEpsilon();
        ray = new Ray(point, new Vector3Dd(1.0, 0.371, 0.137));
        distances = new ArrayList<Double>();
        hits = 0;

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face;
            Ray hit;
            Vector3Dd p;
            int status;
            int j;
            boolean duplicate;

            face = solid.getPolygonsList().get(i);
            if ( face.getContainingPlane() == null ) {
                continue;
            }
            hit = face.getContainingPlane().doIntersection(new Ray(ray));
            if ( hit == null || hit.getT() <= eps ) {
                continue;
            }
            p = hit.getOrigin().add(hit.getDirection().multiply(hit.getT()));
            status = face.testPointInside(p, eps);
            if ( status != Geometry.INSIDE ) {
                continue;
            }

            duplicate = false;
            for ( j = 0; j < distances.size(); j++ ) {
                if ( Math.abs(distances.get(j) - hit.getT()) <= eps ) {
                    duplicate = true;
                    break;
                }
            }
            if ( !duplicate ) {
                distances.add(hit.getT());
                hits++;
            }
        }

        return (hits % 2) == 1 ? Geometry.INSIDE : Geometry.OUTSIDE;
    }

    static boolean axisAlignedCellSelected(boolean insideA,
                                                   boolean insideB,
                                                   int op)
    {
        if ( op == UNION ) {
            return insideA || insideB;
        }
        if ( op == INTERSECTION ) {
            return insideA && insideB;
        }
        return insideA && !insideB;
    }

    private static void addAxisAlignedBoundaryQuad(
        AxisAlignedCellBooleanBuilder builder,
        int axis,
        boolean positiveSide,
        int ix,
        int iy,
        int iz)
    {
        if ( axis == 0 && !positiveSide ) {
            builder.addQuad(new int[][] {
                {ix, iy, iz}, {ix, iy, iz + 1},
                {ix, iy + 1, iz + 1}, {ix, iy + 1, iz}
            });
        }
        else if ( axis == 0 ) {
            builder.addQuad(new int[][] {
                {ix + 1, iy, iz}, {ix + 1, iy + 1, iz},
                {ix + 1, iy + 1, iz + 1}, {ix + 1, iy, iz + 1}
            });
        }
        else if ( axis == 1 && !positiveSide ) {
            builder.addQuad(new int[][] {
                {ix, iy, iz}, {ix + 1, iy, iz},
                {ix + 1, iy, iz + 1}, {ix, iy, iz + 1}
            });
        }
        else if ( axis == 1 ) {
            builder.addQuad(new int[][] {
                {ix, iy + 1, iz}, {ix, iy + 1, iz + 1},
                {ix + 1, iy + 1, iz + 1}, {ix + 1, iy + 1, iz}
            });
        }
        else if ( axis == 2 && !positiveSide ) {
            builder.addQuad(new int[][] {
                {ix, iy, iz}, {ix, iy + 1, iz},
                {ix + 1, iy + 1, iz}, {ix + 1, iy, iz}
            });
        }
        else {
            builder.addQuad(new int[][] {
                {ix, iy, iz + 1}, {ix + 1, iy, iz + 1},
                {ix + 1, iy + 1, iz + 1}, {ix, iy + 1, iz + 1}
            });
        }
    }

    static PolyhedralBoundedSolid buildAxisAlignedCellBooleanFallback(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        ArrayList<Double> xs;
        ArrayList<Double> ys;
        ArrayList<Double> zs;
        boolean[][][] occupied;
        AxisAlignedCellBooleanBuilder builder;
        int ix;
        int iy;
        int iz;

        if ( !isAxisAlignedSolid(inSolidA) ||
             !isAxisAlignedSolid(inSolidB) ) {
            return null;
        }

        xs = uniqueVertexCoordinates(inSolidA, 0);
        ys = uniqueVertexCoordinates(inSolidA, 1);
        zs = uniqueVertexCoordinates(inSolidA, 2);
        for ( ix = 0; ix < inSolidB.getVerticesList().size(); ix++ ) {
            Vector3Dd p = inSolidB.getVerticesList().get(ix).position;
            addUniqueCoordinate(xs, p.x());
            addUniqueCoordinate(ys, p.y());
            addUniqueCoordinate(zs, p.z());
        }

        if ( xs.size() < 2 || ys.size() < 2 || zs.size() < 2 ||
             xs.size() > 16 || ys.size() > 16 || zs.size() > 16 ) {
            return null;
        }

        occupied = new boolean[xs.size() - 1][ys.size() - 1][zs.size() - 1];
        for ( ix = 0; ix < xs.size() - 1; ix++ ) {
            for ( iy = 0; iy < ys.size() - 1; iy++ ) {
                for ( iz = 0; iz < zs.size() - 1; iz++ ) {
                    Vector3Dd sample;
                    boolean insideA;
                    boolean insideB;

                    sample = new Vector3Dd(
                        (xs.get(ix) + xs.get(ix + 1)) * 0.5,
                        (ys.get(iy) + ys.get(iy + 1)) * 0.5,
                        (zs.get(iz) + zs.get(iz + 1)) * 0.5);
                    insideA = classifyPointForAxisAlignedFallback(
                        inSolidA, sample) == Geometry.INSIDE;
                    insideB = classifyPointForAxisAlignedFallback(
                        inSolidB, sample) == Geometry.INSIDE;
                    occupied[ix][iy][iz] =
                        axisAlignedCellSelected(insideA, insideB, op);
                }
            }
        }

        builder = new AxisAlignedCellBooleanBuilder(xs, ys, zs);
        for ( ix = 0; ix < xs.size() - 1; ix++ ) {
            for ( iy = 0; iy < ys.size() - 1; iy++ ) {
                for ( iz = 0; iz < zs.size() - 1; iz++ ) {
                    if ( !occupied[ix][iy][iz] ) {
                        continue;
                    }
                    if ( ix == 0 || !occupied[ix - 1][iy][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 0, false,
                            ix, iy, iz);
                    }
                    if ( ix == xs.size() - 2 ||
                         !occupied[ix + 1][iy][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 0, true,
                            ix, iy, iz);
                    }
                    if ( iy == 0 || !occupied[ix][iy - 1][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 1, false,
                            ix, iy, iz);
                    }
                    if ( iy == ys.size() - 2 ||
                         !occupied[ix][iy + 1][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 1, true,
                            ix, iy, iz);
                    }
                    if ( iz == 0 || !occupied[ix][iy][iz - 1] ) {
                        addAxisAlignedBoundaryQuad(builder, 2, false,
                            ix, iy, iz);
                    }
                    if ( iz == zs.size() - 2 ||
                         !occupied[ix][iy][iz + 1] ) {
                        addAxisAlignedBoundaryQuad(builder, 2, true,
                            ix, iy, iz);
                    }
                }
            }
        }

        return builder.result();
    }

    private static ArrayList<Double> uniformCoordinates(double min,
                                                        double max,
                                                        int divisions)
    {
        ArrayList<Double> coordinates;
        int i;

        coordinates = new ArrayList<Double>();
        for ( i = 0; i <= divisions; i++ ) {
            coordinates.add(min + (max - min) * i / divisions);
        }
        return coordinates;
    }

    private static PolyhedralBoundedSolid
    buildUniformSampledCellBooleanFallback(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        final int divisions = 12;
        ArrayList<Double> xs;
        ArrayList<Double> ys;
        ArrayList<Double> zs;
        boolean[][][] occupied;
        AxisAlignedCellBooleanBuilder builder;
        double[] bounds;
        boolean anyOccupied;
        int ix;
        int iy;
        int iz;

        if ( inSolidA == null ||
             inSolidB == null ||
             inSolidA.getVerticesList().size() <= 0 ||
             inSolidB.getVerticesList().size() <= 0 ) {
            return null;
        }

        bounds = inSolidA.getMinMax();
        if ( op == UNION ) {
            double[] boundsB = inSolidB.getMinMax();

            bounds[0] = Math.min(bounds[0], boundsB[0]);
            bounds[1] = Math.min(bounds[1], boundsB[1]);
            bounds[2] = Math.min(bounds[2], boundsB[2]);
            bounds[3] = Math.max(bounds[3], boundsB[3]);
            bounds[4] = Math.max(bounds[4], boundsB[4]);
            bounds[5] = Math.max(bounds[5], boundsB[5]);
        }

        xs = uniformCoordinates(bounds[0], bounds[3], divisions);
        ys = uniformCoordinates(bounds[1], bounds[4], divisions);
        zs = uniformCoordinates(bounds[2], bounds[5], divisions);
        occupied = new boolean[divisions][divisions][divisions];
        anyOccupied = false;

        for ( ix = 0; ix < divisions; ix++ ) {
            for ( iy = 0; iy < divisions; iy++ ) {
                for ( iz = 0; iz < divisions; iz++ ) {
                    Vector3Dd sample;
                    boolean insideA;
                    boolean insideB;

                    sample = new Vector3Dd(
                        (xs.get(ix) + xs.get(ix + 1)) * 0.5,
                        (ys.get(iy) + ys.get(iy + 1)) * 0.5,
                        (zs.get(iz) + zs.get(iz + 1)) * 0.5);
                    insideA = classifyPointForAxisAlignedFallback(
                        inSolidA, sample) == Geometry.INSIDE;
                    insideB = classifyPointForAxisAlignedFallback(
                        inSolidB, sample) == Geometry.INSIDE;
                    occupied[ix][iy][iz] =
                        axisAlignedCellSelected(insideA, insideB, op);
                    anyOccupied |= occupied[ix][iy][iz];
                }
            }
        }

        if ( !anyOccupied ) {
            return null;
        }

        builder = new AxisAlignedCellBooleanBuilder(xs, ys, zs);
        for ( ix = 0; ix < divisions; ix++ ) {
            for ( iy = 0; iy < divisions; iy++ ) {
                for ( iz = 0; iz < divisions; iz++ ) {
                    if ( !occupied[ix][iy][iz] ) {
                        continue;
                    }
                    if ( ix == 0 || !occupied[ix - 1][iy][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 0, false,
                            ix, iy, iz);
                    }
                    if ( ix == divisions - 1 ||
                         !occupied[ix + 1][iy][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 0, true,
                            ix, iy, iz);
                    }
                    if ( iy == 0 || !occupied[ix][iy - 1][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 1, false,
                            ix, iy, iz);
                    }
                    if ( iy == divisions - 1 ||
                         !occupied[ix][iy + 1][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 1, true,
                            ix, iy, iz);
                    }
                    if ( iz == 0 || !occupied[ix][iy][iz - 1] ) {
                        addAxisAlignedBoundaryQuad(builder, 2, false,
                            ix, iy, iz);
                    }
                    if ( iz == divisions - 1 ||
                         !occupied[ix][iy][iz + 1] ) {
                        addAxisAlignedBoundaryQuad(builder, 2, true,
                            ix, iy, iz);
                    }
                }
            }
        }
        return builder.result();
    }
}
