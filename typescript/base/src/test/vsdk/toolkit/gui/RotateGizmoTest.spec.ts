import { describe, expect, it } from "vitest";
import { Double } from "java/lang/Double.js";
import { ColorRgb } from "vsdk/toolkit/common/color/ColorRgb.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RotateGizmo } from "vsdk/toolkit/gui/gizmo/RotateGizmo.js";
import { ViewportElementScaler } from "vsdk/toolkit/gui/viewport/ViewportElementScaler.js";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { Math as JavaMath } from "java/lang/Math.js";

/**
Exercises the size, ring geometry, Torus based model, selection and numeric
input of the rotate gizmo, independently of any rendering technology.
 */
describe("RotateGizmoTest", () => {
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

    function createCamera(distanceFactor: number): Camera {
        let camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(5, -6, 4).multiply(distanceFactor));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    function createScaler(width: number, height: number): ViewportElementScaler {
        let scaler = new ViewportElementScaler();

        scaler.setScreenResolution(width, height);
        return scaler;
    }

    function createGizmo(camera: Camera): RotateGizmo {
        let gizmo = new RotateGizmo(camera);

        gizmo.setTransformationMatrix(new Matrix4x4d());
        return gizmo;
    }

    /**
    @return a point of the ring of the gizmo (in its frame, seen from its
    camera), at the given angle around its axis
    */
    function pointOnRing(gizmo: RotateGizmo, ring: number, degrees: number): Vector3Dd {
        let axes = [new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1)];
        let u = axes[(ring + 1) % 3];
        let v = axes[(ring + 2) % 3];
        let angle = JavaMath.toRadians(degrees);

        return gizmo
            .getPosition()
            .add(
                u
                    .multiply(Math.cos(angle) * gizmo.getRingRadius())
                    .add(v.multiply(Math.sin(angle) * gizmo.getRingRadius())),
            );
    }

    function rayThrough(camera: Camera, point: Vector3Dd): Ray {
        let pixel = camera.projectPointUsingRayMethod(point);

        return camera.generateRay(javaRound(pixel.x()), javaRound(pixel.y()));
    }

    //= Size and width ====================================================

    it("given_legacyResolution_when_applyScale_then_keepsDesignedSizeAndWidths", () => {
        // Arrange
        let gizmo = new RotateGizmo(createCamera(1));

        // Act
        gizmo.applyScale(createScaler(1024, 768));

        // Assert
        isEqualTo(gizmo.getApparentSizeInPixels(), 100);
        for (let ring = 0; ring < 3; ring++) {
            closeTo(gizmo.getRingLineWidth(ring), RotateGizmo.DEFAULT_LINE_WIDTH, EPS);
        }
    });

    it("given_highResolution_when_applyScale_then_sizeAndWidthsAreDoubled", () => {
        // Arrange
        let gizmo = new RotateGizmo(createCamera(1));

        // Act
        gizmo.setBaseRingLineWidth(1, 3.0);
        gizmo.applyScale(createScaler(2560, 1600));

        // Assert
        isEqualTo(gizmo.getApparentSizeInPixels(), 200);
        closeTo(gizmo.getRingLineWidth(0), 2 * RotateGizmo.DEFAULT_LINE_WIDTH, EPS);
        closeTo(gizmo.getRingLineWidth(1), 6.0, EPS);
        closeTo(gizmo.getRingLineWidth(2), 2 * RotateGizmo.DEFAULT_LINE_WIDTH, EPS);
    });

    it("given_userChosenBaseSize_when_applyScaleRepeatedly_then_baseSizeIsKept", () => {
        // Arrange
        let gizmo = new RotateGizmo(createCamera(1));
        let scaler = createScaler(2560, 1600);

        // Act
        gizmo.setBaseApparentSizeInPixels(150);
        gizmo.applyScale(scaler);
        gizmo.applyScale(scaler);

        // Assert
        isEqualTo(gizmo.getBaseApparentSizeInPixels(), 150);
        isEqualTo(gizmo.getApparentSizeInPixels(), 300);
    });

    it("given_invalidValues_when_set_then_areIgnored", () => {
        // Arrange
        let gizmo = new RotateGizmo(createCamera(1));

        // Act
        gizmo.setRingLineWidth(0, 0);
        gizmo.setRingLineWidth(0, -1);
        gizmo.setBaseRingLineWidth(0, 0);
        gizmo.setBaseApparentSizeInPixels(0);

        // Assert
        closeTo(gizmo.getRingLineWidth(0), RotateGizmo.DEFAULT_LINE_WIDTH, EPS);
        closeTo(gizmo.getBaseRingLineWidth(0), RotateGizmo.DEFAULT_LINE_WIDTH, EPS);
        isEqualTo(gizmo.getBaseApparentSizeInPixels(), 100);
    });

    it("given_invalidRing_when_asked_then_isRejected", () => {
        // Arrange
        let gizmo = new RotateGizmo(createCamera(1));

        // Act & Assert
        expect(() => gizmo.getRingLineWidth(3)).toThrow(IllegalArgumentException);
    });

    it("given_farAndNearCameras_when_transformationSet_then_ringsKeepTheirApparentSizeInPixels", () => {
        for (const distanceFactor of [0.5, 1, 20]) {
            // Arrange
            let camera = createCamera(distanceFactor);
            let gizmo = createGizmo(camera);

            // Act: pixel distance from the center to a point of a ring
            let center = camera.projectPointUsingRayMethod(gizmo.getPosition());
            let onRing = camera.projectPointUsingRayMethod(
                new Vector3Dd(gizmo.getRingRadius(), 0, 0).add(gizmo.getPosition()),
            );

            // Assert: the world radius is about 0.8 times the apparent size,
            // whatever the distance (perspective distorts it a little)
            expect(Vector3Dd.distance(center, onRing)).toBeGreaterThanOrEqual(50.0);
            expect(Vector3Dd.distance(center, onRing)).toBeLessThanOrEqual(110.0);
        }
    });

    //= Torus based model =================================================

    it("given_gizmo_when_transformationSet_then_ringsAreToriOfTheGizmoRadiusAroundTheirAxes", () => {
        // Arrange
        let frame = RotateGizmo.createRotationFromAnglesInDegrees(30, 20, 10).withTranslation(new Vector3Dd(1, 2, 3));
        let gizmo = new RotateGizmo(createCamera(1));

        // Act
        gizmo.setTransformationMatrix(frame);

        // Assert
        for (let ring = 0; ring < 3; ring++) {
            let torus = gizmo.getRingModel(ring);
            let axisOfTorus = gizmo
                .getRingInstance(ring)
                .getRotation()
                .multiply(new Vector3Dd(0, 0, 1));

            closeTo(torus.getMajorRadius(), gizmo.getRingRadius(), EPS);
            expect(torus.getMinorRadius()).toBeGreaterThan(0.0);
            expect(torus.getMinorRadius()).toBeLessThan(torus.getMajorRadius());
            expect(Vector3Dd.distance(axisOfTorus, gizmo.getAxisDirection(ring))).toBeLessThan(EPS);
            expect(Vector3Dd.distance(gizmo.getRingInstance(ring).getPosition(), new Vector3Dd(1, 2, 3))).toBeLessThan(
                EPS,
            );
        }
        // Axes of the rings are the axes of the frame
        expect(
            Vector3Dd.distance(gizmo.getAxisDirection(0), frame.withoutTranslation().multiply(new Vector3Dd(1, 0, 0))),
        ).toBeLessThan(EPS);
    });

    it("given_thickRing_when_transformationSet_then_tubeCoversItsWidth", () => {
        // Arrange
        let gizmo = createGizmo(createCamera(1));

        // Act
        gizmo.setRingLineWidth(1, 60);
        gizmo.updateGeometryState();

        // Assert
        expect(gizmo.getRingModel(1).getMinorRadius()).toBeGreaterThanOrEqual(
            gizmo.getRingLineWidthInWorldUnits(1) / 2 - EPS,
        );
        expect(gizmo.getRingModel(1).getMinorRadius()).toBeGreaterThan(gizmo.getRingModel(0).getMinorRadius());
    });

    /**
    Independent oracle for picking: the smallest distance between a ray and the
    center line of a ring of a gizmo in the identity frame, found by sampling
    the ring.
    */
    function distanceFromRayToRing(ray: Ray, gizmo: RotateGizmo, ring: number): number {
        let axes = [new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1)];
        let u = axes[(ring + 1) % 3].multiply(gizmo.getRingRadius());
        let v = axes[(ring + 2) % 3].multiply(gizmo.getRingRadius());
        let direction = ray.getDirection().normalized();
        let best = Double.MAX_VALUE;
        let samples = 4000;

        for (let i = 0; i < samples; i++) {
            let angle = (2 * Math.PI * i) / samples;
            let w = u
                .multiply(Math.cos(angle))
                .add(v.multiply(Math.sin(angle)))
                .subtract(ray.getOrigin());

            best = Math.min(best, w.subtract(direction.multiply(w.dotProduct(direction))).length());
        }
        return best;
    }

    /**
    Same oracle as `distanceFromRayToRing`, for the camera ring (in the
    identity frame): the smallest distance between a ray and its centerline.
    */
    function distanceFromRayToCameraRing(ray: Ray, gizmo: RotateGizmo): number {
        let u = gizmo.getCameraPlaneRightDirection().multiply(gizmo.getCameraRingRadius());
        let v = gizmo.getCameraPlaneUpDirection().multiply(gizmo.getCameraRingRadius());
        let direction = ray.getDirection().normalized();
        let best = Double.MAX_VALUE;
        let samples = 4000;

        for (let i = 0; i < samples; i++) {
            let angle = (2 * Math.PI * i) / samples;
            let w = u
                .multiply(Math.cos(angle))
                .add(v.multiply(Math.sin(angle)))
                .subtract(ray.getOrigin());

            best = Math.min(best, w.subtract(direction.multiply(w.dotProduct(direction))).length());
        }
        return best;
    }

    /**
    Independent oracle for the visibility clipping: whether a point (assumed
    on the sphere that contains the rings, centered at the origin) faces the
    camera, the same way `RotateGizmo.isPointOnVisibleHemisphere` defines it.
    */
    function visibilityMetric(camera: Camera, point: Vector3Dd): number {
        let normal = point.normalized();
        let viewDirection =
            camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL
                ? camera.getFront()
                : point.subtract(camera.getPosition()).normalized();

        return normal.dotProduct(viewDirection);
    }

    /**
    @return the visibility metric (see `visibilityMetric`) of the point of a
    ring (in the identity frame, i.e. its position is the origin) nearest to
    the ray, found by `distanceFromRayToRing`; negative if it can be picked
    */
    function nearestRingPointVisibilityMetric(ray: Ray, gizmo: RotateGizmo, camera: Camera, ring: number): number {
        let axes = [new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1)];
        let u = axes[(ring + 1) % 3].multiply(gizmo.getRingRadius());
        let v = axes[(ring + 2) % 3].multiply(gizmo.getRingRadius());
        let direction = ray.getDirection().normalized();
        let best = Double.MAX_VALUE;
        let bestPoint = null;
        let samples = 4000;

        for (let i = 0; i < samples; i++) {
            let angle = (2 * Math.PI * i) / samples;
            let point = u.multiply(Math.cos(angle)).add(v.multiply(Math.sin(angle)));
            let w = point.subtract(ray.getOrigin());
            let distance = w.subtract(direction.multiply(w.dotProduct(direction))).length();

            if (distance < best) {
                best = distance;
                bestPoint = point;
            }
        }
        return visibilityMetric(camera, bestPoint);
    }

    // Java runs this oracle in its regular suite (no @Tag("slow")); its
    // ~280M temporary vectors take about two minutes in V8, hence the timeout.
    // Yielding after each pixel row keeps the vitest worker RPC responsive.
    it("given_gridOfRays_when_picked_then_agreesWithTheDistanceToTheRings", async () => {
        let checked = 0;
        let hits = 0;

        for (const distanceFactor of [0.5, 1, 6, 40]) {
            let camera = createCamera(distanceFactor);
            let gizmo = createGizmo(camera);
            let tube = gizmo.getRingModel(0).getMinorRadius();

            for (let y = 50; y < 350; y += 6) {
                for (let x = 50; x < 350; x += 6) {
                    // Arrange
                    let ray = camera.generateRay(x, y);
                    let surelyHit = new Array<boolean>(4).fill(false);
                    let anyHit = false;
                    let ambiguous = false;

                    for (let ring = 0; ring < 3; ring++) {
                        let distance = distanceFromRayToRing(ray, gizmo, ring);
                        let metric = nearestRingPointVisibilityMetric(ray, gizmo, camera, ring);

                        // Rays grazing a tube, or whose nearest point of the
                        // ring is close to the visible/hidden silhouette, may
                        // go either way (the picking uses the actual ray hit,
                        // this oracle the nearest point of the ring's centerline)
                        ambiguous = (distance > 0.9 * tube && distance < 1.1 * tube) || ambiguous;
                        ambiguous = Math.abs(metric) < 0.25 || ambiguous;
                        surelyHit[ring] = distance <= 0.9 * tube && metric < 0;
                        anyHit = surelyHit[ring] || anyHit;
                    }

                    let cameraDistance = distanceFromRayToCameraRing(ray, gizmo);
                    let cameraTube = gizmo.getCameraRingModel().getMinorRadius();

                    ambiguous = (cameraDistance > 0.9 * cameraTube && cameraDistance < 1.1 * cameraTube) || ambiguous;
                    surelyHit[3] = cameraDistance <= 0.9 * cameraTube;
                    anyHit = surelyHit[3] || anyHit;
                    if (ambiguous) {
                        continue;
                    }

                    // Act
                    let picked = gizmo.pickRing(ray);

                    // Assert

                    checked++;
                    if (anyHit) {
                        hits++;
                        isNotEqualTo(picked, RotateGizmo.NULL_GROUP);
                        expect(surelyHit[picked - 1]).toBe(true);
                    } else {
                        isEqualTo(picked, RotateGizmo.NULL_GROUP);
                    }
                }
                await new Promise((resolve) => setTimeout(resolve, 0));
            }
        }
        // The grid really has both rays that hit rings and rays that miss them
        expect(checked).toBeGreaterThan(1000);
        expect(hits).toBeGreaterThan(50);
        expect(checked - hits).toBeGreaterThan(1000);
    }, 300_000);

    it("given_raysToVisiblePointsOfEachRing_when_picked_then_aRingIsAlwaysFound", () => {
        for (const distanceFactor of [0.5, 1, 6, 40]) {
            let camera = createCamera(distanceFactor);
            let gizmo = createGizmo(camera);

            for (let ring = 0; ring < 3; ring++) {
                for (let degrees = 5; degrees < 360; degrees += 15) {
                    // Arrange
                    let point = pointOnRing(gizmo, ring, degrees);

                    // The far half of a ring is deliberately not pickable
                    if (!gizmo.isPointOnVisibleHemisphere(point)) {
                        continue;
                    }

                    let ray = rayThrough(camera, point);

                    // Act
                    let picked = gizmo.pickRing(ray);

                    // Assert: the ring pointed at, or another one in front of it
                    isNotEqualTo(picked, RotateGizmo.NULL_GROUP);
                    if (picked !== RotateGizmo.CAMERA_RING_GROUP) {
                        expect(distanceFromRayToRing(ray, gizmo, picked - 1)).toBeLessThanOrEqual(
                            gizmo.getRingModel(picked - 1).getMinorRadius(),
                        );
                    }
                }
            }
        }
    });

    it("given_rayFarFromRings_when_picked_then_nothingIsFound", () => {
        // Arrange
        let camera = createCamera(1);
        let gizmo = createGizmo(camera);

        // Act & Assert
        isEqualTo(gizmo.pickRing(camera.generateRay(2, 2)), RotateGizmo.NULL_GROUP);
    });

    it("given_rotatedGizmoAndPosition_when_picked_then_ringsFollowTheFrame", () => {
        // Arrange
        let camera = createCamera(1);
        let gizmo = new RotateGizmo(camera);

        gizmo.setTransformationMatrix(
            RotateGizmo.createRotationFromAnglesInDegrees(40, 25, -15).withTranslation(new Vector3Dd(0.5, -0.5, 0)),
        );

        // Act & Assert: points of the ring, built from the axes of the frame
        let rotation = gizmo.getTransformationMatrix().withoutTranslation();
        let u = rotation.multiply(new Vector3Dd(0, 0, 1));
        let v = rotation.multiply(new Vector3Dd(1, 0, 0));
        let point = gizmo
            .getPosition()
            .add(u.multiply(Math.cos(1.0) * gizmo.getRingRadius()))
            .add(v.multiply(Math.sin(1.0) * gizmo.getRingRadius()));

        isEqualTo(gizmo.pickRing(rayThrough(camera, point)), RotateGizmo.Y_RING_GROUP);
    });

    //= Strips of quads ===================================================

    it("given_gizmo_when_buildingRingStrips_then_onlyTheVisibleHalfIsAribbonAroundTheRing", () => {
        // Arrange
        let camera = createCamera(1);
        let gizmo = createGizmo(camera);

        for (let ring = 0; ring < 3; ring++) {
            // Act
            let arcs = gizmo.buildRingStrips(ring);

            // Assert: at least one arc, and not the whole ring (it is clipped)
            expect(sizeOf(arcs)).toBeGreaterThan(0);
            let totalPairs = 0;

            for (const strip of arcs) {
                isEqualTo(strip.length % 2, 0);
                totalPairs += strip.length / 2;
                for (let i = 0; i < strip.length; i += 2) {
                    let middle = strip[i].add(strip[i + 1]).multiply(0.5);
                    let fromCenter = middle.subtract(gizmo.getPosition());

                    closeTo(fromCenter.length(), gizmo.getRingRadius(), 1.0e-3);
                    closeTo(fromCenter.dotProduct(gizmo.getAxisDirection(ring)), 0.0, EPS);
                }
            }
            expect(totalPairs).toBeLessThan(RotateGizmo.RING_SEGMENTS + 1);
            expect(totalPairs).toBeGreaterThan(RotateGizmo.RING_SEGMENTS / 4);
        }
    });

    it("given_ringWidthsInPixels_when_buildingStrips_then_widthsAreDifferentAndMatchPixels", () => {
        // Arrange
        let camera = createCamera(1);
        let gizmo = createGizmo(camera);

        gizmo.setRingLineWidth(0, 2);
        gizmo.setRingLineWidth(1, 8);
        gizmo.setRingLineWidth(2, 4);
        gizmo.updateGeometryState();

        // Act: the width of the ribbon is the same all around a ring, so any
        // vertex pair (of the first visible arc) is a valid sample of it
        let widthsInWorld = new Array<number>(3).fill(0);
        let widthsInPixels = new Array<number>(3).fill(0);

        for (let ring = 0; ring < 3; ring++) {
            let strip = gizmo.buildRingStrips(ring)[0]!;
            let a = strip[0];
            let b = strip[1];
            let middle = a.add(b).multiply(0.5);
            let pixelA = camera.projectPointUsingRayMethod(a);
            let pixelB = camera.projectPointUsingRayMethod(b);

            widthsInWorld[ring] = Vector3Dd.distance(a, b);
            widthsInPixels[ring] = Vector3Dd.distance(pixelA, pixelB);
            closeTo(widthsInWorld[ring], gizmo.getRingLineWidthInWorldUnits(ring), EPS);
            expect(camera.projectPointUsingRayMethod(middle)).not.toBeNull();
        }

        // Assert: 8 px ring is four times wider than the 2 px one
        closeTo(widthsInWorld[1] / widthsInWorld[0], 4.0, EPS);
        closeTo(widthsInWorld[2] / widthsInWorld[0], 2.0, EPS);
        // ... and it looks as wide as asked (perspective distorts it a little)
        expect(widthsInPixels[1]).toBeGreaterThanOrEqual(5.0);
        expect(widthsInPixels[1]).toBeLessThanOrEqual(11.0);
        expect(widthsInPixels[0]).toBeGreaterThanOrEqual(1.0);
        expect(widthsInPixels[0]).toBeLessThanOrEqual(3.0);
    });

    it("given_orthogonalCamera_when_buildingRingStrips_then_ribbonFacesTheCamera", () => {
        // Arrange
        let camera = createCamera(1);

        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.updateVectors();
        let gizmo = createGizmo(camera);

        // Act & Assert: the width direction is never the direction of view
        for (const strip of gizmo.buildRingStrips(2)) {
            for (let i = 0; i < strip.length; i += 2) {
                let across = strip[i].subtract(strip[i + 1]);

                expect(across.length()).toBeGreaterThan(0.0);
                expect(Math.abs(across.normalized().dotProduct(camera.getFront().normalized()))).toBeLessThan(
                    1.0 - 1.0e-3,
                );
            }
        }
    });

    it("given_cameraLookingAlongAxis_when_buildingRingStrips_then_isNotDegenerate", () => {
        // Arrange
        let camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(0, 0, 10));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        let gizmo = createGizmo(camera);

        // Act: ring around the axis the camera looks along (seen face on, so
        // it is not clipped), and one seen edge-on (clipped, but not degenerate)
        let faceOn = gizmo.buildRingStrips(2);
        let edgeOn = gizmo.buildRingStrips(0);

        // Assert
        expect(sizeOf(faceOn)).toBe(1);
        expect(sizeOf(faceOn[0]!)).toBe(2 * (RotateGizmo.RING_SEGMENTS + 1));
        for (const arcs of [faceOn, edgeOn]) {
            for (const strip of arcs) {
                for (const v of strip) {
                    expect(Number.isNaN(v.x()) || Number.isNaN(v.y()) || Number.isNaN(v.z())).toBe(false);
                }
            }
        }
    });

    //= Camera ring ========================================================

    it("given_gizmo_when_buildingCameraRingStrip_then_isAWholeRibbonFacingTheCamera", () => {
        // Arrange
        let camera = createCamera(1);
        let gizmo = createGizmo(camera);

        // Act
        let strip = gizmo.buildCameraRingStrip();

        // Assert: whole (not clipped), at the camera ring radius, around the
        // camera axis, further out than the axis rings
        expect(sizeOf(strip)).toBe(2 * (RotateGizmo.RING_SEGMENTS + 1));
        expect(gizmo.getCameraRingRadius()).toBeGreaterThan(gizmo.getRingRadius());
        for (let i = 0; i < strip.length; i += 2) {
            let middle = strip[i].add(strip[i + 1]).multiply(0.5);
            let fromCenter = middle.subtract(gizmo.getPosition());

            closeTo(fromCenter.length(), gizmo.getCameraRingRadius(), 1.0e-3);
            closeTo(fromCenter.dotProduct(gizmo.getCameraAxisDirection()), 0.0, EPS);
        }
    });

    it("given_noSelection_when_askingCameraRingColor_then_isGray", () => {
        // Arrange
        let gizmo = createGizmo(createCamera(1));

        // Act & Assert
        closeTo(gizmo.getCameraRingColor().r(), gizmo.getCameraRingColor().g(), EPS);
        closeTo(gizmo.getCameraRingColor().g(), gizmo.getCameraRingColor().b(), EPS);

        // ... chosen: yellow
        gizmo.setPersistentSelection(RotateGizmo.CAMERA_RING_GROUP);
        isEqualTo(gizmo.getCameraRingColor(), new ColorRgb(1, 1, 0));
    });

    it("given_pointsOfTheCameraRing_when_picked_then_theCameraRingIsFound", () => {
        // Arrange
        let camera = createCamera(1);
        let gizmo = createGizmo(camera);

        for (let degrees = 5; degrees < 360; degrees += 15) {
            let u = gizmo.getCameraPlaneRightDirection();
            let v = gizmo.getCameraPlaneUpDirection();
            let angle = JavaMath.toRadians(degrees);
            let point = gizmo
                .getPosition()
                .add(u.multiply(Math.cos(angle) * gizmo.getCameraRingRadius()))
                .add(v.multiply(Math.sin(angle) * gizmo.getCameraRingRadius()));

            // Act & Assert: every point of the camera ring is pickable, as
            // it always faces the camera and is never clipped
            isEqualTo(gizmo.pickRing(rayThrough(camera, point)), RotateGizmo.CAMERA_RING_GROUP);
        }
    });

    //= Selection and colors ==============================================

    it("given_noSelection_when_askingColors_then_ringsHaveTheirRgbColors", () => {
        // Arrange
        let gizmo = createGizmo(createCamera(1));

        // Act & Assert
        isEqualTo(gizmo.getCurrentSelection(), RotateGizmo.NULL_GROUP);
        expect(gizmo.getRingColor(0).r()).toBeGreaterThan(gizmo.getRingColor(0).g());
        expect(gizmo.getRingColor(1).g()).toBeGreaterThan(gizmo.getRingColor(1).r());
        expect(gizmo.getRingColor(2).b()).toBeGreaterThan(gizmo.getRingColor(2).r());
    });

    it("given_hoverOrChosenRing_when_askingColors_then_thatRingIsYellow", () => {
        // Arrange
        let gizmo = createGizmo(createCamera(1));
        let yellow = new ColorRgb(1, 1, 0);

        // Act & Assert: cursor over the Y ring
        gizmo.setVolatileSelection(RotateGizmo.Y_RING_GROUP);
        isEqualTo(gizmo.getRingColor(1), yellow);
        isNotEqualTo(gizmo.getRingColor(0), yellow);
        isNotEqualTo(gizmo.getRingColor(2), yellow);

        // ... the cursor leaves: goes back to RGB
        gizmo.setVolatileSelection(RotateGizmo.NULL_GROUP);
        isNotEqualTo(gizmo.getRingColor(1), yellow);

        // ... but a chosen ring stays yellow
        gizmo.setPersistentSelection(RotateGizmo.Z_RING_GROUP);
        isEqualTo(gizmo.getRingColor(2), yellow);

        // ... and the cursor over another one takes over while it is there
        gizmo.setVolatileSelection(RotateGizmo.X_RING_GROUP);
        isEqualTo(gizmo.getRingColor(0), yellow);
        isNotEqualTo(gizmo.getRingColor(2), yellow);
    });

    //= Numeric input =====================================================

    it("given_orientation_when_inputGizmoAsked_then_showsDegreesWithTwoDecimals", () => {
        // Arrange
        let gizmo = new RotateGizmo(createCamera(1));

        gizmo.setTransformationMatrix(RotateGizmo.createRotationFromAnglesInDegrees(30.5, -20.25, 10));

        // Act
        let input = gizmo.getInputGizmo();

        // Assert: 3 axis angles plus the (always 0) relative camera field
        isEqualTo(input.getNumberOfFields(), 4);
        isEqualTo(input.getDecimals(), 2);
        isEqualTo(input.getDisplayText(0), "30.50");
        isEqualTo(input.getDisplayText(1), "-20.25");
        isEqualTo(input.getDisplayText(2), "10.00");
        isEqualTo(input.getDisplayText(3), "0.00");
        isEqualTo(input.getReferenceText(), "-000.00");
    });

    it("given_highlightedRing_when_inputGizmoAsked_then_itsFieldIsHighlighted", () => {
        // Arrange
        let gizmo = createGizmo(createCamera(1));

        gizmo.setVolatileSelection(RotateGizmo.Z_RING_GROUP);

        // Act
        let input = gizmo.getInputGizmo();

        // Assert
        expect(input.isFieldHighlighted(0)).toBe(false);
        expect(input.isFieldHighlighted(1)).toBe(false);
        expect(input.isFieldHighlighted(2)).toBe(true);
        isEqualTo(input.getFieldColor(0), gizmo.getRingColor(0));
    });

    it("given_angles_when_convertedToRotationAndBack_then_areTheSame", () => {
        let cases = [
            [0, 0, 0],
            [45, 0, 0],
            [0, 45, 0],
            [0, 0, 45],
            [30, 20, 10],
            [-170, 60, 170],
            [120, -80, -100],
            [10, 89.5, 20],
        ];

        for (const angles of cases) {
            // Act
            let rotation = RotateGizmo.createRotationFromAnglesInDegrees(angles[0], angles[1], angles[2]);
            let back = RotateGizmo.extractAnglesInDegrees(rotation);

            // Assert
            closeTo(back[0], angles[0], 1.0e-6);
            closeTo(back[1], angles[1], 1.0e-6);
            closeTo(back[2], angles[2], 1.0e-6);
        }
    });

    it("given_gimbalLock_when_anglesExtracted_then_theyGiveTheSameOrientation", () => {
        let cases = [
            [25, 90, 40],
            [-70, -90, 15],
        ];

        for (const angles of cases) {
            // Arrange
            let rotation = RotateGizmo.createRotationFromAnglesInDegrees(angles[0], angles[1], angles[2]);

            // Act
            let back = RotateGizmo.extractAnglesInDegrees(rotation);
            let rebuilt = RotateGizmo.createRotationFromAnglesInDegrees(back[0], back[1], back[2]);

            // Assert
            expect(rebuilt.epsilonEquals(rotation, 1.0e-9)).toBe(true);
        }
    });

    it("given_angleMoreThanHalfTurn_when_anglesExtracted_then_equivalentOrientationIsGiven", () => {
        // Arrange
        let rotation = RotateGizmo.createRotationFromAnglesInDegrees(200, 0, 0);

        // Act
        let back = RotateGizmo.extractAnglesInDegrees(rotation);

        // Assert
        closeTo(back[0], -160.0, 1.0e-6);
        expect(
            RotateGizmo.createRotationFromAnglesInDegrees(back[0], back[1], back[2]).epsilonEquals(rotation, 1.0e-9),
        ).toBe(true);
    });

    //= Rotation arc ======================================================

    it("given_noArc_when_asked_then_thereIsNothingToShow", () => {
        // Arrange
        let gizmo = createGizmo(createCamera(1));

        // Act & Assert
        expect(gizmo.isArcVisible()).toBe(false);
        isEqualTo(gizmo.getArcRing(), -1);
        expect(sizeOf(gizmo.buildArcFan())).toBe(0);
        expect(gizmo.getArcLabelPosition()).toBeNull();
    });

    it("given_arc_when_fanBuilt_then_isASectorOfTheRingRadiusAroundTheCenter", () => {
        // Arrange
        let gizmo = new RotateGizmo(createCamera(1));

        gizmo.setTransformationMatrix(new Matrix4x4d().withTranslation(new Vector3Dd(1, 2, 3)));
        let u = new Vector3Dd(0, 0, 1);
        let v = new Vector3Dd(1, 0, 0);

        // Act: an arc of 60 degrees around the Y axis, that starts at 30
        gizmo.setArc(1, u, v, JavaMath.toRadians(30), JavaMath.toRadians(60));
        let fan = gizmo.buildArcFan();

        // Assert
        expect(gizmo.isArcVisible()).toBe(true);
        isEqualTo(fan.length, 2 + 20);
        expect(Vector3Dd.distance(fan[0], new Vector3Dd(1, 2, 3))).toBeLessThan(EPS);
        for (let i = 1; i < fan.length; i++) {
            let fromCenter = fan[i].subtract(fan[0]);

            closeTo(fromCenter.length(), gizmo.getRingRadius(), EPS);
            closeTo(fromCenter.y(), 0.0, EPS);
        }
        let first = fan[1].subtract(fan[0]);
        let last = fan[fan.length - 1].subtract(fan[0]);
        let firstAngle = JavaMath.toDegrees(Math.atan2(first.dotProduct(v), first.dotProduct(u)));
        let lastAngle = JavaMath.toDegrees(Math.atan2(last.dotProduct(v), last.dotProduct(u)));

        closeTo(firstAngle, 30.0, 1.0e-6);
        closeTo(lastAngle, 90.0, 1.0e-6);
    });

    it("given_arcOfMoreThanATurn_when_fanBuilt_then_isAWholeDisc", () => {
        // Arrange
        let gizmo = createGizmo(createCamera(1));

        gizmo.setArc(2, new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), 0, JavaMath.toRadians(1000));

        // Act
        let fan = gizmo.buildArcFan();

        // Assert
        isEqualTo(fan.length, 2 + 120);
        expect(Vector3Dd.distance(fan[1], fan[fan.length - 1])).toBeLessThan(EPS);
        closeTo(gizmo.getArcSweepInDegrees(), 1000.0, 1.0e-6);
    });

    it("given_arcWithNoAngle_when_fanBuilt_then_thereIsNothingToDraw", () => {
        // Arrange
        let gizmo = createGizmo(createCamera(1));

        gizmo.setArc(0, new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1), 1.0, 0.0);

        // Act & Assert
        expect(gizmo.isArcVisible()).toBe(true);
        expect(sizeOf(gizmo.buildArcFan())).toBe(0);
    });

    it("given_highlightedRing_when_arcColorAsked_then_isTheColorOfTheAxisNotYellow", () => {
        // Arrange
        let gizmo = createGizmo(createCamera(1));

        gizmo.setVolatileSelection(RotateGizmo.Y_RING_GROUP);
        gizmo.setArc(1, new Vector3Dd(0, 0, 1), new Vector3Dd(1, 0, 0), 0, 1.0);

        // Act & Assert
        isEqualTo(gizmo.getRingColor(1), new ColorRgb(1, 1, 0));
        isNotEqualTo(gizmo.getArcColor(), new ColorRgb(1, 1, 0));
        expect(gizmo.getArcColor().g()).toBeGreaterThan(gizmo.getArcColor().r());
    });

    it("given_arc_when_labelPositionAsked_then_isJustOutsideTheRingAtTheMiddleOfTheArc", () => {
        // Arrange
        let gizmo = createGizmo(createCamera(1));

        gizmo.setArc(2, new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), 0, JavaMath.toRadians(90));

        // Act
        let label = gizmo.getArcLabelPosition();

        // Assert: 45 degrees, farther than the ring
        closeTo(label.x(), label.y(), EPS);
        expect(label.length()).toBeGreaterThan(gizmo.getRingRadius());
        closeTo(label.z(), 0.0, EPS);
        gizmo.clearArc();
        expect(gizmo.getArcLabelPosition()).toBeNull();
    });
});
