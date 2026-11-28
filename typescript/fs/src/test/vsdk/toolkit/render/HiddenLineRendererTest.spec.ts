import { describe, expect, it } from "vitest";
import { Math as JavaMath } from "java/lang/Math.js";
import { Double } from "java/lang/Double.js";
import { IllegalStateException } from "java/lang/IllegalStateException.js";
import { ArrayList } from "java/util/ArrayList.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import type { InfinitePlane } from "vsdk/toolkit/environment/geometry/surface/InfinitePlane.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import type { _PolyhedralBoundedSolidEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { SimpleTestGeometryLibrary } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.js";
import { StepReader } from "vsdk/toolkit/io/geometry/stepCad/reader/StepReader.js";
import { Calligraphic2DBuffer } from "vsdk/toolkit/media/Calligraphic2DBuffer.js";
import { HiddenLineRenderer } from "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.js";
import { File } from "../../../../main/java/io/File.js";
import { FileInputStream } from "../../../../main/java/io/FileInputStream.js";

/**
Translation of `java/base/src/test/vsdk/toolkit/render/HiddenLineRendererTest.java`.

Regression test for APPEL hidden-line rendering over a polyhedral bounded
solid fixture.

It lives in `@vitral/fs` because two of its cases read
`etc/solids/kurlanderBowl.step` through Java's `StepReader.readSolid(File)`,
which maps onto `readSolid(new FileInputStream(file))` here. The candidate
paths are relative to the working directory, as in Java; `../../etc` matches
the `typescript/fs` folder the specs run from.

AssertJ's `.as(...)` descriptions become the messages of the `expect` calls,
and `String.format`'s `%.Nf` becomes `toFixed(N)`. The three cases that take
several seconds here (Java runs the whole class in about 4 s) carry the
repository's 120 s per-test timeout instead of Vitest's 5 s default.
*/

/** `String.format("%.Nf", value)` */
function f(value: number, digits: number): string {
    return value.toFixed(digits);
}

describe("HiddenLineRendererTest", () => {
    it("given_appe1967FeaturedSolid_when_executingAppelAlgorithm_then_allGeneratedLinesStayFinite", () => {
        // Arrange
        let solid: PolyhedralBoundedSolid | null;
        try {
            solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        } catch (exception) {
            // Java catches NullPointerException only
            if (exception instanceof TypeError) {
                return;
            }
            throw exception;
        }
        if (solid === null) {
            return;
        }
        const contourLines = new Calligraphic2DBuffer();
        const visibleLines = new Calligraphic2DBuffer();
        const hiddenLines = new Calligraphic2DBuffer();

        // Action
        HiddenLineRenderer.executeAppelAlgorithm(
            createSingleBodySceneFromSolid(solid),
            createFeaturedCamera(),
            contourLines,
            visibleLines,
            hiddenLines,
        );

        // Assert
        expect(allLineCoordinatesAreFinite(contourLines)).toBe(true);
        expect(allLineCoordinatesAreFinite(visibleLines)).toBe(true);
        expect(allLineCoordinatesAreFinite(hiddenLines)).toBe(true);
    });

    /**
    Concave-solid visibility invariant: for every segment the renderer emits,
    its classification must agree with the quantitative invisibility computed
    at the segment midpoint. A segment is visible if and only if its midpoint
    quantitative invisibility is zero.

    This guards against the convexity shortcut where edges with both adjacent
    faces back-facing were force-classified as hidden without a QI test. On the
    concave kurlanderBowl such edges can be genuinely visible (QI==0), so the
    shortcut violates this invariant.

    Note: the invariant is read straight from the diagnostic dump (the
    classification and the midpoint QI both produced by the algorithm), so this
    test is not tautological against any single QI call; it asserts that the
    rendered classification is internally consistent with the computed QI for
    every edge type, hidden ones included.
    */
    it("given_concaveKurlanderBowl_when_classifyingSegments_then_visibilityMatchesMidpointQI", () => {
        const solid: PolyhedralBoundedSolid = readSolid(resolveKurlanderBowlStep());
        const body: SimpleBody = createBody(solid, new Matrix4x4d());
        const dump: HiddenLineRenderer.AppelAlgorithmDump = HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
            createSingleBodyScene(body),
            createFeaturedCamera(),
            new Calligraphic2DBuffer(),
            new Calligraphic2DBuffer(),
            new Calligraphic2DBuffer(),
        );

        for (let i = 0; i < dump.edges.size(); i++) {
            const edge: HiddenLineRenderer.AppelEdgeDump = dump.edges.get(i);
            for (let j = 0; j < edge.segments.size(); j++) {
                const segment: HiddenLineRenderer.AppelSegmentDump = edge.segments.get(j);
                const classifiedVisible: boolean = "visible" === segment.classification;
                const unoccluded: boolean = segment.midpointQuantitativeInvisibility === 0;
                expect(
                    classifiedVisible,
                    "Segment of edge " +
                        edge.edgeIndex +
                        " (type=" +
                        edge.edgeTypeName +
                        ") classified '" +
                        segment.classification +
                        "' but midpoint QI is " +
                        segment.midpointQuantitativeInvisibility +
                        "; classification must agree with QI",
                ).toBe(unoccluded);
            }
        }
    }, 120_000);

    it("given_rotatedKurlanderBowl_when_dumpingEdges_then_worldEndpointsStayOnTransformedEdges", () => {
        const solid: PolyhedralBoundedSolid = readSolid(resolveKurlanderBowlStep());
        const rotation: Matrix4x4d = new Matrix4x4d().axisRotation(JavaMath.toRadians(27.0), 0.0, 0.0, 1.0);
        const body: SimpleBody = createBody(solid, rotation);
        const dump: HiddenLineRenderer.AppelAlgorithmDump = HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
            createSingleBodyScene(body),
            createFeaturedCamera(),
            new Calligraphic2DBuffer(),
            new Calligraphic2DBuffer(),
            new Calligraphic2DBuffer(),
        );

        for (let i = 0; i < dump.edges.size(); i++) {
            const edge: HiddenLineRenderer.AppelEdgeDump = dump.edges.get(i);
            expect(
                isEdgeEndpointPairPresentInBody(solid, body, edge.start!, edge.end!),
                "Dumped edge endpoints must stay on transformed solid edges",
            ).toBe(true);
        }
    }, 120_000);

    /**
    Independent regression guard for quantitative-invisibility robustness on the
    APPE1967 featured object (slot + through-hole), whose axis-aligned faces make
    grazing lines of sight common. For every segment the renderer emits, an
    independent ray oracle recomputes visibility. The oracle samples four
    jittered lines of sight and only votes when they all agree, so genuinely
    ambiguous silhouette points (measure zero) are skipped and only stable,
    decidable discrepancies fail the test.

    The oracle's point-in-face test, occluder counting and tie handling are
    written here from scratch, independent of the production
    computeQuantitativeInvisibility, so this is not tautological.
    */
    it("given_featuredObject_when_classifyingSegments_then_visibilityMatchesIndependentRayOracle", () => {
        const solid: PolyhedralBoundedSolid | null = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        if (solid === null) {
            return;
        }
        const body: SimpleBody = createBody(solid, new Matrix4x4d());
        const camera: Camera = createFeaturedCamera();
        const dump: HiddenLineRenderer.AppelAlgorithmDump = HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
            createSingleBodyScene(body),
            camera,
            new Calligraphic2DBuffer(),
            new Calligraphic2DBuffer(),
            new Calligraphic2DBuffer(),
        );

        const eye: Vector3Dd = camera.getPosition();
        for (let i = 0; i < dump.edges.size(); i++) {
            const edge: HiddenLineRenderer.AppelEdgeDump = dump.edges.get(i);
            for (let j = 0; j < edge.segments.size(); j++) {
                const segment: HiddenLineRenderer.AppelSegmentDump = edge.segments.get(j);
                const oracle: number = independentStableQiJittered(solid, eye, segment.midpoint!);
                if (oracle < 0) {
                    // Oracle itself unstable here (true silhouette grazing):
                    // the point is geometrically ambiguous, so skip it.
                    continue;
                }
                const classifiedVisible: boolean = "visible" === segment.classification;
                expect(
                    classifiedVisible,
                    "Edge " +
                        edge.edgeIndex +
                        " segment classified '" +
                        segment.classification +
                        "' but independent ray oracle QI is " +
                        oracle +
                        " at midpoint (" +
                        f(segment.midpoint!.x(), 4) +
                        "," +
                        f(segment.midpoint!.y(), 4) +
                        "," +
                        f(segment.midpoint!.z(), 4) +
                        ")",
                ).toBe(oracle === 0);
            }
        }
    });

    /**
    Strict, multi-view visibility check for the MANT1986 split test object
    (SolidModelNames.SPLIT_TEST_PART_1). This concave prism with a notch is the
    object the visual debugger shows wrong hidden/visible/contour lines on. For
    several solid orientations (and the featured camera) every segment the
    renderer emits is compared against the independent jittered ray oracle. Only
    stable (non-silhouette) discrepancies are reported, so failures are real QI
    miscalculations, not measure-zero grazing ties.
    */
    it("given_splitTestPart1_atManyAngles_then_visibilityMatchesIndependentRayOracle", () => {
        const anglesDeg: number[] = [0.0, 27.0, 73.0, 130.0, 211.0, 300.0];
        const camera: Camera = createFeaturedCamera();
        const eye: Vector3Dd = camera.getPosition();
        let disagreements = "";

        for (let a = 0; a < anglesDeg.length; a++) {
            const solid: PolyhedralBoundedSolid = SimpleTestGeometryLibrary.createTestObjectMANT1986_1();
            const rotation: Matrix4x4d = new Matrix4x4d().axisRotation(
                JavaMath.toRadians(anglesDeg[a]!),
                0.0,
                0.0,
                1.0,
            );
            const body: SimpleBody = createBody(solid, rotation);
            const dump: HiddenLineRenderer.AppelAlgorithmDump = HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
                createSingleBodyScene(body),
                camera,
                new Calligraphic2DBuffer(),
                new Calligraphic2DBuffer(),
                new Calligraphic2DBuffer(),
            );

            for (let i = 0; i < dump.edges.size(); i++) {
                const edge: HiddenLineRenderer.AppelEdgeDump = dump.edges.get(i);
                for (let j = 0; j < edge.segments.size(); j++) {
                    const segment: HiddenLineRenderer.AppelSegmentDump = edge.segments.get(j);
                    const oracle: number = independentStableQi(solid, body, eye, segment.midpoint!);
                    if (oracle < 0) {
                        continue;
                    }
                    const classifiedVisible: boolean = "visible" === segment.classification;
                    if (classifiedVisible !== (oracle === 0)) {
                        disagreements +=
                            "angle=" +
                            f(anglesDeg[a]!, 0) +
                            " edge=" +
                            edge.edgeIndex +
                            " type=" +
                            edge.edgeTypeName +
                            " seg=" +
                            j +
                            " class=" +
                            segment.classification +
                            " oracleQI=" +
                            oracle +
                            " rendererQI=" +
                            segment.midpointQuantitativeInvisibility +
                            " mid=(" +
                            f(segment.midpoint!.x(), 3) +
                            "," +
                            f(segment.midpoint!.y(), 3) +
                            "," +
                            f(segment.midpoint!.z(), 3) +
                            ")\n";
                    }
                }
            }
        }

        expect(disagreements.length, "Renderer/oracle visibility disagreements:\n" + disagreements).toBe(0);
    });

    /**
    Guards the quantitative-invisibility fix where a perpendicular jitter
    counted faces INCIDENT to the sample point (its own surface, including the
    silhouette edge running through a face boundary) as phantom occluders, on the
    axis-aligned APPE1967 featured object. At rot(45,60) the whole edge v(47-48)'s
    neighbour edge 31 was drawn hidden though it is fully visible. Compared
    against the 12-box ground truth, skipping the band around true transitions.
    */
    it("given_featuredObject_atRotation_then_visibleEdgeNotPhantomHidden", () => {
        const solid: PolyhedralBoundedSolid | null = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        if (solid === null) {
            return;
        }
        const camera: Camera = createFeaturedCamera();
        const eye: Vector3Dd = camera.getPosition();
        const rotation: Matrix4x4d = new Matrix4x4d()
            .axisRotation(JavaMath.toRadians(45.0), 0.0, 0.0, 1.0)
            .multiply(new Matrix4x4d().axisRotation(JavaMath.toRadians(60.0), 1.0, 0.0, 0.0));
        const body: SimpleBody = createBody(solid, rotation);
        const mInv: Matrix4x4d = body.getTransformationMatrix().inverse();
        const dump: HiddenLineRenderer.AppelAlgorithmDump = HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
            createSingleBodyScene(body),
            camera,
            new Calligraphic2DBuffer(),
            new Calligraphic2DBuffer(),
            new Calligraphic2DBuffer(),
        );

        let bad = "";
        let edge: HiddenLineRenderer.AppelEdgeDump | null = null;
        for (let i = 0; i < dump.edges.size(); i++) {
            if (dump.edges.get(i).edgeIndex === 31) {
                edge = dump.edges.get(i);
            }
        }
        expect(edge, "edge index 31 present").not.toBeNull();
        const samples = 30;
        const band: number = 1.5 / samples;
        for (let k = 1; k < samples; k++) {
            const t: number = k / samples;
            const p: Vector3Dd = edge!.start!.multiply(1 - t).add(edge!.end!.multiply(t));
            const occ: boolean = featuredOccluded(mInv, eye, p);
            const pa: Vector3Dd = edge!.start!.multiply(1 - (t - band)).add(edge!.end!.multiply(t - band));
            const pb: Vector3Dd = edge!.start!.multiply(1 - (t + band)).add(edge!.end!.multiply(t + band));
            if (featuredOccluded(mInv, eye, pa) !== occ || featuredOccluded(mInv, eye, pb) !== occ) {
                continue;
            }
            let cls: string | null = null;
            for (let j = 0; j < edge!.segments.size(); j++) {
                const sd: HiddenLineRenderer.AppelSegmentDump = edge!.segments.get(j);
                if (t >= sd.tStart - 1e-9 && t <= sd.tEnd + 1e-9) {
                    cls = sd.classification;
                    break;
                }
            }
            const rendVisible: boolean = "visible" === cls;
            if (rendVisible === occ) {
                bad +=
                    "t=" +
                    f(t, 3) +
                    " truth=" +
                    (occ ? "hidden" : "visible") +
                    " renderer=" +
                    (rendVisible ? "visible" : "hidden") +
                    "\n";
            }
        }
        expect(bad.length, "Phantom-occluder mismatches on a visible featured edge:\n" + bad).toBe(0);
    });

    /**
    Partial-occlusion / segment-splitting guard for SPLIT_TEST_PART_1. Earlier
    the renderer split an edge at a contour crossing only when it could also
    resolve the (dead) deltaQI value; when the deltaQI projection fell on a face
    LIMIT or a degenerate plane the split was discarded, leaving a partially
    occluded edge drawn fully visible (not clipped, "muy largo") or fully hidden
    (visible part "no detectado"). First seen on edge v11-v12 occluded by face
    3-4-11-10 (face id 5).

    For several body orientations that produce genuine partial occlusion, every
    edge is densely sampled and the renderer's per-segment classification is
    compared against an orientation-free ground-truth ray-march of the rotated
    solid, skipping the measure-zero band around each true visibility transition.
    */
    it("given_splitTestPart1_partialOcclusion_then_edgesClippedToGroundTruth", () => {
        const rotations: number[][] = [
            [135, 45],
            [90, 120],
            [105, 105],
            [120, 75],
            [150, 30],
            [165, 15],
            [75, 120],
            [180, 0],
        ];
        const camera: Camera = createFeaturedCamera();
        const eye: Vector3Dd = camera.getPosition();
        let bad = "";

        for (const r of rotations) {
            const rz: Matrix4x4d = new Matrix4x4d().axisRotation(JavaMath.toRadians(r[0]!), 0.0, 0.0, 1.0);
            const rx: Matrix4x4d = new Matrix4x4d().axisRotation(JavaMath.toRadians(r[1]!), 1.0, 0.0, 0.0);
            const rotation: Matrix4x4d = rz.multiply(rx);
            const body: SimpleBody = createBody(SimpleTestGeometryLibrary.createTestObjectMANT1986_1(), rotation);
            const mInv: Matrix4x4d = body.getTransformationMatrix().inverse();

            const dump: HiddenLineRenderer.AppelAlgorithmDump = HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
                createSingleBodyScene(body),
                camera,
                new Calligraphic2DBuffer(),
                new Calligraphic2DBuffer(),
                new Calligraphic2DBuffer(),
            );

            for (let ei = 0; ei < dump.edges.size(); ei++) {
                const e: HiddenLineRenderer.AppelEdgeDump = dump.edges.get(ei);
                const samples = 40;
                const band: number = 1.5 / samples;
                for (let k = 1; k < samples; k++) {
                    const t: number = k / samples;
                    const occ: boolean = edgePointOccluded(mInv, eye, e, t);
                    if (
                        edgePointOccluded(mInv, eye, e, t - band) !== occ ||
                        edgePointOccluded(mInv, eye, e, t + band) !== occ
                    ) {
                        continue;
                    }
                    let cls: string | null = null;
                    for (let j = 0; j < e.segments.size(); j++) {
                        const sd: HiddenLineRenderer.AppelSegmentDump = e.segments.get(j);
                        if (t >= sd.tStart - 1e-9 && t <= sd.tEnd + 1e-9) {
                            cls = sd.classification;
                            break;
                        }
                    }
                    const rendVisible: boolean = "visible" === cls;
                    if (rendVisible === occ) {
                        bad +=
                            "rot(" +
                            r[0] +
                            "," +
                            r[1] +
                            ") edge=" +
                            e.edgeIndex +
                            " t=" +
                            f(t, 3) +
                            " truth=" +
                            (occ ? "hidden" : "visible") +
                            " renderer=" +
                            (rendVisible ? "visible" : "hidden") +
                            "\n";
                    }
                }
            }
        }
        expect(bad.length, "Partial-occlusion clipping mismatches vs ground truth:\n" + bad).toBe(0);
    }, 120_000);

    /**
    Edge-type (contour / visible / hidden) classification guard for
    SPLIT_TEST_PART_1. The renderer styles silhouette (contour) edges differently
    from interior visible edges, and that styling depends on a correct per-face
    front/back test. The test used to derive the face normal from the cross
    product of the first three loop vertices, which flips to an inward normal at
    a reflex corner of the notched cap and mislabels faces. This guard recomputes
    each adjacent face's front/back independently (Newell normal over the whole
    world-space loop, oriented outward, compared with the eye) and asserts the
    renderer's edge type agrees. Edges with a near-silhouette (edge-on) adjacent
    face are skipped as genuinely ambiguous.
    */
    it("given_splitTestPart1_atManyAngles_then_edgeTypesMatchIndependentFrontBack", () => {
        // Z-only and tilted (Rz*Rx) orientations; the tilted ones present the
        // notched cap's reflex corner to the camera, where the old first-three-
        // vertex normal flipped.
        const rotations: number[][] = [
            [0, 0],
            [27, 0],
            [73, 0],
            [130, 0],
            [211, 0],
            [300, 0],
            [105, 105],
            [135, 45],
            [150, 30],
            [90, 120],
            [120, 75],
            [165, 15],
        ];
        const camera: Camera = createFeaturedCamera();
        const eye: Vector3Dd = camera.getPosition();
        let bad = "";

        for (let a = 0; a < rotations.length; a++) {
            const solid: PolyhedralBoundedSolid = SimpleTestGeometryLibrary.createTestObjectMANT1986_1();
            const rz: Matrix4x4d = new Matrix4x4d().axisRotation(JavaMath.toRadians(rotations[a]![0]!), 0.0, 0.0, 1.0);
            const rx: Matrix4x4d = new Matrix4x4d().axisRotation(JavaMath.toRadians(rotations[a]![1]!), 1.0, 0.0, 0.0);
            const rotation: Matrix4x4d = rz.multiply(rx);
            const body: SimpleBody = createBody(solid, rotation);
            const m: Matrix4x4d = body.getTransformationMatrix();
            const dump: HiddenLineRenderer.AppelAlgorithmDump = HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
                createSingleBodyScene(body),
                camera,
                new Calligraphic2DBuffer(),
                new Calligraphic2DBuffer(),
                new Calligraphic2DBuffer(),
            );

            for (let i = 0; i < dump.edges.size(); i++) {
                const e: HiddenLineRenderer.AppelEdgeDump = dump.edges.get(i);
                const signA: number = faceFrontSign(solid, m, eye, e.face1Id);
                const signB: number = faceFrontSign(solid, m, eye, e.face2Id);
                if (signA === 0 || signB === 0) {
                    continue; // near-silhouette face: ambiguous, skip
                }
                const frontA: boolean = signA > 0;
                const frontB: boolean = signB > 0;
                const expected: string = frontA && frontB ? "visible" : frontA || frontB ? "contour" : "hidden";
                if (expected !== e.edgeTypeName) {
                    bad +=
                        "rot(" +
                        rotations[a]![0] +
                        "," +
                        rotations[a]![1] +
                        ") edge=" +
                        e.edgeIndex +
                        " faces=[" +
                        e.face1Id +
                        "," +
                        e.face2Id +
                        "] expected=" +
                        expected +
                        " renderer=" +
                        e.edgeTypeName +
                        "\n";
                }
            }
        }
        expect(bad.length, "Edge-type/front-back disagreements:\n" + bad).toBe(0);
    });
});

