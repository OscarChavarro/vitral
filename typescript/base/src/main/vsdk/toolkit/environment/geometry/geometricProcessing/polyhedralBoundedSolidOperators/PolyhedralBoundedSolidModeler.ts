import { PolyhedralBoundedSolidEulerOperators } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";

// Vitral classes
import { VSDK } from "../../../../common/VSDK.js";
import { Logger } from "../../../../common/logging/Logger.js";
import { Matrix4x4d } from "../../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import type { InfinitePlane } from "../../surface/InfinitePlane.js";
import { ParametricCurve } from "../../curve/ParametricCurve.js";
import { PolyhedralBoundedSolid } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidValidationEngine } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidFace } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import type { _PolyhedralBoundedSolidVertex } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { ProcessingElement } from "../../../../processing/ProcessingElement.js";
import { PolyhedralBoundedSolidTopologyEditing } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.js";
import { _BoundaryRepresentationFromCurveBuildState } from "./construction/_BoundaryRepresentationFromCurveBuildState.js";
import { _PolyhedralBoundedSolidSplitter } from "./slicing/_PolyhedralBoundedSolidSplitter.js";
import { _PolyhedralBoundedSolidSetOperator } from "./booleans/_PolyhedralBoundedSolidSetOperator.js";
import { PolyhedralBoundedSolidStatistics } from "../../../../common/statistics/PolyhedralBoundedSolidStatistics.js";

/**
Utility class with static modeling and boolean operations specific to
`PolyhedralBoundedSolid`.

This class contains creation/sweep/split/set-op helpers and centralizes the
polyhedral B-Rep operations previously exposed through `GeometricModeler`.
*/
export class PolyhedralBoundedSolidModeler extends ProcessingElement {
    public static readonly UNION = 1;
    public static readonly INTERSECTION = 2;
    public static readonly SUBTRACT = 3;

    // Weld tolerance for glyph/poly-line simplification, expressed as a
    // fraction of the contour bounding-box diagonal.
    private static readonly GLYPH_WELD_RELATIVE_FACTOR = 1.0e-2;

    /** Java 17 `Math.toRadians(double)`: `angdeg * DEGREES_TO_RADIANS`. */
    private static readonly DEGREES_TO_RADIANS = 0.017453292519943295;

