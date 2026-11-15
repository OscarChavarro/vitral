// Port of cpp/base/src/test/bounding_volume_hierarchy_tests.cpp
import { describe, expect, it } from "vitest";
import { ArrayList } from "java/util/ArrayList.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { AabbBinHierarchy } from "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/AabbBinHierarchy.js";
import { AabbBoundingVolume } from "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/AabbBoundingVolume.js";
import { AxisAlignedBoundingBox } from "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/AxisAlignedBoundingBox.js";
import type { PreRayHitElement } from "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/PreRayHitElement.js";
import { RaySlabReciprocals } from "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/RaySlabReciprocals.js";
import { GeometryBounds } from "vsdk/toolkit/environment/geometry/element/GeometryBounds.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";

const expectVectorEq = (expected: Vector3Dd, actual: Vector3Dd): void => {
    expect([actual.x(), actual.y(), actual.z()]).toEqual([expected.x(), expected.y(), expected.z()]);
};

describe("RaySlabReciprocals", () => {
    it("DegenerateComponentsUseTheOriginTest", () => {
        const r = new RaySlabReciprocals(new Vector3Dd(2.0, 5e-13, -4.0));
        expect(r.getInverseX()).toBe(0.5);
        expect(r.isDegenerateX()).toBe(false);
        expect(r.isDegenerateY()).toBe(true);
        expect(r.getInverseY()).toBe(0.0);
        expect(r.getInverseZ()).toBe(-0.25);
        expect(r.isDegenerateZ()).toBe(false);
        const edge = new RaySlabReciprocals(new Vector3Dd(1e-12, -1e-12, 0.0));
        expect(edge.isDegenerateX()).toBe(false);
        expect(edge.isDegenerateY()).toBe(false);
        expect(edge.isDegenerateZ()).toBe(true);
    });
});

describe("AxisAlignedBoundingBox", () => {
    it("ForwardSlabTestAgreesWithCachedReciprocals", () => {
        const box = new AxisAlignedBoundingBox(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1));
        const origins = [
            new Vector3Dd(-3, 0, 0),
            new Vector3Dd(3, 0, 0),
            new Vector3Dd(0, 0, 0),
            new Vector3Dd(-3, 2, 0),
            new Vector3Dd(-3, 1, 0),
            new Vector3Dd(0, 0, 5),
        ];
        const directions = [
            new Vector3Dd(1, 0, 0),
            new Vector3Dd(1, 0, 0),
            new Vector3Dd(0, 1, 0),
            new Vector3Dd(1, 0, 0),
            new Vector3Dd(1, 0, 0),
            new Vector3Dd(0.1, 0.1, -1),
        ];
        const expected = [true, false, true, false, true, true];
        for (let i = 0; i < 6; i++) {
            const ray = new Ray(origins[i]!, directions[i]!);
            expect(box.intersectsRayForward(ray), String(i)).toBe(expected[i]);
            expect(box.intersectsRayForward(ray, new RaySlabReciprocals(directions[i]!)), String(i)).toBe(expected[i]);
        }
    });

    it("BoundedSlabTestStopsAtMaximumParameter", () => {
        const box = new AxisAlignedBoundingBox(new Vector3Dd(2, -1, -1), new Vector3Dd(4, 1, 1));
        const ray = new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(1, 0, 0));
        expect(box.intersectsRayBefore(ray, 10.0)).toBe(true);
        expect(box.intersectsRayBefore(ray, 2.0)).toBe(true);
        expect(box.intersectsRayBefore(ray, 1.5)).toBe(false);
        const backwards = new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(-1, 0, 0));
        expect(box.intersectsRayBefore(backwards, 10.0)).toBe(false);
        expect(box.intersectsRayForward(backwards)).toBe(false);
    });

    it("CombinationsAndUnboundedState", () => {
        const a = new AxisAlignedBoundingBox(new Vector3Dd(0, 0, 0), new Vector3Dd(2, 2, 2));
        const b = new AxisAlignedBoundingBox(new Vector3Dd(1, -1, 1), new Vector3Dd(3, 1, 4));
        const u = a.enclosing(b);
        expectVectorEq(new Vector3Dd(0, -1, 0), u.getMin());
        expectVectorEq(new Vector3Dd(3, 2, 4), u.getMax());
        const n = a.intersection(b);
        expectVectorEq(new Vector3Dd(1, 0, 1), n.getMin());
        expectVectorEq(new Vector3Dd(2, 1, 2), n.getMax());
        expectVectorEq(new Vector3Dd(1, 1, 1), a.centroid());
        expect(
            AxisAlignedBoundingBox.empty()
                .expandedBy(new Vector3Dd(5, 6, 7))
                .getMin()
                .x(),
        ).toBe(5.0);
        expect(b.extent(0)).toBe(2.0);
        expect(b.extent(2)).toBe(3.0);
        expect(AxisAlignedBoundingBox.unbounded().isUnbounded()).toBe(true);
        expect(a.isUnbounded()).toBe(false);
        expect(a.containsPointWithTolerance(new Vector3Dd(2.05, 1, 1), 0.1)).toBe(true);
        expect(a.containsPointWithTolerance(new Vector3Dd(2.2, 1, 1), 0.1)).toBe(false);
    });

    it("TransformedCornersEncloseThePlacedBox", () => {
        let translation = new Matrix4x4d();
        translation = translation.translation(10, 0, 0);
        const placed = AxisAlignedBoundingBox.fromTransformedCorners(
            new Vector3Dd(-1, -1, -1),
            new Vector3Dd(1, 1, 1),
            translation,
        );
        const same = AxisAlignedBoundingBox.fromTransformedCorners(
            new Vector3Dd(-1, -1, -1),
            new Vector3Dd(1, 1, 1),
            null,
        );
        expectVectorEq(new Vector3Dd(-1, -1, -1), same.getMin());
        expect(placed.isUnbounded()).toBe(false);
        expect(placed.containsPointWithTolerance(placed.centroid(), 0.0)).toBe(true);
        expect(placed.extent(0)).toBe(2.0);
        expect(placed.getMin().x()).toBe(9.0);
        expect(placed.getMax().x()).toBe(11.0);
    });

    it("FromGeometryBoundsKeepsOnlyFiniteBounds", () => {
        const finite = new GeometryBounds();
        expect(finite.setFinite(new Vector3Dd(-1, -2, -3), new Vector3Dd(4, 5, 6))).toBe(true);
        const box = AxisAlignedBoundingBox.fromGeometryBounds(finite);
        expectVectorEq(new Vector3Dd(-1, -2, -3), box.getMin());
        expectVectorEq(new Vector3Dd(4, 5, 6), box.getMax());
        const other = new GeometryBounds();
        other.setUnbounded();
        expect(AxisAlignedBoundingBox.fromGeometryBounds(other).isUnbounded()).toBe(true);
        other.setEmpty();
        expect(AxisAlignedBoundingBox.fromGeometryBounds(other).isUnbounded()).toBe(true);
        other.setIndeterminate();
        expect(AxisAlignedBoundingBox.fromGeometryBounds(other).isUnbounded()).toBe(true);
    });
});