// The APPE1967 featured object is the union of 12 axis-aligned boxes
// (center x,y,z; half-extent x,y,z), giving a trivial, trustworthy
// point-in-solid for ground-truth visibility.
const FEATURED_BOXES: number[][] = [
    [0.5, 0.1, 0.1, 0.5, 0.1, 0.1],
    [0.5, 0.9, 0.1, 0.5, 0.1, 0.1],
    [0.1, 0.5, 0.1, 0.1, 0.5, 0.1],
    [0.9, 0.5, 0.1, 0.1, 0.5, 0.1],
    [0.1, 0.5, 0.1, 0.1, 0.5, 0.1],
    [0.1, 0.5, 0.9, 0.1, 0.5, 0.1],
    [0.1, 0.1, 0.5, 0.1, 0.1, 0.5],
    [0.1, 0.9, 0.5, 0.1, 0.1, 0.5],
    [0.3, 0.5, 0.5, 0.3, 0.1, 0.1],
    [0.5, 0.5, 0.5, 0.1, 0.1, 0.5],
    [0.7, 0.5, 0.9, 0.3, 0.1, 0.1],
    [0.9, 0.5, 0.9, 0.1, 0.5, 0.1],
];

function insideFeaturedLocal(p: Vector3Dd): boolean {
    for (const b of FEATURED_BOXES) {
        if (
            Math.abs(p.x() - b[0]!) <= b[3]! - 1e-7 &&
            Math.abs(p.y() - b[1]!) <= b[4]! - 1e-7 &&
            Math.abs(p.z() - b[2]!) <= b[5]! - 1e-7
        ) {
            return true;
        }
    }
    return false;
}

