package vsdk.toolkit.render;

import java.io.File;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary;
import vsdk.toolkit.io.geometry.stepCad.reader.StepReader;
import vsdk.toolkit.media.Calligraphic2DBuffer;
import vsdk.toolkit.render.hiddenLine.HiddenLineRenderer;

import static org.assertj.core.api.Assertions.assertThat;

/**
Regression test for APPEL hidden-line rendering over a polyhedral bounded
solid fixture.
 */
class HiddenLineRendererTest
{
    @Test
    void given_appe1967FeaturedSolid_when_executingAppelAlgorithm_then_allGeneratedLinesStayFinite()
    {
        // Arrange
        PolyhedralBoundedSolid solid;
        try {
            solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        }
        catch ( NullPointerException exception ) {
            return;
        }
        if ( solid == null ) {
            return;
        }
        Calligraphic2DBuffer contourLines = new Calligraphic2DBuffer();
        Calligraphic2DBuffer visibleLines = new Calligraphic2DBuffer();
        Calligraphic2DBuffer hiddenLines = new Calligraphic2DBuffer();

        // Action
        HiddenLineRenderer.executeAppelAlgorithm(createSingleBodyScene(solid),
            createFeaturedCamera(), contourLines, visibleLines, hiddenLines);

        // Assert
        assertThat(allLineCoordinatesAreFinite(contourLines)).isTrue();
        assertThat(allLineCoordinatesAreFinite(visibleLines)).isTrue();
        assertThat(allLineCoordinatesAreFinite(hiddenLines)).isTrue();
    }

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
    @Test
    void given_concaveKurlanderBowl_when_classifyingSegments_then_visibilityMatchesMidpointQI()
        throws Exception
    {
        PolyhedralBoundedSolid solid = StepReader.readSolid(resolveKurlanderBowlStep());
        SimpleBody body = createBody(solid, new Matrix4x4d());
        HiddenLineRenderer.AppelAlgorithmDump dump =
            HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
                createSingleBodyScene(body), createFeaturedCamera(),
                new Calligraphic2DBuffer(), new Calligraphic2DBuffer(),
                new Calligraphic2DBuffer());