describe("AabbBoundingVolume", () => {
    it("FiltersAreConservativeAndCopiesAreIndependent", () => {
        const volume = new AabbBoundingVolume(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1));
        const filter: PreRayHitElement = volume;
        const hit = new Ray(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0));
        const miss = new Ray(new Vector3Dd(-5, 3, 0), new Vector3Dd(1, 0, 0));
        expect(filter.mayIntersect(hit)).toBe(true);
        expect(filter.mayIntersect(miss)).toBe(false);
        expect(filter.mayIntersectBefore(hit, 4.0)).toBe(true);
        expect(filter.mayIntersectBefore(hit, 3.0)).toBe(false);
        expect(volume.containsPoint(new Vector3Dd(0, 0, 0), 0.0)).toBe(true);
        const copy = volume.copy();
        expectVectorEq(new Vector3Dd(1, 1, 1), copy.axisAlignedExtent().getMax());
        expect(copy.isUnbounded()).toBe(false);
        expect(new AabbBoundingVolume().isUnbounded()).toBe(true);
    });
});

const addUnitBox = (boxes: ArrayList<AxisAlignedBoundingBox>, ids: ArrayList<number>, x: number, id: number): void => {
    boxes.add(new AxisAlignedBoundingBox(new Vector3Dd(x, -0.5, -0.5), new Vector3Dd(x + 1, 0.5, 0.5)));
    ids.add(id);
};

describe("AabbBinHierarchy", () => {
    it("BinsFollowCentroidOrderAlongTheWidestAxis", () => {
        const boxes = new ArrayList<AxisAlignedBoundingBox>();
        const ids = new ArrayList<number>();
        // Twenty boxes on the x axis given in reverse order, ids 100..119.
        for (let i = 0; i < 20; i++) addUnitBox(boxes, ids, 3.0 * (19 - i), 100 + i);
        const hierarchy = new AabbBinHierarchy();
        hierarchy.build(boxes, ids);
        expect(hierarchy.getBinCount()).toBe(4);
        expect(hierarchy.getMemberCount()).toBe(20);
        expect(hierarchy.getBinBounds(0).getMin().x()).toBe(0.0);
        expect(hierarchy.getBinBounds(0).getMax().x()).toBe(13.0);
        expect(hierarchy.getBinBounds(3).getMax().x()).toBe(57.0 + 1.0);
    });

    it("QueriesListEveryCrossedPrimitiveAndNoOther", () => {
        const boxes = new ArrayList<AxisAlignedBoundingBox>();
        const ids = new ArrayList<number>();
        for (let i = 0; i < 20; i++) addUnitBox(boxes, ids, 3.0 * i, i);
        const hierarchy = new AabbBinHierarchy();
        hierarchy.build(boxes, ids);
        const out = new Int32Array(32);
        // A vertical ray through the box at x in [9, 10] crosses only id 3.
        const vertical = new Ray(new Vector3Dd(9.5, 5, 0), new Vector3Dd(0, -1, 0));
        expect(hierarchy.gatherCandidates(vertical, new RaySlabReciprocals(vertical.getDirection()), out, 32)).toBe(1);
        expect(out[0]).toBe(3);
        // A ray along the row crosses all of them, in bin and member order.
        const along = new Ray(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0));
        expect(hierarchy.gatherCandidates(along, new RaySlabReciprocals(along.getDirection()), out, 32)).toBe(20);
        for (let i = 0; i < 20; i++) expect(out[i]).toBe(i);
        expect(hierarchy.gatherCandidates(along, new RaySlabReciprocals(along.getDirection()), out, 5)).toBe(-1);
        const away = new Ray(new Vector3Dd(-5, 0, 0), new Vector3Dd(-1, 0, 0));
        expect(hierarchy.gatherCandidates(away, new RaySlabReciprocals(away.getDirection()), out, 32)).toBe(0);
        const empty = new AabbBinHierarchy();
        empty.build(new ArrayList<AxisAlignedBoundingBox>(), new ArrayList<number>());
        expect(empty.getBinCount()).toBe(0);
        expect(empty.gatherCandidates(along, new RaySlabReciprocals(along.getDirection()), out, 32)).toBe(0);
    });
});