function featuredOccluded(mInv: Matrix4x4d, eye: Vector3Dd, target: Vector3Dd): boolean {
    const d: Vector3Dd = target.subtract(eye);
    const n = 3000;
    for (let i = 1; i < n; i++) {
        const s: number = i / n;
        if (s >= 1.0 - 3e-4) {
            break;
        }
        if (insideFeaturedLocal(mInv.multiply(eye.add(d.multiply(s))))) {
            return true;
        }
    }
    return false;
}

/** +1 if the face (by id) is front-facing from the eye, -1 if back-facing,
    0 if near edge-on (ambiguous). Uses an outward Newell normal over the
    world-transformed loop, independent of the renderer's plane code. */
function faceFrontSign(solid: PolyhedralBoundedSolid, m: Matrix4x4d, eye: Vector3Dd, faceId: number): number {
    let face: _PolyhedralBoundedSolidFace | null = null;
    for (let i = 0; i < solid.getPolygonsList().size(); i++) {
        if (solid.getPolygonsList().get(i).id === faceId) {
            face = solid.getPolygonsList().get(i);
            break;
        }
    }
    if (face === null) {
        return 0;
    }
    const poly: ArrayList<Vector3Dd> = transformedOuterLoop(face, m);
    if (poly.size() < 3) {
        return 0;
    }
    // Newell normal over the world-space loop, traversed in BREP winding
    // order: since the body transform preserves orientation, this is the
    // outward normal directly (no centroid heuristic needed).
    const n: Vector3Dd | null = newellNormal(poly);
    if (n === null) {
        return 0;
    }
    const centroid: Vector3Dd = polygonCentroid(poly);
    const toEye: Vector3Dd = eye.subtract(centroid);
    const cosine: number = n.dotProduct(toEye) / (n.length() * toEye.length() + 1e-18);
    if (Math.abs(cosine) < 1.0e-3) {
        return 0;
    }
    return cosine > 0.0 ? 1 : -1;
}

