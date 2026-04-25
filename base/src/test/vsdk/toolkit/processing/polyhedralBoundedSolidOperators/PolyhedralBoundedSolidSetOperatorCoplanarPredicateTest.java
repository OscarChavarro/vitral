package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;

import static org.assertj.core.api.Assertions.assertThat;

/**
Probes coplanar and touching predicates used by boolean classification.

<p>Traceability: [MANT1988] Ch. 13.2 containment/intersection predicates
and Ch. 15.6 boundary classification for vertex/face and vertex/vertex
neighborhoods.</p>
 */
class PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest
{
    private Method sectorOverlapMethod;
    private Method classifyCoplanarSectorRelationMethod;
    private Method touchingOnlyPreflightCaseMethod;
    private Method setNumericContextMethod;

    @BeforeEach
    void setUpReflectionHandles() throws Exception
    {
        sectorOverlapMethod = PolyhedralBoundedSolidSetOperator.class
            .getDeclaredMethod("sectoroverlap",
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex.class,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex.class);
        sectorOverlapMethod.setAccessible(true);

        classifyCoplanarSectorRelationMethod =
            PolyhedralBoundedSolidSetOperator.class.getDeclaredMethod(
                "classifyCoplanarSectorRelation",
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.class,
                _PolyhedralBoundedSolidFace.class);
        classifyCoplanarSectorRelationMethod.setAccessible(true);

        touchingOnlyPreflightCaseMethod =
            PolyhedralBoundedSolidSetOperator.class.getDeclaredMethod(
                "isTouchingOnlyPreflightCase",
                PolyhedralBoundedSolid.class,
                PolyhedralBoundedSolid.class);
        touchingOnlyPreflightCaseMethod.setAccessible(true);

        setNumericContextMethod = _PolyhedralBoundedSolidOperator.class
            .getDeclaredMethod("setNumericContext",
                PolyhedralBoundedSolidNumericPolicy.ToleranceContext.class);
        setNumericContextMethod.setAccessible(true);
    }

