package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

// Java classes
import java.util.ArrayList;

// Vitral classes
import java.util.List;
import vsdk.toolkit.common.statistics.PolyhedralBoundedSolidStatistics;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.VSDKFatalException;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidStructuralCopy;
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
    Serializes the public set operations and splits: the kernel keeps
    per-call state in static fields (Fable5.2Analysis.md Stage F step 3).
    */
    private static final Object KERNEL_LOCK = new Object();

    // Weld tolerance for glyph/poly-line simplification, expressed as a
    // fraction of the contour bounding-box diagonal.
    private static final double GLYPH_WELD_RELATIVE_FACTOR = 1.0e-2;

    /**
    Applies a transformation matrix to all vertices in the solid.
    @param solid target solid instance.
    @param transformation transformation matrix to apply.
    */
    public static void applyTransformation(
        PolyhedralBoundedSolid solid,
        Matrix4x4d transformation
    )
    {
        int i;
        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex = solid.getVerticesList().get(i);
            vertex.position = transformation.multiply(vertex.position);
        }
    }

    private static _PolyhedralBoundedSolidHalfEdge[] findWireSweepEnds(
        PolyhedralBoundedSolid solid)
    {
        _PolyhedralBoundedSolidHalfEdge first;
        _PolyhedralBoundedSolidHalfEdge last;

        first = solid.getPolygonsList().get(0).boundariesList.get(0)
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

    private static boolean isOnXAxis(Vector3Dd p, double tolerance)
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
                he.startingVertex.position = new Vector3Dd(x, 0.0, 0.0);
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
        if ( solid == null || solid.getPolygonsList().size() < 1 || numberOfFaces < 3 ) {
            return;
        }

        _PolyhedralBoundedSolidHalfEdge[] ends = findWireSweepEnds(solid);
        _PolyhedralBoundedSolidHalfEdge first = ends[0];
        _PolyhedralBoundedSolidHalfEdge last = ends[1];
        _PolyhedralBoundedSolidFace headf = solid.getPolygonsList().get(0);

        double axisTolerance = VSDK.EPSILON * 100.0;
        Vector3Dd firstEndpointPosition = new Vector3Dd(
            first.next().startingVertex.position);
        Vector3Dd lastEndpointPosition = new Vector3Dd(
            last.startingVertex.position);
        boolean firstEndpointOnAxis = isOnXAxis(firstEndpointPosition,
            axisTolerance);
        boolean lastEndpointOnAxis = isOnXAxis(lastEndpointPosition,
            axisTolerance);

        _PolyhedralBoundedSolidHalfEdge cfirst;
        _PolyhedralBoundedSolidHalfEdge scan = null;
        _PolyhedralBoundedSolidFace tailf;
        Vector3Dd v;
        Matrix4x4d rotation;

        cfirst = first;
        rotation = new Matrix4x4d();
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

    private static List<Vector3Dd> sampleCurveSegment(ParametricCurve curve,
                                                     int segmentIndex)
    {
        // Approximate one parametric segment as a polyLine.
        return curve.calculatePoints(segmentIndex, false);
    }

    private static void startLoopWithSeedPoint(_BoundaryRepresentationFromCurveBuildState state,
                                               Vector3Dd point)
    {
        state.beginningOfLoop = false;
        if ( state.firstLoop ) {
            // [MANT1988] 12.2: first contour starts with MVFS.
            PolyhedralBoundedSolidEulerOperators.mvfs(state.solid, new Vector3Dd(point), state.nextVertexId,
                state.nextFaceId);
            state.nextVertexId++;
            state.nextFaceId++;
        }
        else {
            // Additional contours are connected and converted into rings.
            PolyhedralBoundedSolidEulerOperators.smev(state.solid, 1, state.nextVertexId-1, state.nextVertexId,
                new Vector3Dd(point));
            state.nextVertexId++;
            PolyhedralBoundedSolidEulerOperators.kemr(state.solid, 1, 1, state.nextVertexId-2, state.nextVertexId-1,
                state.nextVertexId-1, state.nextVertexId-2);
            state.lastLoopStartVertexId = state.nextVertexId-1;
        }

        state.firstPointInLoop = new Vector3Dd(point);
        state.lastAcceptedPoint = new Vector3Dd(point);
        state.verticesInCurrentLoop = 1;
    }

    private static boolean shouldAcceptPolyLinePoint(_BoundaryRepresentationFromCurveBuildState state,
                                                     Vector3Dd point)
    {
        return Vector3Dd.distance(point, state.lastAcceptedPoint) >
            state.weldEpsilon &&
            Vector3Dd.distance(point, state.firstPointInLoop) >
            state.weldEpsilon;
    }

    private static void appendPointToCurrentLoop(_BoundaryRepresentationFromCurveBuildState state,
                                                 Vector3Dd point)
    {
        PolyhedralBoundedSolidEulerOperators.smev(state.solid, 1, state.nextVertexId-1, state.nextVertexId,
            new Vector3Dd(point));
        state.nextVertexId++;
        state.lastAcceptedPoint = new Vector3Dd(point);
        state.verticesInCurrentLoop++;
    }

    private static void processSampledSegment(_BoundaryRepresentationFromCurveBuildState state,
                                              List<Vector3Dd> polyline)
    {
        int j;
        for ( j = 0; j < polyline.size(); j++ ) {
            Vector3Dd point = polyline.get(j);
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
        if ( state.verticesInCurrentLoop < 3 ) {
            Logger.reportMessage(null, VSDK.WARNING, "closeLoopWithMef",
                "Degenerate glyph loop with " + state.verticesInCurrentLoop +
                " distinct vertices after welding; result may be invalid.");
        }

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
        state.verticesInCurrentLoop = 0;
    }

    public static PolyhedralBoundedSolid createBrepFromParametricCurve(
        ParametricCurve curve)
    {
        int i;
        _BoundaryRepresentationFromCurveBuildState state = new _BoundaryRepresentationFromCurveBuildState();
        double[] minMax = curve.getMinMax();
        double dx;
        double dy;
        double dz;
        double bboxDiagonal;

        if ( minMax == null || minMax.length < 6 ) {
            Logger.reportMessage(null, VSDK.WARNING,
                "createBrepFromParametricCurve",
                "Glyph bbox unavailable, falling back to BREP_BIG_EPSILON weld.");
        }
        else {
            dx = minMax[3] - minMax[0];
            dy = minMax[4] - minMax[1];
            dz = minMax[5] - minMax[2];
            bboxDiagonal = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if ( Double.isFinite(bboxDiagonal) && bboxDiagonal > 0.0 ) {
                state.weldEpsilon = Math.max(
                    PolyhedralBoundedSolidNumericPolicy.BREP_BIG_EPSILON,
                    GLYPH_WELD_RELATIVE_FACTOR * bboxDiagonal);
            }
            else {
                Logger.reportMessage(null, VSDK.WARNING,
                    "createBrepFromParametricCurve",
                    "Glyph bbox degenerate, falling back to BREP_BIG_EPSILON weld.");
            }
        }

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
        synchronized ( KERNEL_LOCK ) {
            try {
                _PolyhedralBoundedSolidSplitter.split(inSolid,
                    inSplittingPlane, outSolidsAbove, outSolidsBelow);
            }
            catch ( VSDKFatalException e ) {
                throw new SolidOperationFailedException(
                    "Split failed: " + e.getMessage(), e);
            }
        }
    }

    /**
    Convenience wrapper over `_PolyhedralBoundedSolidSetOperator.setOp`.
    Strict result validation is enabled by default.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        return setOp(inSolidA, inSolidB, op, false, true, true);
    }

    /**
    Convenience wrapper over `_PolyhedralBoundedSolidSetOperator.setOp`
    including debug mode. Strict result validation is enabled by default.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op,
        boolean withDebug)
    {
        return setOp(inSolidA, inSolidB, op, withDebug, true, true);
    }

    /**
    Convenience wrapper over `_PolyhedralBoundedSolidSetOperator.setOp`
    including debug mode and optional final face maximization. Strict result
    validation is enabled by default.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op,
        boolean withDebug,
        boolean maximizeResultFaces)
    {
        return setOp(inSolidA, inSolidB, op, withDebug,
            maximizeResultFaces, true);
    }

    /**
    Regularized set operation ([MANT1988] Ch. 15) with configurable strict
    result validation.

    <p>The operands are not modified: the operation runs on structural
    copies of them ([MANT1988] Sec. 15.8, `scopy`). Use
    {@link #setOpDestructive} to skip the copies when the operands are no
    longer needed; it consumes them as the book's algorithm does.</p>

    <p>Strict validation performs global shell/Euler analysis and an
    all-face-pairs intersection scan. It is enabled by default by the shorter
    overloads; callers can pass {@code false} here for an explicit
    performance/legacy-compatibility opt-out.</p>

    <p>Empty results. A solid with no faces is returned when the regularized
    result is empty (A \ A, the intersection of disjoint or touching
    solids, A \ B with A inside B); {@link #isEmptyResult} tells it apart.
    When the operands prove that the result cannot be empty (a union of
    non-empty solids, an intersection of solids whose interiors overlap,
    A \ B with a point of A strictly outside B) and the pipeline returned
    nothing, {@link SolidOperationFailedException} is raised instead of
    silently returning an empty solid.</p>

    <p>The public operations of this class are serialized on one lock: the
    kernel still keeps per-call state in static fields, so concurrent calls
    from several threads run one after the other.</p>

    @throws NonManifoldBooleanResultException when the result would be
        non-manifold (a union of solids touching only along edges or at
        vertices); raised even when {@code doStrictValidation} is false
    @throws InvalidSolidResultException when a completed boolean result
        fails the strict B-Rep postcondition
    @throws SolidOperationFailedException when the operation fails or lost
        the solid
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op,
        boolean withDebug,
        boolean maximizeResultFaces,
        boolean doStrictValidation)
    {
        synchronized ( KERNEL_LOCK ) {
            PolyhedralBoundedSolid result;

            PolyhedralBoundedSolidStatistics.recordSetOpCall(op);
            result = runSetOp(
                PolyhedralBoundedSolidStructuralCopy.copy(inSolidA),
                PolyhedralBoundedSolidStructuralCopy.copy(inSolidB),
                op, withDebug, maximizeResultFaces, doStrictValidation);
            if ( isEmptyResult(result) &&
                 resultCannotBeEmpty(inSolidA, inSolidB, op) ) {
                throw new SolidOperationFailedException(
                    "Boolean operation lost the solid: op=" +
                    operationName(op) + " returned an empty solid but " +
                    "the operands prove the result is not empty (path=" +
                    getLastSetOpResultPath() + ")");
            }
            return result;
        }
    }

    /**
    Destructive form of {@link #setOp(PolyhedralBoundedSolid,
    PolyhedralBoundedSolid, int, boolean, boolean, boolean)}: no copies are
    made and the operands are consumed ([MANT1988] Sec. 15.8: "the parts of
    A and B not required in the final result will remain in the original
    solids"). After the call the operands hold fragments and must not be
    used. The empty-result witness check is not done, since it needs intact
    operands.
    */
    public static PolyhedralBoundedSolid setOpDestructive(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op,
        boolean withDebug,
        boolean maximizeResultFaces,
        boolean doStrictValidation)
    {
        synchronized ( KERNEL_LOCK ) {
            PolyhedralBoundedSolidStatistics.recordSetOpCall(op);
            return runSetOp(inSolidA, inSolidB, op, withDebug,
                maximizeResultFaces, doStrictValidation);
        }
    }

    /**
    True for the empty solid a set operation or a split returns when the
    regularized result is empty: no faces at all.
    */
    public static boolean isEmptyResult(PolyhedralBoundedSolid result)
    {
        return result != null && result.getPolygonsList().size() == 0;
    }

    private static PolyhedralBoundedSolid runSetOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op,
        boolean withDebug,
        boolean maximizeResultFaces,
        boolean doStrictValidation)
    {
        try {
            return _PolyhedralBoundedSolidSetOperator.setOp(inSolidA,
                inSolidB, op, withDebug, maximizeResultFaces,
                doStrictValidation);
        }
        catch ( VSDKFatalException e ) {
            throw new SolidOperationFailedException(
                "Boolean operation failed: op=" + operationName(op) +
                " (path=" + getLastSetOpResultPath() + "): " +
                e.getMessage(), e);
        }
    }

    /**
    Witnesses that the regularized result of `op` on the intact operands
    is not empty. Only sufficient conditions are used, so false means
    "could be empty", never "is empty".
    */
    private static boolean resultCannotBeEmpty(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        boolean emptyA = inSolidA == null || isEmptyResult(inSolidA);
        boolean emptyB = inSolidB == null || isEmptyResult(inSolidB);

        if ( op == UNION ) {
            return !emptyA || !emptyB;
        }
        if ( emptyA || (emptyB && op == INTERSECTION) ) {
            return false;
        }
        if ( op == INTERSECTION ) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier
                .interiorsOverlap(inSolidA, inSolidB);
        }
        if ( op == SUBTRACT ) {
            return emptyB ||
                _PolyhedralBoundedSolidSetNonIntersectingClassifier
                    .hasBoundaryPointOutside(inSolidA, inSolidB);
        }
        return false;
    }

    private static String operationName(int op)
    {
        switch ( op ) {
          case UNION: return "UNION";
          case INTERSECTION: return "INTERSECTION";
          case SUBTRACT: return "SUBTRACT";
          default: return "op#" + op;
        }
    }

    /**
    Reports which path produced the last `setOp` result on the calling
    thread: "normal-pipeline" when the [MANT1988] Ch. 15 pipeline answered,
    otherwise the name of the preflight that did (for example
    "touching-only-preflight" or "partial-coplanar-area-preflight").
    Returns null before the first call or when the last call failed before
    producing a result.
    */
    public static String getLastSetOpResultPath()
    {
        return _PolyhedralBoundedSolidSetOperator.getLastResultPath();
    }
}
