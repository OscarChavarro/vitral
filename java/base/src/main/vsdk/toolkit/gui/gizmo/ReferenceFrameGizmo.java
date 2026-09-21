package vsdk.toolkit.gui.gizmo;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

/**
Gizmo that represents the orientation of the world reference frame (X, Y and
Z axes) as seen from a camera. It is designed to be drawn as a small
indicator over a corner of a viewport, as done by 3D modeling programs.

This class holds the model needed to draw the gizmo (size, axes, colors,
labels and label positions) and the operations to estimate the rotation the
axes must have to be shown from a given camera. It does not depend on any
rendering technology: renderers such as `Jogl4ReferenceFrameGizmoRenderer`
are responsible for the actual painting.

The gizmo is defined in a canonical space where the origin is at the center
of the square area of `getSizeInPixels()` pixels devoted to it, and the
visible axes have length `getAxisLength()` (the area covers the range
[-1, 1] in each direction).
*/
public class ReferenceFrameGizmo extends Gizmo {
    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;
    public static final int AXIS_Z = 2;
    public static final int NUMBER_OF_AXES = 3;

    public static final int DEFAULT_SIZE_IN_PIXELS = 64;
    public static final double DEFAULT_AXIS_LENGTH = 1.0;
    public static final double DEFAULT_LINE_WIDTH = 1.0;

    private static final ColorRgb[] DEFAULT_AXIS_COLORS = {
        new ColorRgb(0.78, 0, 0),
        new ColorRgb(0, 0.61, 0),
        new ColorRgb(0, 0, 0.76)
    };
    private static final String[] DEFAULT_AXIS_LABELS = {"X", "Y", "Z"};

    private int sizeInPixels;
    private double axisLength;
    private double lineWidth;
    private boolean visible;
    private final ColorRgb[] axisColors;
    private final String[] axisLabels;

    public ReferenceFrameGizmo() {
        sizeInPixels = DEFAULT_SIZE_IN_PIXELS;
        axisLength = DEFAULT_AXIS_LENGTH;
        lineWidth = DEFAULT_LINE_WIDTH;
        visible = true;
        axisColors = DEFAULT_AXIS_COLORS.clone();
        axisLabels = DEFAULT_AXIS_LABELS.clone();
    }

    /**
    @return the width and height, in pixels, of the square area where the
    gizmo is drawn
    */
    public int getSizeInPixels() {
        return sizeInPixels;
    }

    /**
    @param sizeInPixels width and height, in pixels, of the square area where
    the gizmo is drawn; not positive values are ignored
    */
    public void setSizeInPixels(int sizeInPixels) {
        if ( sizeInPixels > 0 ) {
            this.sizeInPixels = sizeInPixels;
        }
    }

    /**
    @return the length of the axes, in the canonical space of the gizmo
    */
    public double getAxisLength() {
        return axisLength;
    }

    /**
    @param axisLength length of the axes, in the canonical space of the gizmo;
    not positive values are ignored
    */
    public void setAxisLength(double axisLength) {
        if ( axisLength > 0.0 ) {
            this.axisLength = axisLength;
        }
    }

    /**
    @return the width, in pixels, of the lines that draw the axes
    */
    public double getLineWidth() {
        return lineWidth;
    }

    /**
    @param lineWidth width, in pixels, of the lines that draw the axes; not
    positive values are ignored
    */
    public void setLineWidth(double lineWidth) {
        if ( lineWidth > 0.0 ) {
            this.lineWidth = lineWidth;
        }
    }