    /**
    Applies a transformation matrix to all vertices in the solid.
    @param solid target solid instance.
    @param transformation transformation matrix to apply.
    */
    public static applyTransformation(solid: PolyhedralBoundedSolid, transformation: Matrix4x4d): void {
        let i: number;
        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const vertex = solid.getVerticesList().get(i)!;
            vertex.position = transformation.multiply(vertex.position);
        }
    }

    private static findWireSweepEnds(solid: PolyhedralBoundedSolid): _PolyhedralBoundedSolidHalfEdge[] {
        let first: _PolyhedralBoundedSolidHalfEdge;
        let last: _PolyhedralBoundedSolidHalfEdge;

        first = solid.getPolygonsList().get(0)!.boundariesList.get(0)!.boundaryStartHalfEdge!;
        while (first.parentEdge !== first.next()!.parentEdge) {
            first = first.next()!;
        }
        last = first.next()!;
        while (last.parentEdge !== last.next()!.parentEdge) {
            last = last.next()!;
        }
        return [first, last];
    }

    private static isOnXAxis(p: Vector3Dd, tolerance: number): boolean {
        return Math.abs(p.y()) <= tolerance && Math.abs(p.z()) <= tolerance;
    }

    private static collapseFaceToAxisVertex(face: _PolyhedralBoundedSolidFace | null, x: number): void {
        if (face === null) {
            return;
        }

        let i: number;
        for (i = 0; i < face.boundariesList.size(); i++) {
            const loop = face.boundariesList.get(i)!;
            const start = loop.boundaryStartHalfEdge;
            let he = start;
            if (he === null) {
                continue;
            }
            do {
                he!.startingVertex.position = new Vector3Dd(x, 0.0, 0.0);
                he = he!.next();
            } while (he !== null && he !== start);
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
    public static rotationalSweepExtrudeWireAroundXAxis(
        solid: PolyhedralBoundedSolid | null,
        numberOfFaces: number,
    ): void {
        if (solid === null || solid.getPolygonsList().size() < 1 || numberOfFaces < 3) {
            return;
        }

        const ends = PolyhedralBoundedSolidModeler.findWireSweepEnds(solid);
        const first = ends[0]!;
        let last = ends[1]!;
        const headf = solid.getPolygonsList().get(0);

        const axisTolerance = VSDK.EPSILON * 100.0;
        const firstEndpointPosition = new Vector3Dd(first.next()!.startingVertex.position);
        const lastEndpointPosition = new Vector3Dd(last.startingVertex.position);
        const firstEndpointOnAxis = PolyhedralBoundedSolidModeler.isOnXAxis(firstEndpointPosition, axisTolerance);
        const lastEndpointOnAxis = PolyhedralBoundedSolidModeler.isOnXAxis(lastEndpointPosition, axisTolerance);

        let cfirst: _PolyhedralBoundedSolidHalfEdge;
        let scan: _PolyhedralBoundedSolidHalfEdge | null = null;
        let v: Vector3Dd;
        let rotation: Matrix4x4d;

        cfirst = first;
        rotation = new Matrix4x4d();
        rotation = rotation.axisRotation((2 * Math.PI) / numberOfFaces, 1, 0, 0);

        let i: number;
        for (i = 0; i < numberOfFaces - 1; i++) {
            v = rotation.multiply(cfirst.next()!.startingVertex.position);
            PolyhedralBoundedSolidEulerOperators.lmev(
                solid,
                cfirst.next(),
                cfirst.next(),
                solid.getMaxVertexId() + 1,
                v,
            );
            scan = cfirst.next();

            while (scan !== last.next()) {
                v = rotation.multiply(scan!.previous()!.startingVertex.position);
                PolyhedralBoundedSolidEulerOperators.lmev(
                    solid,
                    scan!.previous(),
                    scan!.previous(),
                    solid.getMaxVertexId() + 1,
                    v,
                );
                PolyhedralBoundedSolidEulerOperators.lmef(
                    solid,
                    scan!.previous()!.previous(),
                    scan!.next(),
                    solid.getMaxFaceId() + 1,
                );
                scan = scan!.next()!.next()!.mirrorHalfEdge();
            }
            last = scan!;
            cfirst = cfirst.next()!.next()!.mirrorHalfEdge()!;
        }

        const tailf = PolyhedralBoundedSolidEulerOperators.lmef(
            solid,
            cfirst.next(),
            first.mirrorHalfEdge(),
            solid.getMaxFaceId() + 1,
        );
        while (cfirst !== scan) {
            PolyhedralBoundedSolidEulerOperators.lmef(
                solid,
                cfirst,
                cfirst.next()!.next()!.next(),
                solid.getMaxFaceId() + 1,
            );
            cfirst = cfirst.previous()!.mirrorHalfEdge()!.previous()!;
        }

        // [MANT1988] 12.2: if a profile endpoint lies on the rotation axis,
        // cap vertices collapse to a single pole instead of leaving a
        // degenerate ring of coincident points.
        if (firstEndpointOnAxis) {
            PolyhedralBoundedSolidModeler.collapseFaceToAxisVertex(headf, firstEndpointPosition.x());
        }
        if (lastEndpointOnAxis) {
            PolyhedralBoundedSolidModeler.collapseFaceToAxisVertex(tailf, lastEndpointPosition.x());
        }
        if (firstEndpointOnAxis || lastEndpointOnAxis) {
            PolyhedralBoundedSolidTopologyEditing.maximizeFaces(solid);
        }

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    }

    private static isBreakMarker(curve: ParametricCurve, segmentIndex: number): boolean {
        return curve.types[segmentIndex] === ParametricCurve.BREAK;
    }

    private static sampleCurveSegment(curve: ParametricCurve, segmentIndex: number): Vector3Dd[] {
        // Approximate one parametric segment as a polyLine.
        return curve.calculatePoints(segmentIndex, false);
    }

    private static startLoopWithSeedPoint(state: _BoundaryRepresentationFromCurveBuildState, point: Vector3Dd): void {
        state.beginningOfLoop = false;
        if (state.firstLoop) {
            // [MANT1988] 12.2: first contour starts with MVFS.
            PolyhedralBoundedSolidEulerOperators.mvfs(
                state.solid,
                new Vector3Dd(point),
                state.nextVertexId,
                state.nextFaceId,
            );
            state.nextVertexId++;
            state.nextFaceId++;
        } else {
            // Additional contours are connected and converted into rings.
            PolyhedralBoundedSolidEulerOperators.smev(
                state.solid,
                1,
                state.nextVertexId - 1,
                state.nextVertexId,
                new Vector3Dd(point),
            );
            state.nextVertexId++;
            PolyhedralBoundedSolidEulerOperators.kemr(
                state.solid,
                1,
                1,
                state.nextVertexId - 2,
                state.nextVertexId - 1,
                state.nextVertexId - 1,
                state.nextVertexId - 2,
            );
            state.lastLoopStartVertexId = state.nextVertexId - 1;
        }

        state.firstPointInLoop = new Vector3Dd(point);
        state.lastAcceptedPoint = new Vector3Dd(point);
        state.verticesInCurrentLoop = 1;
    }

    private static shouldAcceptPolyLinePoint(
        state: _BoundaryRepresentationFromCurveBuildState,
        point: Vector3Dd,
    ): boolean {
        return (
            Vector3Dd.distance(point, state.lastAcceptedPoint!) > state.weldEpsilon &&
            Vector3Dd.distance(point, state.firstPointInLoop!) > state.weldEpsilon
        );
    }

    private static appendPointToCurrentLoop(state: _BoundaryRepresentationFromCurveBuildState, point: Vector3Dd): void {
        PolyhedralBoundedSolidEulerOperators.smev(
            state.solid,
            1,
            state.nextVertexId - 1,
            state.nextVertexId,
            new Vector3Dd(point),
        );
        state.nextVertexId++;
        state.lastAcceptedPoint = new Vector3Dd(point);
        state.verticesInCurrentLoop++;
    }

    private static processSampledSegment(
        state: _BoundaryRepresentationFromCurveBuildState,
        polyline: readonly Vector3Dd[],
    ): void {
        let j: number;
        for (j = 0; j < polyline.length; j++) {
            const point = polyline[j]!;
            if (state.beginningOfLoop) {
                PolyhedralBoundedSolidModeler.startLoopWithSeedPoint(state, point);
            } else if (PolyhedralBoundedSolidModeler.shouldAcceptPolyLinePoint(state, point)) {
                PolyhedralBoundedSolidModeler.appendPointToCurrentLoop(state, point);
            }
        }
    }

    private static closeLoopWithMef(state: _BoundaryRepresentationFromCurveBuildState): void {
        if (state.verticesInCurrentLoop < 3) {
            Logger.reportMessage(
                null,
                VSDK.WARNING,
                "closeLoopWithMef",
                "Degenerate glyph loop with " +
                    state.verticesInCurrentLoop +
                    " distinct vertices after welding; result may be invalid.",
            );
        }

        // [MANT1988] 12.2: close current wire by creating the face boundary.
        PolyhedralBoundedSolidEulerOperators.mef(
            state.solid,
            1,
            1,
            state.lastLoopStartVertexId,
            state.lastLoopStartVertexId + 1,
            state.nextVertexId - 1,
            state.nextVertexId - 2,
            state.nextFaceId,
        );
        state.nextFaceId++;

        if (!state.firstLoop) {
            // For inner contours, merge ring into the first face.
            PolyhedralBoundedSolidEulerOperators.kfmrh(state.solid, 2, state.nextFaceId - 1);
        }
        state.firstLoop = false;
        state.beginningOfLoop = true;
        state.lastAcceptedPoint = null;
        state.verticesInCurrentLoop = 0;
    }

    public static createBrepFromParametricCurve(curve: ParametricCurve): PolyhedralBoundedSolid {
        let i: number;
        const state = new _BoundaryRepresentationFromCurveBuildState();
        const minMax = curve.getMinMax();
        let dx: number;
        let dy: number;
        let dz: number;
        let bboxDiagonal: number;

        if (minMax === null || minMax.length < 6) {
            Logger.reportMessage(
                null,
                VSDK.WARNING,
                "createBrepFromParametricCurve",
                "Glyph bbox unavailable, falling back to BREP_BIG_EPSILON weld.",
            );
        } else {
            dx = minMax[3]! - minMax[0]!;
            dy = minMax[4]! - minMax[1]!;
            dz = minMax[5]! - minMax[2]!;
            bboxDiagonal = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (Number.isFinite(bboxDiagonal) && bboxDiagonal > 0.0) {
                state.weldEpsilon = Math.max(
                    PolyhedralBoundedSolidNumericPolicy.BREP_BIG_EPSILON,
                    PolyhedralBoundedSolidModeler.GLYPH_WELD_RELATIVE_FACTOR * bboxDiagonal,
                );
            } else {
                Logger.reportMessage(
                    null,
                    VSDK.WARNING,
                    "createBrepFromParametricCurve",
                    "Glyph bbox degenerate, falling back to BREP_BIG_EPSILON weld.",
                );
            }
        }

        for (i = 1; i < curve.types.length; i++) {
            if (PolyhedralBoundedSolidModeler.isBreakMarker(curve, i)) {
                i++;
                PolyhedralBoundedSolidModeler.closeLoopWithMef(state);
                continue;
            }
            PolyhedralBoundedSolidModeler.processSampledSegment(
                state,
                PolyhedralBoundedSolidModeler.sampleCurveSegment(curve, i),
            );
        }

        PolyhedralBoundedSolidModeler.closeLoopWithMef(state);

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(state.solid);
        return state.solid;
    }

    /**
    Convenience wrapper over `_PolyhedralBoundedSolidSplitter.split`.

    Splits `inSolid` by `inSplittingPlane` and appends resulting pieces to
    `outSolidsAbove` and `outSolidsBelow`.
    */
    public static split(
        inSolid: PolyhedralBoundedSolid,
        inSplittingPlane: InfinitePlane,
        outSolidsAbove: PolyhedralBoundedSolid[],
        outSolidsBelow: PolyhedralBoundedSolid[],
    ): void {
        _PolyhedralBoundedSolidSplitter.split(inSolid, inSplittingPlane, outSolidsAbove, outSolidsBelow);
    }

    /**
    Convenience wrapper over `_PolyhedralBoundedSolidSetOperator.setOp` with
    configurable strict result validation. Java declares four overloads
    (`op`; `op, withDebug`; `op, withDebug, maximizeResultFaces`; and the full
    form); the shorter ones enable final face maximization and strict
    validation.

    <p>Strict validation performs global shell/Euler analysis and an
    all-face-pairs intersection scan. It is enabled by default by the shorter
    overloads; callers can pass `false` here for an explicit
    performance/legacy-compatibility opt-out.</p>

    @throws IllegalStateException when a completed boolean result fails the
        strict B-Rep postcondition
    */
    public static setOp(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        op: number,
        withDebug = false,
        maximizeResultFaces = true,
        doStrictValidation = true,
    ): PolyhedralBoundedSolid {
        PolyhedralBoundedSolidStatistics.recordSetOpCall(op);
        return _PolyhedralBoundedSolidSetOperator.setOp(
            inSolidA,
            inSolidB,
            op,
            withDebug,
            maximizeResultFaces,
            doStrictValidation,
        );
    }
}