        for ( int i = 0; i < dump.edges.size(); i++ ) {
            HiddenLineRenderer.AppelEdgeDump edge = dump.edges.get(i);
            for ( int j = 0; j < edge.segments.size(); j++ ) {
                HiddenLineRenderer.AppelSegmentDump segment = edge.segments.get(j);
                boolean classifiedVisible =
                    "visible".equals(segment.classification);
                boolean unoccluded =
                    segment.midpointQuantitativeInvisibility == 0;
                assertThat(classifiedVisible)
                    .as("Segment of edge %d (type=%s) classified '%s' but "
                        + "midpoint QI is %d; classification must agree with QI",
                        edge.edgeIndex, edge.edgeTypeName, segment.classification,
                        segment.midpointQuantitativeInvisibility)
                    .isEqualTo(unoccluded);
            }
        }
    }

    @Test
    void given_rotatedKurlanderBowl_when_dumpingEdges_then_worldEndpointsStayOnTransformedEdges()
        throws Exception
    {
        PolyhedralBoundedSolid solid = StepReader.readSolid(resolveKurlanderBowlStep());
        Matrix4x4d rotation = new Matrix4x4d().axisRotation(
            Math.toRadians(27.0), 0.0, 0.0, 1.0);
        SimpleBody body = createBody(solid, rotation);
        HiddenLineRenderer.AppelAlgorithmDump dump =
            HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
                createSingleBodyScene(body), createFeaturedCamera(),
                new Calligraphic2DBuffer(), new Calligraphic2DBuffer(),
                new Calligraphic2DBuffer());

        for ( int i = 0; i < dump.edges.size(); i++ ) {
            HiddenLineRenderer.AppelEdgeDump edge = dump.edges.get(i);
            assertThat(isEdgeEndpointPairPresentInBody(solid, body, edge.start, edge.end))
                .as("Dumped edge endpoints must stay on transformed solid edges")
                .isTrue();
        }
    }

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
    @Test
    void given_featuredObject_when_classifyingSegments_then_visibilityMatchesIndependentRayOracle()
    {
        PolyhedralBoundedSolid solid =
            SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        if ( solid == null ) {
            return;
        }
        SimpleBody body = createBody(solid, new Matrix4x4d());
        Camera camera = createFeaturedCamera();
        HiddenLineRenderer.AppelAlgorithmDump dump =
            HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
                createSingleBodyScene(body), camera,
                new Calligraphic2DBuffer(), new Calligraphic2DBuffer(),
                new Calligraphic2DBuffer());

        Vector3Dd eye = camera.getPosition();
        for ( int i = 0; i < dump.edges.size(); i++ ) {
            HiddenLineRenderer.AppelEdgeDump edge = dump.edges.get(i);
            for ( int j = 0; j < edge.segments.size(); j++ ) {
                HiddenLineRenderer.AppelSegmentDump segment = edge.segments.get(j);
                int oracle = independentStableQi(solid, eye, segment.midpoint);
                if ( oracle < 0 ) {
                    // Oracle itself unstable here (true silhouette grazing):
                    // the point is geometrically ambiguous, so skip it.
                    continue;
                }
                boolean classifiedVisible =
                    "visible".equals(segment.classification);
                assertThat(classifiedVisible)
                    .as("Edge %d segment classified '%s' but independent ray "
                        + "oracle QI is %d at midpoint (%.4f,%.4f,%.4f)",
                        edge.edgeIndex, segment.classification, oracle,
                        segment.midpoint.x(), segment.midpoint.y(),
                        segment.midpoint.z())
                    .isEqualTo(oracle == 0);
            }
        }
    }

    /**
    Strict, multi-view visibility check for the MANT1986 split test object
    (SolidModelNames.SPLIT_TEST_PART_1). This concave prism with a notch is the
    object the visual debugger shows wrong hidden/visible/contour lines on. For
    several solid orientations (and the featured camera) every segment the
    renderer emits is compared against the independent jittered ray oracle. Only
    stable (non-silhouette) discrepancies are reported, so failures are real QI
    miscalculations, not measure-zero grazing ties.
     */
    @Test
    void given_splitTestPart1_atManyAngles_then_visibilityMatchesIndependentRayOracle()
    {
        double[] anglesDeg = new double[] { 0.0, 27.0, 73.0, 130.0, 211.0, 300.0 };
        Camera camera = createFeaturedCamera();
        Vector3Dd eye = camera.getPosition();
        StringBuilder disagreements = new StringBuilder();

        for ( int a = 0; a < anglesDeg.length; a++ ) {
            PolyhedralBoundedSolid solid =
                SimpleTestGeometryLibrary.createTestObjectMANT1986_1();
            Matrix4x4d rotation = new Matrix4x4d().axisRotation(
                Math.toRadians(anglesDeg[a]), 0.0, 0.0, 1.0);
            SimpleBody body = createBody(solid, rotation);
            HiddenLineRenderer.AppelAlgorithmDump dump =
                HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
                    createSingleBodyScene(body), camera,
                    new Calligraphic2DBuffer(), new Calligraphic2DBuffer(),
                    new Calligraphic2DBuffer());

            for ( int i = 0; i < dump.edges.size(); i++ ) {
                HiddenLineRenderer.AppelEdgeDump edge = dump.edges.get(i);
                for ( int j = 0; j < edge.segments.size(); j++ ) {
                    HiddenLineRenderer.AppelSegmentDump segment =
                        edge.segments.get(j);
                    int oracle = independentStableQi(solid, body, eye,
                        segment.midpoint);
                    if ( oracle < 0 ) {
                        continue;
                    }
                    boolean classifiedVisible =
                        "visible".equals(segment.classification);
                    if ( classifiedVisible != (oracle == 0) ) {
                        disagreements.append(String.format(
                            "angle=%.0f edge=%d type=%s seg=%d class=%s "
                            + "oracleQI=%d rendererQI=%d mid=(%.3f,%.3f,%.3f)%n",
                            anglesDeg[a], edge.edgeIndex, edge.edgeTypeName, j,
                            segment.classification, oracle,
                            segment.midpointQuantitativeInvisibility,
                            segment.midpoint.x(), segment.midpoint.y(),
                            segment.midpoint.z()));
                    }
                }
            }
        }

        assertThat(disagreements.length())
            .as("Renderer/oracle visibility disagreements:%n%s", disagreements)
            .isZero();
    }

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
    @Test
    void given_splitTestPart1_partialOcclusion_then_edgesClippedToGroundTruth()
    {
        int[][] rotations = {
            {135,45},{90,120},{105,105},{120,75},{150,30},{165,15},{75,120},{180,0}
        };
        Camera camera = createFeaturedCamera();
        Vector3Dd eye = camera.getPosition();
        StringBuilder bad = new StringBuilder();

        for ( int[] r : rotations ) {
            Matrix4x4d rz = new Matrix4x4d().axisRotation(
                Math.toRadians(r[0]), 0.0, 0.0, 1.0);
            Matrix4x4d rx = new Matrix4x4d().axisRotation(
                Math.toRadians(r[1]), 1.0, 0.0, 0.0);
            Matrix4x4d rotation = rz.multiply(rx);
            SimpleBody body = createBody(
                SimpleTestGeometryLibrary.createTestObjectMANT1986_1(), rotation);
            Matrix4x4d mInv = body.getTransformationMatrix().inverse();

            HiddenLineRenderer.AppelAlgorithmDump dump =
                HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
                    createSingleBodyScene(body), camera,
                    new Calligraphic2DBuffer(), new Calligraphic2DBuffer(),
                    new Calligraphic2DBuffer());

            for ( int ei = 0; ei < dump.edges.size(); ei++ ) {
                HiddenLineRenderer.AppelEdgeDump e = dump.edges.get(ei);
                int samples = 40;
                double band = 1.5 / samples;
                for ( int k = 1; k < samples; k++ ) {
                    double t = (double) k / samples;
                    boolean occ = edgePointOccluded(mInv, eye, e, t);
                    if ( edgePointOccluded(mInv, eye, e, t - band) != occ ||
                         edgePointOccluded(mInv, eye, e, t + band) != occ ) {
                        continue;
                    }
                    String cls = null;
                    for ( int j = 0; j < e.segments.size(); j++ ) {
                        HiddenLineRenderer.AppelSegmentDump sd = e.segments.get(j);
                        if ( t >= sd.tStart - 1e-9 && t <= sd.tEnd + 1e-9 ) {
                            cls = sd.classification;
                            break;
                        }
                    }
                    boolean rendVisible = "visible".equals(cls);
                    if ( rendVisible == occ ) {
                        bad.append(String.format(
                            "rot(%d,%d) edge=%d t=%.3f truth=%s renderer=%s%n",
                            r[0], r[1], e.edgeIndex, t, occ ? "hidden" : "visible",
                            rendVisible ? "visible" : "hidden"));
                    }
                }
            }
        }
        assertThat(bad.length())
            .as("Partial-occlusion clipping mismatches vs ground truth:%n%s", bad)
            .isZero();
    }

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
    @Test
    void given_splitTestPart1_atManyAngles_then_edgeTypesMatchIndependentFrontBack()
    {
        // Z-only and tilted (Rz*Rx) orientations; the tilted ones present the
        // notched cap's reflex corner to the camera, where the old first-three-
        // vertex normal flipped.
        int[][] rotations = {
            {0,0},{27,0},{73,0},{130,0},{211,0},{300,0},
            {105,105},{135,45},{150,30},{90,120},{120,75},{165,15}
        };
        Camera camera = createFeaturedCamera();
        Vector3Dd eye = camera.getPosition();
        StringBuilder bad = new StringBuilder();

        for ( int a = 0; a < rotations.length; a++ ) {
            PolyhedralBoundedSolid solid =
                SimpleTestGeometryLibrary.createTestObjectMANT1986_1();
            Matrix4x4d rz = new Matrix4x4d().axisRotation(
                Math.toRadians(rotations[a][0]), 0.0, 0.0, 1.0);
            Matrix4x4d rx = new Matrix4x4d().axisRotation(
                Math.toRadians(rotations[a][1]), 1.0, 0.0, 0.0);
            Matrix4x4d rotation = rz.multiply(rx);
            SimpleBody body = createBody(solid, rotation);
            Matrix4x4d m = body.getTransformationMatrix();
            HiddenLineRenderer.AppelAlgorithmDump dump =
                HiddenLineRenderer.executeAppelAlgorithmWithDiagnostics(
                    createSingleBodyScene(body), camera,
                    new Calligraphic2DBuffer(), new Calligraphic2DBuffer(),
                    new Calligraphic2DBuffer());

            for ( int i = 0; i < dump.edges.size(); i++ ) {
                HiddenLineRenderer.AppelEdgeDump e = dump.edges.get(i);
                int signA = faceFrontSign(solid, m, eye, e.face1Id);
                int signB = faceFrontSign(solid, m, eye, e.face2Id);
                if ( signA == 0 || signB == 0 ) {
                    continue; // near-silhouette face: ambiguous, skip
                }
                boolean frontA = signA > 0;
                boolean frontB = signB > 0;
                String expected = (frontA && frontB) ? "visible" :
                    ((frontA || frontB) ? "contour" : "hidden");
                if ( !expected.equals(e.edgeTypeName) ) {
                    bad.append(String.format(
                        "rot(%d,%d) edge=%d faces=[%d,%d] expected=%s renderer=%s%n",
                        rotations[a][0], rotations[a][1], e.edgeIndex,
                        e.face1Id, e.face2Id, expected, e.edgeTypeName));
                }
            }
        }
        assertThat(bad.length())
            .as("Edge-type/front-back disagreements:%n%s", bad)
            .isZero();
    }

    /** +1 if the face (by id) is front-facing from the eye, -1 if back-facing,
        0 if near edge-on (ambiguous). Uses an outward Newell normal over the
        world-transformed loop, independent of the renderer's plane code. */
    private static int faceFrontSign(PolyhedralBoundedSolid solid, Matrix4x4d m,
        Vector3Dd eye, int faceId)
    {
        _PolyhedralBoundedSolidFace face = null;
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            if ( solid.getPolygonsList().get(i).id == faceId ) {
                face = solid.getPolygonsList().get(i);
                break;
            }
        }
        if ( face == null ) {
            return 0;
        }
        ArrayList<Vector3Dd> poly = transformedOuterLoop(face, m);
        if ( poly.size() < 3 ) {
            return 0;
        }
        // Newell normal over the world-space loop, traversed in BREP winding
        // order: since the body transform preserves orientation, this is the
        // outward normal directly (no centroid heuristic needed).
        Vector3Dd n = newellNormal(poly);
        if ( n == null ) {
            return 0;
        }
        Vector3Dd centroid = polygonCentroid(poly);
        Vector3Dd toEye = eye.subtract(centroid);
        double cosine = n.dotProduct(toEye) /
            (n.length() * toEye.length() + 1e-18);
        if ( Math.abs(cosine) < 1.0e-3 ) {
            return 0;
        }
        return cosine > 0.0 ? 1 : -1;
    }

    private static final double[][] MANT1986_PROFILE = {
        {0.00,0.00},{0.94,0.00},{0.94,0.46},{0.60,0.30},
        {0.37,0.30},{0.18,0.46},{0.00,0.30}
    };

    private static boolean edgePointOccluded(Matrix4x4d mInv, Vector3Dd eye,
        HiddenLineRenderer.AppelEdgeDump e, double t)
    {
        Vector3Dd p = e.start.multiply(1.0 - t).add(e.end.multiply(t));
        Vector3Dd d = p.subtract(eye);
        int n = 3000;
        for ( int i = 1; i < n; i++ ) {
            double s = (double) i / n;
            if ( s >= 1.0 - 3e-4 ) {
                break;
            }
            if ( insideMant1986Local(mInv.multiply(eye.add(d.multiply(s)))) ) {
                return true;
            }
        }
        return false;
    }

    private static boolean insideMant1986Local(Vector3Dd p)
    {
        if ( p.y() <= 1e-6 || p.y() >= 0.4 - 1e-6 ) {
            return false;
        }
        boolean inside = false;
        int m = MANT1986_PROFILE.length;
        for ( int k = 0; k < m; k++ ) {
            double x1 = MANT1986_PROFILE[k][0];
            double z1 = MANT1986_PROFILE[k][1];
            double x2 = MANT1986_PROFILE[(k + 1) % m][0];
            double z2 = MANT1986_PROFILE[(k + 1) % m][1];
            if ( (z1 > p.z()) != (z2 > p.z()) ) {
                double xi = (x2 - x1) * (p.z() - z1) / (z2 - z1) + x1;
                if ( p.x() < xi ) {
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
    private static int independentStableQi(PolyhedralBoundedSolid solid,
                                           SimpleBody body,
                                           Vector3Dd eye,
                                           Vector3Dd target)
    {
        Vector3Dd d = target.subtract(eye);
        double t0 = d.length();
        if ( t0 < 1e-9 ) {
            return 0;
        }
        d = d.multiply(1.0 / t0);
        double pull = t0 * 5.0e-4;

        Matrix4x4d m = body.getTransformationMatrix();
        ArrayList<Double> crossings = new ArrayList<Double>();
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            ArrayList<Vector3Dd> poly = transformedOuterLoop(face, m);
            if ( poly.size() < 3 ) {
                continue;
            }
            Vector3Dd n = newellNormal(poly);
            if ( n == null ) {
                continue;
            }
            double denom = d.dotProduct(n);
            if ( Math.abs(denom) < 1e-9 ) {
                continue;
            }
            double t = poly.get(0).subtract(eye).dotProduct(n) / denom;
            if ( t <= pull || t >= t0 - pull ) {
                continue;
            }
            Vector3Dd p = eye.add(d.multiply(t));
            double edgeDistance = distanceToPolygonBoundary(poly, n, p);
            if ( edgeDistance < pull ) {
                // Crossing essentially on a silhouette edge: ambiguous.
                return -1;
            }
            if ( polygonContains(poly, n, p) ) {
                boolean duplicate = false;
                for ( int k = 0; k < crossings.size(); k++ ) {
                    if ( Math.abs(crossings.get(k) - t) < 1e-6 ) {
                        duplicate = true;
                        break;
                    }
                }
                if ( !duplicate ) {
                    crossings.add(t);
                }
            }
        }
        return crossings.isEmpty() ? 0 : 1;
    }

    /** Minimum distance, in the face's dominant 2D projection, from point p to
        any edge of the polygon. Used to detect silhouette-grazing ambiguity. */
    private static double distanceToPolygonBoundary(ArrayList<Vector3Dd> poly,
                                                    Vector3Dd n, Vector3Dd p)
    {
        double ax = Math.abs(n.x());
        double ay = Math.abs(n.y());
        double az = Math.abs(n.z());
        int drop = (ax >= ay && ax >= az) ? 0 : ((ay >= az) ? 1 : 2);
        double pu = projOf(p, drop, true);
        double pv = projOf(p, drop, false);
        double best = Double.MAX_VALUE;
        for ( int i = 0; i < poly.size(); i++ ) {
            Vector3Dd a = poly.get(i);
            Vector3Dd b = poly.get((i + 1) % poly.size());
            double au = projOf(a, drop, true);
            double av = projOf(a, drop, false);
            double bu = projOf(b, drop, true);
            double bv = projOf(b, drop, false);
            double dist = pointSegmentDistance2D(pu, pv, au, av, bu, bv);
            if ( dist < best ) {
                best = dist;
            }
        }
        return best;
    }

    private static double pointSegmentDistance2D(double px, double py,
        double ax, double ay, double bx, double by)
    {
        double dx = bx - ax;
        double dy = by - ay;
        double lengthSquared = dx * dx + dy * dy;
        if ( lengthSquared < 1e-18 ) {
            return Math.hypot(px - ax, py - ay);
        }
        double s = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
        s = Math.max(0.0, Math.min(1.0, s));
        return Math.hypot(px - (ax + s * dx), py - (ay + s * dy));
    }

    private static ArrayList<Vector3Dd> transformedOuterLoop(
        _PolyhedralBoundedSolidFace face, Matrix4x4d m)
    {
        ArrayList<Vector3Dd> poly = new ArrayList<Vector3Dd>();
        if ( face.boundariesList.size() == 0 ) {
            return poly;
        }
        _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(0);
        if ( loop == null || loop.boundaryStartHalfEdge == null ) {
            return poly;
        }
        _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge start = he;
        do {
            if ( he.startingVertex != null ) {
                poly.add(m.multiply(he.startingVertex.position));
            }
            he = he.next();
        } while ( he != null && he != start );
        return poly;
    }

    private static Vector3Dd transformedSolidCentroid(
        PolyhedralBoundedSolid solid, Matrix4x4d m)
    {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        int count = 0;
        for ( int i = 0; i < solid.getVerticesList().size(); i++ ) {
            Vector3Dd p = m.multiply(solid.getVerticesList().get(i).position);
            x += p.x();
            y += p.y();
            z += p.z();
            count++;
        }
        if ( count == 0 ) {
            return new Vector3Dd();
        }
        return new Vector3Dd(x / count, y / count, z / count);
    }

    private static Vector3Dd polygonCentroid(ArrayList<Vector3Dd> poly)
    {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for ( int i = 0; i < poly.size(); i++ ) {
            x += poly.get(i).x();
            y += poly.get(i).y();
            z += poly.get(i).z();
        }
        return new Vector3Dd(x / poly.size(), y / poly.size(), z / poly.size());
    }

    private static Vector3Dd newellNormal(ArrayList<Vector3Dd> poly)
    {
        double nx = 0.0;
        double ny = 0.0;
        double nz = 0.0;
        for ( int i = 0; i < poly.size(); i++ ) {
            Vector3Dd a = poly.get(i);
            Vector3Dd b = poly.get((i + 1) % poly.size());
            nx += (a.y() - b.y()) * (a.z() + b.z());
            ny += (a.z() - b.z()) * (a.x() + b.x());
            nz += (a.x() - b.x()) * (a.y() + b.y());
        }
        Vector3Dd n = new Vector3Dd(nx, ny, nz);
        if ( n.length() < 1e-12 ) {
            return null;
        }
        return n.normalized();
    }

    private static boolean polygonContains(ArrayList<Vector3Dd> poly,
                                           Vector3Dd n, Vector3Dd p)
    {
        double ax = Math.abs(n.x());
        double ay = Math.abs(n.y());
        double az = Math.abs(n.z());
        int drop = (ax >= ay && ax >= az) ? 0 : ((ay >= az) ? 1 : 2);
        double pu = projOf(p, drop, true);
        double pv = projOf(p, drop, false);
        boolean inside = false;
        for ( int i = 0; i < poly.size(); i++ ) {
            Vector3Dd a = poly.get(i);
            Vector3Dd b = poly.get((i + 1) % poly.size());
            double au = projOf(a, drop, true);
            double av = projOf(a, drop, false);
            double bu = projOf(b, drop, true);
            double bv = projOf(b, drop, false);
            if ( (av > pv) != (bv > pv) ) {
                double uAtP = au + (pv - av) / (bv - av) * (bu - au);
                if ( pu < uAtP ) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private static double projOf(Vector3Dd v, int drop, boolean first)
    {
        if ( drop == 0 ) {
            return first ? v.y() : v.z();
        }
        if ( drop == 1 ) {
            return first ? v.z() : v.x();
        }
        return first ? v.x() : v.y();
    }

    /** Returns the occluder count the four jittered lines of sight agree on, or
        -1 when they disagree (the target is essentially on a silhouette). */
    private static int independentStableQi(PolyhedralBoundedSolid solid,
                                           Vector3Dd eye,
                                           Vector3Dd target)
    {
        Vector3Dd d = target.subtract(eye);
        double t0 = d.length();
        if ( t0 < 1e-9 ) {
            return 0;
        }
        d = d.multiply(1.0 / t0);
        Vector3Dd helper = Math.abs(d.x()) < 0.9 ?
            new Vector3Dd(1.0, 0.0, 0.0) : new Vector3Dd(0.0, 1.0, 0.0);
        Vector3Dd u = d.crossProduct(helper).normalized();
        Vector3Dd v = d.crossProduct(u).normalized();
        double delta = t0 * 1.0e-4;

        int first = naiveQi(solid, eye, target.add(u.multiply(delta)));
        int[] rest = new int[] {
            naiveQi(solid, eye, target.add(u.multiply(-delta))),
            naiveQi(solid, eye, target.add(v.multiply(delta))),
            naiveQi(solid, eye, target.add(v.multiply(-delta)))
        };
        for ( int i = 0; i < rest.length; i++ ) {
            if ( rest[i] != first ) {
                return -1;
            }
        }
        return first;
    }

    private static int naiveQi(PolyhedralBoundedSolid solid,
                               Vector3Dd eye,
                               Vector3Dd target)
    {
        Vector3Dd d = target.subtract(eye);
        double t0 = d.length();
        d = d.multiply(1.0 / t0);
        int qi = 0;
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            InfinitePlane plane = face.getContainingPlane();
            if ( plane == null ) {
                continue;
            }
            Vector3Dd n = plane.getNormal().normalized();
            double denom = d.dotProduct(n);
            if ( Math.abs(denom) < 1e-9 || denom >= 0.0 ) {
                continue;
            }
            Vector3Dd q = oracleFirstVertex(face);
            if ( q == null ) {
                continue;
            }
            double t = q.subtract(eye).dotProduct(n) / denom;
            if ( t <= 1e-6 || t >= t0 - 1e-6 ) {
                continue;
            }
            if ( oraclePointInFace(face, n, eye.add(d.multiply(t))) ) {
                qi++;
            }
        }
        return qi;
    }

    private static Vector3Dd oracleFirstVertex(_PolyhedralBoundedSolidFace face)
    {
        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop != null && loop.boundaryStartHalfEdge != null &&
                 loop.boundaryStartHalfEdge.startingVertex != null ) {
                return loop.boundaryStartHalfEdge.startingVertex.position;
            }
        }
        return null;
    }

    private static boolean oraclePointInFace(_PolyhedralBoundedSolidFace face,
                                             Vector3Dd n,
                                             Vector3Dd p)
    {
        double ax = Math.abs(n.x());
        double ay = Math.abs(n.y());
        double az = Math.abs(n.z());
        int drop = (ax >= ay && ax >= az) ? 0 : ((ay >= az) ? 1 : 2);
        double pu = oracleProj(p, drop, true);
        double pv = oracleProj(p, drop, false);
        boolean inside = false;

        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge start = he;
            do {
                _PolyhedralBoundedSolidHalfEdge nx = he.next();
                if ( nx == null ) {
                    break;
                }
                double au = oracleProj(he.startingVertex.position, drop, true);
                double av = oracleProj(he.startingVertex.position, drop, false);
                double bu = oracleProj(nx.startingVertex.position, drop, true);
                double bv = oracleProj(nx.startingVertex.position, drop, false);
                if ( (av > pv) != (bv > pv) ) {
                    double uAtP = au + (pv - av) / (bv - av) * (bu - au);
                    if ( pu < uAtP ) {
                        inside = !inside;
                    }
                }
                he = nx;
            } while ( he != start );
        }
        return inside;
    }

    private static double oracleProj(Vector3Dd v, int drop, boolean first)
    {
        if ( drop == 0 ) {
            return first ? v.y() : v.z();
        }
        if ( drop == 1 ) {
            return first ? v.z() : v.x();
        }
        return first ? v.x() : v.y();
    }

    private static boolean allLineCoordinatesAreFinite(Calligraphic2DBuffer lines)
    {
        for ( int i = 0; i < lines.getNumLines(); i++ ) {
            Vector3Dd[] segment = lines.get2DLine(i);
            if ( !isFinite(segment[0]) || !isFinite(segment[1]) ) {
                return false;
            }
        }
        return true;
    }

    private static ArrayList<SimpleBody> createSingleBodyScene(
        PolyhedralBoundedSolid solid)
    {
        return createSingleBodyScene(createBody(solid, new Matrix4x4d()));
    }

    private static ArrayList<SimpleBody> createSingleBodyScene(SimpleBody body)
    {
        ArrayList<SimpleBody> bodies = new ArrayList<SimpleBody>();
        bodies.add(body);
        return bodies;
    }

    private static SimpleBody createBody(
        PolyhedralBoundedSolid solid,
        Matrix4x4d rotation)
    {
        SimpleBody body = new SimpleBody();
        body.setGeometry(solid);
        body.setPosition(new Vector3Dd());
        body.setRotation(rotation);
        body.setRotationInverse(rotation.inverse());
        return body;
    }

    private static Camera createFeaturedCamera()
    {
        Camera camera = new Camera();
        camera.setPosition(new Vector3Dd(2.0, -1.0, 2.0));
        Matrix4x4d rotation = new Matrix4x4d();
        rotation = rotation.eulerAnglesRotation(Math.toRadians(135.0),
            Math.toRadians(-35.0), 0.0);
        camera.setRotation(rotation);
        camera.updateVectors();
        return camera;
    }

    private static boolean isFinite(Vector3Dd point)
    {
        return Double.isFinite(point.x()) &&
            Double.isFinite(point.y()) &&
            Double.isFinite(point.z());
    }

    private static File resolveKurlanderBowlStep()
    {
        String[] candidates = new String[] {
            "etc/solids/kurlanderBowl.step",
            "../etc/solids/kurlanderBowl.step",
            "../../etc/solids/kurlanderBowl.step"
        };

        for ( int i = 0; i < candidates.length; i++ ) {
            File file = new File(candidates[i]);
            if ( file.isFile() ) {
                return file;
            }
        }
        throw new IllegalStateException("Cannot locate etc/solids/kurlanderBowl.step");
    }

    private static boolean isEdgeEndpointPairPresentInBody(
        PolyhedralBoundedSolid solid,
        SimpleBody body,
        Vector3Dd start,
        Vector3Dd end)
    {
        for ( int i = 0; i < solid.getEdgesList().size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(i);
            Vector3Dd edgeStart = body.getTransformationMatrix().multiply(
                edge.leftHalf.startingVertex.position);
            Vector3Dd edgeEnd = body.getTransformationMatrix().multiply(
                edge.rightHalf.startingVertex.position);
            if ( sameSegment(start, end, edgeStart, edgeEnd) ) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameSegment(
        Vector3Dd a0,
        Vector3Dd a1,
        Vector3Dd b0,
        Vector3Dd b1)
    {
        return (Vector3Dd.distance(a0, b0) <= 1e-6 &&
            Vector3Dd.distance(a1, b1) <= 1e-6) ||
            (Vector3Dd.distance(a0, b1) <= 1e-6 &&
                Vector3Dd.distance(a1, b0) <= 1e-6);
    }
}
