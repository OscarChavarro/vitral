package vsdk.toolkit.gui;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.gui.gizmo.InputGizmo;
import vsdk.toolkit.gui.gizmo.RotateGizmo;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the size, ring geometry, Torus based model, selection and numeric
input of the rotate gizmo, independently of any rendering technology.
 */
class RotateGizmoTest
{
    private static final double EPS = 1.0e-6;

    private static Camera createCamera(double distanceFactor)
    {
        Camera camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(5, -6, 4).multiply(distanceFactor));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    private static ViewportElementScaler createScaler(int width, int height)
    {
        ViewportElementScaler scaler = new ViewportElementScaler();

        scaler.setScreenResolution(width, height);
        return scaler;
    }

    private static RotateGizmo createGizmo(Camera camera)
    {
        RotateGizmo gizmo = new RotateGizmo(camera);

        gizmo.setTransformationMatrix(new Matrix4x4d());
        return gizmo;
    }

    /**
    @return a point of the ring of the gizmo (in its frame, seen from its
    camera), at the given angle around its axis
    */
    private static Vector3Dd pointOnRing(RotateGizmo gizmo, int ring, double degrees)
    {
        Vector3Dd[] axes = {
            new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1)
        };
        Vector3Dd u = axes[(ring + 1) % 3];
        Vector3Dd v = axes[(ring + 2) % 3];
        double angle = Math.toRadians(degrees);

