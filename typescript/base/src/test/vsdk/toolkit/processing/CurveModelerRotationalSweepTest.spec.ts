import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";

/**
Translation of `java/base/src/test/vsdk/toolkit/processing/CurveModelerRotationalSweepTest.java`.

Exercises rotational sweep generation from an open wire profile represented
as a polyhedral bounded solid.

Traceability: [MANT1988] Ch. 12.3/12.5 sweep construction and the
Euler-operator profile construction style from Ch. 11.5.
*/
describe("CurveModelerRotationalSweepTest", () => {
    it("given_wireWithOneEndpointOnXAxis_when_rotationalSweep_then_returnsValidSolid", () => {
        // Arrange
        const wire: PolyhedralBoundedSolid = createWireProfile(
            new Vector3Dd(0.0, 0.0, 0.0),
            new Vector3Dd(0.8, 0.35, 0.0),
            new Vector3Dd(1.2, 0.5, 0.0),
        );

        // Action
        PolyhedralBoundedSolidModeler.rotationalSweepExtrudeWireAroundXAxis(wire, 16);

        // Assert
        expect(wire.getPolygonsList().size()).toBeGreaterThan(0);
        expect(wire.getEdgesList().size()).toBeGreaterThan(0);
        expect(wire.getVerticesList().size()).toBeGreaterThan(3);
    });

    it("given_wireWithBothEndpointsOnXAxis_when_rotationalSweep_then_returnsValidSolid", () => {
        // Arrange
        const wire: PolyhedralBoundedSolid = createWireProfile(
            new Vector3Dd(0.0, 0.0, 0.0),
            new Vector3Dd(0.6, 0.4, 0.0),
            new Vector3Dd(1.1, 0.0, 0.0),
        );

        // Action
        PolyhedralBoundedSolidModeler.rotationalSweepExtrudeWireAroundXAxis(wire, 18);

        // Assert
        expect(wire.getPolygonsList().size()).toBeGreaterThan(0);
        expect(wire.getEdgesList().size()).toBeGreaterThan(0);
        expect(wire.getVerticesList().size()).toBeGreaterThan(3);
    });

    function createWireProfile(p0: Vector3Dd, p1: Vector3Dd, p2: Vector3Dd): PolyhedralBoundedSolid {
        const wire = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(wire, p0, 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(wire, 1, 1, 2, p1);
        PolyhedralBoundedSolidEulerOperators.smev(wire, 1, 2, 3, p2);
        return wire;
    }
});
