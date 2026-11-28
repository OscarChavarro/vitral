import { describe, expect, it } from "vitest";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { InputGizmo } from "vsdk/toolkit/gui/gizmo/InputGizmo.js";
import { ScaleGizmo } from "vsdk/toolkit/gui/gizmo/ScaleGizmo.js";
import { ViewportElementScaler } from "vsdk/toolkit/gui/viewport/ViewportElementScaler.js";
import { KeyEvent } from "vsdk/toolkit/gui/KeyEvent.js";

/**
Exercises the size, geometric model, selection groups, picking and numeric
input of the scale gizmo, independently of any rendering technology.
 */
describe("ScaleGizmoTest", () => {
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

    function isNotEqualTo(actual: unknown, expected: unknown): void {
        const a = actual as { equals?: (other: unknown) => boolean } | null;
        if (a !== null && typeof a === "object" && typeof a.equals === "function") {
            expect(a.equals(expected)).toBe(false);
        } else {
            expect(actual).not.toEqual(expected);
        }
    }

    function sizeOf(collection: string | { length: number } | { size(): number }): number {
        return typeof collection === "string" || "length" in collection ? collection.length : collection.size();
    }

    /** Java `(int)Math.round(double)`. */
    function javaRound(value: number): number {
        return Math.floor(value + 0.5);
    }

    const EPS = 1.0e-6;

    function createCamera(): Camera {
        let camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(5, -6, 4));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    function createScaler(width: number, height: number): ViewportElementScaler {
        let scaler = new ViewportElementScaler();

        scaler.setScreenResolution(width, height);
        return scaler;
    }

    function createGizmo(camera: Camera): ScaleGizmo {
        let gizmo = new ScaleGizmo(camera);

        gizmo.setTransformationMatrix(new Matrix4x4d());
        return gizmo;
    }

    function rayThrough(camera: Camera, point: Vector3Dd): Ray {
        let pixel = camera.projectPointUsingRayMethod(point);

        return camera.generateRay(javaRound(pixel.x()), javaRound(pixel.y()));
    }

    /**
    @return true if the point is over the line of the axis (that is, its
    components over the other two axes of the frame are zero)
    */
    function isOverAxis(gizmo: ScaleGizmo, point: Vector3Dd, axis: number): boolean {
        for (let other = 0; other < 3; other++) {
            if (other !== axis && Math.abs(distanceToPlane(gizmo, point, other)) > EPS) {
                return false;
            }
        }
        return true;
    }

    /**
    @return the component of the point, relative to the origin of the gizmo,
    along the given axis: its distance to the plane of the other two axes
    */
    function distanceToPlane(gizmo: ScaleGizmo, point: Vector3Dd, axis: number): number {
        return point.subtract(gizmo.getPosition()).dotProduct(gizmo.getAxisDirection(axis));
    }

    /**
    @return the center of the trapezoidal band of the group, a point well
    inside it
    */
    function bandCenter(gizmo: ScaleGizmo, group: number): Vector3Dd {
        let quad = gizmo.buildBandQuad(group);

        return quad[0].add(quad[1]).add(quad[2]).add(quad[3]).multiply(0.25);
    }

    /**
    @return a ray that reaches the given point of the plane of the group
    perpendicularly to it, so no handle of another plane can be in between
    */
    function rayOntoPlaneOf(gizmo: ScaleGizmo, group: number, target: Vector3Dd): Ray {
        let axes = ScaleGizmo.bandAxes(group);
        let normal = gizmo.getAxisDirection(3 - axes[0] - axes[1]);

        return new Ray(target.add(normal.multiply(10)), normal.multiply(-1));
    }

    //= Size ================================================================

    it("given_legacyResolution_when_applyScale_then_keepsDesignedSize", () => {
        // Arrange
        let gizmo = new ScaleGizmo(createCamera());

        // Act
        gizmo.applyScale(createScaler(1024, 768));

        // Assert
        isEqualTo(gizmo.getApparentSizeInPixels(), 100);
    });

    it("given_highResolution_when_applyScale_then_sizeAndContourWidthAreDoubled", () => {
        // Arrange
        let gizmo = new ScaleGizmo(createCamera());

        // Act
        gizmo.applyScale(createScaler(2560, 1600));

        // Assert
        isEqualTo(gizmo.getApparentSizeInPixels(), 200);
        closeTo(gizmo.getLineWidth(), 2 * ScaleGizmo.DEFAULT_LINE_WIDTH, EPS);
    });

    it("given_invalidValues_when_set_then_areIgnored", () => {
        // Arrange
        let gizmo = new ScaleGizmo(createCamera());

        // Act
        gizmo.setBaseApparentSizeInPixels(0);
        gizmo.setBaseApparentSizeInPixels(-5);
        gizmo.setLineWidth(0);
        gizmo.setLineWidth(-1);

        // Assert
        isEqualTo(gizmo.getBaseApparentSizeInPixels(), ScaleGizmo.DEFAULT_APPARENT_SIZE_IN_PIXELS);
        closeTo(gizmo.getLineWidth(), ScaleGizmo.DEFAULT_LINE_WIDTH, EPS);
    });

    //= Geometry =============================================================

    it("given_gizmo_when_elementsAsked_then_thereAreSixOfThem", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());

        // Act & Assert: 3 cylinders and 3 cubes (the flat handles are
        // polygons, not instanced primitives)
        expect(sizeOf(gizmo.getElements())).toBe(6);
        for (const element of gizmo.getElements()) {
            expect(element.getGeometry()).not.toBeNull();
        }
    });

    it("given_gizmo_when_tipPositionsAsked_then_theyFormAnEquilateralTriangle", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());

        // Act
        let t0 = gizmo.getTipPosition(0);
        let t1 = gizmo.getTipPosition(1);
        let t2 = gizmo.getTipPosition(2);

        // Assert
        let d01 = Vector3Dd.distance(t0, t1);
        let d12 = Vector3Dd.distance(t1, t2);
        let d20 = Vector3Dd.distance(t2, t0);

        closeTo(d01, d12, EPS);
        closeTo(d12, d20, EPS);
        expect(d01).toBeGreaterThan(0);
    });

    it("given_gizmo_when_uniformTrianglesBuilt_then_theyShareTheOriginAndLieInThePlanes", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let origin = gizmo.getPosition();

        // Act
        let triangles = gizmo.buildUniformTriangles();

        // Assert: one triangle per plane, all of them with a vertex at the
        // origin of the frame and the other two over the axes of their plane
        expect(sizeOf(triangles)).toBe(9);
        for (let i = 0; i < ScaleGizmo.BAND_GROUPS.length; i++) {
            let group = ScaleGizmo.BAND_GROUPS[i];
            let axes = ScaleGizmo.bandAxes(group);
            let missingAxis = 3 - axes[0] - axes[1];

            isEqualTo(triangles[3 * i], origin);
            expect(isOverAxis(gizmo, triangles[3 * i + 1], axes[0])).toBe(true);
            expect(isOverAxis(gizmo, triangles[3 * i + 2], axes[1])).toBe(true);
            for (let v = 0; v < 3; v++) {
                closeTo(distanceToPlane(gizmo, triangles[3 * i + v], missingAxis), 0.0, EPS);
            }
        }
    });

    it("given_gizmo_when_bandsBuilt_then_theyAreTrapezoidsConfinedToTheirPlane", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());

        for (const group of ScaleGizmo.BAND_GROUPS) {
            let axes = ScaleGizmo.bandAxes(group);
            let missingAxis = 3 - axes[0] - axes[1];

            // Act
            let quad = gizmo.buildBandQuad(group);

            // Assert: 4 corners, two over each axis of the plane, and none of
            // them outside it
            expect(sizeOf(quad)).toBe(4);
            expect(isOverAxis(gizmo, quad[0], axes[0])).toBe(true);
            expect(isOverAxis(gizmo, quad[1], axes[0])).toBe(true);
            expect(isOverAxis(gizmo, quad[2], axes[1])).toBe(true);
            expect(isOverAxis(gizmo, quad[3], axes[1])).toBe(true);
            for (const corner of quad) {
                closeTo(distanceToPlane(gizmo, corner, missingAxis), 0.0, EPS);
            }

            // The parallel sides have different lengths (it is a trapezoid,
            // not a rectangle), and the inner one is the shorter
            let innerSide = Vector3Dd.distance(quad[0], quad[2]);
            let outerSide = Vector3Dd.distance(quad[1], quad[3]);

            expect(innerSide).toBeLessThan(outerSide);
        }
    });

    it("given_gizmo_when_uniformAndBandsBuilt_then_theyAreFlushAtTheInnerEdge", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let triangles = gizmo.buildUniformTriangles();

        for (let i = 0; i < ScaleGizmo.BAND_GROUPS.length; i++) {
            let group = ScaleGizmo.BAND_GROUPS[i];
            let quad = gizmo.buildBandQuad(group);

            // Act & Assert: the outer vertices of the uniform triangle of the
            // plane are the inner corners of its band, so they share that edge
            closeTo(Vector3Dd.distance(triangles[3 * i + 1], quad[0]), 0.0, EPS);
            closeTo(Vector3Dd.distance(triangles[3 * i + 2], quad[2]), 0.0, EPS);
        }
    });

    it("given_gizmo_when_proportionsMeasured_then_theyFollowTheReferenceGizmo", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let origin = gizmo.getPosition();
        let axisLength = Vector3Dd.distance(origin, gizmo.axisPoint(0, 1.0));
        let quad = gizmo.buildBandQuad(ScaleGizmo.XY_GROUP);

        // Act
        let innerReach = Vector3Dd.distance(origin, quad[0]) / axisLength;
        let outerReach = Vector3Dd.distance(origin, quad[1]) / axisLength;
        let tipReach = Vector3Dd.distance(origin, gizmo.getTipPosition(0)) / axisLength;

        // Assert: the bands sit around the middle of the axes, which go on
        // well beyond them up to the cube of the tip
        expect(innerReach).toBeGreaterThanOrEqual(0.45);
        expect(innerReach).toBeLessThanOrEqual(0.6);
        expect(outerReach).toBeGreaterThanOrEqual(0.65);
        expect(outerReach).toBeLessThanOrEqual(0.8);
        expect(tipReach).toBeGreaterThan(outerReach);
        expect(tipReach).toBeGreaterThanOrEqual(0.9);
        expect(tipReach).toBeLessThanOrEqual(1.0);
    });

    it("given_noSelection_when_contourBuilt_then_eachEdgeIsSplitInItsAxisColors", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());

        // Act: two edges (inner and outer) per plane, each one in two halves
        let segments = gizmo.buildContourSegments();

        // Assert
        expect(sizeOf(segments)).toBe(2 * 2 * ScaleGizmo.BAND_GROUPS.length);
        for (let i = 0; i < ScaleGizmo.BAND_GROUPS.length; i++) {
            let axes = ScaleGizmo.bandAxes(ScaleGizmo.BAND_GROUPS[i]);

            // The two halves of the inner edge meet at its middle point, and
            // each one has the color of the axis it ends at
            let first = segments[4 * i]!;
            let second = segments[4 * i + 1]!;

            isEqualTo(first.end(), second.start());
            isEqualTo(first.color(), gizmo.getAxisDisplayColor(axes[0]));
            isEqualTo(second.color(), gizmo.getAxisDisplayColor(axes[1]));
            isNotEqualTo(first.color(), InputGizmo.HIGHLIGHT_COLOR);
        }
    });

    it("given_selectedBand_when_contourBuilt_then_onlyItsEdgesAreYellow", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());

        gizmo.setPersistentSelection(ScaleGizmo.YZ_GROUP);

        // Act
        let segments = gizmo.buildContourSegments();
        let yellow = 0;

        for (const segment of segments) {
            if (segment.color().equals(InputGizmo.HIGHLIGHT_COLOR)) {
                yellow++;
            }
        }

        // Assert: the four halves of the two edges of the YZ band
        isEqualTo(yellow, 4);
    });

    it("given_selectedUniformHandle_when_contourBuilt_then_theThreeInnerEdgesAreYellow", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());

        gizmo.setPersistentSelection(ScaleGizmo.UNIFORM_GROUP);

        // Act
        let segments = gizmo.buildContourSegments();
        let yellow = 0;

        for (const segment of segments) {
            if (segment.color().equals(InputGizmo.HIGHLIGHT_COLOR)) {
                yellow++;
            }
        }

        // Assert: the two halves of the inner edge of each of the 3 planes
        // (which the uniform handle is flush with), and no outer edge
        isEqualTo(yellow, 6);
    });

    //= Selection groups ======================================================

    it("given_axisGroups_when_axisMembershipAsked_then_onlyTheirOwnAxisIsIncluded", () => {
        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.X_AXIS_GROUP, 0)).toBe(true);
        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.X_AXIS_GROUP, 1)).toBe(false);
        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.X_AXIS_GROUP, 2)).toBe(false);
    });

    it("given_twoAxisGroups_when_axisMembershipAsked_then_bothOfTheirAxesAreIncluded", () => {
        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XY_GROUP, 0)).toBe(true);
        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XY_GROUP, 1)).toBe(true);
        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XY_GROUP, 2)).toBe(false);

        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.YZ_GROUP, 1)).toBe(true);
        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.YZ_GROUP, 2)).toBe(true);
        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.YZ_GROUP, 0)).toBe(false);

        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XZ_GROUP, 0)).toBe(true);
        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XZ_GROUP, 2)).toBe(true);
        expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XZ_GROUP, 1)).toBe(false);
    });

    it("given_uniformGroup_when_axisMembershipAsked_then_everyAxisIsIncluded", () => {
        for (let axis = 0; axis < 3; axis++) {
            expect(ScaleGizmo.groupIncludesAxis(ScaleGizmo.UNIFORM_GROUP, axis)).toBe(true);
        }
    });

    it("given_selection_when_axisHighlightAsked_then_matchesGroupMembership", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());

        // Act
        gizmo.setPersistentSelection(ScaleGizmo.XY_GROUP);

        // Assert
        expect(gizmo.isAxisHighlighted(0)).toBe(true);
        expect(gizmo.isAxisHighlighted(1)).toBe(true);
        expect(gizmo.isAxisHighlighted(2)).toBe(false);
        expect(gizmo.isBandHighlighted(ScaleGizmo.XY_GROUP)).toBe(true);
        expect(gizmo.isBandHighlighted(ScaleGizmo.YZ_GROUP)).toBe(false);
        expect(gizmo.isUniformHighlighted()).toBe(false);
    });

    it("given_uniformSelection_when_axisHighlightAsked_then_everyAxisIsHighlighted", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());

        // Act
        gizmo.setPersistentSelection(ScaleGizmo.UNIFORM_GROUP);

        // Assert
        expect(gizmo.isAxisHighlighted(0)).toBe(true);
        expect(gizmo.isAxisHighlighted(1)).toBe(true);
        expect(gizmo.isAxisHighlighted(2)).toBe(true);
        expect(gizmo.isUniformHighlighted()).toBe(true);
    });

    it("given_volatileSelection_when_currentSelectionAsked_then_itTakesPriority", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());

        gizmo.setPersistentSelection(ScaleGizmo.X_AXIS_GROUP);

        // Act
        gizmo.setVolatileSelection(ScaleGizmo.Z_AXIS_GROUP);

        // Assert
        isEqualTo(gizmo.getCurrentSelection(), ScaleGizmo.Z_AXIS_GROUP);

        // Act: with no volatile selection, the persistent one applies again
        gizmo.setVolatileSelection(ScaleGizmo.NULL_GROUP);
        isEqualTo(gizmo.getCurrentSelection(), ScaleGizmo.X_AXIS_GROUP);
    });

    //= Picking ===============================================================

    it("given_rayThroughAnAxisTip_when_picked_then_selectsItsAxisGroup", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);

        // Act
        let ray = rayThrough(camera, gizmo.getTipPosition(0));
        let selection = gizmo.pickElement(ray);

        // Assert
        isEqualTo(selection, ScaleGizmo.X_AXIS_GROUP);
    });

    it("given_rayThroughEachBand_when_picked_then_selectsItsTwoAxisGroup", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);

        for (const group of ScaleGizmo.BAND_GROUPS) {
            // Act: perpendicular to the plane of the band, so no other
            // handle can be in between
            let ray = rayOntoPlaneOf(gizmo, group, bandCenter(gizmo, group));
            let selection = gizmo.pickElement(ray);

            // Assert
            isEqualTo(selection, group);
        }
    });

    it("given_rayThroughEachUniformTriangle_when_picked_then_selectsTheUniformGroup", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let triangles = gizmo.buildUniformTriangles();

        for (let i = 0; i + 2 < triangles.length; i += 3) {
            // The centroid of the triangle, not a vertex shared with the
            // other two (an unrealistic click, and a degenerate case for
            // ray-triangle intersection)
            let centroid = triangles[i]
                .add(triangles[i + 1])
                .add(triangles[i + 2])
                .multiply(1.0 / 3.0);

            // Act: perpendicular to the plane of the triangle
            let ray = rayOntoPlaneOf(gizmo, ScaleGizmo.BAND_GROUPS[i / 3], centroid);
            let selection = gizmo.pickElement(ray);

            // Assert
            isEqualTo(selection, ScaleGizmo.UNIFORM_GROUP);
        }
    });

    it("given_rayThroughEmptySpace_when_picked_then_selectsNothing", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);

        // Act: a corner of the viewport, away from the gizmo near the origin
        let ray = camera.generateRay(5, 5);
        let selection = gizmo.pickElement(ray);

        // Assert
        isEqualTo(selection, ScaleGizmo.NULL_GROUP);
    });

    //= Numeric input =========================================================

    it("given_scaleFactors_when_inputGizmoAsked_then_showsThem", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());

        gizmo.setScale(new Vector3Dd(2, 0.5, 1.25));

        // Act
        let inputGizmo = gizmo.getInputGizmo();

        // Assert
        closeTo(inputGizmo.getValue(0), 2.0, EPS);
        closeTo(inputGizmo.getValue(1), 0.5, EPS);
        closeTo(inputGizmo.getValue(2), 1.25, EPS);
    });

    it("given_digitsTyped_when_enterPressed_then_scaleFactorIsUpdated", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let digit = new KeyEvent();
        let enter = new KeyEvent();

        digit.unicodeId = "3".charCodeAt(0);
        enter.keycode = KeyEvent.KEY_ENTER;

        // Act
        gizmo.processKeyPressedEvent(digit);
        let changed = gizmo.processKeyPressedEvent(enter);

        // Assert
        expect(changed).toBe(true);
        closeTo(gizmo.getScale().x(), 3.0, EPS);
        closeTo(gizmo.getScale().y(), 1.0, EPS);
        closeTo(gizmo.getScale().z(), 1.0, EPS);
    });

    it("given_letterKeys_when_pressed_then_scaleSingleAxis", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let upperX = new KeyEvent();

        upperX.unicodeId = "X".charCodeAt(0);

        // Act
        let changed = gizmo.processKeyPressedEvent(upperX);

        // Assert
        expect(changed).toBe(true);
        closeTo(gizmo.getScale().x(), 1.1, EPS);
        closeTo(gizmo.getScale().y(), 1.0, EPS);
        closeTo(gizmo.getScale().z(), 1.0, EPS);
    });

    it("given_arrowKeys_when_pressedWithoutModifiers_then_theInputGizmoStepsInstead", () => {
        // Arrange: arrow keys are consumed by the input gizmo (selected box:
        // the X scale factor), not the old uniform scaling fallback
        let gizmo = createGizmo(createCamera());
        let up = new KeyEvent();

        up.keycode = KeyEvent.KEY_UP;

        // Act
        let changed = gizmo.processKeyPressedEvent(up);

        // Assert: UP is the level 2 step of InputGizmoValueChangeRules.forScale()
        expect(changed).toBe(true);
        closeTo(gizmo.getScale().x(), 1.1, EPS);
        closeTo(gizmo.getScale().y(), 1.0, EPS);
    });

    it("given_arrowKeyWithControl_when_pressed_then_scalesEveryAxisUniformly", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let up = new KeyEvent();

        up.keycode = KeyEvent.KEY_UP;
        up.modifierMask = KeyEvent.MASK_CTRL;

        // Act
        let changed = gizmo.processKeyPressedEvent(up);

        // Assert
        expect(changed).toBe(true);
        closeTo(gizmo.getScale().x(), 1.1, EPS);
        closeTo(gizmo.getScale().y(), 1.1, EPS);
        closeTo(gizmo.getScale().z(), 1.1, EPS);
    });
});
