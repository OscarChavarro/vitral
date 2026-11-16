import { describe, expect, it } from "vitest";

import { StringBuilder } from "java/lang/StringBuilder.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { InfinitePlane } from "vsdk/toolkit/environment/geometry/surface/InfinitePlane.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

const translation = PolyhedralBoundedSolidVolumeOracle.translation;

/**
Splitting a cube [-1,1]^3 with planes in generic and degenerate position,
checked by volume conservation: vol(above) + vol(below) = vol(solid), every
piece strict-valid, non-empty when the plane really cuts the solid, and on
the side of the plane its list names. The input solid must come back
unchanged and must not appear in the output lists.

<p>Traceability: [MANT1988] Ch. 14. Section 14.1 requires the algorithm to
be "general, i.e., it must work correctly for all possible geometric
configurations of solids and planes"; Sections 14.4-14.5 (splitgenerate,
ON-vertex neighborhoods, reclassification rules, Figs. 14.7-14.9) exist
precisely for vertices that lie on the splitting plane. The planes through
vertices reproduce defect S-01 of the Fable 5.2 kernel analysis
(doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md), fixed in
Stage E.</p>
 */
describe("PolyhedralBoundedSolidSplitterDegeneratePlaneTest", () => {
    const TOLERANCE = 1.0e-6;

    const cuttingPlanes: [string, Vector3Dd, Vector3Dd][] = [
        ["generic oblique plane", new Vector3Dd(0.3, 0.5, 0.8), new Vector3Dd(0.1, 0, 0.05)],
        ["axis plane through the middle", new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 0.1)],
        ["plane containing one cube edge", new Vector3Dd(1, -0.5, 0), new Vector3Dd(1, 1, 0)],
        ["diagonal plane through two opposite edges", new Vector3Dd(1, -1, 0), new Vector3Dd(0, 0, 0)],
        // S-01 (Stage E): planes through vertices.
        ["corner cut through three vertices (x+y+z=1)", new Vector3Dd(1, 1, 1), new Vector3Dd(1, 1, -1)],
        ["generic plane through exactly one vertex", new Vector3Dd(1, 0.7, 0.4), new Vector3Dd(1, 1, -1)],
        ["corner cut with the normal reversed", new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, -1)],
        ["hexagonal section through six edge midpoints", new Vector3Dd(1, 1, 1), new Vector3Dd(0, 0, 0)],
    ];

    const planesThatDoNotCut: [string, Vector3Dd, Vector3Dd, number][] = [
        ["plane missing the cube, cube below", new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 5), -1],
        ["plane missing the cube, cube above", new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, -5), +1],
        ["plane touching one vertex from outside", new Vector3Dd(1, 1, 1), new Vector3Dd(1, 1, 1), -1],
        ["plane containing the top face", new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 1), -1],
        ["plane containing the bottom face", new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, -1), +1],
        ["plane touching one edge from outside", new Vector3Dd(1, 1, 0), new Vector3Dd(1, 1, 0), -1],
    ];

    function assertInputUnchanged(
        solid: PolyhedralBoundedSolid,
        cube: OrientedBox,
        above: PolyhedralBoundedSolid[],
        below: PolyhedralBoundedSolid[],
    ): void {
        expect(solid.getPolygonsList().size(), "input faces after split").toBe(6);
        expect(
            Math.abs(PolyhedralBoundedSolidVolumeOracle.solidVolume(solid) - cube.volume()),
            "input volume after split",
        ).toBeLessThanOrEqual(TOLERANCE);
        expect(
            above.some((p) => p === solid),
            "above holds the input object",
        ).toBe(false);
        expect(
            below.some((p) => p === solid),
            "below holds the input object",
        ).toBe(false);
    }

    function assertValidPieceOnSide(piece: PolyhedralBoundedSolid, plane: InfinitePlane, side: number): number {
        const strictReport = new StringBuilder();
        let i: number;

        expect(piece.getPolygonsList().size(), "piece face count").toBeGreaterThanOrEqual(4);
        const strict = PolyhedralBoundedSolidValidationEngine.validateStrict(piece, strictReport);
        expect(strict, "strict validation: " + strictReport.toString()).toBe(true);
        for (i = 0; i < piece.getVerticesList().size(); i++) {
            expect(
                side * plane.pointDistance(piece.getVerticesList().get(i)!.position),
                "vertex side",
            ).toBeGreaterThanOrEqual(-TOLERANCE);
        }
        return PolyhedralBoundedSolidVolumeOracle.solidVolume(piece);
    }

    it.each(cuttingPlanes)(
        "given_cuttingPlane_when_split_then_volumeIsConservedOnTheRightSides: %s",
        (_name: string, normal: Vector3Dd, point: Vector3Dd) => {
            const cube = new OrientedBox(2, 2, 2, translation(0, 0, 0));
            const solid = cube.toSolid();
            const plane = new InfinitePlane(normal.normalized(), point);
            const above: PolyhedralBoundedSolid[] = [];
            const below: PolyhedralBoundedSolid[] = [];
            let total = 0.0;

            PolyhedralBoundedSolidModeler.split(solid, plane, above, below);

            assertInputUnchanged(solid, cube, above, below);
            expect(above.length, "pieces above").toBeGreaterThan(0);
            expect(below.length, "pieces below").toBeGreaterThan(0);
            for (const piece of above) {
                total += assertValidPieceOnSide(piece, plane, +1);
            }
            for (const piece of below) {
                total += assertValidPieceOnSide(piece, plane, -1);
            }
            expect(Math.abs(total - cube.volume())).toBeLessThanOrEqual(TOLERANCE);
        },
    );

    /**
    A plane that does not cut the solid leaves it whole: a copy goes to the
    list of the side it lies on, an empty solid to the other one. Before
    Stage E the input object itself always went to "above".
    */
    it.each(planesThatDoNotCut)(
        "given_planeThatDoesNotCut_when_split_then_aCopyGoesToItsSide: %s",
        (_name: string, normal: Vector3Dd, point: Vector3Dd, side: number) => {
            const cube = new OrientedBox(2, 2, 2, translation(0, 0, 0));
            const solid = cube.toSolid();
            const plane = new InfinitePlane(normal.normalized(), point);
            const above: PolyhedralBoundedSolid[] = [];
            const below: PolyhedralBoundedSolid[] = [];

            PolyhedralBoundedSolidModeler.split(solid, plane, above, below);

            assertInputUnchanged(solid, cube, above, below);
            expect(above).toHaveLength(1);
            expect(below).toHaveLength(1);
            const whole = side > 0 ? above[0]! : below[0]!;
            const empty = side > 0 ? below[0]! : above[0]!;
            expect(empty.getPolygonsList().size()).toBe(0);
            expect(Math.abs(assertValidPieceOnSide(whole, plane, side) - cube.volume())).toBeLessThanOrEqual(TOLERANCE);
        },
    );

    /**
    A plane through the lower reflex vertex of the L-shaped hollow brick
    cuts a section of two regions that touch at that vertex. Above the
    plane the piece is one solid whose section has two faces sharing the
    vertex; below, the two regions meet only at the vertex and come out as
    two solids. Every piece is strict-valid and the volume is conserved.
    */
    it("given_planeThroughReflexVertex_when_sectionIsPinched_then_simpleFaces", () => {
        const brick = CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.HOLLOW_BRICK)[0]!;
        const plane = new InfinitePlane(new Vector3Dd(-0.85, 1.14, 0.19).normalized(), new Vector3Dd(0.2, 0.8, 0.0));
        const above: PolyhedralBoundedSolid[] = [];
        const below: PolyhedralBoundedSolid[] = [];
        let total = 0.0;

        PolyhedralBoundedSolidModeler.split(brick, plane, above, below);

        expect(above).toHaveLength(1);
        expect(below).toHaveLength(2);
        for (const piece of above) {
            expect(PolyhedralBoundedSolidValidationEngine.validateStrict(piece, new StringBuilder())).toBe(true);
            total += PolyhedralBoundedSolidVolumeOracle.solidVolume(piece);
        }
        for (const piece of below) {
            expect(PolyhedralBoundedSolidValidationEngine.validateStrict(piece, new StringBuilder())).toBe(true);
            total += PolyhedralBoundedSolidVolumeOracle.solidVolume(piece);
        }
        expect(Math.abs(total - PolyhedralBoundedSolidVolumeOracle.solidVolume(brick))).toBeLessThanOrEqual(1.0e-9);
    });
});