        return gizmo.getPosition().add(
            u.multiply(Math.cos(angle) * gizmo.getRingRadius()).add(
            v.multiply(Math.sin(angle) * gizmo.getRingRadius())));
    }

    private static Ray rayThrough(Camera camera, Vector3Dd point)
    {
        Vector3Dd pixel = camera.projectPointUsingRayMethod(point);

        return camera.generateRay((int)Math.round(pixel.x()), (int)Math.round(pixel.y()));
    }

    //= Size and width ====================================================

    @Test
    void given_legacyResolution_when_applyScale_then_keepsDesignedSizeAndWidths()
    {
        // Arrange
        RotateGizmo gizmo = new RotateGizmo(createCamera(1));

        // Act
        gizmo.applyScale(createScaler(1024, 768));

        // Assert
        assertThat(gizmo.getApparentSizeInPixels()).isEqualTo(100);
        for ( int ring = 0; ring < 3; ring++ ) {
            assertThat(gizmo.getRingLineWidth(ring))
                .isCloseTo(RotateGizmo.DEFAULT_LINE_WIDTH, offset(EPS));
        }
    }

    @Test
    void given_highResolution_when_applyScale_then_sizeAndWidthsAreDoubled()
    {
        // Arrange
        RotateGizmo gizmo = new RotateGizmo(createCamera(1));

        // Act
        gizmo.setBaseRingLineWidth(1, 3.0);
        gizmo.applyScale(createScaler(2560, 1600));

        // Assert
        assertThat(gizmo.getApparentSizeInPixels()).isEqualTo(200);
        assertThat(gizmo.getRingLineWidth(0))
            .isCloseTo(2*RotateGizmo.DEFAULT_LINE_WIDTH, offset(EPS));
        assertThat(gizmo.getRingLineWidth(1)).isCloseTo(6.0, offset(EPS));
        assertThat(gizmo.getRingLineWidth(2))
            .isCloseTo(2*RotateGizmo.DEFAULT_LINE_WIDTH, offset(EPS));
    }

    @Test
    void given_userChosenBaseSize_when_applyScaleRepeatedly_then_baseSizeIsKept()
    {
        // Arrange
        RotateGizmo gizmo = new RotateGizmo(createCamera(1));
        ViewportElementScaler scaler = createScaler(2560, 1600);

        // Act
        gizmo.setBaseApparentSizeInPixels(150);
        gizmo.applyScale(scaler);
        gizmo.applyScale(scaler);

        // Assert
        assertThat(gizmo.getBaseApparentSizeInPixels()).isEqualTo(150);
        assertThat(gizmo.getApparentSizeInPixels()).isEqualTo(300);
    }

    @Test
    void given_invalidValues_when_set_then_areIgnored()
    {
        // Arrange
        RotateGizmo gizmo = new RotateGizmo(createCamera(1));

        // Act
        gizmo.setRingLineWidth(0, 0);
        gizmo.setRingLineWidth(0, -1);
        gizmo.setBaseRingLineWidth(0, 0);
        gizmo.setBaseApparentSizeInPixels(0);

        // Assert
        assertThat(gizmo.getRingLineWidth(0))
            .isCloseTo(RotateGizmo.DEFAULT_LINE_WIDTH, offset(EPS));
        assertThat(gizmo.getBaseRingLineWidth(0))
            .isCloseTo(RotateGizmo.DEFAULT_LINE_WIDTH, offset(EPS));
        assertThat(gizmo.getBaseApparentSizeInPixels()).isEqualTo(100);
    }

    @Test
    void given_invalidRing_when_asked_then_isRejected()
    {
        // Arrange
        RotateGizmo gizmo = new RotateGizmo(createCamera(1));

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> gizmo.getRingLineWidth(3));
    }

    @Test
    void given_farAndNearCameras_when_transformationSet_then_ringsKeepTheirApparentSizeInPixels()
    {
        for ( double distanceFactor : new double[] {0.5, 1, 20} ) {
            // Arrange
            Camera camera = createCamera(distanceFactor);
            RotateGizmo gizmo = createGizmo(camera);

            // Act: pixel distance from the center to a point of a ring
            Vector3Dd center = camera.projectPointUsingRayMethod(gizmo.getPosition());
            Vector3Dd onRing = camera.projectPointUsingRayMethod(new Vector3Dd(gizmo.getRingRadius(), 0, 0)
                .add(gizmo.getPosition()));

            // Assert: the world radius is about 0.8 times the apparent size,
            // whatever the distance (perspective distorts it a little)
            assertThat(Vector3Dd.distance(center, onRing)).isBetween(50.0, 110.0);
        }
    }

    //= Torus based model =================================================

    @Test
    void given_gizmo_when_transformationSet_then_ringsAreToriOfTheGizmoRadiusAroundTheirAxes()
    {
        // Arrange
        Matrix4x4d frame = RotateGizmo.createRotationFromAnglesInDegrees(30, 20, 10)
            .withTranslation(new Vector3Dd(1, 2, 3));
        RotateGizmo gizmo = new RotateGizmo(createCamera(1));

        // Act
        gizmo.setTransformationMatrix(frame);

        // Assert
        for ( int ring = 0; ring < 3; ring++ ) {
            Torus torus = gizmo.getRingModel(ring);
            Vector3Dd axisOfTorus = gizmo.getRingInstance(ring).getRotation()
                .multiply(new Vector3Dd(0, 0, 1));

            assertThat(torus.getMajorRadius()).isCloseTo(gizmo.getRingRadius(), offset(EPS));
            assertThat(torus.getMinorRadius()).isGreaterThan(0.0);
            assertThat(torus.getMinorRadius()).isLessThan(torus.getMajorRadius());
            assertThat(Vector3Dd.distance(axisOfTorus, gizmo.getAxisDirection(ring)))
                .isLessThan(EPS);
            assertThat(Vector3Dd.distance(gizmo.getRingInstance(ring).getPosition(),
                new Vector3Dd(1, 2, 3))).isLessThan(EPS);
        }
        // Axes of the rings are the axes of the frame
        assertThat(Vector3Dd.distance(gizmo.getAxisDirection(0),
            frame.withoutTranslation().multiply(new Vector3Dd(1, 0, 0)))).isLessThan(EPS);
    }

    @Test
    void given_thickRing_when_transformationSet_then_tubeCoversItsWidth()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera(1));

        // Act
        gizmo.setRingLineWidth(1, 60);
        gizmo.updateGeometryState();

        // Assert
        assertThat(gizmo.getRingModel(1).getMinorRadius())
            .isGreaterThanOrEqualTo(gizmo.getRingLineWidthInWorldUnits(1) / 2 - EPS);
        assertThat(gizmo.getRingModel(1).getMinorRadius())
            .isGreaterThan(gizmo.getRingModel(0).getMinorRadius());
    }

    /**
    Independent oracle for picking: the smallest distance between a ray and the
    center line of a ring of a gizmo in the identity frame, found by sampling
    the ring.
    */
    private static double distanceFromRayToRing(Ray ray, RotateGizmo gizmo, int ring)
    {
        Vector3Dd[] axes = {
            new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1)
        };
        Vector3Dd u = axes[(ring + 1) % 3].multiply(gizmo.getRingRadius());
        Vector3Dd v = axes[(ring + 2) % 3].multiply(gizmo.getRingRadius());
        Vector3Dd direction = ray.getDirection().normalized();
        double best = Double.MAX_VALUE;
        int samples = 4000;

        for ( int i = 0; i < samples; i++ ) {
            double angle = 2 * Math.PI * i / samples;
            Vector3Dd w = u.multiply(Math.cos(angle)).add(v.multiply(Math.sin(angle)))
                .subtract(ray.getOrigin());

            best = Math.min(best,
                w.subtract(direction.multiply(w.dotProduct(direction))).length());
        }
        return best;
    }

    /**
    Same oracle as `distanceFromRayToRing`, for the camera ring (in the
    identity frame): the smallest distance between a ray and its centerline.
    */
    private static double distanceFromRayToCameraRing(Ray ray, RotateGizmo gizmo)
    {
        Vector3Dd u = gizmo.getCameraPlaneRightDirection().multiply(gizmo.getCameraRingRadius());
        Vector3Dd v = gizmo.getCameraPlaneUpDirection().multiply(gizmo.getCameraRingRadius());
        Vector3Dd direction = ray.getDirection().normalized();
        double best = Double.MAX_VALUE;
        int samples = 4000;

        for ( int i = 0; i < samples; i++ ) {
            double angle = 2 * Math.PI * i / samples;
            Vector3Dd w = u.multiply(Math.cos(angle)).add(v.multiply(Math.sin(angle)))
                .subtract(ray.getOrigin());

            best = Math.min(best,
                w.subtract(direction.multiply(w.dotProduct(direction))).length());
        }
        return best;
    }

    /**
    Independent oracle for the visibility clipping: whether a point (assumed
    on the sphere that contains the rings, centered at the origin) faces the
    camera, the same way `RotateGizmo.isPointOnVisibleHemisphere` defines it.
    */
    private static double visibilityMetric(Camera camera, Vector3Dd point)
    {
        Vector3Dd normal = point.normalized();
        Vector3Dd viewDirection = camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ?
            camera.getFront() : point.subtract(camera.getPosition()).normalized();

        return normal.dotProduct(viewDirection);
    }

    /**
    @return the visibility metric (see `visibilityMetric`) of the point of a
    ring (in the identity frame, i.e. its position is the origin) nearest to
    the ray, found by `distanceFromRayToRing`; negative if it can be picked
    */
    private static double nearestRingPointVisibilityMetric(Ray ray, RotateGizmo gizmo, Camera camera,
                                                            int ring)
    {
        Vector3Dd[] axes = {
            new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1)
        };
        Vector3Dd u = axes[(ring + 1) % 3].multiply(gizmo.getRingRadius());
        Vector3Dd v = axes[(ring + 2) % 3].multiply(gizmo.getRingRadius());
        Vector3Dd direction = ray.getDirection().normalized();
        double best = Double.MAX_VALUE;
        Vector3Dd bestPoint = null;
        int samples = 4000;

        for ( int i = 0; i < samples; i++ ) {
            double angle = 2 * Math.PI * i / samples;
            Vector3Dd point = u.multiply(Math.cos(angle)).add(v.multiply(Math.sin(angle)));
            Vector3Dd w = point.subtract(ray.getOrigin());
            double distance = w.subtract(direction.multiply(w.dotProduct(direction))).length();

            if ( distance < best ) {
                best = distance;
                bestPoint = point;
            }
        }
        return visibilityMetric(camera, bestPoint);
    }

    @Test
    void given_gridOfRays_when_picked_then_agreesWithTheDistanceToTheRings()
    {
        int checked = 0;
        int hits = 0;

        for ( double distanceFactor : new double[] {0.5, 1, 6, 40} ) {
            Camera camera = createCamera(distanceFactor);
            RotateGizmo gizmo = createGizmo(camera);
            double tube = gizmo.getRingModel(0).getMinorRadius();

            for ( int y = 50; y < 350; y += 6 ) {
                for ( int x = 50; x < 350; x += 6 ) {
                    // Arrange
                    Ray ray = camera.generateRay(x, y);
                    boolean[] surelyHit = new boolean[4];
                    boolean anyHit = false;
                    boolean ambiguous = false;

                    for ( int ring = 0; ring < 3; ring++ ) {
                        double distance = distanceFromRayToRing(ray, gizmo, ring);
                        double metric = nearestRingPointVisibilityMetric(ray, gizmo, camera, ring);

                        // Rays grazing a tube, or whose nearest point of the
                        // ring is close to the visible/hidden silhouette, may
                        // go either way (the picking uses the actual ray hit,
                        // this oracle the nearest point of the ring's centerline)
                        ambiguous |= distance > 0.9 * tube && distance < 1.1 * tube;
                        ambiguous |= Math.abs(metric) < 0.25;
                        surelyHit[ring] = distance <= 0.9 * tube && metric < 0;
                        anyHit |= surelyHit[ring];
                    }

                    double cameraDistance = distanceFromRayToCameraRing(ray, gizmo);
                    double cameraTube = gizmo.getCameraRingModel().getMinorRadius();

                    ambiguous |= cameraDistance > 0.9 * cameraTube && cameraDistance < 1.1 * cameraTube;
                    surelyHit[3] = cameraDistance <= 0.9 * cameraTube;
                    anyHit |= surelyHit[3];
                    if ( ambiguous ) {
                        continue;
                    }

                    // Act
                    int picked = gizmo.pickRing(ray);

                    // Assert
                    String description = String.format("pixel (%d, %d), distance factor %.1f",
                        x, y, distanceFactor);

                    checked++;
                    if ( anyHit ) {
                        hits++;
                        assertThat(picked).as(description).isNotEqualTo(RotateGizmo.NULL_GROUP);
                        assertThat(surelyHit[picked - 1]).as(description).isTrue();
                    }
                    else {
                        assertThat(picked).as(description).isEqualTo(RotateGizmo.NULL_GROUP);
                    }
                }
            }
        }
        // The grid really has both rays that hit rings and rays that miss them
        assertThat(checked).isGreaterThan(1000);
        assertThat(hits).isGreaterThan(50);
        assertThat(checked - hits).isGreaterThan(1000);
    }

    @Test
    void given_raysToVisiblePointsOfEachRing_when_picked_then_aRingIsAlwaysFound()
    {
        for ( double distanceFactor : new double[] {0.5, 1, 6, 40} ) {
            Camera camera = createCamera(distanceFactor);
            RotateGizmo gizmo = createGizmo(camera);

            for ( int ring = 0; ring < 3; ring++ ) {
                for ( double degrees = 5; degrees < 360; degrees += 15 ) {
                    // Arrange
                    Vector3Dd point = pointOnRing(gizmo, ring, degrees);

                    // The far half of a ring is deliberately not pickable
                    if ( !gizmo.isPointOnVisibleHemisphere(point) ) {
                        continue;
                    }

                    Ray ray = rayThrough(camera, point);

                    // Act
                    int picked = gizmo.pickRing(ray);

                    // Assert: the ring pointed at, or another one in front of it
                    assertThat(picked)
                        .as("ring %d at %.0f degrees, distance factor %.1f", ring, degrees, distanceFactor)
                        .isNotEqualTo(RotateGizmo.NULL_GROUP);
                    if ( picked != RotateGizmo.CAMERA_RING_GROUP ) {
                        assertThat(distanceFromRayToRing(ray, gizmo, picked - 1))
                            .isLessThanOrEqualTo(gizmo.getRingModel(picked - 1).getMinorRadius());
                    }
                }
            }
        }
    }

    @Test
    void given_rayFarFromRings_when_picked_then_nothingIsFound()
    {
        // Arrange
        Camera camera = createCamera(1);
        RotateGizmo gizmo = createGizmo(camera);

        // Act & Assert
        assertThat(gizmo.pickRing(camera.generateRay(2, 2))).isEqualTo(RotateGizmo.NULL_GROUP);
    }

    @Test
    void given_rotatedGizmoAndPosition_when_picked_then_ringsFollowTheFrame()
    {
        // Arrange
        Camera camera = createCamera(1);
        RotateGizmo gizmo = new RotateGizmo(camera);

        gizmo.setTransformationMatrix(
            RotateGizmo.createRotationFromAnglesInDegrees(40, 25, -15)
                .withTranslation(new Vector3Dd(0.5, -0.5, 0)));

        // Act & Assert: points of the ring, built from the axes of the frame
        Matrix4x4d rotation = gizmo.getTransformationMatrix().withoutTranslation();
        Vector3Dd u = rotation.multiply(new Vector3Dd(0, 0, 1));
        Vector3Dd v = rotation.multiply(new Vector3Dd(1, 0, 0));
        Vector3Dd point = gizmo.getPosition().add(u.multiply(Math.cos(1.0) * gizmo.getRingRadius()))
            .add(v.multiply(Math.sin(1.0) * gizmo.getRingRadius()));

        assertThat(gizmo.pickRing(rayThrough(camera, point))).isEqualTo(RotateGizmo.Y_RING_GROUP);
    }

    //= Strips of quads ===================================================

    @Test
    void given_gizmo_when_buildingRingStrips_then_onlyTheVisibleHalfIsAribbonAroundTheRing()
    {
        // Arrange
        Camera camera = createCamera(1);
        RotateGizmo gizmo = createGizmo(camera);

        for ( int ring = 0; ring < 3; ring++ ) {
            // Act
            java.util.ArrayList<Vector3Dd[]> arcs = gizmo.buildRingStrips(ring);

            // Assert: at least one arc, and not the whole ring (it is clipped)
            assertThat(arcs).isNotEmpty();
            int totalPairs = 0;

            for ( Vector3Dd[] strip : arcs ) {
                assertThat(strip.length % 2).isEqualTo(0);
                totalPairs += strip.length / 2;
                for ( int i = 0; i < strip.length; i += 2 ) {
                    Vector3Dd middle = strip[i].add(strip[i + 1]).multiply(0.5);
                    Vector3Dd fromCenter = middle.subtract(gizmo.getPosition());

                    assertThat(fromCenter.length()).isCloseTo(gizmo.getRingRadius(), offset(1.0e-3));
                    assertThat(fromCenter.dotProduct(gizmo.getAxisDirection(ring)))
                        .isCloseTo(0.0, offset(EPS));
                }
            }
            assertThat(totalPairs).isLessThan(RotateGizmo.RING_SEGMENTS + 1);
            assertThat(totalPairs).isGreaterThan(RotateGizmo.RING_SEGMENTS / 4);
        }
    }

    @Test
    void given_ringWidthsInPixels_when_buildingStrips_then_widthsAreDifferentAndMatchPixels()
    {
        // Arrange
        Camera camera = createCamera(1);
        RotateGizmo gizmo = createGizmo(camera);

        gizmo.setRingLineWidth(0, 2);
        gizmo.setRingLineWidth(1, 8);
        gizmo.setRingLineWidth(2, 4);
        gizmo.updateGeometryState();

        // Act: the width of the ribbon is the same all around a ring, so any
        // vertex pair (of the first visible arc) is a valid sample of it
        double[] widthsInWorld = new double[3];
        double[] widthsInPixels = new double[3];

        for ( int ring = 0; ring < 3; ring++ ) {
            Vector3Dd[] strip = gizmo.buildRingStrips(ring).get(0);
            Vector3Dd a = strip[0];
            Vector3Dd b = strip[1];
            Vector3Dd middle = a.add(b).multiply(0.5);
            Vector3Dd pixelA = camera.projectPointUsingRayMethod(a);
            Vector3Dd pixelB = camera.projectPointUsingRayMethod(b);

            widthsInWorld[ring] = Vector3Dd.distance(a, b);
            widthsInPixels[ring] = Vector3Dd.distance(pixelA, pixelB);
            assertThat(widthsInWorld[ring]).isCloseTo(gizmo.getRingLineWidthInWorldUnits(ring), offset(EPS));
            assertThat(camera.projectPointUsingRayMethod(middle)).isNotNull();
        }

        // Assert: 8 px ring is four times wider than the 2 px one
        assertThat(widthsInWorld[1] / widthsInWorld[0]).isCloseTo(4.0, offset(EPS));
        assertThat(widthsInWorld[2] / widthsInWorld[0]).isCloseTo(2.0, offset(EPS));
        // ... and it looks as wide as asked (perspective distorts it a little)
        assertThat(widthsInPixels[1]).isBetween(5.0, 11.0);
        assertThat(widthsInPixels[0]).isBetween(1.0, 3.0);
    }

    @Test
    void given_orthogonalCamera_when_buildingRingStrips_then_ribbonFacesTheCamera()
    {
        // Arrange
        Camera camera = createCamera(1);

        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.updateVectors();
        RotateGizmo gizmo = createGizmo(camera);

        // Act & Assert: the width direction is never the direction of view
        for ( Vector3Dd[] strip : gizmo.buildRingStrips(2) ) {
            for ( int i = 0; i < strip.length; i += 2 ) {
                Vector3Dd across = strip[i].subtract(strip[i + 1]);

                assertThat(across.length()).isGreaterThan(0.0);
                assertThat(Math.abs(across.normalized().dotProduct(camera.getFront().normalized())))
                    .isLessThan(1.0 - 1.0e-3);
            }
        }
    }

    @Test
    void given_cameraLookingAlongAxis_when_buildingRingStrips_then_isNotDegenerate()
    {
        // Arrange
        Camera camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(0, 0, 10));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        RotateGizmo gizmo = createGizmo(camera);

        // Act: ring around the axis the camera looks along (seen face on, so
        // it is not clipped), and one seen edge-on (clipped, but not degenerate)
        java.util.ArrayList<Vector3Dd[]> faceOn = gizmo.buildRingStrips(2);
        java.util.ArrayList<Vector3Dd[]> edgeOn = gizmo.buildRingStrips(0);

        // Assert
        assertThat(faceOn).hasSize(1);
        assertThat(faceOn.get(0)).hasSize(2 * (RotateGizmo.RING_SEGMENTS + 1));
        for ( java.util.ArrayList<Vector3Dd[]> arcs : new java.util.ArrayList[] {faceOn, edgeOn} ) {
            for ( Vector3Dd[] strip : arcs ) {
                for ( Vector3Dd v : strip ) {
                    assertThat(Double.isNaN(v.x()) || Double.isNaN(v.y()) || Double.isNaN(v.z())).isFalse();
                }
            }
        }
    }

    //= Camera ring ========================================================

    @Test
    void given_gizmo_when_buildingCameraRingStrip_then_isAWholeRibbonFacingTheCamera()
    {
        // Arrange
        Camera camera = createCamera(1);
        RotateGizmo gizmo = createGizmo(camera);

        // Act
        Vector3Dd[] strip = gizmo.buildCameraRingStrip();

        // Assert: whole (not clipped), at the camera ring radius, around the
        // camera axis, further out than the axis rings
        assertThat(strip).hasSize(2 * (RotateGizmo.RING_SEGMENTS + 1));
        assertThat(gizmo.getCameraRingRadius()).isGreaterThan(gizmo.getRingRadius());
        for ( int i = 0; i < strip.length; i += 2 ) {
            Vector3Dd middle = strip[i].add(strip[i + 1]).multiply(0.5);
            Vector3Dd fromCenter = middle.subtract(gizmo.getPosition());

            assertThat(fromCenter.length()).isCloseTo(gizmo.getCameraRingRadius(), offset(1.0e-3));
            assertThat(fromCenter.dotProduct(gizmo.getCameraAxisDirection())).isCloseTo(0.0, offset(EPS));
        }
    }

    @Test
    void given_noSelection_when_askingCameraRingColor_then_isGray()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera(1));

        // Act & Assert
        assertThat(gizmo.getCameraRingColor().r()).isCloseTo(gizmo.getCameraRingColor().g(), offset(EPS));
        assertThat(gizmo.getCameraRingColor().g()).isCloseTo(gizmo.getCameraRingColor().b(), offset(EPS));

        // ... chosen: yellow
        gizmo.setPersistentSelection(RotateGizmo.CAMERA_RING_GROUP);
        assertThat(gizmo.getCameraRingColor()).isEqualTo(new ColorRgb(1, 1, 0));
    }

    @Test
    void given_pointsOfTheCameraRing_when_picked_then_theCameraRingIsFound()
    {
        // Arrange
        Camera camera = createCamera(1);
        RotateGizmo gizmo = createGizmo(camera);

        for ( double degrees = 5; degrees < 360; degrees += 15 ) {
            Vector3Dd u = gizmo.getCameraPlaneRightDirection();
            Vector3Dd v = gizmo.getCameraPlaneUpDirection();
            double angle = Math.toRadians(degrees);
            Vector3Dd point = gizmo.getPosition()
                .add(u.multiply(Math.cos(angle) * gizmo.getCameraRingRadius()))
                .add(v.multiply(Math.sin(angle) * gizmo.getCameraRingRadius()));

            // Act & Assert: every point of the camera ring is pickable, as
            // it always faces the camera and is never clipped
            assertThat(gizmo.pickRing(rayThrough(camera, point)))
                .as("degrees %.0f", degrees)
                .isEqualTo(RotateGizmo.CAMERA_RING_GROUP);
        }
    }

    //= Selection and colors ==============================================

    @Test
    void given_noSelection_when_askingColors_then_ringsHaveTheirRgbColors()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera(1));

        // Act & Assert
        assertThat(gizmo.getCurrentSelection()).isEqualTo(RotateGizmo.NULL_GROUP);
        assertThat(gizmo.getRingColor(0).r()).isGreaterThan(gizmo.getRingColor(0).g());
        assertThat(gizmo.getRingColor(1).g()).isGreaterThan(gizmo.getRingColor(1).r());
        assertThat(gizmo.getRingColor(2).b()).isGreaterThan(gizmo.getRingColor(2).r());
    }

    @Test
    void given_hoverOrChosenRing_when_askingColors_then_thatRingIsYellow()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera(1));
        ColorRgb yellow = new ColorRgb(1, 1, 0);

        // Act & Assert: cursor over the Y ring
        gizmo.setVolatileSelection(RotateGizmo.Y_RING_GROUP);
        assertThat(gizmo.getRingColor(1)).isEqualTo(yellow);
        assertThat(gizmo.getRingColor(0)).isNotEqualTo(yellow);
        assertThat(gizmo.getRingColor(2)).isNotEqualTo(yellow);

        // ... the cursor leaves: goes back to RGB
        gizmo.setVolatileSelection(RotateGizmo.NULL_GROUP);
        assertThat(gizmo.getRingColor(1)).isNotEqualTo(yellow);

        // ... but a chosen ring stays yellow
        gizmo.setPersistentSelection(RotateGizmo.Z_RING_GROUP);
        assertThat(gizmo.getRingColor(2)).isEqualTo(yellow);

        // ... and the cursor over another one takes over while it is there
        gizmo.setVolatileSelection(RotateGizmo.X_RING_GROUP);
        assertThat(gizmo.getRingColor(0)).isEqualTo(yellow);
        assertThat(gizmo.getRingColor(2)).isNotEqualTo(yellow);
    }

    //= Numeric input =====================================================

    @Test
    void given_orientation_when_inputGizmoAsked_then_showsDegreesWithTwoDecimals()
    {
        // Arrange
        RotateGizmo gizmo = new RotateGizmo(createCamera(1));

        gizmo.setTransformationMatrix(
            RotateGizmo.createRotationFromAnglesInDegrees(30.5, -20.25, 10));

        // Act
        InputGizmo input = gizmo.getInputGizmo();

        // Assert: 3 axis angles plus the (always 0) relative camera field
        assertThat(input.getNumberOfFields()).isEqualTo(4);
        assertThat(input.getDecimals()).isEqualTo(2);
        assertThat(input.getDisplayText(0)).isEqualTo("30.50");
        assertThat(input.getDisplayText(1)).isEqualTo("-20.25");
        assertThat(input.getDisplayText(2)).isEqualTo("10.00");
        assertThat(input.getDisplayText(3)).isEqualTo("0.00");
        assertThat(input.getReferenceText()).isEqualTo("-000.00");
    }

    @Test
    void given_highlightedRing_when_inputGizmoAsked_then_itsFieldIsHighlighted()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera(1));

        gizmo.setVolatileSelection(RotateGizmo.Z_RING_GROUP);

        // Act
        InputGizmo input = gizmo.getInputGizmo();

        // Assert
        assertThat(input.isFieldHighlighted(0)).isFalse();
        assertThat(input.isFieldHighlighted(1)).isFalse();
        assertThat(input.isFieldHighlighted(2)).isTrue();
        assertThat(input.getFieldColor(0)).isEqualTo(gizmo.getRingColor(0));
    }

    @Test
    void given_angles_when_convertedToRotationAndBack_then_areTheSame()
    {
        double[][] cases = {
            {0, 0, 0}, {45, 0, 0}, {0, 45, 0}, {0, 0, 45}, {30, 20, 10},
            {-170, 60, 170}, {120, -80, -100}, {10, 89.5, 20}
        };

        for ( double[] angles : cases ) {
            // Act
            Matrix4x4d rotation = RotateGizmo.createRotationFromAnglesInDegrees(
                angles[0], angles[1], angles[2]);
            double[] back = RotateGizmo.extractAnglesInDegrees(rotation);

            // Assert
            assertThat(back[0]).isCloseTo(angles[0], offset(1.0e-6));
            assertThat(back[1]).isCloseTo(angles[1], offset(1.0e-6));
            assertThat(back[2]).isCloseTo(angles[2], offset(1.0e-6));
        }
    }

    @Test
    void given_gimbalLock_when_anglesExtracted_then_theyGiveTheSameOrientation()
    {
        double[][] cases = {{25, 90, 40}, {-70, -90, 15}};

        for ( double[] angles : cases ) {
            // Arrange
            Matrix4x4d rotation = RotateGizmo.createRotationFromAnglesInDegrees(
                angles[0], angles[1], angles[2]);

            // Act
            double[] back = RotateGizmo.extractAnglesInDegrees(rotation);
            Matrix4x4d rebuilt = RotateGizmo.createRotationFromAnglesInDegrees(
                back[0], back[1], back[2]);

            // Assert
            assertThat(rebuilt.epsilonEquals(rotation, 1.0e-9)).isTrue();
        }
    }

    @Test
    void given_angleMoreThanHalfTurn_when_anglesExtracted_then_equivalentOrientationIsGiven()
    {
        // Arrange
        Matrix4x4d rotation = RotateGizmo.createRotationFromAnglesInDegrees(200, 0, 0);

        // Act
        double[] back = RotateGizmo.extractAnglesInDegrees(rotation);

        // Assert
        assertThat(back[0]).isCloseTo(-160.0, offset(1.0e-6));
        assertThat(RotateGizmo.createRotationFromAnglesInDegrees(back[0], back[1], back[2])
            .epsilonEquals(rotation, 1.0e-9)).isTrue();
    }

    //= Rotation arc ======================================================

    @Test
    void given_noArc_when_asked_then_thereIsNothingToShow()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera(1));

        // Act & Assert
        assertThat(gizmo.isArcVisible()).isFalse();
        assertThat(gizmo.getArcRing()).isEqualTo(-1);
        assertThat(gizmo.buildArcFan()).isEmpty();
        assertThat(gizmo.getArcLabelPosition()).isNull();
    }

    @Test
    void given_arc_when_fanBuilt_then_isASectorOfTheRingRadiusAroundTheCenter()
    {
        // Arrange
        RotateGizmo gizmo = new RotateGizmo(createCamera(1));

        gizmo.setTransformationMatrix(new Matrix4x4d().withTranslation(new Vector3Dd(1, 2, 3)));
        Vector3Dd u = new Vector3Dd(0, 0, 1);
        Vector3Dd v = new Vector3Dd(1, 0, 0);

        // Act: an arc of 60 degrees around the Y axis, that starts at 30
        gizmo.setArc(1, u, v, Math.toRadians(30), Math.toRadians(60));
        Vector3Dd[] fan = gizmo.buildArcFan();

        // Assert
        assertThat(gizmo.isArcVisible()).isTrue();
        assertThat(fan.length).isEqualTo(2 + 20);
        assertThat(Vector3Dd.distance(fan[0], new Vector3Dd(1, 2, 3))).isLessThan(EPS);
        for ( int i = 1; i < fan.length; i++ ) {
            Vector3Dd fromCenter = fan[i].subtract(fan[0]);

            assertThat(fromCenter.length()).isCloseTo(gizmo.getRingRadius(), offset(EPS));
            assertThat(fromCenter.y()).isCloseTo(0.0, offset(EPS));
        }
        Vector3Dd first = fan[1].subtract(fan[0]);
        Vector3Dd last = fan[fan.length - 1].subtract(fan[0]);
        double firstAngle = Math.toDegrees(Math.atan2(first.dotProduct(v), first.dotProduct(u)));
        double lastAngle = Math.toDegrees(Math.atan2(last.dotProduct(v), last.dotProduct(u)));

        assertThat(firstAngle).isCloseTo(30.0, offset(1.0e-6));
        assertThat(lastAngle).isCloseTo(90.0, offset(1.0e-6));
    }

    @Test
    void given_arcOfMoreThanATurn_when_fanBuilt_then_isAWholeDisc()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera(1));

        gizmo.setArc(2, new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), 0, Math.toRadians(1000));

        // Act
        Vector3Dd[] fan = gizmo.buildArcFan();

        // Assert
        assertThat(fan.length).isEqualTo(2 + 120);
        assertThat(Vector3Dd.distance(fan[1], fan[fan.length - 1])).isLessThan(EPS);
        assertThat(gizmo.getArcSweepInDegrees()).isCloseTo(1000.0, offset(1.0e-6));
    }

    @Test
    void given_arcWithNoAngle_when_fanBuilt_then_thereIsNothingToDraw()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera(1));

        gizmo.setArc(0, new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1), 1.0, 0.0);

        // Act & Assert
        assertThat(gizmo.isArcVisible()).isTrue();
        assertThat(gizmo.buildArcFan()).isEmpty();
    }

    @Test
    void given_highlightedRing_when_arcColorAsked_then_isTheColorOfTheAxisNotYellow()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera(1));

        gizmo.setVolatileSelection(RotateGizmo.Y_RING_GROUP);
        gizmo.setArc(1, new Vector3Dd(0, 0, 1), new Vector3Dd(1, 0, 0), 0, 1.0);

        // Act & Assert
        assertThat(gizmo.getRingColor(1)).isEqualTo(new ColorRgb(1, 1, 0));
        assertThat(gizmo.getArcColor()).isNotEqualTo(new ColorRgb(1, 1, 0));
        assertThat(gizmo.getArcColor().g()).isGreaterThan(gizmo.getArcColor().r());
    }

    @Test
    void given_arc_when_labelPositionAsked_then_isJustOutsideTheRingAtTheMiddleOfTheArc()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera(1));

        gizmo.setArc(2, new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), 0, Math.toRadians(90));

        // Act
        Vector3Dd label = gizmo.getArcLabelPosition();

        // Assert: 45 degrees, farther than the ring
        assertThat(label.x()).isCloseTo(label.y(), offset(EPS));
        assertThat(label.length()).isGreaterThan(gizmo.getRingRadius());
        assertThat(label.z()).isCloseTo(0.0, offset(EPS));
        gizmo.clearArc();
        assertThat(gizmo.getArcLabelPosition()).isNull();
    }
}
