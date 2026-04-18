package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.processing.SimpleTestGeometryLibrary;

import static org.assertj.core.api.Assertions.assertThat;

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

        setNumericContextMethod = PolyhedralBoundedSolidOperator.class
            .getDeclaredMethod("setNumericContext",
                PolyhedralBoundedSolidNumericPolicy.ToleranceContext.class);
        setNumericContextMethod.setAccessible(true);
    }

    @Test
    void given_coplanarSectors_when_angularIntervalsOverlap_then_sectoroverlapReturnsTrue()
        throws Exception
    {
        PolyhedralBoundedSolid solid = PolyhedralBoundedSolidTestFixtures
            .createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidHalfEdge he = findFaceWithNormal(solid,
            new Vector3D(0.0, 0.0, 1.0)).boundariesList.get(0)
            .boundaryStartHalfEdge;

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid));

        boolean relation = invokeSectorOverlap(
            createSector(he, 0.0, 60.0),
            createSector(he, 30.0, 90.0));

        assertThat(relation).isTrue();
    }

    @Test
    void given_coplanarNeighborSectors_when_theyOnlyShareBoundaryRay_then_sectoroverlapReturnsFalse()
        throws Exception
    {
        PolyhedralBoundedSolid solid = PolyhedralBoundedSolidTestFixtures
            .createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidHalfEdge he = findFaceWithNormal(solid,
            new Vector3D(0.0, 0.0, 1.0)).boundariesList.get(0)
            .boundaryStartHalfEdge;

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid));

        boolean relation = invokeSectorOverlap(
            createSector(he, 0.0, 60.0),
            createSector(he, 60.0, 120.0));

        assertThat(relation).isFalse();
    }

    @Test
    void given_coplanarDisjointSectorsOnSameAngularSide_when_intervalsDoNotIntersect_then_sectoroverlapReturnsFalse()
        throws Exception
    {
        PolyhedralBoundedSolid solid = PolyhedralBoundedSolidTestFixtures
            .createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidHalfEdge he = findFaceWithNormal(solid,
            new Vector3D(0.0, 0.0, 1.0)).boundariesList.get(0)
            .boundaryStartHalfEdge;

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid));

        boolean relation = invokeSectorOverlap(
            createSector(he, -60.0, 0.0),
            createSector(he, -120.0, -80.0));

        assertThat(relation).isFalse();
    }

    @Test
    void given_coincidentCoplanarFaces_when_classifyingLocalSectorAgainstReferenceFace_then_relationIsOverlap()
        throws Exception
    {
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

        sectorInfo.sector = faceA.boundariesList.get(0).boundaryStartHalfEdge;
        int relation = ((Integer)classifyCoplanarSectorRelationMethod.invoke(
            null, sectorInfo, faceB)).intValue();

        assertThat(relation).isEqualTo(
            _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_OVERLAP);
    }

    @Test
    void given_touchingBoxes_when_runningTouchingOnlyPreflight_then_itRecognizesNoVolumetricIntersection()
        throws Exception
    {
        PolyhedralBoundedSolid[] pair =
            PolyhedralBoundedSolidTestFixtures.createTouchingBoxPair();

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolids(pair[0], pair[1]));

        boolean touchingOnly =
            ((Boolean)touchingOnlyPreflightCaseMethod.invoke(null, pair[0],
                pair[1])).booleanValue();

        assertThat(touchingOnly).isTrue();
    }

    @Test
    void given_knownIntersectingMant1986Case_when_runningTouchingOnlyPreflight_then_itDoesNotDowngradeToTouching()
        throws Exception
    {
        PolyhedralBoundedSolid[] pair =
            SimpleTestGeometryLibrary.createTestObjectPairMANT1986_2();

        setNumericContextMethod.invoke(null,
            PolyhedralBoundedSolidNumericPolicy.forSolids(pair[0], pair[1]));

        boolean touchingOnly =
            ((Boolean)touchingOnlyPreflightCaseMethod.invoke(null, pair[0],
                pair[1])).booleanValue();

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

    private static _PolyhedralBoundedSolidFace findFaceWithNormal(
        PolyhedralBoundedSolid solid,
        Vector3D normalHint)
    {
        int i;
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            if ( face.containingPlane != null &&
                 face.containingPlane.getNormal().dotProduct(normalHint) > 0.9 ) {
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
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            if ( face.containingPlane == null ) {
                continue;
            }
            if ( Math.abs(face.containingPlane.pointDistance(pointOnPlane)) <=
                 1.0e-6 &&
                 face.containingPlane.getNormal().dotProduct(normalHint) > 0.9 ) {
                return face;
            }
        }
        return null;
    }
}
