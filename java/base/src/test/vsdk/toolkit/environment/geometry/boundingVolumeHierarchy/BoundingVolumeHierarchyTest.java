package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.Ray;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.GTestAssertions.expectDoubleEq;

/**
Port of `cpp/base/src/test/bounding_volume_hierarchy_tests.cpp`; each test
method is named `<cpp suite>_<cpp test>`.
*/
class BoundingVolumeHierarchyTest
{
    @Test
    void RaySlabReciprocals_DegenerateComponentsUseTheOriginTest()
    {
        RaySlabReciprocals r = new RaySlabReciprocals(new Vector3Dd(2.0, 5e-13, -4.0));
        expectDoubleEq(0.5, r.getInverseX());
        assertThat(r.isDegenerateX()).isFalse();
        assertThat(r.isDegenerateY()).isTrue();
        assertThat(r.getInverseY()).isEqualTo(0.0);
        expectDoubleEq(-0.25, r.getInverseZ());
        assertThat(r.isDegenerateZ()).isFalse();
        RaySlabReciprocals edge = new RaySlabReciprocals(new Vector3Dd(1e-12, -1e-12, 0.0));
        assertThat(edge.isDegenerateX()).isFalse();
        assertThat(edge.isDegenerateY()).isFalse();
        assertThat(edge.isDegenerateZ()).isTrue();
    }

