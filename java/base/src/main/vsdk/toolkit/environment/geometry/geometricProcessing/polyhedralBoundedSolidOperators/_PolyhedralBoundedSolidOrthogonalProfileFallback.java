package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.HashMap;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.sameCoordinate;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.addUniqueCoordinate;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.extractProfileAtX;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.sameProfilePoint;

/**
Structural-shape boolean fallback for orthogonal extruded profiles (X-extruded
YZ and Y-extruded XZ): detects the profile-cell boolean case and rebuilds the
result cell-by-cell. Extracted verbatim from
{@link PolyhedralBoundedSolidSetOperator} in Stage 7 R2 (one family per class);
pure code motion, no behavior change.
 */
final class _PolyhedralBoundedSolidOrthogonalProfileFallback
    extends _PolyhedralBoundedSolidOperator
{
    private static final int PROFILE_X_EXTRUDED_YZ = 0;
    private static final int PROFILE_Y_EXTRUDED_XZ = 1;

    private static final class OrthogonalProfileOperandSpec
    {
        private final int type;
        private final double[] bounds;
        private final ArrayList<Vector3Dd> yzProfile;
        private final ArrayList<Double> rightBoundaryZ;
        private final ArrayList<Double> rightBoundaryX;

        private OrthogonalProfileOperandSpec(int type,
                                             double[] bounds,
                                             ArrayList<Vector3Dd> yzProfile,
                                             ArrayList<Double> rightBoundaryZ,
                                             ArrayList<Double> rightBoundaryX)
        {
            this.type = type;
            this.bounds = bounds;
            this.yzProfile = yzProfile;
            this.rightBoundaryZ = rightBoundaryZ;
            this.rightBoundaryX = rightBoundaryX;
        }

        private boolean contains(double x, double y, double z)
        {
            if ( type == PROFILE_X_EXTRUDED_YZ ) {
                return x >= bounds[0] - numericContext.bigEpsilon() &&
                       x <= bounds[3] + numericContext.bigEpsilon() &&
                       pointInsideYZProfile(yzProfile, y, z);
            }

            return y >= bounds[1] - numericContext.bigEpsilon() &&
                   y <= bounds[4] + numericContext.bigEpsilon() &&
                   z >= bounds[2] - numericContext.bigEpsilon() &&
                   z <= bounds[5] + numericContext.bigEpsilon() &&
                   x >= bounds[0] - numericContext.bigEpsilon() &&
                   x <= rightXAtZ(z) + numericContext.bigEpsilon();
        }

        private double rightXAtZ(double z)
        {
            int i;

            if ( rightBoundaryZ == null || rightBoundaryZ.isEmpty() ) {
                return bounds[3];
            }
            if ( z <= rightBoundaryZ.get(0) + numericContext.bigEpsilon() ) {
                return rightBoundaryX.get(0);
            }
            for ( i = 0; i < rightBoundaryZ.size() - 1; i++ ) {
                double z0;
                double z1;
                double x0;
                double x1;
                double t;

                z0 = rightBoundaryZ.get(i);
                z1 = rightBoundaryZ.get(i + 1);
                x0 = rightBoundaryX.get(i);
                x1 = rightBoundaryX.get(i + 1);
                if ( z <= z1 + numericContext.bigEpsilon() ) {
                    if ( sameCoordinate(z0, z1) ) {
                        return x0;
                    }
                    t = (z - z0) / (z1 - z0);
                    return x0 + (x1 - x0) * t;
                }
            }
            return rightBoundaryX.get(rightBoundaryX.size() - 1);
        }
    }

    private static final class OrthogonalProfileBooleanFallbackSpec
    {
        private final OrthogonalProfileOperandSpec operandA;
        private final OrthogonalProfileOperandSpec operandB;
        private final OrthogonalProfileOperandSpec yExtruded;
        private final double xMin;
        private final double xMax;
        private final ArrayList<Double> ys;
        private final ArrayList<Double> zs;

        private OrthogonalProfileBooleanFallbackSpec(
            OrthogonalProfileOperandSpec operandA,
            OrthogonalProfileOperandSpec operandB,
            OrthogonalProfileOperandSpec yExtruded,
            double xMin,
            double xMax,
            ArrayList<Double> ys,
            ArrayList<Double> zs)
        {
            this.operandA = operandA;
            this.operandB = operandB;
            this.yExtruded = yExtruded;
            this.xMin = xMin;
            this.xMax = xMax;
            this.ys = ys;
            this.zs = zs;
        }

        private double xAtBoundary(int boundary, double z)
        {
            if ( boundary == 0 ) {
                return xMin;
            }
            if ( boundary == 1 ) {
                return yExtruded.rightXAtZ(z);
            }
            return xMax;
        }

        private Vector3Dd point(int boundary, int iy, int iz)
        {
            double z;

            z = zs.get(iz);
            return new Vector3Dd(xAtBoundary(boundary, z), ys.get(iy), z);
        }
    }

    private static final class ProfileCellBooleanBuilder
    {
        private final PolyhedralBoundedSolid solid;
        private final HashMap<String, _PolyhedralBoundedSolidVertex> vertices;
        private final HashMap<String, _PolyhedralBoundedSolidEdge> edges;
        private int nextVertexId;
        private int nextFaceId;

        private ProfileCellBooleanBuilder()
        {
            solid = new PolyhedralBoundedSolid();
            vertices = new HashMap<String, _PolyhedralBoundedSolidVertex>();
            edges = new HashMap<String, _PolyhedralBoundedSolidEdge>();
            nextVertexId = 1;
            nextFaceId = 1;
        }

        private long coordinateKey(double value)
        {
            return Math.round(value * 1000000000000.0);
        }

        private String vertexKey(Vector3Dd point)
        {
            return coordinateKey(point.x()) + ":" +
                   coordinateKey(point.y()) + ":" +
                   coordinateKey(point.z());
        }

        private _PolyhedralBoundedSolidVertex vertexAt(Vector3Dd point)
        {
            String key;
            _PolyhedralBoundedSolidVertex vertex;

            key = vertexKey(point);
            vertex = vertices.get(key);
            if ( vertex != null ) {
                return vertex;
            }

            vertex = new _PolyhedralBoundedSolidVertex(solid, point,
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

        private boolean degenerateQuad(Vector3Dd[] corners)
        {
            int i;

            for ( i = 0; i < corners.length; i++ ) {
                if ( sameProfilePoint(corners[i],
                         corners[(i + 1) % corners.length]) ) {
                    return true;
                }
            }
            return false;
        }

        private void addQuad(Vector3Dd[] corners)
        {
            _PolyhedralBoundedSolidFace face;
            _PolyhedralBoundedSolidLoop loop;
            _PolyhedralBoundedSolidHalfEdge[] halfEdges;
            _PolyhedralBoundedSolidVertex[] faceVertices;
            int i;

            if ( corners == null || corners.length < 3 ||
                 degenerateQuad(corners) ) {
                return;
            }

            face = new _PolyhedralBoundedSolidFace(solid, nextFaceId);
            solid.setMaxFaceId(nextFaceId);
            nextFaceId++;
            loop = new _PolyhedralBoundedSolidLoop(face);
            halfEdges = new _PolyhedralBoundedSolidHalfEdge[corners.length];
            faceVertices =
                new _PolyhedralBoundedSolidVertex[corners.length];

            for ( i = 0; i < corners.length; i++ ) {
                faceVertices[i] = vertexAt(corners[i]);
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

    private static boolean pointInsideYZProfile(ArrayList<Vector3Dd> profile,
                                                double y,
                                                double z)
    {
        boolean inside;
        int i;
        int j;

        if ( profile == null || profile.size() < 3 ) {
            return false;
        }

        inside = false;
        j = profile.size() - 1;
        for ( i = 0; i < profile.size(); i++ ) {
            double yi;
            double zi;
            double yj;
            double zj;

            yi = profile.get(i).y();
            zi = profile.get(i).z();
            yj = profile.get(j).y();
            zj = profile.get(j).z();
            if ( ((zi > z) != (zj > z)) &&
                 y < (yj - yi) * (z - zi) / (zj - zi) + yi ) {
                inside = !inside;
            }
            j = i;
        }
        return inside;
    }

    private static OrthogonalProfileOperandSpec createXExtrudedYZSpec(
        PolyhedralBoundedSolid solid,
        ArrayList<Double> xs,
        ArrayList<Double> ys,
        ArrayList<Double> zs)
    {
        ArrayList<Vector3Dd> profile;
        double[] bounds;

        if ( xs.size() != 2 || ys.size() != 4 || zs.size() != 3 ||
             solid.getVerticesList().size() != 16 ) {
            return null;
        }
        bounds = solid.getMinMax();
        profile = extractProfileAtX(solid, bounds[0]);
        if ( profile == null || profile.size() < 3 ) {
            profile = extractProfileAtX(solid, bounds[3]);
        }
        if ( profile == null || profile.size() < 3 ) {
            return null;
        }
        return new OrthogonalProfileOperandSpec(PROFILE_X_EXTRUDED_YZ,
            bounds, profile, null, null);
    }

    private static OrthogonalProfileOperandSpec createYExtrudedXZSpec(
        PolyhedralBoundedSolid solid,
        ArrayList<Double> xs,
        ArrayList<Double> ys,
        ArrayList<Double> zs)
    {
        ArrayList<Double> rightZ;
        ArrayList<Double> rightX;
        double[] bounds;
        int i;

        if ( xs.size() != 3 || ys.size() != 2 || zs.size() != 3 ||
             solid.getVerticesList().size() != 10 ) {
            return null;
        }

        bounds = solid.getMinMax();
        rightZ = new ArrayList<Double>();
        rightX = new ArrayList<Double>();
        for ( i = 0; i < zs.size(); i++ ) {
            double z;
            double maxX;
            int j;

            z = zs.get(i);
            maxX = -Double.MAX_VALUE;
            for ( j = 0; j < solid.getVerticesList().size(); j++ ) {
                Vector3Dd p;

                p = solid.getVerticesList().get(j).position;
                if ( sameCoordinate(p.z(), z) && p.x() > maxX ) {
                    maxX = p.x();
                }
            }
            if ( maxX <= -Double.MAX_VALUE / 2.0 ) {
                return null;
            }
            rightZ.add(z);
            rightX.add(maxX);
        }

        return new OrthogonalProfileOperandSpec(PROFILE_Y_EXTRUDED_XZ,
            bounds, null, rightZ, rightX);
    }

    private static OrthogonalProfileOperandSpec createOrthogonalProfileSpec(
        PolyhedralBoundedSolid solid)
    {
        ArrayList<Double> xs;
        ArrayList<Double> ys;
        ArrayList<Double> zs;
        OrthogonalProfileOperandSpec spec;

        xs = uniqueVertexCoordinates(solid, 0);
        ys = uniqueVertexCoordinates(solid, 1);
        zs = uniqueVertexCoordinates(solid, 2);

        spec = createYExtrudedXZSpec(solid, xs, ys, zs);
        if ( spec != null ) {
            return spec;
        }
        return createXExtrudedYZSpec(solid, xs, ys, zs);
    }

    private static OrthogonalProfileBooleanFallbackSpec
    prepareOrthogonalProfileBooleanFallbackSpec(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        OrthogonalProfileOperandSpec specA;
        OrthogonalProfileOperandSpec specB;
        OrthogonalProfileOperandSpec yExtruded;
        OrthogonalProfileOperandSpec xExtruded;
        ArrayList<Double> ys;
        ArrayList<Double> zs;
        int i;

        specA = createOrthogonalProfileSpec(inSolidA);
        specB = createOrthogonalProfileSpec(inSolidB);
        if ( specA == null || specB == null || specA.type == specB.type ) {
            return null;
        }
        yExtruded = specA.type == PROFILE_Y_EXTRUDED_XZ ? specA : specB;
        xExtruded = specA.type == PROFILE_X_EXTRUDED_YZ ? specA : specB;

        if ( !sameCoordinate(yExtruded.bounds[0], xExtruded.bounds[0]) ||
             !sameCoordinate(yExtruded.bounds[1], xExtruded.bounds[1]) ||
             !sameCoordinate(yExtruded.bounds[2], xExtruded.bounds[2]) ||
             !sameCoordinate(yExtruded.bounds[4], xExtruded.bounds[4]) ||
             !sameCoordinate(yExtruded.bounds[5], xExtruded.bounds[5]) ||
             yExtruded.bounds[3] >= xExtruded.bounds[3] -
                 numericContext.bigEpsilon() ) {
            return null;
        }

        ys = uniqueVertexCoordinates(inSolidA, 1);
        zs = uniqueVertexCoordinates(inSolidA, 2);
        for ( i = 0; i < inSolidB.getVerticesList().size(); i++ ) {
            Vector3Dd p;

            p = inSolidB.getVerticesList().get(i).position;
            addUniqueCoordinate(ys, p.y());
            addUniqueCoordinate(zs, p.z());
        }
        if ( ys.size() < 2 || zs.size() < 2 ||
             ys.size() > 8 || zs.size() > 8 ) {
            return null;
        }

        return new OrthogonalProfileBooleanFallbackSpec(
            specA, specB, yExtruded, xExtruded.bounds[0],
            xExtruded.bounds[3], ys, zs);
    }

    private static boolean profileCellSelected(
        OrthogonalProfileBooleanFallbackSpec spec,
        int op,
        int zone,
        int iy,
        int iz)
    {
        double y;
        double z;
        double x0;
        double x1;
        double x;
        boolean insideA;
        boolean insideB;

        y = (spec.ys.get(iy) + spec.ys.get(iy + 1)) * 0.5;
        z = (spec.zs.get(iz) + spec.zs.get(iz + 1)) * 0.5;
        x0 = spec.xAtBoundary(zone, z);
        x1 = spec.xAtBoundary(zone + 1, z);
        if ( x1 <= x0 + numericContext.bigEpsilon() ) {
            return false;
        }
        x = (x0 + x1) * 0.5;
        insideA = spec.operandA.contains(x, y, z);
        insideB = spec.operandB.contains(x, y, z);
        return _PolyhedralBoundedSolidAxisAlignedCellFallback.axisAlignedCellSelected(insideA, insideB, op);
    }

    private static void addProfileBoundaryQuad(
        ProfileCellBooleanBuilder builder,
        OrthogonalProfileBooleanFallbackSpec spec,
        int zone,
        int iy,
        int iz,
        int side)
    {
        int left;
        int right;

        left = zone;
        right = zone + 1;
        if ( side == 0 ) {
            builder.addQuad(new Vector3Dd[] {
                spec.point(left, iy, iz), spec.point(left, iy, iz + 1),
                spec.point(left, iy + 1, iz + 1),
                spec.point(left, iy + 1, iz)
            });
        }
        else if ( side == 1 ) {
            builder.addQuad(new Vector3Dd[] {
                spec.point(right, iy, iz), spec.point(right, iy + 1, iz),
                spec.point(right, iy + 1, iz + 1),
                spec.point(right, iy, iz + 1)
            });
        }
        else if ( side == 2 ) {
            builder.addQuad(new Vector3Dd[] {
                spec.point(left, iy, iz), spec.point(right, iy, iz),
                spec.point(right, iy, iz + 1),
                spec.point(left, iy, iz + 1)
            });
        }
        else if ( side == 3 ) {
            builder.addQuad(new Vector3Dd[] {
                spec.point(left, iy + 1, iz),
                spec.point(left, iy + 1, iz + 1),
                spec.point(right, iy + 1, iz + 1),
                spec.point(right, iy + 1, iz)
            });
        }
        else if ( side == 4 ) {
            builder.addQuad(new Vector3Dd[] {
                spec.point(left, iy, iz), spec.point(left, iy + 1, iz),
                spec.point(right, iy + 1, iz),
                spec.point(right, iy, iz)
            });
        }
        else {
            builder.addQuad(new Vector3Dd[] {
                spec.point(left, iy, iz + 1),
                spec.point(right, iy, iz + 1),
                spec.point(right, iy + 1, iz + 1),
                spec.point(left, iy + 1, iz + 1)
            });
        }
    }

    static PolyhedralBoundedSolid buildOrthogonalProfileBooleanFallback(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        OrthogonalProfileBooleanFallbackSpec spec;
        ProfileCellBooleanBuilder builder;
        boolean[][][] occupied;
        int zone;
        int iy;
        int iz;

        spec = prepareOrthogonalProfileBooleanFallbackSpec(inSolidA, inSolidB);
        if ( spec == null ) {
            return null;
        }

        occupied =
            new boolean[2][spec.ys.size() - 1][spec.zs.size() - 1];
        for ( zone = 0; zone < 2; zone++ ) {
            for ( iy = 0; iy < spec.ys.size() - 1; iy++ ) {
                for ( iz = 0; iz < spec.zs.size() - 1; iz++ ) {
                    occupied[zone][iy][iz] =
                        profileCellSelected(spec, op, zone, iy, iz);
                }
            }
        }

        builder = new ProfileCellBooleanBuilder();
        for ( zone = 0; zone < 2; zone++ ) {
            for ( iy = 0; iy < spec.ys.size() - 1; iy++ ) {
                for ( iz = 0; iz < spec.zs.size() - 1; iz++ ) {
                    if ( !occupied[zone][iy][iz] ) {
                        continue;
                    }
                    if ( zone == 0 || !occupied[zone - 1][iy][iz] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 0);
                    }
                    if ( zone == 1 || !occupied[zone + 1][iy][iz] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 1);
                    }
                    if ( iy == 0 || !occupied[zone][iy - 1][iz] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 2);
                    }
                    if ( iy == spec.ys.size() - 2 ||
                         !occupied[zone][iy + 1][iz] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 3);
                    }
                    if ( iz == 0 || !occupied[zone][iy][iz - 1] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 4);
                    }
                    if ( iz == spec.zs.size() - 2 ||
                         !occupied[zone][iy][iz + 1] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 5);
                    }
                }
            }
        }

        return builder.result();
    }
}
