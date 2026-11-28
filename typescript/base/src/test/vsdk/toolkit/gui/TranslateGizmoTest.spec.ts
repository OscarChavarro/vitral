import { describe, expect, it } from "vitest";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { TranslateGizmo } from "vsdk/toolkit/gui/gizmo/TranslateGizmo.js";
import { TranslateGizmoLineSegment } from "vsdk/toolkit/gui/gizmo/TranslateGizmoLineSegment.js";
import { ViewportElementScaler } from "vsdk/toolkit/gui/viewport/ViewportElementScaler.js";

/**
Exercises the size, line width and line geometry of the translate gizmo,
independently of any rendering technology.
 */
describe("TranslateGizmoTest", () => {
    const EPS = 1.0e-6;

    function createPerspectiveCamera(): Camera {
        const camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(5, -6, 4));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    function createScaler(width: number, height: number): ViewportElementScaler {
        const scaler = new ViewportElementScaler();

        scaler.setScreenResolution(width, height);
        return scaler;
    }

    function closeTo(actual: number, expected: number, offset: number): void {
        expect(Math.abs(actual - expected)).toBeLessThanOrEqual(offset);
    }

    it("given_legacyResolution_when_applyScale_then_keepsDesignedSizeAndWidth", () => {
        // Arrange
        const gizmo = new TranslateGizmo(createPerspectiveCamera());

        // Act
        gizmo.applyScale(createScaler(1024, 768));

        // Assert
        expect(gizmo.getApparentSizeInPixels()).toBe(100);
        closeTo(gizmo.getLineWidth(), 1.0, EPS);
    });

    it("given_highResolution_when_applyScale_then_sizeAndWidthAreDoubled", () => {
        // Arrange
        const gizmo = new TranslateGizmo(createPerspectiveCamera());

        // Act
        gizmo.applyScale(createScaler(2560, 1600));

        // Assert
        expect(gizmo.getApparentSizeInPixels()).toBe(200);
        closeTo(gizmo.getLineWidth(), 2.0, EPS);
    });

    it("given_userChosenBaseSize_when_applyScaleRepeatedly_then_baseSizeIsKept", () => {
        // Arrange
        const gizmo = new TranslateGizmo(createPerspectiveCamera());
        const scaler = createScaler(2560, 1600);

        // Act
        gizmo.setBaseApparentSizeInPixels(150);
        gizmo.applyScale(scaler);
        gizmo.applyScale(scaler);

        // Assert
        expect(gizmo.getBaseApparentSizeInPixels()).toBe(150);
        expect(gizmo.getApparentSizeInPixels()).toBe(300);
    });

    it("given_invalidValues_when_set_then_areIgnored", () => {
        // Arrange
        const gizmo = new TranslateGizmo(createPerspectiveCamera());

        // Act
        gizmo.setLineWidth(0);
        gizmo.setLineWidth(-1);
        gizmo.setBaseApparentSizeInPixels(0);

        // Assert
        closeTo(gizmo.getLineWidth(), 1.0, EPS);
        expect(gizmo.getBaseApparentSizeInPixels()).toBe(100);
    });

    it("given_gizmoInView_when_gettingLines_then_axesAndPlaneSegmentsAreGiven", () => {
        // Arrange
        const camera = createPerspectiveCamera();
        const gizmo = new TranslateGizmo(camera);

        // Act
        gizmo.setTransformationMatrix(new Matrix4x4d());
        const lines = gizmo.getLineSegments();

        // Assert: 3 axis shafts and 6 plane handle segments
        expect(lines).toHaveLength(9);
        for (const line of lines) {
            expect(Vector3Dd.distance(line.start(), line.end())).toBeGreaterThan(0.0);
        }
    });

    it("given_lineWidthInPixels_when_buildingStrip_then_widthMatchesPixelsAndFacesCamera", () => {
        // Arrange
        const camera = createPerspectiveCamera();
        const gizmo = new TranslateGizmo(camera);

        gizmo.applyScale(createScaler(2560, 1600));
        gizmo.setTransformationMatrix(new Matrix4x4d());
        const line = gizmo.getLineSegments()[0]!;

        // Act
        const strip = gizmo.buildLineStrip(line)!;

        // Assert
        const across = strip[0]!.subtract(strip[1]!);
        const along = line.end().subtract(line.start()).normalized();
        const pixelsPerUnit = gizmo.getApparentSizeInPixels() / gizmo.getCurrentScale();
        const toEye = camera.getPosition().subtract(line.start());

        expect(strip).toHaveLength(4);
        closeTo(across.length() * pixelsPerUnit, gizmo.getLineWidth(), EPS);
        closeTo(across.dotProduct(along), 0.0, EPS);
        closeTo(across.normalized().dotProduct(toEye.normalized()), 0.0, EPS);
    });

    it("given_zeroLengthLine_when_buildingStrip_then_returnsNull", () => {
        // Arrange
        const gizmo = new TranslateGizmo(createPerspectiveCamera());
        gizmo.setTransformationMatrix(new Matrix4x4d());
        const line = gizmo.getLineSegments()[0]!;

        // Act
        const degenerate = new TranslateGizmoLineSegment(line.start(), line.start(), line.color());
        const strip = gizmo.buildLineStrip(degenerate);

        // Assert
        expect(strip).toBeNull();
    });
});
