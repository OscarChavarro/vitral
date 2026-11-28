// Port of java/base/src/test/vsdk/toolkit/environment/geometry/volume/TorusTest.java
import { describe, expect, it } from "vitest";

import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { Torus } from "vsdk/toolkit/environment/geometry/volume/Torus.js";

/** AssertJ `isCloseTo(expected, offset(tolerance))`. */
function expectCloseTo(actual: number, expected: number, tolerance: number): void {
    expect(Math.abs(actual - expected)).toBeLessThanOrEqual(tolerance);
}

/**
Exercises the intersection of rays with a torus (whose axis is the Z axis),
specially the rejection of roots of the intersection equation that do not
belong to the surface.
 */
describe("TorusTest", () => {
    const EPS = 1.0e-6;

    it("given_rayAlongTheAxisOfATube_when_intersected_then_hitsTheNearestSurface", () => {
        // Arrange
        const torus = new Torus(1.0, 0.25);
        const ray = new Ray(new Vector3Dd(1, 0, 5), new Vector3Dd(0, 0, -1));

        // Act
        const hit = torus.doIntersectionFirstHit(ray);

        // Assert
        expect(hit).not.toBeNull();
        expectCloseTo(hit!.getT(), 4.75, EPS);
    });

    it("given_rayThroughTheHole_when_intersected_then_missesTheTorus", () => {
        // Arrange
        const torus = new Torus(1.0, 0.25);
        const ray = new Ray(new Vector3Dd(0, 0, 5), new Vector3Dd(0, 0, -1));

        // Act & Assert
        expect(torus.doIntersectionFirstHit(ray)).toBeNull();
    });

    it("given_veryDistantOrigin_when_intersected_then_hitsTheOuterEquator", () => {
        // Arrange
        const torus = new Torus(1.0, 0.25);
        const ray = new Ray(new Vector3Dd(1000, 0, 0), new Vector3Dd(-1, 0, 0));

        // Act
        const hit = torus.doIntersectionFirstHit(ray);

        // Assert
        expect(hit).not.toBeNull();
        expectCloseTo(hit!.getT(), 998.75, 1.0e-4);
    });

    it("given_rayPassingNearTheTorusButNotTouchingIt_when_intersected_then_missesIt", () => {
        // Arrange: ray found by a rotate gizmo, for which the quartic solver
        // gives a real root (t = 11.26) whose point is far from the torus
        const torus = new Torus(1.0132456102380434, 0.07599342076785325);
        const ray = new Ray(
            new Vector3Dd(-1.9999999999999996, -3.0, 2.5000000000000004),
            new Vector3Dd(0.3958157631719224, 0.7164180056829774, -0.5745216469010146),
        );

        // Act & Assert
        expect(torus.doIntersectionFirstHit(ray)).toBeNull();
    });

    it("given_rayTouchingTheTube_when_intersected_then_hitPointIsOnTheSurface", () => {
        // Arrange
        const torus = new Torus(2.0, 0.5);
        const direction = new Vector3Dd(0.3, 0.2, -1).normalized();
        const target = new Vector3Dd(0, 2.0, 0.5);
        const ray = new Ray(target.subtract(direction.multiply(7)), direction);

        // Act
        const hit = torus.doIntersectionFirstHit(ray);

        // Assert
        expect(hit).not.toBeNull();
        const point = ray.getOrigin().add(direction.multiply(hit!.getT()));
        const distanceToCircle = Math.hypot(Math.hypot(point.x(), point.y()) - 2.0, point.z());

        expectCloseTo(distanceToCircle, 0.5, EPS);
        expect(hit!.getT()).toBeLessThanOrEqual(7.0 + EPS);
    });
});
