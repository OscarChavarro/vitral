package vsdk.toolkit.environment.geometry.surface;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.RayCrossingTestCollector;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayBoundary;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayCrossingSupport;
import vsdk.toolkit.environment.geometry.element.RayFirstHitStatus;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.Triangle;
import vsdk.toolkit.environment.material.SimpleMaterial;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.GTestAssertions.expectDoubleEq;

/**
Port of `cpp/base/src/test/triangle_mesh_contract_tests.cpp`; each test method
is named `<cpp suite>_<cpp test>`.
*/
class TriangleMeshContractTest
{
    @Test
    void TriangleMeshContract_OrderedCrossingsCarryFaceAndFirstHitDetails()
    {
        TriangleMesh mesh = new TriangleMesh();
        mesh.initVertexPositionsArray(6);
        double[] positions = mesh.getVertexPositions();
        final double[] values = {0,0,0, 1,0,0, 0,1,0, 0,0,2, 1,0,2, 0,1,2};
        for ( int i = 0; i < 18; ++i ) positions[i] = values[i];
        mesh.initTriangleArrays(2);
        mesh.setTriangleAt(0, new Triangle(0,1,2));
        mesh.setTriangleAt(1, new Triangle(3,4,5));
        mesh.initVertexUvsArray();
        double[] uv = mesh.getVertexUvs();
        uv[6]=0; uv[7]=0; uv[8]=1; uv[9]=0; uv[10]=0; uv[11]=1;
        final Ray ray = Ray.fromAffine(new Vector3Dd(0.25,0.25,4), new Vector3Dd(0,0,-1));
        final RayCrossingQuery query = new RayCrossingQuery(2,4);
        RayCrossingTestCollector events = new RayCrossingTestCollector();
        assertThat(mesh.emitRayCrossings(ray, query, events)).isEqualTo(RayCrossingSupport.Supported);
        assertThat(events.roots).hasSize(2);
        expectDoubleEq(2, events.roots.get(0));
        expectDoubleEq(4, events.roots.get(1));
        assertThat(events.surfaceIndices.get(0)).isEqualTo(1);
        assertThat(events.surfaceIndices.get(1)).isEqualTo(0);
        assertThat(events.geometries.get(0)).isSameAs(mesh);
        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL | RayHit.DETAIL_UV, false);
        RayBoundary boundary = new RayBoundary();
        assertThat(mesh.queryFirstHit(ray, query, hit, boundary)).isEqualTo(RayFirstHitStatus.Hit);
        expectDoubleEq(2, hit.getHitDistance());
        expectDoubleEq(2, hit.point.z());
        expectDoubleEq(1, hit.normal.z());
        expectDoubleEq(0.25, hit.u);
        expectDoubleEq(0.25, hit.v);
        assertThat(boundary.surfaceIndex).isEqualTo(1);
        assertThat(boundary.geometry).isSameAs(mesh);
        assertThat(hit.getAvailableDetailMask())
            .isEqualTo(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL | RayHit.DETAIL_UV);
        RayBoundary copied = new RayBoundary(boundary);
        assertThat(copied.surfaceIndex).isEqualTo(1);
        RayBoundary assigned = new RayBoundary();
        assigned.assign(copied);
        assertThat(assigned.surfaceIndex).isEqualTo(1);
        RayHit deferred = new RayHit(RayHit.DETAIL_NORMAL | RayHit.DETAIL_UV, false);
        assertThat(mesh.querySurfaceDetail(ray, boundary, deferred)).isEqualTo(RayFirstHitStatus.Hit);
        expectDoubleEq(1, deferred.normal.z());
        expectDoubleEq(0.25, deferred.u);
        expectDoubleEq(0.25, deferred.v);
        boundary.normalReversed = true;
        assertThat(mesh.querySurfaceDetail(ray, boundary, deferred)).isEqualTo(RayFirstHitStatus.Hit);
        expectDoubleEq(-1, deferred.normal.z());
        boundary.surfaceIndex = 99;
        assertThat(mesh.querySurfaceDetail(ray, boundary, deferred)).isEqualTo(RayFirstHitStatus.InvalidInput);
        assertThat(deferred.hasHitDistance()).isFalse();
        assertThat(mesh.queryFirstHit(ray, new RayCrossingQuery(2.1,3.9), hit, boundary)).isEqualTo(RayFirstHitStatus.Miss);
        assertThat(hit.hasHitDistance()).isFalse();
        assertThat(boundary.surfaceIndex).isEqualTo(-1);
    }

    @Test
    void TriangleMeshContract_CoplanarityInvalidMeshAndEmptyAreExplicit()
    {
        TriangleMesh mesh = new TriangleMesh();
        mesh.initVertexPositionsArray(6);
        double[] positions = mesh.getVertexPositions();
        final double[] values = {0,0,0, 1,0,0, 0,1,0, 0.25,0,0, 0.25,1,0, 0.25,0,1};
        for ( int i = 0; i < 18; ++i ) positions[i] = values[i];
        mesh.initTriangleArrays(2);
        mesh.setTriangleAt(0, new Triangle(0,1,2));
        mesh.setTriangleAt(1, new Triangle(3,4,5));
        final Ray ray = Ray.fromAffine(new Vector3Dd(0.25,0.25,2), new Vector3Dd(0,0,-1));
        RayCrossingTestCollector events = new RayCrossingTestCollector();
        assertThat(mesh.emitRayCrossings(ray, new RayCrossingQuery(0,4), events)).isEqualTo(RayCrossingSupport.Indeterminate);
        assertThat(events.roots).isEmpty();
        assertThat(mesh.queryFirstHit(ray, new RayCrossingQuery(0,4), null)).isEqualTo(RayFirstHitStatus.Indeterminate);
        mesh.getTriangleIndexes()[4] = 999;
        assertThat(mesh.emitRayCrossings(ray, new RayCrossingQuery(0,4), events)).isEqualTo(RayCrossingSupport.Indeterminate);
        GeometryBounds bounds = new GeometryBounds();
        mesh.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Indeterminate);
        TriangleMesh empty = new TriangleMesh();
        assertThat(empty.emitRayCrossings(ray, new RayCrossingQuery(0,4), events)).isEqualTo(RayCrossingSupport.Supported);
        assertThat(empty.queryFirstHit(
            Ray.fromAffine(new Vector3Dd(), new Vector3Dd()), new RayCrossingQuery(0,4), null))
            .isEqualTo(RayFirstHitStatus.InvalidInput);
        assertThat(empty.queryFirstHit(ray, new RayCrossingQuery(0,4,0.01), null)).isEqualTo(RayFirstHitStatus.Unsupported);
    }

    /**
    Java has no material ownership flag (`setOwnsMaterials`): the garbage
    collector owns the materials. An absent material table (null) is the
    Java form of the cpp empty list.
    */
    @Test
    void TriangleMeshContract_MathematicalCopyDoesNotBorrowOwnedAppearance()
    {
        TriangleMesh copied = null;
        {
            TriangleMesh original = new TriangleMesh();
            original.initVertexPositionsArray(3);
            double[] positions = original.getVertexPositions();
            final double[] values = {0,0,0, 1,0,0, 0,1,0};
            for ( int i = 0; i < 9; ++i ) positions[i] = values[i];
            original.initTriangleArrays(1);
            original.setTriangleAt(0, new Triangle(0,1,2));
            SimpleMaterial[] materials = {new SimpleMaterial()};
            original.setMaterials(materials);
            copied = (TriangleMesh)original.copyGeometry();
            assertThat(copied).isNotNull();
            assertThat(copied.getMaterials() == null || copied.getMaterials().length == 0).isTrue();
        }
        RayBoundary boundary = new RayBoundary();
        assertThat(copied.queryFirstHit(
            Ray.fromAffine(new Vector3Dd(0.25,0.25,1), new Vector3Dd(0,0,-1)),
            new RayCrossingQuery(0,2), null, boundary)).isEqualTo(RayFirstHitStatus.Hit);
        assertThat(boundary.geometry).isSameAs(copied);
    }
}