    @Test
    void AxisAlignedBoundingBox_ForwardSlabTestAgreesWithCachedReciprocals()
    {
        AxisAlignedBoundingBox box = new AxisAlignedBoundingBox(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1));
        final Vector3Dd[] origins = {
            new Vector3Dd(-3, 0, 0), new Vector3Dd(3, 0, 0), new Vector3Dd(0, 0, 0),
            new Vector3Dd(-3, 2, 0), new Vector3Dd(-3, 1, 0), new Vector3Dd(0, 0, 5)};
        final Vector3Dd[] directions = {
            new Vector3Dd(1, 0, 0), new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0),
            new Vector3Dd(1, 0, 0), new Vector3Dd(1, 0, 0), new Vector3Dd(0.1, 0.1, -1)};
        final boolean[] expected = {true, false, true, false, true, true};
        for ( int i = 0; i < 6; i++ ) {
            Ray ray = new Ray(origins[i], directions[i]);
            assertThat(box.intersectsRayForward(ray)).as("" + i).isEqualTo(expected[i]);
            assertThat(box.intersectsRayForward(ray, new RaySlabReciprocals(directions[i])))
                .as("" + i).isEqualTo(expected[i]);
        }
    }

    @Test
    void AxisAlignedBoundingBox_BoundedSlabTestStopsAtMaximumParameter()
    {
        AxisAlignedBoundingBox box = new AxisAlignedBoundingBox(new Vector3Dd(2, -1, -1), new Vector3Dd(4, 1, 1));
        Ray ray = new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(1, 0, 0));
        assertThat(box.intersectsRayBefore(ray, 10.0)).isTrue();
        assertThat(box.intersectsRayBefore(ray, 2.0)).isTrue();
        assertThat(box.intersectsRayBefore(ray, 1.5)).isFalse();
        Ray backwards = new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(-1, 0, 0));
        assertThat(box.intersectsRayBefore(backwards, 10.0)).isFalse();
        assertThat(box.intersectsRayForward(backwards)).isFalse();
    }

    @Test
    void AxisAlignedBoundingBox_CombinationsAndUnboundedState()
    {
        AxisAlignedBoundingBox a = new AxisAlignedBoundingBox(new Vector3Dd(0, 0, 0), new Vector3Dd(2, 2, 2));
        AxisAlignedBoundingBox b = new AxisAlignedBoundingBox(new Vector3Dd(1, -1, 1), new Vector3Dd(3, 1, 4));
        AxisAlignedBoundingBox u = a.enclosing(b);
        assertThat(u.getMin()).isEqualTo(new Vector3Dd(0, -1, 0));
        assertThat(u.getMax()).isEqualTo(new Vector3Dd(3, 2, 4));
        AxisAlignedBoundingBox n = a.intersection(b);
        assertThat(n.getMin()).isEqualTo(new Vector3Dd(1, 0, 1));
        assertThat(n.getMax()).isEqualTo(new Vector3Dd(2, 1, 2));
        assertThat(a.centroid()).isEqualTo(new Vector3Dd(1, 1, 1));
        expectDoubleEq(5.0, AxisAlignedBoundingBox.empty().expandedBy(new Vector3Dd(5, 6, 7)).getMin().x());
        expectDoubleEq(2.0, b.extent(0));
        expectDoubleEq(3.0, b.extent(2));
        assertThat(AxisAlignedBoundingBox.unbounded().isUnbounded()).isTrue();
        assertThat(a.isUnbounded()).isFalse();
        assertThat(a.containsPointWithTolerance(new Vector3Dd(2.05, 1, 1), 0.1)).isTrue();
        assertThat(a.containsPointWithTolerance(new Vector3Dd(2.2, 1, 1), 0.1)).isFalse();
    }

    @Test
    void AxisAlignedBoundingBox_TransformedCornersEncloseThePlacedBox()
    {
        Matrix4x4d translation = new Matrix4x4d();
        translation = translation.translation(10, 0, 0);
        AxisAlignedBoundingBox placed = AxisAlignedBoundingBox.fromTransformedCorners(
            new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1), translation);
        AxisAlignedBoundingBox same = AxisAlignedBoundingBox.fromTransformedCorners(
            new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1), null);
        assertThat(same.getMin()).isEqualTo(new Vector3Dd(-1, -1, -1));
        assertThat(placed.isUnbounded()).isFalse();
        assertThat(placed.containsPointWithTolerance(placed.centroid(), 0.0)).isTrue();
        expectDoubleEq(2.0, placed.extent(0));
        expectDoubleEq(9.0, placed.getMin().x());
        expectDoubleEq(11.0, placed.getMax().x());
    }

    @Test
    void AxisAlignedBoundingBox_FromGeometryBoundsKeepsOnlyFiniteBounds()
    {
        GeometryBounds finite = new GeometryBounds();
        assertThat(finite.setFinite(new Vector3Dd(-1, -2, -3), new Vector3Dd(4, 5, 6))).isTrue();
        AxisAlignedBoundingBox box = AxisAlignedBoundingBox.fromGeometryBounds(finite);
        assertThat(box.getMin()).isEqualTo(new Vector3Dd(-1, -2, -3));
        assertThat(box.getMax()).isEqualTo(new Vector3Dd(4, 5, 6));
        GeometryBounds other = new GeometryBounds();
        other.setUnbounded();
        assertThat(AxisAlignedBoundingBox.fromGeometryBounds(other).isUnbounded()).isTrue();
        other.setEmpty();
        assertThat(AxisAlignedBoundingBox.fromGeometryBounds(other).isUnbounded()).isTrue();
        other.setIndeterminate();
        assertThat(AxisAlignedBoundingBox.fromGeometryBounds(other).isUnbounded()).isTrue();
    }

    @Test
    void AabbBoundingVolume_FiltersAreConservativeAndCopiesAreIndependent()
    {
        AabbBoundingVolume volume = new AabbBoundingVolume(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1));
        final PreRayHitElement filter = volume;
        Ray hit = new Ray(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0));
        Ray miss = new Ray(new Vector3Dd(-5, 3, 0), new Vector3Dd(1, 0, 0));
        assertThat(filter.mayIntersect(hit)).isTrue();
        assertThat(filter.mayIntersect(miss)).isFalse();
        assertThat(filter.mayIntersectBefore(hit, 4.0)).isTrue();
        assertThat(filter.mayIntersectBefore(hit, 3.0)).isFalse();
        assertThat(volume.containsPoint(new Vector3Dd(0, 0, 0), 0.0)).isTrue();
        BoundingVolumeHierarchy copy = volume.copy();
        assertThat(copy.axisAlignedExtent().getMax()).isEqualTo(new Vector3Dd(1, 1, 1));
        assertThat(copy.isUnbounded()).isFalse();
        assertThat(new AabbBoundingVolume().isUnbounded()).isTrue();
    }

    private static void addUnitBox(ArrayList<AxisAlignedBoundingBox> boxes,
        ArrayList<Integer> ids, double x, int id)
    {
        boxes.add(new AxisAlignedBoundingBox(new Vector3Dd(x, -0.5, -0.5), new Vector3Dd(x + 1, 0.5, 0.5)));
        ids.add(id);
    }

    @Test
    void AabbBinHierarchy_BinsFollowCentroidOrderAlongTheWidestAxis()
    {
        ArrayList<AxisAlignedBoundingBox> boxes = new ArrayList<>();
        ArrayList<Integer> ids = new ArrayList<>();
        // Twenty boxes on the x axis given in reverse order, ids 100..119.
        for ( int i = 0; i < 20; i++ ) addUnitBox(boxes, ids, 3.0 * (19 - i), 100 + i);
        AabbBinHierarchy hierarchy = new AabbBinHierarchy();
        hierarchy.build(boxes, ids);
        assertThat(hierarchy.getBinCount()).isEqualTo(4);
        assertThat(hierarchy.getMemberCount()).isEqualTo(20);
        expectDoubleEq(0.0, hierarchy.getBinBounds(0).getMin().x());
        expectDoubleEq(13.0, hierarchy.getBinBounds(0).getMax().x());
        expectDoubleEq(57.0 + 1.0, hierarchy.getBinBounds(3).getMax().x());
    }

    @Test
    void AabbBinHierarchy_QueriesListEveryCrossedPrimitiveAndNoOther()
    {
        ArrayList<AxisAlignedBoundingBox> boxes = new ArrayList<>();
        ArrayList<Integer> ids = new ArrayList<>();
        for ( int i = 0; i < 20; i++ ) addUnitBox(boxes, ids, 3.0 * i, i);
        AabbBinHierarchy hierarchy = new AabbBinHierarchy();
        hierarchy.build(boxes, ids);
        int[] out = new int[32];
        // A vertical ray through the box at x in [9, 10] crosses only id 3.
        Ray vertical = new Ray(new Vector3Dd(9.5, 5, 0), new Vector3Dd(0, -1, 0));
        assertThat(hierarchy.gatherCandidates(vertical,
            new RaySlabReciprocals(vertical.getDirection()), out, 32)).isEqualTo(1);
        assertThat(out[0]).isEqualTo(3);
        // A ray along the row crosses all of them, in bin and member order.
        Ray along = new Ray(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0));
        assertThat(hierarchy.gatherCandidates(along,
            new RaySlabReciprocals(along.getDirection()), out, 32)).isEqualTo(20);
        for ( int i = 0; i < 20; i++ ) assertThat(out[i]).isEqualTo(i);
        assertThat(hierarchy.gatherCandidates(along,
            new RaySlabReciprocals(along.getDirection()), out, 5)).isEqualTo(-1);
        Ray away = new Ray(new Vector3Dd(-5, 0, 0), new Vector3Dd(-1, 0, 0));
        assertThat(hierarchy.gatherCandidates(away,
            new RaySlabReciprocals(away.getDirection()), out, 32)).isEqualTo(0);
        AabbBinHierarchy empty = new AabbBinHierarchy();
        empty.build(new ArrayList<AxisAlignedBoundingBox>(), new ArrayList<Integer>());
        assertThat(empty.getBinCount()).isEqualTo(0);
        assertThat(empty.gatherCandidates(along, new RaySlabReciprocals(along.getDirection()), out, 32)).isEqualTo(0);
    }
}