const MANT1986_PROFILE: number[][] = [
    [0.0, 0.0],
    [0.94, 0.0],
    [0.94, 0.46],
    [0.6, 0.3],
    [0.37, 0.3],
    [0.18, 0.46],
    [0.0, 0.3],
];

function edgePointOccluded(mInv: Matrix4x4d, eye: Vector3Dd, e: HiddenLineRenderer.AppelEdgeDump, t: number): boolean {
    const p: Vector3Dd = e.start!.multiply(1.0 - t).add(e.end!.multiply(t));
    const d: Vector3Dd = p.subtract(eye);
    const n = 3000;
    for (let i = 1; i < n; i++) {
        const s: number = i / n;
        if (s >= 1.0 - 3e-4) {
            break;
        }
        if (insideMant1986Local(mInv.multiply(eye.add(d.multiply(s))))) {
            return true;
        }
    }
    return false;
}

function insideMant1986Local(p: Vector3Dd): boolean {
    if (p.y() <= 1e-6 || p.y() >= 0.4 - 1e-6) {
        return false;
    }
    let inside = false;
    const m: number = MANT1986_PROFILE.length;
    for (let k = 0; k < m; k++) {
        const x1: number = MANT1986_PROFILE[k]![0]!;
        const z1: number = MANT1986_PROFILE[k]![1]!;
        const x2: number = MANT1986_PROFILE[(k + 1) % m]![0]!;
        const z2: number = MANT1986_PROFILE[(k + 1) % m]![1]!;
        if (z1 > p.z() !== z2 > p.z()) {
            const xi: number = ((x2 - x1) * (p.z() - z1)) / (z2 - z1) + x1;
            if (p.x() < xi) {
                inside = !inside;
            }
        }
    }
    return inside;
}

