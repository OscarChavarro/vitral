package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

// Java classes
import java.util.ArrayList;

// Vitral classes
import java.util.List;
import vsdk.toolkit.common.PolyhedralBoundedSolidStatistics;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.ParametricCurve;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.processing.ProcessingElement;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;

/**
Utility class with static modeling and boolean operations specific to
`PolyhedralBoundedSolid`.

This class contains creation/sweep/split/set-op helpers and centralizes the
polyhedral B-Rep operations previously exposed through `GeometricModeler`.
*/
public class PolyhedralBoundedSolidModeler extends ProcessingElement
{
    public static final int UNION = 1;
    public static final int INTERSECTION = 2;
    public static final int SUBTRACT = 3;

    /**
    Implements the construction style from [MANT1988] section 12.2 / program
    12.1.

    Builds an arc on plane `z = height`, centered at (`cx`, `cy`), radius `radius`.
    The start vertex already exists in `faceId` and is identified by
    `vertexId`; the method appends `n` new edge steps from `phi1` to `phi2`
    (degrees, counterclockwise, 0 degrees on +X).
    */
    public static void addArcToExistingFace(
        PolyhedralBoundedSolid solid,
        int faceId,
        int vertexId,
        double cx,
        double cy,
        double radius,
        double height,
        double phi1,
        double phi2,
        int n)
    {
        double x;
        double y;
        double angle;
        double inc;
        int prev;
        int i;
        int nextVertexId;

        angle = Math.toRadians(phi1);
        inc = Math.toRadians((phi2 - phi1) / (n));
        prev = vertexId;
        for ( i = 0; i < n; i++ ) {
            angle += inc;
            x = cx + radius * Math.cos(angle);
            y = cy + radius * Math.sin(angle);
            nextVertexId = solid.getMaxVertexId() + 1;
            PolyhedralBoundedSolidEulerOperators.smev(solid, faceId, prev, nextVertexId, new Vector3D(x, y, height));
            prev = nextVertexId;
        }
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    }

