import { describe, expect, it } from "vitest";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { ReferenceFrameGizmo } from "vsdk/toolkit/gui/gizmo/ReferenceFrameGizmo.js";
import { ViewportElementScaler } from "vsdk/toolkit/gui/viewport/ViewportElementScaler.js";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { Math as JavaMath } from "java/lang/Math.js";

/**
Exercises the rendering independent model and rotation estimation of the
reference frame gizmo.
 */
describe("ReferenceFrameGizmoTest", () => {
    function closeTo(actual: number, expected: number, offset: number): void {
        expect(Math.abs(actual - expected)).toBeLessThanOrEqual(offset);
    }

    /** AssertJ `isEqualTo`: Java `equals` when the value defines it. */
    function isEqualTo(actual: unknown, expected: unknown): void {
        const a = actual as { equals?: (other: unknown) => boolean } | null;
        if (a !== null && typeof a === "object" && typeof a.equals === "function") {
            expect(a.equals(expected)).toBe(true);
        } else {
            expect(actual).toEqual(expected);
        }
    }

    function sizeOf(collection: string | { length: number } | { size(): number }): number {
        return typeof collection === "string" || "length" in collection ? collection.length : collection.size();
    }

    const EPS = 1.0e-9;

    it("given_newGizmo_when_queried_then_hasDefaultModel", () => {
        // Arrange / Act
        let gizmo = new ReferenceFrameGizmo();

        // Assert
        expect(gizmo.isVisible()).toBe(true);
        isEqualTo(gizmo.getSizeInPixels(), ReferenceFrameGizmo.DEFAULT_SIZE_IN_PIXELS);
        isEqualTo(gizmo.getAxisLabel(ReferenceFrameGizmo.AXIS_X), "X");
        isEqualTo(gizmo.getAxisLabel(ReferenceFrameGizmo.AXIS_Y), "Y");
        isEqualTo(gizmo.getAxisLabel(ReferenceFrameGizmo.AXIS_Z), "Z");
        isEqualTo(gizmo.getAxisEnd(ReferenceFrameGizmo.AXIS_Y).y(), gizmo.getAxisLength());
    });

    it("given_notPositiveValues_when_set_then_areIgnored", () => {
        // Arrange
        let gizmo = new ReferenceFrameGizmo();

        // Act
        gizmo.setSizeInPixels(0);
        gizmo.setAxisLength(-1);

        // Assert
        isEqualTo(gizmo.getSizeInPixels(), ReferenceFrameGizmo.DEFAULT_SIZE_IN_PIXELS);
        isEqualTo(gizmo.getAxisLength(), ReferenceFrameGizmo.DEFAULT_AXIS_LENGTH);
        expect(() => gizmo.getAxisColor(3)).toThrow(IllegalArgumentException);
    });

    it("given_identityCameraRotation_when_estimateAxisDirection_then_followsGizmoConvention", () => {
        // Arrange
        let gizmo = new ReferenceFrameGizmo();
        let identity = Matrix4x4d.identityMatrix();

        // Act
        let x = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_X, identity);
        let y = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_Y, identity);
        let z = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_Z, identity);

        // Assert
        assertVector(x, 0, 0, -1);
        assertVector(y, -1, 0, 0);
        assertVector(z, 0, 1, 0);
    });

    it("given_rotatedCamera_when_estimateOrientation_then_axesRemainOrthonormal", () => {
        // Arrange
        let gizmo = new ReferenceFrameGizmo();
        let cameraRotation = new Matrix4x4d().eulerAnglesRotation(JavaMath.toRadians(35), JavaMath.toRadians(-20), 0);

        // Act
        let x = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_X, cameraRotation);
        let y = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_Y, cameraRotation);
        let z = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_Z, cameraRotation);

        // Assert
        closeTo(x.dotProduct(y), 0, EPS);
        closeTo(y.dotProduct(z), 0, EPS);
        closeTo(x.dotProduct(z), 0, EPS);
        closeTo(x.crossProduct(y).dotProduct(z), 1, EPS);
    });

    it("given_screenResolutions_when_applyScale_then_sizeAndLineWidthFollowThem", () => {
        // Arrange
        let legacy = new ReferenceFrameGizmo();
        let large = new ReferenceFrameGizmo();
        let legacyScaler = new ViewportElementScaler();
        let largeScaler = new ViewportElementScaler();
        legacyScaler.setScreenResolution(1024, 768);
        largeScaler.setScreenResolution(2560, 1600);

        // Act
        legacy.applyScale(legacyScaler);
        large.applyScale(largeScaler);

        // Assert
        isEqualTo(legacy.getSizeInPixels(), 64);
        closeTo(legacy.getLineWidth(), 1.0, EPS);
        isEqualTo(large.getSizeInPixels(), 128);
        closeTo(large.getLineWidth(), 2.0, EPS);
    });

    it("given_axisAndLineWidth_when_buildAxisStrip_then_isRectangleOfThatWidth", () => {
        // Arrange
        let gizmo = new ReferenceFrameGizmo();
        gizmo.setSizeInPixels(100);
        gizmo.setLineWidth(4);
        let identity = Matrix4x4d.identityMatrix();

        // Act
        let strip = gizmo.buildAxisStrip(ReferenceFrameGizmo.AXIS_Y, identity);

        // Assert: 4 pixels of width are 4/50 canonical units (area of 100 pixels covers [-1, 1])
        expect(sizeOf(strip)).toBe(4);
        closeTo(Vector3Dd.distance(strip[0], strip[1]), 0.08, EPS);
        closeTo(Vector3Dd.distance(strip[2], strip[3]), 0.08, EPS);
        closeTo(strip[0].z(), strip[1].z(), EPS);
    });

    it("given_axisPointingToViewer_when_buildAxisStrip_then_isSquareDot", () => {
        // Arrange: with the identity camera rotation, the Y axis points along -X and Z along +Y
        // (see given_identityCameraRotation...), so X points along -Z: to the viewer
        let gizmo = new ReferenceFrameGizmo();
        gizmo.setSizeInPixels(100);
        gizmo.setLineWidth(4);

        // Act
        let strip = gizmo.buildAxisStrip(ReferenceFrameGizmo.AXIS_X, Matrix4x4d.identityMatrix());

        // Assert
        closeTo(Vector3Dd.distance(strip[0], strip[1]), 0.08, EPS);
        closeTo(Math.hypot(strip[2].x() - strip[0].x(), strip[2].y() - strip[0].y()), 0.08, EPS);
    });

    function assertVector(v: Vector3Dd, x: number, y: number, z: number): void {
        closeTo(v.x(), x, EPS);
        closeTo(v.y(), y, EPS);
        closeTo(v.z(), z, EPS);
    }
});
