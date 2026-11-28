// Port of java/base/src/test/vsdk/toolkit/environment/geometry/volume/BoxConeContainmentTest.java
import { describe, expect, it } from "vitest";

import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Cone } from "vsdk/toolkit/environment/geometry/volume/Cone.js";

/**
Exercises the classification of points against boxes (centered at the
origin) and cones (axis along Z, base at z = 0), as used by the voxelization
of solids.
 */
describe("BoxConeContainmentTest", () => {
    const TOLERANCE = 0.01;

    it("given_box_when_pointsClassified_then_insideLimitAndOutsideAreDistinguished", () => {
        // Arrange
        const box = new Box(2, 4, 6);

        // Act & Assert
        expect(box.doContainmentTest(new Vector3Dd(0.5, -1.5, 2.5), TOLERANCE)).toBe(Geometry.INSIDE);
        expect(box.doContainmentTest(new Vector3Dd(1.0, 0, 0), TOLERANCE)).toBe(Geometry.LIMIT);
        expect(box.doContainmentTest(new Vector3Dd(0, -2.005, 0), TOLERANCE)).toBe(Geometry.LIMIT);
        expect(box.doContainmentTest(new Vector3Dd(0, 0, 3.1), TOLERANCE)).toBe(Geometry.OUTSIDE);
        expect(box.doContainmentTest(new Vector3Dd(1.5, 0, 0), TOLERANCE)).toBe(Geometry.OUTSIDE);
    });

    it("given_cone_when_pointsClassified_then_sideAndCapsAreRespected", () => {
        // Arrange: radius 1 at z = 0, apex at z = 2
        const cone = new Cone(1, 0, 2);

        // Act & Assert
        expect(cone.doContainmentTest(new Vector3Dd(0, 0, 1), TOLERANCE)).toBe(Geometry.INSIDE);
        // At z = 1 the radius is 0.5
        expect(cone.doContainmentTest(new Vector3Dd(0.45, 0, 1), TOLERANCE)).toBe(Geometry.INSIDE);
        expect(cone.doContainmentTest(new Vector3Dd(0.6, 0, 1), TOLERANCE)).toBe(Geometry.OUTSIDE);
        expect(cone.doContainmentTest(new Vector3Dd(0, 0.5, 1), TOLERANCE)).toBe(Geometry.LIMIT);
        expect(cone.doContainmentTest(new Vector3Dd(0.2, 0, 0), TOLERANCE)).toBe(Geometry.LIMIT);
        expect(cone.doContainmentTest(new Vector3Dd(0, 0, -0.5), TOLERANCE)).toBe(Geometry.OUTSIDE);
        expect(cone.doContainmentTest(new Vector3Dd(0, 0, 2.5), TOLERANCE)).toBe(Geometry.OUTSIDE);
    });

    it("given_cylinder_when_pointsClassified_then_itBehavesAsAStraightTube", () => {
        // Arrange
        const cylinder = new Cone(1, 1, 2);

        // Act & Assert
        expect(cylinder.doContainmentTest(new Vector3Dd(0.9, 0, 1.9), TOLERANCE)).toBe(Geometry.INSIDE);
        expect(cylinder.doContainmentTest(new Vector3Dd(0, 1.0, 1), TOLERANCE)).toBe(Geometry.LIMIT);
        expect(cylinder.doContainmentTest(new Vector3Dd(0.8, 0.8, 1), TOLERANCE)).toBe(Geometry.OUTSIDE);
    });
});