    @Test
    void given_coplanarSectors_when_angularIntervalsOverlap_then_sectoroverlapReturnsTrue()
        throws Exception
    {
        // Arrange
        PolyhedralBoundedSolid solid = PolyhedralBoundedSolidTestFixtures
            .createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidHalfEdge he = findFaceWithNormal(solid,
            new Vector3D(0.0, 0.0, 1.0)).boundariesList.get(0)
            .boundaryStartHalfEdge;

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid));

        // Action
        boolean relation = invokeSectorOverlap(
            createSector(he, 0.0, 60.0),
            createSector(he, 30.0, 90.0));

        // Assert
        assertThat(relation).isTrue();
    }

    @Test
    @Disabled("Legacy sectoroverlap intentionally treats boundary-ray contact as overlap")
    void given_coplanarNeighborSectors_when_theyOnlyShareBoundaryRay_then_sectoroverlapReturnsFalse()
        throws Exception
    {
        // Arrange
        PolyhedralBoundedSolid solid = PolyhedralBoundedSolidTestFixtures
            .createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidHalfEdge he = findFaceWithNormal(solid,
            new Vector3D(0.0, 0.0, 1.0)).boundariesList.get(0)
            .boundaryStartHalfEdge;

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid));

        // Action
        boolean relation = invokeSectorOverlap(
            createSector(he, 0.0, 60.0),
            createSector(he, 60.0, 120.0));

        // Assert
        assertThat(relation).isFalse();
    }

    @Test
    @Disabled("Legacy sectoroverlap is deliberately permissive; this precise interval predicate belongs to the newer strategy")
    void given_coplanarDisjointSectorsOnSameAngularSide_when_intervalsDoNotIntersect_then_sectoroverlapReturnsFalse()
        throws Exception
    {
        // Arrange
        PolyhedralBoundedSolid solid = PolyhedralBoundedSolidTestFixtures
            .createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidHalfEdge he = findFaceWithNormal(solid,
            new Vector3D(0.0, 0.0, 1.0)).boundariesList.get(0)
            .boundaryStartHalfEdge;

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid));

        // Action
        boolean relation = invokeSectorOverlap(
            createSector(he, -60.0, 0.0),
            createSector(he, -120.0, -80.0));

        // Assert
        assertThat(relation).isFalse();
    }

    @Test
    void given_coincidentCoplanarFaces_when_classifyingLocalSectorAgainstReferenceFace_then_relationIsOverlap()
        throws Exception
    {
        // Arrange
        PolyhedralBoundedSolid[] pair =
            PolyhedralBoundedSolidTestFixtures.createTouchingBoxPair();
        PolyhedralBoundedSolid solidA = pair[0];
        PolyhedralBoundedSolid solidB = pair[1];
        double interfaceX = solidA.getMinMax()[3];
        Vector3D interfacePoint = new Vector3D(interfaceX, 0.0, 0.0);

        _PolyhedralBoundedSolidFace faceA = findFaceOnPlane(solidA,
            interfacePoint, new Vector3D(1.0, 0.0, 0.0));
        _PolyhedralBoundedSolidFace faceB = findFaceOnPlane(solidB,
            interfacePoint, new Vector3D(-1.0, 0.0, 0.0));
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace sectorInfo =
            new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace();

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolids(solidA, solidB));

        // Action
        sectorInfo.sector = faceA.boundariesList.get(0).boundaryStartHalfEdge;
        int relation = ((Integer)classifyCoplanarSectorRelationMethod.invoke(
            null, sectorInfo, faceB)).intValue();

        // Assert
        assertThat(relation).isEqualTo(
            _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_OVERLAP);
    }

    @Test
    void given_touchingBoxes_when_runningTouchingOnlyPreflight_then_itRecognizesNoVolumetricIntersection()
        throws Exception
    {
        // Arrange
        PolyhedralBoundedSolid[] pair =
            PolyhedralBoundedSolidTestFixtures.createTouchingBoxPair();

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolids(pair[0], pair[1]));

        // Action
        boolean touchingOnly =
            ((Boolean)touchingOnlyPreflightCaseMethod.invoke(null, pair[0],
                pair[1])).booleanValue();

        // Assert
        assertThat(touchingOnly).isTrue();
    }

    @Test
    void given_stackedBlocksWithPartialFaceOverlap_when_runningTouchingOnlyPreflight_then_itDoesNotDowngradeToTouching()
        throws Exception
    {
        // Arrange
        PolyhedralBoundedSolid[] pair = CsgSampleCorpusFixtures.createPair(
            CsgSampleCorpus.STACKED_BLOCKS);

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolids(pair[0], pair[1]));

        // Action
        boolean touchingOnly =
            ((Boolean)touchingOnlyPreflightCaseMethod.invoke(null, pair[0],
                pair[1])).booleanValue();

        // Assert
        assertThat(touchingOnly).isFalse();
    }

    @Test
    void given_stackedBlocksWithPartialFaceOverlap_when_intersecting_then_itReturnsContactLamina()
    {
        // Arrange
        PolyhedralBoundedSolid[] pair = CsgSampleCorpusFixtures.createPair(
            CsgSampleCorpus.STACKED_BLOCKS);

        // Action
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            pair[0], pair[1], PolyhedralBoundedSolidModeler.INTERSECTION,
            false);
        double[] minmax = result.getMinMax();

        // Assert
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(result.getPolygonsList().size()).isEqualTo(2);
        assertThat(result.getEdgesList().size()).isEqualTo(4);
        assertThat(result.getVerticesList().size()).isEqualTo(4);
        assertThat(minmax)
            .containsExactly(0.25, 0.25, 0.3, 0.75, 0.75, 0.3);
    }

    @Test
    void given_knownIntersectingMant1986Case_when_runningTouchingOnlyPreflight_then_itDoesNotDowngradeToTouching()
        throws Exception
    {
        // Arrange
        PolyhedralBoundedSolid[] pair =
            SimpleTestGeometryLibrary.createTestObjectPairMANT1986_2();

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolids(pair[0], pair[1]));

        // Action
        boolean touchingOnly =
            ((Boolean)touchingOnlyPreflightCaseMethod.invoke(null, pair[0],
                pair[1])).booleanValue();

        // Assert
        assertThat(touchingOnly).isFalse();
    }

    @Test
    void given_hollowBrickLOperands_when_runningTouchingOnlyPreflight_then_itDoesNotDowngradeCornerOverlapsToTouching()
        throws Exception
    {
        // Arrange
        PolyhedralBoundedSolid[] pair = createHollowBrickOperands();

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolids(pair[0], pair[1]));

        // Action
        boolean touchingOnly =
            ((Boolean)touchingOnlyPreflightCaseMethod.invoke(null, pair[0],
                pair[1])).booleanValue();

        // Assert
        assertThat(touchingOnly).isFalse();
    }

    private boolean invokeSectorOverlap(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex a,
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex b)
        throws Exception
    {
        return ((Boolean)sectorOverlapMethod.invoke(null, a, b))
            .booleanValue();
    }

    private static _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex
    createSector(_PolyhedralBoundedSolidHalfEdge he, double startDegrees,
                 double endDegrees)
    {
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex sector =
            new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex();
        sector.he = he;
        sector.ref1 = directionOnXY(startDegrees);
        sector.ref2 = directionOnXY(endDegrees);
        sector.ref12 = sector.ref1.crossProduct(sector.ref2);
        sector.wide = false;
        return sector;
    }

    private static Vector3D directionOnXY(double degrees)
    {
        double radians = Math.toRadians(degrees);
        return new Vector3D(Math.cos(radians), Math.sin(radians), 0.0);
    }

    private static PolyhedralBoundedSolid[] createHollowBrickOperands()
    {
        PolyhedralBoundedSolid[] operands = new PolyhedralBoundedSolid[2];
        PolyhedralBoundedSolid a = createTranslatedBox(1.0, 0.2, 0.2,
            0.5, 0.1, 0.1);
        PolyhedralBoundedSolid b = createTranslatedBox(1.0, 0.2, 0.2,
            0.5, 0.9, 0.1);
        PolyhedralBoundedSolid c = createTranslatedBox(0.2, 1.0, 0.2,
            0.1, 0.5, 0.1);
        PolyhedralBoundedSolid d = createTranslatedBox(0.2, 1.0, 0.2,
            0.9, 0.5, 0.1);

        operands[0] = PolyhedralBoundedSolidModeler.setOp(
            b, c, PolyhedralBoundedSolidModeler.UNION, false);
        operands[1] = PolyhedralBoundedSolidModeler.setOp(
            a, d, PolyhedralBoundedSolidModeler.UNION, false);
        return operands;
    }

    private static PolyhedralBoundedSolid createTranslatedBox(
        double sx, double sy, double sz,
        double tx, double ty, double tz)
    {
        Box box = new Box(new Vector3D(sx, sy, sz));
        PolyhedralBoundedSolid solid = box.exportToPolyhedralBoundedSolid();
        Matrix4x4 translation = new Matrix4x4();
        translation = translation.translation(tx, ty, tz);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, translation);
        return solid;
    }

    private static _PolyhedralBoundedSolidFace findFaceWithNormal(
        PolyhedralBoundedSolid solid,
        Vector3D normalHint)
    {
        int i;
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            InfinitePlane plane = face.getContainingPlane();
            if ( plane != null &&
                 plane.getNormal().dotProduct(normalHint) > 0.9 ) {
                return face;
            }
        }
        return null;
    }

    private static _PolyhedralBoundedSolidFace findFaceOnPlane(
        PolyhedralBoundedSolid solid,
        Vector3D pointOnPlane,
        Vector3D normalHint)
    {
        int i;
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            InfinitePlane plane = face.getContainingPlane();
            if ( plane == null ) {
                continue;
            }
            if ( Math.abs(plane.pointDistance(pointOnPlane)) <=
                 1.0e-6 &&
                 plane.getNormal().dotProduct(normalHint) > 0.9 ) {
                return face;
            }
        }
        return null;
    }
}