    /**
    Sets the size of the area and the line width of the gizmo to the values
    that make it look proportional to the screen resolution: the default
    values (designed for legacy resolutions) multiplied by the scale of the
    given scaler.

    @param scaler scaler informed of the resolution of the screen
    */
    public void applyScale(ViewportElementScaler scaler) {
        if ( scaler == null ) {
            return;
        }
        setSizeInPixels(scaler.scaleSize(DEFAULT_SIZE_IN_PIXELS));
        setLineWidth(scaler.scaleLength(DEFAULT_LINE_WIDTH));
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @return the end point of the axis, in the canonical space of the gizmo
    */
    public Vector3Dd getAxisEnd(int axis) {
        checkAxis(axis);
        return unitVector(axis).multiply(axisLength);
    }

    /**
    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @return the color used to draw the axis and its label
    */
    public ColorRgb getAxisColor(int axis) {
        checkAxis(axis);
        return axisColors[axis];
    }

    /**
    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @return the text that names the axis
    */
    public String getAxisLabel(int axis) {
        checkAxis(axis);
        return axisLabels[axis];
    }

    /**
    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @return the position, in the canonical space of the gizmo, where the label
    of the axis must be anchored: the end of the axis
    */
    public Vector3Dd getLabelPosition(int axis) {
        return getAxisEnd(axis);
    }

    /**
    Estimates the rotation to apply to the axes of the gizmo (defined in world
    space) so they are seen from a camera with the given orientation: the
    inverse of the camera rotation, composed with the change of convention
    between the camera frame (front, left, up) and the canonical space of the
    gizmo, where the screen plane is XY and Z points to the viewer.

    @param cameraRotation rotation of the camera, as given by
    `Camera.getRotation()`
    @return the rotation matrix to apply to the axes of the gizmo
    */
    public Matrix4x4d estimateOrientation(Matrix4x4d cameraRotation) {
        Matrix4x4d viewRotation = new Matrix4x4d()
            .axisRotation(Math.toRadians(90), -1, 0, 0)
            .multiply(new Matrix4x4d().axisRotation(Math.toRadians(90), 0, 0, 1));

        return viewRotation.multiply(cameraRotation.invert());
    }

    /**
    Estimates the direction in which an axis of the gizmo points, once the
    orientation for the given camera is applied.

    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @param cameraRotation rotation of the camera, as given by
    `Camera.getRotation()`
    @return an unit vector with the estimated direction of the axis
    */
    public Vector3Dd estimateAxisDirection(int axis, Matrix4x4d cameraRotation) {
        checkAxis(axis);
        return estimateOrientation(cameraRotation).multiply(unitVector(axis)).normalized();
    }

    /**
    Builds the geometry to draw an axis as a line with thickness, seen from a
    camera: a triangle strip (a rectangle of 4 vertices, in strip order) in
    the canonical space of the gizmo, over the plane of the screen. The width
    of the rectangle is `getLineWidth()` pixels, and it is extended half of
    that width at both ends (square caps), so the joint of the axes at the
    origin has no gaps and an axis pointing to the viewer is seen as a dot.

    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @param cameraRotation rotation of the camera, as given by
    `Camera.getRotation()`
    @return the 4 vertices of the triangle strip; the z coordinates keep the
    depth of the ends of the axis
    */
    public Vector3Dd[] buildAxisStrip(int axis, Matrix4x4d cameraRotation) {
        checkAxis(axis);

        Matrix4x4d orientation = estimateOrientation(cameraRotation);
        Vector3Dd start = orientation.multiply(new Vector3Dd(0, 0, 0));
        Vector3Dd end = orientation.multiply(getAxisEnd(axis));

        double dx = end.x() - start.x();
        double dy = end.y() - start.y();
        double length = Math.sqrt(dx * dx + dy * dy);

        if ( length < VSDK.EPSILON ) {
            dx = 1;
            dy = 0;
        }
        else {
            dx /= length;
            dy /= length;
        }

        // One canonical unit is half the size of the area, in pixels
        double halfWidth = lineWidth / sizeInPixels;
        double px = -dy * halfWidth;
        double py = dx * halfWidth;
        double sx = start.x() - dx * halfWidth;
        double sy = start.y() - dy * halfWidth;
        double ex = end.x() + dx * halfWidth;
        double ey = end.y() + dy * halfWidth;

        return new Vector3Dd[] {
            new Vector3Dd(sx + px, sy + py, start.z()),
            new Vector3Dd(sx - px, sy - py, start.z()),
            new Vector3Dd(ex + px, ey + py, end.z()),
            new Vector3Dd(ex - px, ey - py, end.z())
        };
    }

    private static Vector3Dd unitVector(int axis) {
        return new Vector3Dd(
            axis == AXIS_X ? 1 : 0,
            axis == AXIS_Y ? 1 : 0,
            axis == AXIS_Z ? 1 : 0);
    }

    private static void checkAxis(int axis) {
        if ( axis < 0 || axis >= NUMBER_OF_AXES ) {
            throw new IllegalArgumentException("axis must be AXIS_X, AXIS_Y or AXIS_Z");
        }
    }
}