    /**
    Implements [MANT1988] section 12.2 / program 12.2.

    Creates a planar circular lamina as a single face by:
    1) creating an initial vertex (`mvfs`)
    2) adding arc vertices (`addArc`)
    3) closing the loop (`smef`)
    */
    public static PolyhedralBoundedSolid createCircularLamina(
        double cx, double cy, double rad, double h, int n)
    {
        PolyhedralBoundedSolid solid;

        solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3D(cx + rad, cy, h), 1, 1);
        addArcToExistingFace(solid, 1, 1, cx, cy, rad, h, 0,
            (n - 1) * 360.0 / n, n-1);
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, n, 1, 2);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    /**
    Generalized translational sweep of a face, inspired by [MANT1988] 12.3.1.

    Instead of pure translation, transform `T` can include translation,
    rotation, and scale in the face-local context.

    PRE: `face` is closed and planar.
    */
    public static void translationalSweepExtrudeFace(
        PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidFace face,
        Matrix4x4 transformationMatrix)
    {
        _PolyhedralBoundedSolidLoop l;
        _PolyhedralBoundedSolidHalfEdge first;
        _PolyhedralBoundedSolidHalfEdge scan;
        _PolyhedralBoundedSolidVertex v;
        Vector3D newPos;
        int i;

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            l = face.boundariesList.get(i);
            first = l.boundaryStartHalfEdge;
            scan = first.next();
            v = scan.startingVertex;
            newPos = transformationMatrix.multiply(v.position);
            PolyhedralBoundedSolidEulerOperators.lmev(solid, scan, scan, solid.getMaxVertexId()+1, newPos);
            while ( scan != first ) {
                v = scan.next().startingVertex;
                newPos = transformationMatrix.multiply(v.position);
                PolyhedralBoundedSolidEulerOperators.lmev(solid, scan.next(), scan.next(),
                    solid.getMaxVertexId()+1, newPos);
                PolyhedralBoundedSolidEulerOperators.lmef(solid, scan.previous(), scan.next().next(),
                    solid.getMaxFaceId()+1);
                scan = (scan.next().mirrorHalfEdge()).next();
            }
            PolyhedralBoundedSolidEulerOperators.lmef(solid, scan.previous(), scan.next().next(),
                solid.getMaxFaceId()+1);
        }
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    }

    /**
    Variant of `translationalSweepExtrudeFace` with an extra planar check pass.

    After sweep generation, each created side face is tested for planarity.
    If a face is non-planar, it is split once (`lmef`) to triangulate it.
    */
    public static void translationalSweepExtrudeFacePlanar(
        PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidFace face,
        Matrix4x4 transformationMatrix)
    {
        _PolyhedralBoundedSolidLoop l;
        _PolyhedralBoundedSolidHalfEdge first;
        _PolyhedralBoundedSolidHalfEdge scan;
        _PolyhedralBoundedSolidVertex v;
        Vector3D newPos;
        ArrayList<Integer> newFaces = new ArrayList<>();
        int i;
        int newFaceId;

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            l = face.boundariesList.get(i);
            first = l.boundaryStartHalfEdge;
            scan = first.next();
            v = scan.startingVertex;
            newPos = transformationMatrix.multiply(v.position);
            PolyhedralBoundedSolidEulerOperators.lmev(solid, scan, scan, solid.getMaxVertexId()+1, newPos);
            while ( scan != first ) {
                v = scan.next().startingVertex;
                newPos = transformationMatrix.multiply(v.position);
                PolyhedralBoundedSolidEulerOperators.lmev(solid, scan.next(), scan.next(),
                    solid.getMaxVertexId()+1, newPos);
                newFaceId = solid.getMaxFaceId()+1;
                PolyhedralBoundedSolidEulerOperators.lmef(solid, scan.previous(), scan.next().next(), newFaceId);
                newFaces.add(newFaceId);
                scan = (scan.next().mirrorHalfEdge()).next();
            }
            newFaceId = solid.getMaxFaceId()+1;
            PolyhedralBoundedSolidEulerOperators.lmef(solid, scan.previous(), scan.next().next(), newFaceId);
            newFaces.add(newFaceId);
        }

        _PolyhedralBoundedSolidFace newFace;
        for ( i = 0; i < newFaces.size(); i++ ) {
            newFaceId = newFaces.get(i);
            newFace = solid.findFace(newFaceId);
            if ( !PolyhedralBoundedSolidGeometricValidator.validateFaceIsPlanar(newFace) ) {
                scan = newFace.boundariesList.get(0).boundaryStartHalfEdge;
                newFaceId = solid.getMaxFaceId()+1;
                PolyhedralBoundedSolidEulerOperators.lmef(solid, scan.next(), scan.previous(), newFaceId);
            }
        }

        while ( !newFaces.isEmpty() ) {
            newFaces.remove(0);
        }

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    }

    private static _PolyhedralBoundedSolidHalfEdge[] findWireSweepEnds(
        PolyhedralBoundedSolid solid)
    {
        _PolyhedralBoundedSolidHalfEdge first;
        _PolyhedralBoundedSolidHalfEdge last;

        first = solid.polygonsList.get(0).boundariesList.get(0)
            .boundaryStartHalfEdge;
        while ( first.parentEdge != first.next().parentEdge ) {
            first = first.next();
        }
        last = first.next();
        while ( last.parentEdge != last.next().parentEdge ) {
            last = last.next();
        }
        return new _PolyhedralBoundedSolidHalfEdge[] { first, last  };
    }

    private static boolean isOnXAxis(Vector3D p, double tolerance)
    {
        return Math.abs(p.y()) <= tolerance && Math.abs(p.z()) <= tolerance;
    }

    private static void collapseFaceToAxisVertex(
        _PolyhedralBoundedSolidFace face, double x)
    {
        if ( face == null ) {
            return;
        }

        int i;
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge he = start;
            if ( he == null ) {
                continue;
            }
            do {
                he.startingVertex.position = new Vector3D(x, 0.0, 0.0);
                he = he.next();
            } while ( he != null && he != start );
        }
    }

    /**
    Rotational sweep of an open wire profile around the X axis, following the
    construction style of [MANT1988] 12.2 and 12.5.

    PRE:
    - `solid` is a wire-like profile (single face, open loop)
    - profile lies on `z = 0`
    - `numberOfFaces >= 3`
    */
    public static void rotationalSweepExtrudeWireAroundXAxis(
        PolyhedralBoundedSolid solid, int numberOfFaces)
    {
        if ( solid == null || solid.polygonsList.size() < 1 || numberOfFaces < 3 ) {
            return;
        }

        _PolyhedralBoundedSolidHalfEdge[] ends = findWireSweepEnds(solid);
        _PolyhedralBoundedSolidHalfEdge first = ends[0];
        _PolyhedralBoundedSolidHalfEdge last = ends[1];
        _PolyhedralBoundedSolidFace headf = solid.polygonsList.get(0);

        double axisTolerance = VSDK.EPSILON * 100.0;
        Vector3D firstEndpointPosition = new Vector3D(
            first.next().startingVertex.position);
        Vector3D lastEndpointPosition = new Vector3D(
            last.startingVertex.position);
        boolean firstEndpointOnAxis = isOnXAxis(firstEndpointPosition,
            axisTolerance);
        boolean lastEndpointOnAxis = isOnXAxis(lastEndpointPosition,
            axisTolerance);

        _PolyhedralBoundedSolidHalfEdge cfirst;
        _PolyhedralBoundedSolidHalfEdge scan = null;
        _PolyhedralBoundedSolidFace tailf;
        Vector3D v;
        Matrix4x4 rotation;

        cfirst = first;
        rotation = new Matrix4x4();
        rotation = rotation.axisRotation((2*Math.PI) / numberOfFaces, 1, 0, 0);

        int i;
        for ( i = 0; i < numberOfFaces-1; i++ ) {
            v = rotation.multiply(cfirst.next().startingVertex.position);
            PolyhedralBoundedSolidEulerOperators.lmev(solid, cfirst.next(), cfirst.next(), solid.getMaxVertexId()+1,
                v);
            scan = cfirst.next();

            while ( scan != last.next() ) {
                v = rotation.multiply(scan.previous().startingVertex.position);
                PolyhedralBoundedSolidEulerOperators.lmev(solid, scan.previous(), scan.previous(),
                    solid.getMaxVertexId()+1, v);
                PolyhedralBoundedSolidEulerOperators.lmef(solid, scan.previous().previous(), scan.next(),
                    solid.getMaxFaceId()+1);
                scan = (scan.next().next()).mirrorHalfEdge();
            }
            last = scan;
            cfirst = (cfirst.next().next()).mirrorHalfEdge();
        }

        tailf = PolyhedralBoundedSolidEulerOperators.lmef(solid, cfirst.next(), first.mirrorHalfEdge(),
            solid.getMaxFaceId()+1);
        while ( cfirst != scan ) {
            PolyhedralBoundedSolidEulerOperators.lmef(solid, cfirst, cfirst.next().next().next(),
                solid.getMaxFaceId()+1);
            cfirst = (cfirst.previous()).mirrorHalfEdge().previous();
        }

        // [MANT1988] 12.2: if a profile endpoint lies on the rotation axis,
        // cap vertices collapse to a single pole instead of leaving a
        // degenerate ring of coincident points.
        if ( firstEndpointOnAxis ) {
            collapseFaceToAxisVertex(headf, firstEndpointPosition.x());
        }
        if ( lastEndpointOnAxis ) {
            collapseFaceToAxisVertex(tailf, lastEndpointPosition.x());
        }
        if ( firstEndpointOnAxis || lastEndpointOnAxis ) {
            PolyhedralBoundedSolidTopologyEditing.maximizeFaces(solid);
        }

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    }

    private static boolean isBreakMarker(ParametricCurve curve, int segmentIndex)
    {
        return curve.types.get(segmentIndex) == ParametricCurve.BREAK;
    }

    private static List<Vector3D> sampleCurveSegment(ParametricCurve curve,
                                                     int segmentIndex)
    {
        // Approximate one parametric segment as a polyLine.
        return curve.calculatePoints(segmentIndex, false);
    }

    private static void startLoopWithSeedPoint(_BoundaryRepresentationFromCurveBuildState state,
                                               Vector3D point)
    {
        state.beginningOfLoop = false;
        if ( state.firstLoop ) {
            // [MANT1988] 12.2: first contour starts with MVFS.
            PolyhedralBoundedSolidEulerOperators.mvfs(state.solid, new Vector3D(point), state.nextVertexId,
                state.nextFaceId);
            state.nextVertexId++;
            state.nextFaceId++;
        }
        else {
            // Additional contours are connected and converted into rings.
            PolyhedralBoundedSolidEulerOperators.smev(state.solid, 1, state.nextVertexId-1, state.nextVertexId,
                new Vector3D(point));
            state.nextVertexId++;
            PolyhedralBoundedSolidEulerOperators.kemr(state.solid, 1, 1, state.nextVertexId-2, state.nextVertexId-1,
                state.nextVertexId-1, state.nextVertexId-2);
            state.lastLoopStartVertexId = state.nextVertexId-1;
        }

        state.firstPointInLoop = new Vector3D(point);
        state.lastAcceptedPoint = new Vector3D(point);
    }

    private static boolean shouldAcceptPolyLinePoint(_BoundaryRepresentationFromCurveBuildState state,
                                                     Vector3D point)
    {
        return VSDK.vectorDistance(point, state.lastAcceptedPoint) >
            VSDK.EPSILON &&
            VSDK.vectorDistance(point, state.firstPointInLoop) > VSDK.EPSILON;
    }

    private static void appendPointToCurrentLoop(_BoundaryRepresentationFromCurveBuildState state,
                                                 Vector3D point)
    {
        PolyhedralBoundedSolidEulerOperators.smev(state.solid, 1, state.nextVertexId-1, state.nextVertexId,
            new Vector3D(point));
        state.nextVertexId++;
        state.lastAcceptedPoint = new Vector3D(point);
    }

    private static void processSampledSegment(_BoundaryRepresentationFromCurveBuildState state,
                                              List<Vector3D> polyline)
    {
        int j;
        for ( j = 0; j < polyline.size(); j++ ) {
            Vector3D point = polyline.get(j);
            if ( state.beginningOfLoop ) {
                startLoopWithSeedPoint(state, point);
            }
            else if ( shouldAcceptPolyLinePoint(state, point) ) {
                appendPointToCurrentLoop(state, point);
            }
        }
    }

    private static void closeLoopWithMef(_BoundaryRepresentationFromCurveBuildState state)
    {
        // [MANT1988] 12.2: close current wire by creating the face boundary.
        PolyhedralBoundedSolidEulerOperators.mef(state.solid, 1, 1,
            state.lastLoopStartVertexId, state.lastLoopStartVertexId+1,
            state.nextVertexId-1, state.nextVertexId-2, state.nextFaceId);
        state.nextFaceId++;

        if ( !state.firstLoop ) {
            // For inner contours, merge ring into the first face.
            PolyhedralBoundedSolidEulerOperators.kfmrh(state.solid, 2, state.nextFaceId-1);
        }
        state.firstLoop = false;
        state.beginningOfLoop = true;
        state.lastAcceptedPoint = null;
    }

    public static PolyhedralBoundedSolid createBrepFromParametricCurve(
        ParametricCurve curve)
    {
        int i;
        _BoundaryRepresentationFromCurveBuildState state = new _BoundaryRepresentationFromCurveBuildState();

        for ( i = 1; i < curve.types.size(); i++ ) {
            if ( isBreakMarker(curve, i) ) {
                i++;
                closeLoopWithMef(state);
                continue;
            }
            processSampledSegment(state, sampleCurveSegment(curve, i));
        }

        closeLoopWithMef(state);

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(
            state.solid);
        return state.solid;
    }

    /**
    Convenience wrapper over `_PolyhedralBoundedSolidSplitter.split`.

    Splits `inSolid` by `inSplittingPlane` and appends resulting pieces to
    `outSolidsAbove` and `outSolidsBelow`.
    */
    public static void split(
        PolyhedralBoundedSolid inSolid,
        InfinitePlane inSplittingPlane,
        List<PolyhedralBoundedSolid> outSolidsAbove,
        List<PolyhedralBoundedSolid> outSolidsBelow)
    {
        _PolyhedralBoundedSolidSplitter.split(inSolid, inSplittingPlane,
                                             outSolidsAbove, outSolidsBelow);
    }

    /**
    Convenience wrapper over `PolyhedralBoundedSolidSetOperator.setOp`.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        PolyhedralBoundedSolidStatistics.recordSetOpCall(op);
        return PolyhedralBoundedSolidSetOperator.setOp(inSolidA, inSolidB, op);
    }

    /**
    Convenience wrapper over `PolyhedralBoundedSolidSetOperator.setOp`
    including debug mode.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op,
        boolean withDebug)
    {
        PolyhedralBoundedSolidStatistics.recordSetOpCall(op);
        return PolyhedralBoundedSolidSetOperator.setOp(inSolidA, inSolidB, op,
            withDebug);
    }

    /**
    Convenience wrapper over `PolyhedralBoundedSolidSetOperator.setOp`
    including debug mode and optional final face maximization.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op,
        boolean withDebug,
        boolean maximizeResultFaces)
    {
        PolyhedralBoundedSolidStatistics.recordSetOpCall(op);
        return PolyhedralBoundedSolidSetOperator.setOp(inSolidA, inSolidB, op,
            withDebug, maximizeResultFaces);
    }
}