/**
Orientation-free, ground-truth visibility oracle for a point on the surface
of a single closed solid, independent of the production QI.

A surface point is visible from the eye iff the open segment (eye, point)
crosses no face interior strictly before the point. Orientation of the face
normals is irrelevant: only the count of strict-interior boundary crossings
before the target matters (incident faces sit at t ~= |eye-point| and are
excluded by the pull-back cut-off). This deliberately does NOT jitter the
target perpendicular to the line of sight (the technique the production
kernel uses), so it does not share the kernel's failure mode of pushing the
sample across its own incident faces at concave edges.

@return 0 if visible, 1 if occluded, or -1 when a crossing lands on a face
        boundary (a measure-zero silhouette grazing) and the answer is
        genuinely ambiguous, so the caller should skip the sample.
*/
function independentStableQi(
    solid: PolyhedralBoundedSolid,
    body: SimpleBody,
    eye: Vector3Dd,
    target: Vector3Dd,
): number {
    let d: Vector3Dd = target.subtract(eye);
    const t0: number = d.length();
    if (t0 < 1e-9) {
        return 0;
    }
    d = d.multiply(1.0 / t0);
    const pull: number = t0 * 5.0e-4;

    const m: Matrix4x4d = body.getTransformationMatrix();
    const crossings: ArrayList<number> = new ArrayList<number>();
    for (let i = 0; i < solid.getPolygonsList().size(); i++) {
        const face: _PolyhedralBoundedSolidFace = solid.getPolygonsList().get(i);
        const poly: ArrayList<Vector3Dd> = transformedOuterLoop(face, m);
        if (poly.size() < 3) {
            continue;
        }
        const n: Vector3Dd | null = newellNormal(poly);
        if (n === null) {
            continue;
        }
        const denom: number = d.dotProduct(n);
        if (Math.abs(denom) < 1e-9) {
            continue;
        }
        const t: number = poly.get(0).subtract(eye).dotProduct(n) / denom;
        if (t <= pull || t >= t0 - pull) {
            continue;
        }
        const p: Vector3Dd = eye.add(d.multiply(t));
        const edgeDistance: number = distanceToPolygonBoundary(poly, n, p);
        if (edgeDistance < pull) {
            // Crossing essentially on a silhouette edge: ambiguous.
            return -1;
        }
        if (polygonContains(poly, n, p)) {
            let duplicate = false;
            for (let k = 0; k < crossings.size(); k++) {
                if (Math.abs(crossings.get(k) - t) < 1e-6) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                crossings.add(t);
            }
        }
    }
    return crossings.isEmpty() ? 0 : 1;
}

