package vsdk.toolkit.gui;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.InputGizmo;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the size, geometric model, selection groups, picking and numeric
input of the scale gizmo, independently of any rendering technology.
 */
class ScaleGizmoTest
{
    private static final double EPS = 1.0e-6;

    private static Camera createCamera()
    {
        Camera camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(5, -6, 4));
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

    private static ScaleGizmo createGizmo(Camera camera)
    {
        ScaleGizmo gizmo = new ScaleGizmo(camera);

        gizmo.setTransformationMatrix(new Matrix4x4d());
        return gizmo;
    }

    private static Ray rayThrough(Camera camera, Vector3Dd point)
    {
        Vector3Dd pixel = camera.projectPointUsingRayMethod(point);

        return camera.generateRay((int)Math.round(pixel.x()), (int)Math.round(pixel.y()));
    }

    /**
    @return true if the point is over the line of the axis (that is, its
    components over the other two axes of the frame are zero)
    */
    private static boolean isOverAxis(ScaleGizmo gizmo, Vector3Dd point, int axis)
    {
        for ( int other = 0; other < 3; other++ ) {
            if ( other != axis && Math.abs(distanceToPlane(gizmo, point, other)) > EPS ) {
                return false;
            }
        }
        return true;
    }

    /**
    @return the component of the point, relative to the origin of the gizmo,
    along the given axis: its distance to the plane of the other two axes
    */
    private static double distanceToPlane(ScaleGizmo gizmo, Vector3Dd point, int axis)
    {
        return point.subtract(gizmo.getPosition()).dotProduct(gizmo.getAxisDirection(axis));
    }

    /**
    @return the center of the trapezoidal band of the group, a point well
    inside it
    */
    private static Vector3Dd bandCenter(ScaleGizmo gizmo, int group)
    {
        Vector3Dd[] quad = gizmo.buildBandQuad(group);

        return quad[0].add(quad[1]).add(quad[2]).add(quad[3]).multiply(0.25);
    }

    /**
    @return a ray that reaches the given point of the plane of the group
    perpendicularly to it, so no handle of another plane can be in between
    */
    private static Ray rayOntoPlaneOf(ScaleGizmo gizmo, int group, Vector3Dd target)
    {
        int[] axes = ScaleGizmo.bandAxes(group);
        Vector3Dd normal = gizmo.getAxisDirection(3 - axes[0] - axes[1]);

        return new Ray(target.add(normal.multiply(10)), normal.multiply(-1));
    }

    //= Size ================================================================

    @Test
    void given_legacyResolution_when_applyScale_then_keepsDesignedSize()
    {
        // Arrange
        ScaleGizmo gizmo = new ScaleGizmo(createCamera());

        // Act
        gizmo.applyScale(createScaler(1024, 768));

        // Assert
        assertThat(gizmo.getApparentSizeInPixels()).isEqualTo(100);
    }

    @Test
    void given_highResolution_when_applyScale_then_sizeAndContourWidthAreDoubled()
    {
        // Arrange
        ScaleGizmo gizmo = new ScaleGizmo(createCamera());

        // Act
        gizmo.applyScale(createScaler(2560, 1600));

        // Assert
        assertThat(gizmo.getApparentSizeInPixels()).isEqualTo(200);
        assertThat(gizmo.getLineWidth())
            .isCloseTo(2*ScaleGizmo.DEFAULT_LINE_WIDTH, offset(EPS));
    }

    @Test
    void given_invalidValues_when_set_then_areIgnored()
    {
        // Arrange
        ScaleGizmo gizmo = new ScaleGizmo(createCamera());

        // Act
        gizmo.setBaseApparentSizeInPixels(0);
        gizmo.setBaseApparentSizeInPixels(-5);
        gizmo.setLineWidth(0);
        gizmo.setLineWidth(-1);

        // Assert
        assertThat(gizmo.getBaseApparentSizeInPixels())
            .isEqualTo(ScaleGizmo.DEFAULT_APPARENT_SIZE_IN_PIXELS);
        assertThat(gizmo.getLineWidth())
            .isCloseTo(ScaleGizmo.DEFAULT_LINE_WIDTH, offset(EPS));
    }

    //= Geometry =============================================================

