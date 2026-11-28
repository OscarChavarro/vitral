// Translation of java/base/src/test/vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatchTest.java
import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { ParametricCurve } from "vsdk/toolkit/environment/geometry/curve/ParametricCurve.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { ParametricBiCubicPatch } from "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.js";

/**
Exercises the evaluation of bicubic patches. `Vector3Dd` is immutable, so the
point must be the returned value of `evaluate` (the former form, which wrote
it into a given vector, returned always the origin).
*/
describe("ParametricBiCubicPatchTest", () => {
    const EPS = 1.0e-9;

    /** AssertJ `isCloseTo(expected, offset(EPS))` */
    const expectCloseTo = (actual: number, expected: number, tolerance: number = EPS): void => {
        expect(Math.abs(actual - expected)).toBeLessThanOrEqual(tolerance);
    };

    const buildPlanarBezierPatch = (): ParametricBiCubicPatch => {
        const points: Vector3Dd[][] = [];
        for (let i = 0; i < 4; i++) {
            points.push([]);
            for (let j = 0; j < 4; j++) {
                points[i]![j] = new Vector3Dd(i / 3.0, j / 3.0, 0);
            }
        }
        const patch = new ParametricBiCubicPatch();
        patch.buildBezierPatch(points);
        return patch;
    };

    const buildDomeNet = (): Vector3Dd[][] => {
        const points: Vector3Dd[][] = [];
        for (let i = 0; i < 4; i++) {
            points.push([]);
            for (let j = 0; j < 4; j++) {
                const inner = (i === 1 || i === 2) && (j === 1 || j === 2);
                points[i]![j] = new Vector3Dd(i / 3.0 + 0.1 * Math.sin(j), j / 3.0, inner ? 1.2 : 0.2 * i);
            }
        }
        return points;
    };

    const buildEditorContour = (): ParametricCurve => {
        const contour = new ParametricCurve();

        contour.addPoint(
            [new Vector3Dd(0, 0, 0), new Vector3Dd(0, -1, 0), new Vector3Dd(1, 0, 0)],
            ParametricCurve.HERMITE,
        );
        contour.addPoint(
            [new Vector3Dd(1, 0, 0), new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0)],
            ParametricCurve.HERMITE,
        );
        contour.addPoint(
            [new Vector3Dd(1, 1, 0.4), new Vector3Dd(0, 1, 0), new Vector3Dd(-1, 0, 0)],
            ParametricCurve.HERMITE,
        );
        contour.addPoint(
            [new Vector3Dd(0, 1, 0), new Vector3Dd(-1, 0, 0), new Vector3Dd(0, -1, 0)],
            ParametricCurve.HERMITE,
        );
        contour.addPoint(contour.getPoint(0), ParametricCurve.HERMITE);
        return contour;
    };

    it("given_planarBilinearBezierMesh_when_evaluated_then_matchesTheTypeScriptPort", () => {
        // Arrange
        const points: Vector3Dd[][] = [];
        for (let i = 0; i < 4; i++) {
            points.push([]);
            for (let j = 0; j < 4; j++) {
                points[i]![j] = new Vector3Dd(i / 3.0, j / 3.0, 0);
            }
        }
        const patch = new ParametricBiCubicPatch();
        patch.buildBezierPatch(points);

        // Act
        const position = patch.evaluate(0.5, 0.5);
        const tangent = patch.evaluateTangent(0.5, 0.5);
        const binormal = patch.evaluateBinormal(0.5, 0.5);
        const normal = patch.evaluateNormal(0.5, 0.5);

        // Assert
        expectCloseTo(position.x(), 0.5);
        expectCloseTo(position.y(), 0.5);
        expectCloseTo(position.z(), 0.0);
        expectCloseTo(tangent.x(), 1.0);
        expectCloseTo(binormal.y(), 1.0);
        expectCloseTo(normal.z(), 1.0);
    });

    it("given_fergusonPatch_when_evaluatedAtItsCorners_then_givesTheContourPoints", () => {
        // Arrange: the patch created by the scene editor
        const patch = new ParametricBiCubicPatch();
        patch.buildFergusonPatch(buildEditorContour());

        // Act
        const p00 = patch.evaluate(0, 0);
        const p10 = patch.evaluate(1, 0);
        const p11 = patch.evaluate(1, 1);
        const p01 = patch.evaluate(0, 1);
        const legacy = patch.evaluate(new Vector3Dd(), 1, 1);

        // Assert
        expect(p00.subtract(new Vector3Dd(0, 0, 0)).length()).toBeLessThan(EPS);
        expect(p10.subtract(new Vector3Dd(1, 0, 0)).length()).toBeLessThan(EPS);
        expect(p11.subtract(new Vector3Dd(1, 1, 0.4)).length()).toBeLessThan(EPS);
        expect(p01.subtract(new Vector3Dd(0, 1, 0)).length()).toBeLessThan(EPS);
        expect(legacy.equals(p11)).toBe(true);
    });

    it("given_planarBezierPatch_when_rayHitsItsInterior_then_reportsExactHitData", () => {
        // Arrange
        const patch = buildPlanarBezierPatch();
        const ray = new Ray(new Vector3Dd(0.3, 0.6, 5), new Vector3Dd(0, 0, -1));
        const hit = new RayHit();

        // Act
        const intersects = patch.doIntersectionFirstHit(ray, hit);

        // Assert
        expect(intersects).toBe(true);
        expectCloseTo(hit.getHitDistance(), 5.0);
        expect(hit.point.subtract(new Vector3Dd(0.3, 0.6, 0)).length()).toBeLessThan(EPS);
        expectCloseTo(hit.normal.z(), 1.0);
        expectCloseTo(hit.u, 0.3);
        expectCloseTo(hit.v, 0.6);
        expectCloseTo(hit.tangent.x(), 1.0);
    });

    it("given_planarBezierPatch_when_rayMissesOrPointsAway_then_reportsNoHit", () => {
        // Arrange
        const patch = buildPlanarBezierPatch();
        const outside = new Ray(new Vector3Dd(1.5, 0.5, 5), new Vector3Dd(0, 0, -1));
        const away = new Ray(new Vector3Dd(0.5, 0.5, 5), new Vector3Dd(0, 0, 1));
        const parallel = new Ray(new Vector3Dd(-1, 0.5, 1), new Vector3Dd(1, 0, 0));

        // Act / Assert
        expect(patch.doIntersectionFirstHit(outside, new RayHit())).toBe(false);
        expect(patch.doIntersectionFirstHit(away, new RayHit())).toBe(false);
        expect(patch.doIntersectionFirstHit(parallel, new RayHit())).toBe(false);
        expect(patch.doIntersectionFirstHit(outside)).toBeNull();
    });

    it("given_bezierPatch_when_askedForBezierNet_then_returnsItsControlPoints", () => {
        // Arrange
        const points = buildDomeNet();
        const patch = new ParametricBiCubicPatch();
        patch.buildBezierPatch(points);

        // Act / Assert
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                expect(patch.getBezierControlPoint(i, j)!.subtract(points[i]![j]!).length()).toBeLessThan(EPS);
            }
        }
    });

    it("given_curvedPatches_when_raysAimAtSurfacePoints_then_hitsLieOnTheSurface", () => {
        // Arrange
        const dome = new ParametricBiCubicPatch();
        dome.buildBezierPatch(buildDomeNet());
        const ferguson = new ParametricBiCubicPatch();
        ferguson.buildFergusonPatch(buildEditorContour());
        const patches = [dome, ferguson];
        const eyes = [new Vector3Dd(0.3, -0.2, 4), new Vector3Dd(-3, 2, 1.5), new Vector3Dd(2, 3, -2)];

        for (const patch of patches) {
            for (const eye of eyes) {
                for (let a = 1; a < 10; a++) {
                    for (let b = 1; b < 10; b++) {
                        const target = patch.evaluate(a / 10.0, b / 10.0);
                        const direction = target.subtract(eye).normalized();
                        const targetDistance = target.subtract(eye).length();
                        const hit = new RayHit();

                        // Act
                        const intersects = patch.doIntersectionFirstHit(new Ray(eye, direction), hit);

                        // Assert: the nearest hit is on the surface, not
                        // beyond the aimed point, and its (u, v) are coherent
                        expect(intersects).toBe(true);
                        expect(hit.getHitDistance()).toBeLessThanOrEqual(targetDistance + 1.0e-6);
                        const onSurface = patch.evaluate(hit.u, hit.v);
                        expect(onSurface.subtract(hit.point).length()).toBeLessThan(1.0e-6);
                        expect(hit.normal.dotProduct(direction)).toBeLessThanOrEqual(0.0);
                    }
                }
            }
        }
    });

    it("given_planarAndCurvedPatches_when_rayLeavesFromTheSurface_then_doesNotHitItself", () => {
        // Arrange: shadow rays as built by LightingShader, with an origin
        // offset of VSDK.EPSILON along the light direction
        const dome = new ParametricBiCubicPatch();
        dome.buildBezierPatch(buildDomeNet());
        const lightDirection = new Vector3Dd(-0.1, 0.05, 1).normalized();

        for (let a = 0; a <= 20; a++) {
            for (let b = 0; b <= 20; b++) {
                const p = dome.evaluate(a / 20.0, b / 20.0);
                const origin = p.add(lightDirection.multiply(1.0e-6));
                const hit = new RayHit(RayHit.DETAIL_NONE, false);

                // Act
                const intersects = dome.doIntersectionFirstHit(new Ray(origin, lightDirection), hit);

                // Assert
                expect(intersects, `self hit from (${a}, ${b}) at distance ${hit.getHitDistance()}`).toBe(false);
            }
        }
    });

    it("given_fergusonPatch_when_minMaxRequested_then_containsSampledSurface", () => {
        // Arrange
        const patch = new ParametricBiCubicPatch();
        patch.buildFergusonPatch(buildEditorContour());

        // Act
        const minMax = patch.getMinMax();

        // Assert
        for (let a = 0; a <= 20; a++) {
            for (let b = 0; b <= 20; b++) {
                const p = patch.evaluate(a / 20.0, b / 20.0);
                expect(p.x()).toBeGreaterThanOrEqual(minMax[0]! - EPS);
                expect(p.x()).toBeLessThanOrEqual(minMax[3]! + EPS);
                expect(p.y()).toBeGreaterThanOrEqual(minMax[1]! - EPS);
                expect(p.y()).toBeLessThanOrEqual(minMax[4]! + EPS);
                expect(p.z()).toBeGreaterThanOrEqual(minMax[2]! - EPS);
                expect(p.z()).toBeLessThanOrEqual(minMax[5]! + EPS);
            }
        }
    });
});