/** Minimum distance, in the face's dominant 2D projection, from point p to
    any edge of the polygon. Used to detect silhouette-grazing ambiguity. */
function distanceToPolygonBoundary(poly: ArrayList<Vector3Dd>, n: Vector3Dd, p: Vector3Dd): number {
    const ax: number = Math.abs(n.x());
    const ay: number = Math.abs(n.y());
    const az: number = Math.abs(n.z());
    const drop: number = ax >= ay && ax >= az ? 0 : ay >= az ? 1 : 2;
    const pu: number = projOf(p, drop, true);
    const pv: number = projOf(p, drop, false);
    let best: number = Double.MAX_VALUE;
    for (let i = 0; i < poly.size(); i++) {
        const a: Vector3Dd = poly.get(i);
        const b: Vector3Dd = poly.get((i + 1) % poly.size());
        const au: number = projOf(a, drop, true);
        const av: number = projOf(a, drop, false);
        const bu: number = projOf(b, drop, true);
        const bv: number = projOf(b, drop, false);
        const dist: number = pointSegmentDistance2D(pu, pv, au, av, bu, bv);
        if (dist < best) {
            best = dist;
        }
    }
    return best;
}

function pointSegmentDistance2D(px: number, py: number, ax: number, ay: number, bx: number, by: number): number {
    const dx: number = bx - ax;
    const dy: number = by - ay;
    const lengthSquared: number = dx * dx + dy * dy;
    if (lengthSquared < 1e-18) {
        return Math.hypot(px - ax, py - ay);
    }
    let s: number = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
    s = Math.max(0.0, Math.min(1.0, s));
    return Math.hypot(px - (ax + s * dx), py - (ay + s * dy));
}

function transformedOuterLoop(face: _PolyhedralBoundedSolidFace, m: Matrix4x4d): ArrayList<Vector3Dd> {
    const poly: ArrayList<Vector3Dd> = new ArrayList<Vector3Dd>();
    if (face.boundariesList.size() === 0) {
        return poly;
    }
    const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(0);
    if (loop === null || loop.boundaryStartHalfEdge === null) {
        return poly;
    }
    let he: _PolyhedralBoundedSolidHalfEdge | null = loop.boundaryStartHalfEdge;
    const start: _PolyhedralBoundedSolidHalfEdge = he;
    do {
        if (he.startingVertex !== null) {
            poly.add(m.multiply(he.startingVertex.position));
        }
        he = he.next();
    } while (he !== null && he !== start);
    return poly;
}

