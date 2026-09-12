import { describe, expect, it } from "vitest";

import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidTestFixtures } from "./PolyhedralBoundedSolidTestFixtures.js";

describe("PolyhedralBoundedSolidTopologySummaryTest", () => {
    function createCylinder(radius: number, height: number, z: number): PolyhedralBoundedSolid {
        const solid = PolyhedralBoundedSolidModeler.createCircularLamina(0.0, 0.0, radius, 0.0, 24);
        let sweep = new Matrix4x4d();
        sweep = sweep.translation(0.0, 0.0, height);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1)!, sweep);
        if (z !== 0.0) {
            let translation = new Matrix4x4d();
            translation = translation.translation(0.0, 0.0, z);
            PolyhedralBoundedSolidModeler.applyTransformation(solid, translation);
        }
        return solid;
    }

    it("given_box_when_summarized_then_itHasOneSphericalShell", () => {
        const box = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);

        const summary = PolyhedralBoundedSolidTopologySummary.from(box);

        expect(summary.getShellCount()).toBe(1);
        expect(summary.getAdjustedEulerCharacteristic()).toBe(2);
        expect(summary.isEveryFaceReachedExactlyOnce()).toBe(true);
        expect(summary.getInvalidEdgeAdjacencyCount()).toBe(0);
        expect(summary.getShells()[0]!.isClosedOrientableEulerCompatible()).toBe(true);
    });

    it("given_twoDisjointBoxes_when_summarized_then_eachShellHasChiTwo", () => {
        const pair = PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();
        pair[0]!.merge(pair[1]!);

        const summary = PolyhedralBoundedSolidTopologySummary.from(pair[0]!);

        expect(summary.getShellCount()).toBe(2);
        expect(summary.getAdjustedEulerCharacteristic()).toBe(4);
        expect(summary.getShells().map((shell) => shell.getAdjustedEulerCharacteristic())).toEqual([2, 2]);
    });

    it("given_throughTube_when_summarized_then_innerLoopsAdjustChiToZero", () => {
        const outer = createCylinder(2.0, 2.0, 0.0);
        const cutter = createCylinder(1.0, 2.2, -0.1);

        const tube = PolyhedralBoundedSolidModeler.setOp(
            outer,
            cutter,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
            true,
            false,
        );
        const summary = PolyhedralBoundedSolidTopologySummary.from(tube);

        let hasFaceWithInnerLoop = false;
        for (let i = 0; i < tube.getPolygonsList().size(); i++) {
            if (tube.getPolygonsList().get(i)!.boundariesList.size() > 1) {
                hasFaceWithInnerLoop = true;
                break;
            }
        }
        expect(hasFaceWithInnerLoop).toBe(true);
        expect(summary.getShellCount()).toBe(1);
        expect(summary.getAdjustedEulerCharacteristic()).toBe(0);
        expect(summary.getShells()[0]!.getAdjustedEulerCharacteristic()).toBe(0);
        expect(summary.hasUniversalContradiction()).toBe(false);
    });

    it("given_duplicateFaceReference_when_summarized_then_reachabilityFails", () => {
        const box = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        box.getPolygonsList().add(box.getPolygonsList().get(0)!);

        const summary = PolyhedralBoundedSolidTopologySummary.from(box);

        expect(summary.isEveryFaceReachedExactlyOnce()).toBe(false);
        expect(summary.hasUniversalContradiction()).toBe(true);
    });
});
