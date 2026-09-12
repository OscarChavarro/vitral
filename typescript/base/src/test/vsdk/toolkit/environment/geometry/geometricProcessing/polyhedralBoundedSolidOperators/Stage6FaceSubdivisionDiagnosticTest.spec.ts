import { describe, expect, it } from "vitest";

import { Math as JavaMath } from "java/lang/Math.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidGeometricValidator } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import type { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgKurlanderBowlFixture } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.js";
import { _PolyhedralBoundedSolidSetFinisher } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetFinisher.js";
import { yieldToEventLoop } from "./_HarnessEventLoopYield.js";

/**
Harness accommodation: JUnit has no per-test time budget; every case here
rebuilds the Kurlander bowl and its motifs through dozens of booleans.
*/
const KURLANDER_TIMEOUT_MS = 1_800_000;

/**
Stage-6 diagnostic and regression net (doc/plan-csg-boolean-fix-stage6.md):
moons 23/28/33/38 (all at z=9.0, one per quadrant) visually showed spurious
face subdivisions on bowl faces far away from the moon imprint, caused by the
unguarded vertex snap in `_PolyhedralBoundedSolidSetIntersector`
dragging remote vertices onto extended face planes.  This test quantifies the
result face structure for an OK moon (21) and the four formerly failing moons,
with and without the post-process stage (maximizeFaces +
triangulateNonPlanarFaces), and asserts that no non-planar faces remain, that
finish() never needs to ear-clip, and that the bowl region antipodal to the
moon stays bit-identical to the untouched operand.
*/
describe("Stage6FaceSubdivisionDiagnosticTest", () => {
    const MOTIFS = [21, 23, 28, 33, 38];

    /** `String.format("%<width>.<decimals>f", value)`. */
    function fixed(value: number, width: number, decimals: number): string {
        const text = Number.isNaN(value) ? "NaN" : value.toFixed(decimals);
        return text.length >= width ? text : " ".repeat(width - text.length) + text;
    }

    /** `String.format("%.3e", value)` in Java's `d.dddeNN` shape. */
    function scientific(value: number, decimals: number): string {
        if (Number.isNaN(value)) {
            return "NaN";
        }
        const text = value.toExponential(decimals);
        const [mantissa, exponent] = text.split("e");
        const sign = exponent!.startsWith("-") ? "-" : "+";
        const digits = exponent!.replace(/^[+-]/, "");
        return mantissa + "e" + sign + (digits.length < 2 ? "0" + digits : digits);
    }

    function planarityResidual(face: _PolyhedralBoundedSolidFace): number {
        let worst = 0.0;
        let li: number;
        let k: number;

        if (face.getContainingPlane() === null) {
            return Number.NaN;
        }
        for (li = 0; li < face.boundariesList.size(); li++) {
            const loop = face.boundariesList.get(li)!;
            for (k = 0; k < loop.halfEdgesList.size(); k++) {
                const d = Math.abs(
                    face.getContainingPlane()!.pointDistance(loop.halfEdgesList.get(k)!.startingVertex.position),
                );
                if (d > worst) {
                    worst = d;
                }
            }
        }
        return worst;
    }

    function faceCentroidCylindrical(face: _PolyhedralBoundedSolidFace): number[] {
        const outer = face.boundariesList.get(0)!;
        let cx = 0.0;
        let cy = 0.0;
        let cz = 0.0;
        const n = outer.halfEdgesList.size();
        let k: number;

        for (k = 0; k < n; k++) {
            cx += outer.halfEdgesList.get(k)!.startingVertex.position.x();
            cy += outer.halfEdgesList.get(k)!.startingVertex.position.y();
            cz += outer.halfEdgesList.get(k)!.startingVertex.position.z();
        }
        cx /= n;
        cy /= n;
        cz /= n;
        return [JavaMath.toDegrees(Math.atan2(cy, cx)), cz, Math.sqrt(cx * cx + cy * cy)];
    }

    /**
    Prints vertices whose cylindrical coordinates fall in the back-region
    window (azimuth 20..70 degrees, z 1.0..1.7) where the spurious face
    subdivisions of motifs 23/28/33/38 were observed.
    @return the sorted, formatted vertex lines for comparison.
    */
    function dumpRegionVertices(tag: string, solid: PolyhedralBoundedSolid): string[] {
        const lines: string[] = [];
        let vi: number;

        for (vi = 0; vi < solid.getVerticesList().size(); vi++) {
            const x = solid.getVerticesList().get(vi)!.position.x();
            const y = solid.getVerticesList().get(vi)!.position.y();
            const z = solid.getVerticesList().get(vi)!.position.z();
            const az = JavaMath.toDegrees(Math.atan2(y, x));
            const r = Math.sqrt(x * x + y * y);

            if (az >= 20.0 && az <= 70.0 && z >= 1.0 && z <= 1.7) {
                lines.push(
                    "az=" +
                        fixed(az, 8, 3) +
                        " z=" +
                        fixed(z, 8, 5) +
                        " r=" +
                        fixed(r, 8, 5) +
                        " p=<" +
                        x.toFixed(6) +
                        ", " +
                        y.toFixed(6) +
                        ", " +
                        z.toFixed(6) +
                        ">",
                );
            }
        }
        lines.sort();
        for (const line of lines) {
            console.log(tag + " " + line);
        }
        return lines;
    }

    /**
    Prints every face whose maximum vertex distance to its containing plane
    exceeds one tenth of the solid's planarity threshold, plus whether the
    geometric validator considers it planar.  Used to detect "borderline"
    faces whose planarity verdict could flip from run to run.
    */
    function dumpNonPlanarOrMarginalFaces(tag: string, solid: PolyhedralBoundedSolid): void {
        let fi: number;

        for (fi = 0; fi < solid.getPolygonsList().size(); fi++) {
            const face = solid.getPolygonsList().get(fi)!;
            const residual = planarityResidual(face);
            const planar = PolyhedralBoundedSolidGeometricValidator.validateFaceIsPlanar(face);
            if (!planar || residual > 1.0e-6) {
                const c = faceCentroidCylindrical(face);
                console.log(
                    tag +
                        " f" +
                        face.id +
                        " residual=" +
                        scientific(residual, 3) +
                        " planar=" +
                        planar +
                        " az=" +
                        fixed(c[0]!, 7, 1) +
                        " z=" +
                        fixed(c[1]!, 6, 3) +
                        " r=" +
                        fixed(c[2]!, 6, 3) +
                        " n=" +
                        face.boundariesList.get(0)!.halfEdgesList.size(),
                );
            }
        }
    }

    /**
    Prints the centroid of every triangular face in cylindrical coordinates
    (azimuth in degrees around the bowl axis, z, radial distance) so that
    spurious triangles far away from the moon imprint can be spotted.
    */
    function dumpTriangleCentroids(tag: string, result: PolyhedralBoundedSolid): void {
        let fi: number;

        for (fi = 0; fi < result.getPolygonsList().size(); fi++) {
            const face = result.getPolygonsList().get(fi)!;
            const outer = face.boundariesList.get(0)!;
            if (outer.halfEdgesList.size() !== 3) {
                continue;
            }

            let cx = 0.0;
            let cy = 0.0;
            let cz = 0.0;
            let k: number;

            for (k = 0; k < 3; k++) {
                cx += outer.halfEdgesList.get(k)!.startingVertex.position.x();
                cy += outer.halfEdgesList.get(k)!.startingVertex.position.y();
                cz += outer.halfEdgesList.get(k)!.startingVertex.position.z();
            }
            cx /= 3.0;
            cy /= 3.0;
            cz /= 3.0;

            const azimuthDeg = JavaMath.toDegrees(Math.atan2(cy, cx));
            const radial = Math.sqrt(cx * cx + cy * cy);

            console.log(
                tag +
                    " tri f" +
                    face.id +
                    " az=" +
                    fixed(azimuthDeg, 7, 1) +
                    " z=" +
                    fixed(cz, 6, 3) +
                    " r=" +
                    fixed(radial, 6, 3),
            );
        }
    }

    function runOne(motifIndex: number, maximizeResultFaces: boolean): void {
        const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motifIndex);
        const bowlFaces = operands[0]!.getPolygonsList().size();
        const result = PolyhedralBoundedSolidModeler.setOp(
            operands[0]!,
            operands[1]!,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
            maximizeResultFaces,
        );

        const tag = "[STAGE6] motif=" + motifIndex + " postProcess=" + (maximizeResultFaces ? "ON " : "OFF");

        expect(result, "result of motif " + motifIndex).not.toBeNull();
        expect(result.getPolygonsList().size(), "result face count of motif " + motifIndex).toBeGreaterThan(0);

        // Java uses a TreeMap<Integer,Integer>: keys iterate in ascending order.
        const histogram = new Map<number, number>();
        const bigFaces: string[] = [];
        let nonPlanarCount = 0;
        let fi: number;

        for (fi = 0; fi < result.getPolygonsList().size(); fi++) {
            const face = result.getPolygonsList().get(fi)!;
            const outer = face.boundariesList.get(0)!;
            const size = outer.halfEdgesList.size();
            histogram.set(size, (histogram.get(size) ?? 0) + 1);
            const planar = PolyhedralBoundedSolidGeometricValidator.validateFaceIsPlanar(face);
            if (!planar) {
                nonPlanarCount++;
            }
            if (size >= 8 || face.boundariesList.size() > 1) {
                bigFaces.push(
                    "f" + face.id + ":n=" + size + ":loops=" + face.boundariesList.size() + ":planar=" + planar,
                );
            }
        }

        const histogramText =
            "{" +
            [...histogram.keys()]
                .sort((a, b) => a - b)
                .map((key) => key + "=" + histogram.get(key))
                .join(", ") +
            "}";

        console.log(
            tag +
                " bowlFaces=" +
                bowlFaces +
                " resultFaces=" +
                result.getPolygonsList().size() +
                " triangulated=" +
                _PolyhedralBoundedSolidSetFinisher.getLastTriangulatedFaceCount() +
                " nonPlanar=" +
                nonPlanarCount +
                " histogram=" +
                histogramText,
        );
        if (bigFaces.length !== 0) {
            console.log(tag + " bigFaces=[" + bigFaces.join(", ") + "]");
        }
        if (!maximizeResultFaces) {
            dumpTriangleCentroids(tag, result);
        }

        // Stage-6 regression net: subtracting a single moon must not leave any
        // non-planar face in the result, and finish() must not need to
        // ear-clip any face. Before the stage-6 fix (containment-guarded
        // vertex snap in _PolyhedralBoundedSolidSetIntersector), motifs
        // 23/28/33/38 produced 14 ear-clipped faces from bowl vertices that
        // the Generate snap had dragged onto far-away face planes.
        expect(nonPlanarCount, "non-planar faces in result of motif " + motifIndex).toBe(0);
        expect(
            _PolyhedralBoundedSolidSetFinisher.getLastTriangulatedFaceCount(),
            "faces ear-clipped by finish() for motif " + motifIndex,
        ).toBe(0);
    }

    it(
        "diagnose_faceStructurePerMotif",
        async () => {
            let i: number;

            for (i = 0; i < MOTIFS.length; i++) {
                await yieldToEventLoop();
                runOne(MOTIFS[i]!, true);
                await yieldToEventLoop();
                runOne(MOTIFS[i]!, false);
            }
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "diagnose_bowlOperandPlanarityResiduals",
        () => {
            const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(23);
            dumpNonPlanarOrMarginalFaces("[STAGE6] bowlA", operands[0]!);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "diagnose_backRegionVertexDisplacement",
        () => {
            const bowlReference = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(23)[0]!;
            const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(23);
            const result = PolyhedralBoundedSolidModeler.setOp(
                operands[0]!,
                operands[1]!,
                PolyhedralBoundedSolidModeler.SUBTRACT,
                false,
                false,
            );

            const referenceLines = dumpRegionVertices("[STAGE6] backRef", bowlReference);
            const resultLines = dumpRegionVertices("[STAGE6] backRes", result);
            dumpNonPlanarOrMarginalFaces("[STAGE6] res23", result);

            // Stage-6 regression net: the bowl region diametrically opposite the
            // moon imprint must be bit-identical to the untouched bowl operand.
            // Before the stage-6 fix, the Generate vertex snap moved the bowl
            // vertex antipodal to the moon by ~2e-5 onto an extended face plane
            // of the moon cylinder.
            expect(resultLines, "back-region vertices after bowl-moon subtraction").toEqual(referenceLines);
        },
        KURLANDER_TIMEOUT_MS,
    );
});