function polygonCentroid(poly: ArrayList<Vector3Dd>): Vector3Dd {
    let x = 0.0;
    let y = 0.0;
    let z = 0.0;
    for (let i = 0; i < poly.size(); i++) {
        x += poly.get(i).x();
        y += poly.get(i).y();
        z += poly.get(i).z();
    }
    return new Vector3Dd(x / poly.size(), y / poly.size(), z / poly.size());
}

function newellNormal(poly: ArrayList<Vector3Dd>): Vector3Dd | null {
    let nx = 0.0;
    let ny = 0.0;
    let nz = 0.0;
    for (let i = 0; i < poly.size(); i++) {
        const a: Vector3Dd = poly.get(i);
        const b: Vector3Dd = poly.get((i + 1) % poly.size());
        nx += (a.y() - b.y()) * (a.z() + b.z());
        ny += (a.z() - b.z()) * (a.x() + b.x());
        nz += (a.x() - b.x()) * (a.y() + b.y());
    }
    const n = new Vector3Dd(nx, ny, nz);
    if (n.length() < 1e-12) {
        return null;
    }
    return n.normalized();
}

function polygonContains(poly: ArrayList<Vector3Dd>, n: Vector3Dd, p: Vector3Dd): boolean {
    const ax: number = Math.abs(n.x());
    const ay: number = Math.abs(n.y());
    const az: number = Math.abs(n.z());
    const drop: number = ax >= ay && ax >= az ? 0 : ay >= az ? 1 : 2;
    const pu: number = projOf(p, drop, true);
    const pv: number = projOf(p, drop, false);
    let inside = false;
    for (let i = 0; i < poly.size(); i++) {
        const a: Vector3Dd = poly.get(i);
        const b: Vector3Dd = poly.get((i + 1) % poly.size());
        const au: number = projOf(a, drop, true);
        const av: number = projOf(a, drop, false);
        const bu: number = projOf(b, drop, true);
        const bv: number = projOf(b, drop, false);
        if (av > pv !== bv > pv) {
            const uAtP: number = au + ((pv - av) / (bv - av)) * (bu - au);
            if (pu < uAtP) {
                inside = !inside;
            }
        }
    }
    return inside;
}

function projOf(v: Vector3Dd, drop: number, first: boolean): number {
    if (drop === 0) {
        return first ? v.y() : v.z();
    }
    if (drop === 1) {
        return first ? v.z() : v.x();
    }
    return first ? v.x() : v.y();
}

/** Returns the occluder count the four jittered lines of sight agree on, or
    -1 when they disagree (the target is essentially on a silhouette).
    Java's `independentStableQi(solid, eye, target)` overload. */
function independentStableQiJittered(solid: PolyhedralBoundedSolid, eye: Vector3Dd, target: Vector3Dd): number {
    let d: Vector3Dd = target.subtract(eye);
    const t0: number = d.length();
    if (t0 < 1e-9) {
        return 0;
    }
    d = d.multiply(1.0 / t0);
    const helper: Vector3Dd = Math.abs(d.x()) < 0.9 ? new Vector3Dd(1.0, 0.0, 0.0) : new Vector3Dd(0.0, 1.0, 0.0);
    const u: Vector3Dd = d.crossProduct(helper).normalized();
    const v: Vector3Dd = d.crossProduct(u).normalized();
    const delta: number = t0 * 1.0e-4;

    const first: number = naiveQi(solid, eye, target.add(u.multiply(delta)));
    const rest: number[] = [
        naiveQi(solid, eye, target.add(u.multiply(-delta))),
        naiveQi(solid, eye, target.add(v.multiply(delta))),
        naiveQi(solid, eye, target.add(v.multiply(-delta))),
    ];
    for (let i = 0; i < rest.length; i++) {
        if (rest[i] !== first) {
            return -1;
        }
    }
    return first;
}

function naiveQi(solid: PolyhedralBoundedSolid, eye: Vector3Dd, target: Vector3Dd): number {
    let d: Vector3Dd = target.subtract(eye);
    const t0: number = d.length();
    d = d.multiply(1.0 / t0);
    let qi = 0;
    for (let i = 0; i < solid.getPolygonsList().size(); i++) {
        const face: _PolyhedralBoundedSolidFace = solid.getPolygonsList().get(i);
        const plane: InfinitePlane | null = face.getContainingPlane();
        if (plane === null) {
            continue;
        }
        const n: Vector3Dd = plane.getNormal().normalized();
        const denom: number = d.dotProduct(n);
        if (Math.abs(denom) < 1e-9 || denom >= 0.0) {
            continue;
        }
        const q: Vector3Dd | null = oracleFirstVertex(face);
        if (q === null) {
            continue;
        }
        const t: number = q.subtract(eye).dotProduct(n) / denom;
        if (t <= 1e-6 || t >= t0 - 1e-6) {
            continue;
        }
        if (oraclePointInFace(face, n, eye.add(d.multiply(t)))) {
            qi++;
        }
    }
    return qi;
}

function oracleFirstVertex(face: _PolyhedralBoundedSolidFace): Vector3Dd | null {
    for (let i = 0; i < face.boundariesList.size(); i++) {
        const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i);
        if (
            loop !== null &&
            loop.boundaryStartHalfEdge !== null &&
            loop.boundaryStartHalfEdge.startingVertex !== null
        ) {
            return loop.boundaryStartHalfEdge.startingVertex.position;
        }
    }
    return null;
}

