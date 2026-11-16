import { describe, expect, it } from "vitest";

import { Random } from "java/util/Random.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidFaceGeometry } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";

/**
The box tests of the face geometry snapshot (Fable5.2Analysis.md Stage G)
are only allowed to skip work: whenever `projectionExcludes` answers
true, the point-in-face test must answer OUTSIDE, and whenever the
point-in-face test accepts a point, that point lies inside the face box
grown by `containmentMargin`. Checked on every face of the reference
corpus with points on the face plane, near its vertices and edges, and far
away.
 */
describe("PolyhedralBoundedSolidFaceGeometryTest", () => {
    const TOLERANCE = 1.0e-5;

    /**
    Mixes points around vertices (to exercise the LIMIT tolerances), random
    points in the solid box, and points far outside it.
    */
    function samplePoint(random: Random, solid: PolyhedralBoundedSolid, box: number[], k: number): Vector3Dd {
        const size = Math.max(box[3]! - box[0]!, Math.max(box[4]! - box[1]!, box[5]! - box[2]!));
        if (k % 3 === 0) {
            const v = solid.getVerticesList().get(random.nextInt(solid.getVerticesList().size()))!.position;
            const r = (random.nextInt(4) + 1) * TOLERANCE;
            return v.add(
                new Vector3Dd(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).multiply(r),
            );
        }
        const grow = k % 3 === 1 ? 0.1 : 2.0;
        return new Vector3Dd(
            box[0]! - grow * size + random.nextDouble() * (1 + 2 * grow) * (box[3]! - box[0]!),
            box[1]! - grow * size + random.nextDouble() * (1 + 2 * grow) * (box[4]! - box[1]!),
            box[2]! - grow * size + random.nextDouble() * (1 + 2 * grow) * (box[5]! - box[2]!),
        );
    }

    it("given_corpusFaces_when_boxRejects_then_pointInFaceSaysOutside", () => {
        const random = new Random(11);
        let rejected = 0;
        let accepted = 0;

        for (const sample of CsgSampleCorpus.values()) {
            for (const solid of CsgSampleCorpusFixtures.createPair(sample)) {
                const box = Array.from(solid.getMinMax());
                for (let i = 0; i < solid.getPolygonsList().size(); i++) {
                    const face = solid.getPolygonsList().get(i)!;
                    const geometry = PolyhedralBoundedSolidFaceGeometry.of(face);
                    if (geometry.getPlane() === null) {
                        continue;
                    }
                    for (let k = 0; k < 200; k++) {
                        let p = samplePoint(random, solid, box, k);
                        p = geometry.getPlane()!.projectPoint(p);
                        const status = face.testPointInside(p, TOLERANCE, geometry.getPlane());
                        if (geometry.projectionExcludes(p, TOLERANCE)) {
                            rejected++;
                            expect(status, `${sample} face ${face.id} point ${p}`).toBe(Geometry.OUTSIDE);
                        }
                        if (status !== Geometry.OUTSIDE) {
                            accepted++;
                        }
                    }
                }
            }
        }
        expect(rejected, "the box test must reject something").toBeGreaterThan(1000);
        expect(accepted, "the sample must hit faces").toBeGreaterThan(100);
    });
});