    @Test
    void given_gizmo_when_elementsAsked_then_thereAreSixOfThem()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());

        // Act & Assert: 3 cylinders and 3 cubes (the flat handles are
        // polygons, not instanced primitives)
        assertThat(gizmo.getElements()).hasSize(6);
        for ( SimpleBody element : gizmo.getElements() ) {
            assertThat(element.getGeometry()).isNotNull();
        }
    }

    @Test
    void given_gizmo_when_tipPositionsAsked_then_theyFormAnEquilateralTriangle()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());

        // Act
        Vector3Dd t0 = gizmo.getTipPosition(0);
        Vector3Dd t1 = gizmo.getTipPosition(1);
        Vector3Dd t2 = gizmo.getTipPosition(2);

        // Assert
        double d01 = Vector3Dd.distance(t0, t1);
        double d12 = Vector3Dd.distance(t1, t2);
        double d20 = Vector3Dd.distance(t2, t0);

        assertThat(d01).isCloseTo(d12, offset(EPS));
        assertThat(d12).isCloseTo(d20, offset(EPS));
        assertThat(d01).isGreaterThan(0);
    }

    @Test
    void given_gizmo_when_uniformTrianglesBuilt_then_theyShareTheOriginAndLieInThePlanes()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());
        Vector3Dd origin = gizmo.getPosition();

        // Act
        Vector3Dd[] triangles = gizmo.buildUniformTriangles();

        // Assert: one triangle per plane, all of them with a vertex at the
        // origin of the frame and the other two over the axes of their plane
        assertThat(triangles).hasSize(9);
        for ( int i = 0; i < ScaleGizmo.BAND_GROUPS.length; i++ ) {
            int group = ScaleGizmo.BAND_GROUPS[i];
            int[] axes = ScaleGizmo.bandAxes(group);
            int missingAxis = 3 - axes[0] - axes[1];

            assertThat(triangles[3*i]).isEqualTo(origin);
            assertThat(isOverAxis(gizmo, triangles[3*i + 1], axes[0])).isTrue();
            assertThat(isOverAxis(gizmo, triangles[3*i + 2], axes[1])).isTrue();
            for ( int v = 0; v < 3; v++ ) {
                assertThat(distanceToPlane(gizmo, triangles[3*i + v], missingAxis))
                    .isCloseTo(0.0, offset(EPS));
            }
        }
    }

    @Test
    void given_gizmo_when_bandsBuilt_then_theyAreTrapezoidsConfinedToTheirPlane()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());

        for ( int group : ScaleGizmo.BAND_GROUPS ) {
            int[] axes = ScaleGizmo.bandAxes(group);
            int missingAxis = 3 - axes[0] - axes[1];

            // Act
            Vector3Dd[] quad = gizmo.buildBandQuad(group);

            // Assert: 4 corners, two over each axis of the plane, and none of
            // them outside it
            assertThat(quad).hasSize(4);
            assertThat(isOverAxis(gizmo, quad[0], axes[0])).isTrue();
            assertThat(isOverAxis(gizmo, quad[1], axes[0])).isTrue();
            assertThat(isOverAxis(gizmo, quad[2], axes[1])).isTrue();
            assertThat(isOverAxis(gizmo, quad[3], axes[1])).isTrue();
            for ( Vector3Dd corner : quad ) {
                assertThat(distanceToPlane(gizmo, corner, missingAxis))
                    .isCloseTo(0.0, offset(EPS));
            }

            // The parallel sides have different lengths (it is a trapezoid,
            // not a rectangle), and the inner one is the shorter
            double innerSide = Vector3Dd.distance(quad[0], quad[2]);
            double outerSide = Vector3Dd.distance(quad[1], quad[3]);

            assertThat(innerSide).isLessThan(outerSide);
        }
    }

    @Test
    void given_gizmo_when_uniformAndBandsBuilt_then_theyAreFlushAtTheInnerEdge()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());
        Vector3Dd[] triangles = gizmo.buildUniformTriangles();

        for ( int i = 0; i < ScaleGizmo.BAND_GROUPS.length; i++ ) {
            int group = ScaleGizmo.BAND_GROUPS[i];
            Vector3Dd[] quad = gizmo.buildBandQuad(group);

            // Act & Assert: the outer vertices of the uniform triangle of the
            // plane are the inner corners of its band, so they share that edge
            assertThat(Vector3Dd.distance(triangles[3*i + 1], quad[0]))
                .isCloseTo(0.0, offset(EPS));
            assertThat(Vector3Dd.distance(triangles[3*i + 2], quad[2]))
                .isCloseTo(0.0, offset(EPS));
        }
    }

    @Test
    void given_gizmo_when_proportionsMeasured_then_theyFollowTheReferenceGizmo()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());
        Vector3Dd origin = gizmo.getPosition();
        double axisLength = Vector3Dd.distance(origin, gizmo.axisPoint(0, 1.0));
        Vector3Dd[] quad = gizmo.buildBandQuad(ScaleGizmo.XY_GROUP);

        // Act
        double innerReach = Vector3Dd.distance(origin, quad[0])/axisLength;
        double outerReach = Vector3Dd.distance(origin, quad[1])/axisLength;
        double tipReach = Vector3Dd.distance(origin, gizmo.getTipPosition(0))/axisLength;

        // Assert: the bands sit around the middle of the axes, which go on
        // well beyond them up to the cube of the tip
        assertThat(innerReach).isBetween(0.45, 0.60);
        assertThat(outerReach).isBetween(0.65, 0.80);
        assertThat(tipReach).isGreaterThan(outerReach);
        assertThat(tipReach).isBetween(0.9, 1.0);
    }

    @Test
    void given_noSelection_when_contourBuilt_then_eachEdgeIsSplitInItsAxisColors()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());

        // Act: two edges (inner and outer) per plane, each one in two halves
        ArrayList<ScaleGizmo.ContourSegment> segments = gizmo.buildContourSegments();

        // Assert
        assertThat(segments).hasSize(2*2*ScaleGizmo.BAND_GROUPS.length);
        for ( int i = 0; i < ScaleGizmo.BAND_GROUPS.length; i++ ) {
            int[] axes = ScaleGizmo.bandAxes(ScaleGizmo.BAND_GROUPS[i]);

            // The two halves of the inner edge meet at its middle point, and
            // each one has the color of the axis it ends at
            ScaleGizmo.ContourSegment first = segments.get(4*i);
            ScaleGizmo.ContourSegment second = segments.get(4*i + 1);

            assertThat(first.end()).isEqualTo(second.start());
            assertThat(first.color()).isEqualTo(gizmo.getAxisDisplayColor(axes[0]));
            assertThat(second.color()).isEqualTo(gizmo.getAxisDisplayColor(axes[1]));
            assertThat(first.color()).isNotEqualTo(InputGizmo.HIGHLIGHT_COLOR);
        }
    }

    @Test
    void given_selectedBand_when_contourBuilt_then_onlyItsEdgesAreYellow()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());

        gizmo.setPersistentSelection(ScaleGizmo.YZ_GROUP);

        // Act
        ArrayList<ScaleGizmo.ContourSegment> segments = gizmo.buildContourSegments();
        int yellow = 0;

        for ( ScaleGizmo.ContourSegment segment : segments ) {
            if ( segment.color().equals(InputGizmo.HIGHLIGHT_COLOR) ) {
                yellow++;
            }
        }

        // Assert: the four halves of the two edges of the YZ band
        assertThat(yellow).isEqualTo(4);
    }

    @Test
    void given_selectedUniformHandle_when_contourBuilt_then_theThreeInnerEdgesAreYellow()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());

        gizmo.setPersistentSelection(ScaleGizmo.UNIFORM_GROUP);

        // Act
        ArrayList<ScaleGizmo.ContourSegment> segments = gizmo.buildContourSegments();
        int yellow = 0;

        for ( ScaleGizmo.ContourSegment segment : segments ) {
            if ( segment.color().equals(InputGizmo.HIGHLIGHT_COLOR) ) {
                yellow++;
            }
        }

        // Assert: the two halves of the inner edge of each of the 3 planes
        // (which the uniform handle is flush with), and no outer edge
        assertThat(yellow).isEqualTo(6);
    }

    //= Selection groups ======================================================

    @Test
    void given_axisGroups_when_axisMembershipAsked_then_onlyTheirOwnAxisIsIncluded()
    {
        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.X_AXIS_GROUP, 0)).isTrue();
        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.X_AXIS_GROUP, 1)).isFalse();
        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.X_AXIS_GROUP, 2)).isFalse();
    }

    @Test
    void given_twoAxisGroups_when_axisMembershipAsked_then_bothOfTheirAxesAreIncluded()
    {
        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XY_GROUP, 0)).isTrue();
        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XY_GROUP, 1)).isTrue();
        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XY_GROUP, 2)).isFalse();

        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.YZ_GROUP, 1)).isTrue();
        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.YZ_GROUP, 2)).isTrue();
        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.YZ_GROUP, 0)).isFalse();

        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XZ_GROUP, 0)).isTrue();
        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XZ_GROUP, 2)).isTrue();
        assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.XZ_GROUP, 1)).isFalse();
    }

    @Test
    void given_uniformGroup_when_axisMembershipAsked_then_everyAxisIsIncluded()
    {
        for ( int axis = 0; axis < 3; axis++ ) {
            assertThat(ScaleGizmo.groupIncludesAxis(ScaleGizmo.UNIFORM_GROUP, axis)).isTrue();
        }
    }

    @Test
    void given_selection_when_axisHighlightAsked_then_matchesGroupMembership()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());

        // Act
        gizmo.setPersistentSelection(ScaleGizmo.XY_GROUP);

        // Assert
        assertThat(gizmo.isAxisHighlighted(0)).isTrue();
        assertThat(gizmo.isAxisHighlighted(1)).isTrue();
        assertThat(gizmo.isAxisHighlighted(2)).isFalse();
        assertThat(gizmo.isBandHighlighted(ScaleGizmo.XY_GROUP)).isTrue();
        assertThat(gizmo.isBandHighlighted(ScaleGizmo.YZ_GROUP)).isFalse();
        assertThat(gizmo.isUniformHighlighted()).isFalse();
    }

    @Test
    void given_uniformSelection_when_axisHighlightAsked_then_everyAxisIsHighlighted()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());

        // Act
        gizmo.setPersistentSelection(ScaleGizmo.UNIFORM_GROUP);

        // Assert
        assertThat(gizmo.isAxisHighlighted(0)).isTrue();
        assertThat(gizmo.isAxisHighlighted(1)).isTrue();
        assertThat(gizmo.isAxisHighlighted(2)).isTrue();
        assertThat(gizmo.isUniformHighlighted()).isTrue();
    }

    @Test
    void given_volatileSelection_when_currentSelectionAsked_then_itTakesPriority()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());

        gizmo.setPersistentSelection(ScaleGizmo.X_AXIS_GROUP);

        // Act
        gizmo.setVolatileSelection(ScaleGizmo.Z_AXIS_GROUP);

        // Assert
        assertThat(gizmo.getCurrentSelection()).isEqualTo(ScaleGizmo.Z_AXIS_GROUP);

        // Act: with no volatile selection, the persistent one applies again
        gizmo.setVolatileSelection(ScaleGizmo.NULL_GROUP);
        assertThat(gizmo.getCurrentSelection()).isEqualTo(ScaleGizmo.X_AXIS_GROUP);
    }

    //= Picking ===============================================================

    @Test
    void given_rayThroughAnAxisTip_when_picked_then_selectsItsAxisGroup()
    {
        // Arrange
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);

        // Act
        Ray ray = rayThrough(camera, gizmo.getTipPosition(0));
        int selection = gizmo.pickElement(ray);

        // Assert
        assertThat(selection).isEqualTo(ScaleGizmo.X_AXIS_GROUP);
    }

    @Test
    void given_rayThroughEachBand_when_picked_then_selectsItsTwoAxisGroup()
    {
        // Arrange
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);

        for ( int group : ScaleGizmo.BAND_GROUPS ) {
            // Act: perpendicular to the plane of the band, so no other
            // handle can be in between
            Ray ray = rayOntoPlaneOf(gizmo, group, bandCenter(gizmo, group));
            int selection = gizmo.pickElement(ray);

            // Assert
            assertThat(selection).isEqualTo(group);
        }
    }

    @Test
    void given_rayThroughEachUniformTriangle_when_picked_then_selectsTheUniformGroup()
    {
        // Arrange
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);
        Vector3Dd[] triangles = gizmo.buildUniformTriangles();

        for ( int i = 0; i + 2 < triangles.length; i += 3 ) {
            // The centroid of the triangle, not a vertex shared with the
            // other two (an unrealistic click, and a degenerate case for
            // ray-triangle intersection)
            Vector3Dd centroid = triangles[i].add(triangles[i + 1])
                .add(triangles[i + 2]).multiply(1.0/3.0);

            // Act: perpendicular to the plane of the triangle
            Ray ray = rayOntoPlaneOf(gizmo, ScaleGizmo.BAND_GROUPS[i/3], centroid);
            int selection = gizmo.pickElement(ray);

            // Assert
            assertThat(selection).isEqualTo(ScaleGizmo.UNIFORM_GROUP);
        }
    }

    @Test
    void given_rayThroughEmptySpace_when_picked_then_selectsNothing()
    {
        // Arrange
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);

        // Act: a corner of the viewport, away from the gizmo near the origin
        Ray ray = camera.generateRay(5, 5);
        int selection = gizmo.pickElement(ray);

        // Assert
        assertThat(selection).isEqualTo(ScaleGizmo.NULL_GROUP);
    }

    //= Numeric input =========================================================

    @Test
    void given_scaleFactors_when_inputGizmoAsked_then_showsThem()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());

        gizmo.setScale(new Vector3Dd(2, 0.5, 1.25));

        // Act
        InputGizmo inputGizmo = gizmo.getInputGizmo();

        // Assert
        assertThat(inputGizmo.getValue(0)).isCloseTo(2.0, offset(EPS));
        assertThat(inputGizmo.getValue(1)).isCloseTo(0.5, offset(EPS));
        assertThat(inputGizmo.getValue(2)).isCloseTo(1.25, offset(EPS));
    }

    @Test
    void given_digitsTyped_when_enterPressed_then_scaleFactorIsUpdated()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());
        KeyEvent digit = new KeyEvent();
        KeyEvent enter = new KeyEvent();

        digit.unicode_id = '3';
        enter.keycode = KeyEvent.KEY_ENTER;

        // Act
        gizmo.processKeyPressedEvent(digit);
        boolean changed = gizmo.processKeyPressedEvent(enter);

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.getScale().x()).isCloseTo(3.0, offset(EPS));
        assertThat(gizmo.getScale().y()).isCloseTo(1.0, offset(EPS));
        assertThat(gizmo.getScale().z()).isCloseTo(1.0, offset(EPS));
    }

    @Test
    void given_letterKeys_when_pressed_then_scaleSingleAxis()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());
        KeyEvent upperX = new KeyEvent();

        upperX.unicode_id = 'X';

        // Act
        boolean changed = gizmo.processKeyPressedEvent(upperX);

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.getScale().x()).isCloseTo(1.1, offset(EPS));
        assertThat(gizmo.getScale().y()).isCloseTo(1.0, offset(EPS));
        assertThat(gizmo.getScale().z()).isCloseTo(1.0, offset(EPS));
    }

    @Test
    void given_arrowKeys_when_pressedWithoutModifiers_then_theInputGizmoStepsInstead()
    {
        // Arrange: arrow keys are consumed by the input gizmo (selected box:
        // the X scale factor), not the old uniform scaling fallback
        ScaleGizmo gizmo = createGizmo(createCamera());
        KeyEvent up = new KeyEvent();

        up.keycode = KeyEvent.KEY_UP;

        // Act
        boolean changed = gizmo.processKeyPressedEvent(up);

        // Assert: UP is the level 2 step of InputGizmoValueChangeRules.forScale()
        assertThat(changed).isTrue();
        assertThat(gizmo.getScale().x()).isCloseTo(1.1, offset(EPS));
        assertThat(gizmo.getScale().y()).isCloseTo(1.0, offset(EPS));
    }

    @Test
    void given_arrowKeyWithControl_when_pressed_then_scalesEveryAxisUniformly()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());
        KeyEvent up = new KeyEvent();

        up.keycode = KeyEvent.KEY_UP;
        up.modifierMask = KeyEvent.MASK_CTRL;

        // Act
        boolean changed = gizmo.processKeyPressedEvent(up);

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.getScale().x()).isCloseTo(1.1, offset(EPS));
        assertThat(gizmo.getScale().y()).isCloseTo(1.1, offset(EPS));
        assertThat(gizmo.getScale().z()).isCloseTo(1.1, offset(EPS));
    }
}