function oraclePointInFace(face: _PolyhedralBoundedSolidFace, n: Vector3Dd, p: Vector3Dd): boolean {
    const ax: number = Math.abs(n.x());
    const ay: number = Math.abs(n.y());
    const az: number = Math.abs(n.z());
    const drop: number = ax >= ay && ax >= az ? 0 : ay >= az ? 1 : 2;
    const pu: number = oracleProj(p, drop, true);
    const pv: number = oracleProj(p, drop, false);
    let inside = false;

    for (let i = 0; i < face.boundariesList.size(); i++) {
        const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i);
        if (loop === null || loop.boundaryStartHalfEdge === null) {
            continue;
        }
        let he: _PolyhedralBoundedSolidHalfEdge = loop.boundaryStartHalfEdge;
        const start: _PolyhedralBoundedSolidHalfEdge = he;
        do {
            const nx: _PolyhedralBoundedSolidHalfEdge | null = he.next();
            if (nx === null) {
                break;
            }
            const au: number = oracleProj(he.startingVertex.position, drop, true);
            const av: number = oracleProj(he.startingVertex.position, drop, false);
            const bu: number = oracleProj(nx.startingVertex.position, drop, true);
            const bv: number = oracleProj(nx.startingVertex.position, drop, false);
            if (av > pv !== bv > pv) {
                const uAtP: number = au + ((pv - av) / (bv - av)) * (bu - au);
                if (pu < uAtP) {
                    inside = !inside;
                }
            }
            he = nx;
        } while (he !== start);
    }
    return inside;
}

function oracleProj(v: Vector3Dd, drop: number, first: boolean): number {
    if (drop === 0) {
        return first ? v.y() : v.z();
    }
    if (drop === 1) {
        return first ? v.z() : v.x();
    }
    return first ? v.x() : v.y();
}

function allLineCoordinatesAreFinite(lines: Calligraphic2DBuffer): boolean {
    for (let i = 0; i < lines.getNumLines(); i++) {
        const segment: [Vector3Dd, Vector3Dd] = lines.get2DLine(i);
        if (!isFinite(segment[0]) || !isFinite(segment[1])) {
            return false;
        }
    }
    return true;
}

/** Java's `createSingleBodyScene(PolyhedralBoundedSolid)` overload. */
function createSingleBodySceneFromSolid(solid: PolyhedralBoundedSolid): ArrayList<SimpleBody> {
    return createSingleBodyScene(createBody(solid, new Matrix4x4d()));
}

function createSingleBodyScene(body: SimpleBody): ArrayList<SimpleBody> {
    const bodies: ArrayList<SimpleBody> = new ArrayList<SimpleBody>();
    bodies.add(body);
    return bodies;
}

function createBody(solid: PolyhedralBoundedSolid, rotation: Matrix4x4d): SimpleBody {
    const body = new SimpleBody();
    body.setGeometry(solid);
    body.setPosition(new Vector3Dd());
    body.setRotation(rotation);
    body.setRotationInverse(rotation.inverse());
    return body;
}

function createFeaturedCamera(): Camera {
    const camera = new Camera();
    camera.setPosition(new Vector3Dd(2.0, -1.0, 2.0));
    let rotation: Matrix4x4d = new Matrix4x4d();
    rotation = rotation.eulerAnglesRotation(JavaMath.toRadians(135.0), JavaMath.toRadians(-35.0), 0.0);
    camera.setRotation(rotation);
    camera.updateVectors();
    return camera;
}

function isFinite(point: Vector3Dd): boolean {
    return Double.isFinite(point.x()) && Double.isFinite(point.y()) && Double.isFinite(point.z());
}

/** Java's `StepReader.readSolid(File)`: the reader over the file's stream. */
function readSolid(stepFile: File): PolyhedralBoundedSolid {
    const in_ = new FileInputStream(stepFile);
    try {
        return StepReader.readSolid(in_);
    } finally {
        in_.close();
    }
}

function resolveKurlanderBowlStep(): File {
    const candidates: string[] = [
        "etc/solids/kurlanderBowl.step",
        "../etc/solids/kurlanderBowl.step",
        "../../etc/solids/kurlanderBowl.step",
    ];

    for (let i = 0; i < candidates.length; i++) {
        const file = new File(candidates[i]!);
        if (file.isFile()) {
            return file;
        }
    }
    throw new IllegalStateException("Cannot locate etc/solids/kurlanderBowl.step");
}

function isEdgeEndpointPairPresentInBody(
    solid: PolyhedralBoundedSolid,
    body: SimpleBody,
    start: Vector3Dd,
    end: Vector3Dd,
): boolean {
    for (let i = 0; i < solid.getEdgesList().size(); i++) {
        const edge: _PolyhedralBoundedSolidEdge = solid.getEdgesList().get(i);
        const edgeStart: Vector3Dd = body.getTransformationMatrix().multiply(edge.leftHalf!.startingVertex.position);
        const edgeEnd: Vector3Dd = body.getTransformationMatrix().multiply(edge.rightHalf!.startingVertex.position);
        if (sameSegment(start, end, edgeStart, edgeEnd)) {
            return true;
        }
    }
    return false;
}

function sameSegment(a0: Vector3Dd, a1: Vector3Dd, b0: Vector3Dd, b1: Vector3Dd): boolean {
    return (
        (Vector3Dd.distance(a0, b0) <= 1e-6 && Vector3Dd.distance(a1, b1) <= 1e-6) ||
        (Vector3Dd.distance(a0, b1) <= 1e-6 && Vector3Dd.distance(a1, b0) <= 1e-6)
    );
}
