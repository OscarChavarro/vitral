package vsdk.toolkit.gui.gizmo;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

/**
Gizmo to specify the scale of an object along the X, Y and Z axes of its
frame (in the style of 3ds Max's scale gizmo, and following the structure of
`TranslateGizmo` and `RotateGizmo`).

- Geometric model: each axis is a straight cylinder (see `getElements`) from
  the origin of the frame out along it, capped by a small cube at its tip
  (both easy to point at with the cursor).
- Two-axis handles: for each pair of axes, a flat trapezoidal band (a quad,
  see `buildBandQuad`) contained in the plane those two axes span (XY, YZ or
  XZ): its two parallel sides join the axes at `BAND_INNER_REACH` and
  `BAND_OUTER_REACH` of their length, and its other two sides lie on the axes
  themselves. Picking one scales both of its axes (see `XY_GROUP`,
  `YZ_GROUP`, `XZ_GROUP`).
- Uniform (all-axis) handle: three triangles (see `buildUniformTriangles`),
  one in each of those same planes, all of them sharing the vertex at the
  origin of the frame and reaching the inner side of the bands, which they
  are flush with. Picking any of them scales the three axes together (see
  `UNIFORM_GROUP`).
- Size: like `TranslateGizmo` and `RotateGizmo`, the gizmo keeps an apparent
  size in pixels from its camera, scaled for the resolution of the screen
  (see `applyScale`).
- Selection: the handle under the cursor is the volatile selection and the
  one clicked is the persistent selection (see `pickElement`,
  `ScaleGizmoInteractionTechnique`). The axes the current selection scales
  (see `groupIncludesAxis`, `getCurrentSelection`) are drawn yellow instead
  of with their own color.
- Flat handles are not drawn as surfaces: only their contour is (see
  `buildContourSegments`), with each of its edges split in two halves, each
  one with the color of the axis it ends at. The handle that is the current
  selection draws its contour yellow, and only it fills its interior, with a
  translucent gray (see `HANDLE_FILL_COLOR`).
- Numeric input: the scale factors (see `getScale`) are shown and edited with
  a nested `InputGizmo` (see `getInputGizmo`), stepped by
  `InputGizmoValueChangeRules.forScale()`. It takes priority over the
  letter-driven scaling below while it consumes the key.
- Letter-driven scaling: `x`/`X`, `y`/`Y` and `z`/`Z` shrink/grow a single
  axis, and the arrow keys (when the input gizmo does not consume them, i.e.
  `Ctrl` or `Alt` is held) shrink/grow every axis uniformly.

The transformation matrix of the gizmo has the orientation and the position of
the frame its handles belong to; the scale factors themselves are kept apart
(see `getScale`), as they are not a rigid transformation of that frame.
*/
public class ScaleGizmo extends Gizmo {
    /// Number of axes, and of the values of the input gizmo
    public static final int AXIS_COUNT = 3;

    /// Selection groups
    public static final int NULL_GROUP = 0;
    public static final int X_AXIS_GROUP = 1;
    public static final int Y_AXIS_GROUP = 2;
    public static final int Z_AXIS_GROUP = 3;
    /// Two-axis handle: the trapezoidal band in the XY plane
    public static final int XY_GROUP = 4;
    /// Two-axis handle: the trapezoidal band in the YZ plane
    public static final int YZ_GROUP = 5;
    /// Two-axis handle: the trapezoidal band in the XZ plane
    public static final int XZ_GROUP = 6;
    /// All-axis handle: the three triangles that share the origin of the
    /// frame, one in each of the XY, YZ and XZ planes
    public static final int UNIFORM_GROUP = 7;

    /// The two-axis groups, in the order `buildBandQuad` and the renderers
    /// walk them
    public static final int[] BAND_GROUPS = {XY_GROUP, YZ_GROUP, XZ_GROUP};

    /// Indexes of `elementInstances`, in the order `getElements` gives them
    private static final int X_SHAFT_ELEMENT = 1;
    private static final int X_TIP_ELEMENT = 2;
    private static final int Y_SHAFT_ELEMENT = 3;
    private static final int Y_TIP_ELEMENT = 4;
    private static final int Z_SHAFT_ELEMENT = 5;
    private static final int Z_TIP_ELEMENT = 6;

    /// Size and contour width designed for legacy resolutions; the width
    /// matches TranslateGizmo#DEFAULT_LINE_WIDTH so both gizmos look
    /// visually consistent (same ribbon thickness) in the 3D UI
    public static final int DEFAULT_APPARENT_SIZE_IN_PIXELS = 100;
    public static final double DEFAULT_LINE_WIDTH = 1.0;

    /// Color the interior of the handle currently selected is filled with
    public static final ColorRgb HANDLE_FILL_COLOR = new ColorRgb(0.88, 0.88, 0.88);

    /// Total length of an axis (shaft and tip together), and shape of its
    /// parts, relative to the apparent size of the gizmo. The radius of the
    /// shaft is not here: it follows `lineWidth`, like the contour of the
    /// flat handles, so both look consistent (see `getLineWidthInWorldUnits`)
    private static final double AXIS_LENGTH = 0.8;
    private static final double TIP_SIZE = 0.06;
    /// Fractions of the length of an axis where the parallel sides of a
    /// two-axis band meet it. The inner one is also the boundary the uniform
    /// handle reaches, so band and uniform handle are flush
    private static final double BAND_INNER_REACH = 0.52;
    private static final double BAND_OUTER_REACH = 0.73;

    private Matrix4x4d T;
    private Camera camera;

    /// Scale factors shown (and edited) by the gizmo; kept apart from `T`,
    /// which only carries the orientation and position of its frame
    private Vector3Dd scale;

    /// Apparent size (in legacy resolution pixels) the user has chosen
    private int baseApparentSizeInPixels;
    /// Apparent size in pixels of the screen currently in use
    private int apparentSizeInPixels;
    /// Width, in pixels, of the contour of the flat handles
    private double lineWidth;
    private double currentScale;

    /// Geometric model based in primitive instancing: primitive concretions.
    /// The two-axis bands and the uniform handle are flat polygons instead
    /// (see `buildBandQuad`, `buildUniformTriangles`)
    private final Cone shaftModel;
    private final Box tipModel;

    /// Geometric model based in primitive instancing: primitive instances,
    /// always of size 6, in the order given by the `*_ELEMENT` constants
    private final ArrayList<SimpleBody> elementInstances;

    private final ColorRgb[] axisColors;

    /// Interaction state
    private final InputGizmo inputGizmo;
    private int persistentSelection;
    private int volatileSelection;

    /**
    @param cam camera the gizmo is seen from, or null if it is not known yet
    (see `setCamera`)
    */
    public ScaleGizmo(Camera cam)
    {
        T = new Matrix4x4d();
        scale = new Vector3Dd(1, 1, 1);
        baseApparentSizeInPixels = DEFAULT_APPARENT_SIZE_IN_PIXELS;
        apparentSizeInPixels = DEFAULT_APPARENT_SIZE_IN_PIXELS;
        lineWidth = DEFAULT_LINE_WIDTH;
        currentScale = 1.0;
        persistentSelection = NULL_GROUP;
        volatileSelection = NULL_GROUP;

        ReferenceFrameGizmo axisColorSource = new ReferenceFrameGizmo();

        axisColors = new ColorRgb[AXIS_COUNT];
        inputGizmo = new InputGizmo(AXIS_COUNT, InputGizmo.DECIMALS, 1,
            InputGizmoValueChangeRules.forScale());
        for ( int axis = 0; axis < AXIS_COUNT; axis++ ) {
            axisColors[axis] = axisColorSource.getAxisColor(axis);
            inputGizmo.setFieldColor(axis, axisColors[axis]);
        }

        double initialShaftRadius = getLineWidthInWorldUnits() / 2;
        shaftModel = new Cone(initialShaftRadius, initialShaftRadius, AXIS_LENGTH - TIP_SIZE);
        tipModel = new Box(TIP_SIZE, TIP_SIZE, TIP_SIZE);

        elementInstances = new ArrayList<>();
        for ( int i = 0; i < 6; i++ ) {
            elementInstances.add(new SimpleBody());
        }

        setCamera(cam);
    }

    /**
    Creates a gizmo with no camera yet (see `setCamera`); its apparent size
    can not be computed until one is set.
    */
    public ScaleGizmo()
    {
        this(null);
    }

    //= Camera and size =====================================================

    public final void setCamera(Camera cam)
    {
        camera = cam;
    }

    public Camera getCamera()
    {
        return camera;
    }

    public int getApparentSizeInPixels()
    {
        return apparentSizeInPixels;
    }

    public void setApparentSizeInPixels(int size)
    {
        apparentSizeInPixels = size;
    }

    /**
    @return the apparent size the gizmo is designed to have in legacy
    resolutions, in pixels; it is the size chosen by the user, before scaling
    it for the resolution of the screen
    */
    public int getBaseApparentSizeInPixels()
    {
        return baseApparentSizeInPixels;
    }

    /**
    @param size apparent size of the gizmo in legacy resolutions, in pixels;
    not positive values are ignored. It is used the next time `applyScale`
    is called
    */
    public void setBaseApparentSizeInPixels(int size)
    {
        if ( size > 0 ) {
            baseApparentSizeInPixels = size;
        }
    }

    /**
    @return the width, in pixels, of the contour of the flat handles
    */
    public double getLineWidth()
    {
        return lineWidth;
    }

    /**
    @param lineWidth width, in pixels, of the contour of the flat handles;
    not positive values are ignored
    */
    public void setLineWidth(double lineWidth)
    {
        if ( lineWidth > 0.0 ) {
            this.lineWidth = lineWidth;
        }
    }

    /**
    Sets the apparent size and the contour width of the gizmo to the values
    that make it look proportional to the screen resolution: the base values
    (designed for legacy resolutions) multiplied by the scale of the given
    scaler. The new size is used the next time the geometry state is updated
    (see `setTransformationMatrix`, `updateGeometryState`).

    @param scaler scaler informed of the resolution of the screen
    */
    public void applyScale(ViewportElementScaler scaler)
    {
        if ( scaler == null ) {
            return;
        }
        setApparentSizeInPixels(scaler.scaleSize(baseApparentSizeInPixels));
        setLineWidth(scaler.scaleLength(DEFAULT_LINE_WIDTH));
    }

    /**
    @return the factor applied to the gizmo geometry so it keeps its apparent
    size in pixels, as seen from its camera
    */
    public double getCurrentScale()
    {
        return currentScale;
    }

    /**
    @return the width of the shaft of the axes and of the contour of the flat
    handles, converted from pixels to world units, as seen from its camera at
    its current apparent size
    */
    public double getLineWidthInWorldUnits()
    {
        return lineWidth * currentScale / apparentSizeInPixels;
    }

    //= Transformation and scale factors ===================================

    public Vector3Dd getPosition()
    {
        return T.extractTranslation();
    }

    public void setPosition(Vector3Dd p)
    {
        setTransformationMatrix(T.withTranslation(p));
    }

    /**
    Sets the frame (orientation and position) of the gizmo and recalculates
    its geometry.
    @param T orientation and position of the frame of the gizmo
    */
    public void setTransformationMatrix(Matrix4x4d T)
    {
        this.T = T;
        updateGeometryState();
    }

    public Matrix4x4d getTransformationMatrix()
    {
        return T;
    }

    /**
    @return the scale factors shown (and edited) by the gizmo, along the X, Y
    and Z axes of its frame
    */
    public Vector3Dd getScale()
    {
        return scale;
    }

    /**
    @param scale scale factors, along the X, Y and Z axes of the frame of the
    gizmo; null is ignored
    */
    public void setScale(Vector3Dd scale)
    {
        if ( scale != null ) {
            this.scale = scale;
        }
    }

    /**
    Recalculates the apparent size and the geometry of the gizmo from its
    current transformation, apparent size, selection and camera. The size is
    kept as it was if it can not be measured, or there is no camera yet.
    PRE: the transformation matrix of the gizmo has been set.
    */
    public void updateGeometryState()
    {
        updateScale();
        calculateGeometryState();
    }

    private void updateScale()
    {
        if ( camera == null || T == null ) {
            return;
        }
        camera.updateVectors();

        Vector3Dd p = getPosition();
        Vector3Dd right = camera.getLeft().multiply(-1).normalized();
        Vector3Dd a = camera.projectPointUsingRayMethod(p);
        Vector3Dd b = camera.projectPointUsingRayMethod(p.add(right));

        if ( a != null && b != null ) {
            double factor = Vector3Dd.distance(a, b);

            if ( factor > VSDK.EPSILON ) {
                currentScale = ((double)apparentSizeInPixels)/factor;
            }
        }
    }

    //= Selection ============================================================

    /**
    @return the selected group (one of the `*_GROUP` constants) chosen by the
    user with a click, or `NULL_GROUP`
    */
    public int getPersistentSelection()
    {
        return persistentSelection;
    }

    /**
    @param selection group (one of the `*_GROUP` constants) chosen by the user
    with a click
    */
    public void setPersistentSelection(int selection)
    {
        persistentSelection = selection;
    }

    /**
    @return the group (one of the `*_GROUP` constants) currently under the
    cursor, or `NULL_GROUP`
    */
    public int getVolatileSelection()
    {
        return volatileSelection;
    }

    /**
    @param selection group (one of the `*_GROUP` constants) currently under
    the cursor
    */
    public void setVolatileSelection(int selection)
    {
        volatileSelection = selection;
    }

    /**
    @return the group (one of the `*_GROUP` constants) that is highlighted and
    manipulated: the one under the cursor, or the chosen one if the cursor is
    not over any group
    */
    public int getCurrentSelection()
    {
        if ( volatileSelection == NULL_GROUP ) {
            return persistentSelection;
        }
        return volatileSelection;
    }

    /**
    @param group one of the `*_GROUP` constants
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return true if scaling with `group` changes `axis`
    */
    public static boolean groupIncludesAxis(int group, int axis)
    {
        checkAxis(axis);
        return switch ( group ) {
            case X_AXIS_GROUP -> axis == 0;
            case Y_AXIS_GROUP -> axis == 1;
            case Z_AXIS_GROUP -> axis == 2;
            case XY_GROUP -> axis == 0 || axis == 1;
            case YZ_GROUP -> axis == 1 || axis == 2;
            case XZ_GROUP -> axis == 0 || axis == 2;
            case UNIFORM_GROUP -> true;
            default -> false;
        };
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return true if the axis is highlighted (drawn yellow) because the group
    currently selected scales it
    */
    public boolean isAxisHighlighted(int axis)
    {
        return groupIncludesAxis(getCurrentSelection(), axis);
    }

    /**
    @param group `XY_GROUP`, `YZ_GROUP` or `XZ_GROUP`
    @return true if the band is the current selection, so it is drawn yellow
    */
    public boolean isBandHighlighted(int group)
    {
        return getCurrentSelection() == group;
    }

    /**
    @return true if the uniform (all-axis) handle is the current selection, so
    it is drawn yellow
    */
    public boolean isUniformHighlighted()
    {
        return getCurrentSelection() == UNIFORM_GROUP;
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return the color the cylinder and the cube of the axis must be drawn
    with: the one of the axis, or yellow if the current selection scales it
    */
    public ColorRgb getAxisDisplayColor(int axis)
    {
        checkAxis(axis);
        return isAxisHighlighted(axis) ? InputGizmo.HIGHLIGHT_COLOR : axisColors[axis];
    }

    /**
    A straight piece of the contour of the flat handles, in world space, with
    the color it must be drawn with.
    @param start first end of the piece
    @param end second end of the piece
    @param color color the piece must be drawn with
    */
    public record ContourSegment(Vector3Dd start, Vector3Dd end, ColorRgb color) {
    }

    /**
    Gives the contour of the flat handles, which are drawn as outlines and not
    as surfaces: the inner and the outer edge of each two-axis band (the inner
    one is also the boundary the uniform handle reaches). Each edge is split at
    its middle point, so each half takes the color of the axis it ends at,
    unless the handle it belongs to is the current selection: then the whole
    edge is yellow.
    PRE: the transformation matrix of the gizmo has been set.

    @return the pieces of the contour, in world space
    */
    public ArrayList<ContourSegment> buildContourSegments()
    {
        ArrayList<ContourSegment> segments = new ArrayList<ContourSegment>();

        for ( int group : BAND_GROUPS ) {
            int[] axes = bandAxes(group);
            Vector3Dd[] quad = buildBandQuad(group);

            // The inner edge is shared with the uniform handle, so either of
            // the two being selected highlights it
            addContourEdge(segments, quad[0], quad[2], axes,
                isBandHighlighted(group) || isUniformHighlighted());
            addContourEdge(segments, quad[1], quad[3], axes, isBandHighlighted(group));
        }
        return segments;
    }

    /**
    Adds the two halves of an edge that goes from the axis `axes[0]` to the
    axis `axes[1]`, each one with the color of the axis it ends at (or yellow
    if the handle is highlighted).
    */
    private void addContourEdge(ArrayList<ContourSegment> segments,
                                Vector3Dd a, Vector3Dd b, int[] axes, boolean highlighted)
    {
        Vector3Dd middle = a.add(b).multiply(0.5);
        ColorRgb colorA = highlighted ? InputGizmo.HIGHLIGHT_COLOR : axisColors[axes[0]];
        ColorRgb colorB = highlighted ? InputGizmo.HIGHLIGHT_COLOR : axisColors[axes[1]];

        segments.add(new ContourSegment(a, middle, colorA));
        segments.add(new ContourSegment(middle, b, colorB));
    }

    /**
    Finds the handle a ray points at, using the geometric model of the gizmo:
    the axis cylinders and cubes, the two-axis bands and the triangles of the
    uniform handle.
    PRE: the transformation matrix of the gizmo has been set.

    @param ray ray, in world space, i.e. the one the cursor defines
    @return the `*_GROUP` constant of the nearest handle hit by the ray, or
    `NULL_GROUP` if it hits none
    */
    public int pickElement(Ray ray)
    {
        double nearestDistance = Double.MAX_VALUE;
        int nearestGroup = NULL_GROUP;
        int index = 1;

        for ( SimpleBody element : elementInstances ) {
            Ray hit = element.getGeometry() != null ?
                element.doIntersectionFirstHit(ray.withT(Double.MAX_VALUE)) : null;

            if ( hit != null && hit.getT() < nearestDistance ) {
                nearestDistance = hit.getT();
                nearestGroup = groupOfElement(index);
            }
            index++;
        }

        for ( int group : BAND_GROUPS ) {
            Vector3Dd[] quad = buildBandQuad(group);
            // The quad, as the two triangles of its strip
            Double t = nearestOfTwoTriangles(ray, quad[0], quad[1], quad[2],
                quad[1], quad[3], quad[2]);

            if ( t != null && t < nearestDistance ) {
                nearestDistance = t;
                nearestGroup = group;
            }
        }

        Vector3Dd[] triangles = buildUniformTriangles();

        for ( int i = 0; i + 2 < triangles.length; i += 3 ) {
            Double t = rayTriangleT(ray.getOrigin(), ray.getDirection(),
                triangles[i], triangles[i + 1], triangles[i + 2]);

            if ( t != null && t < nearestDistance ) {
                nearestDistance = t;
                nearestGroup = UNIFORM_GROUP;
            }
        }

        return nearestGroup;
    }

    /**
    @param element one of the `*_ELEMENT` constants
    @return the group the element belongs to
    */
    private static int groupOfElement(int element)
    {
        return switch ( element ) {
            case X_SHAFT_ELEMENT, X_TIP_ELEMENT -> X_AXIS_GROUP;
            case Y_SHAFT_ELEMENT, Y_TIP_ELEMENT -> Y_AXIS_GROUP;
            case Z_SHAFT_ELEMENT, Z_TIP_ELEMENT -> Z_AXIS_GROUP;
            default -> NULL_GROUP;
        };
    }

    //= Geometry ==============================================================

    public ArrayList<SimpleBody> getElements()
    {
        return elementInstances;
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return the direction, in world space, of the axis
    */
    public Vector3Dd getAxisDirection(int axis)
    {
        checkAxis(axis);

        Matrix4x4d rotation = new Matrix4x4d(T).withoutTranslation();

        return rotation.multiply(unitAxis(axis)).normalized();
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return the position, in world space, of the tip (the center of its cube)
    of the axis
    PRE: the transformation matrix of the gizmo has been set.
    */
    public Vector3Dd getTipPosition(int axis)
    {
        double tipSize = currentScale*TIP_SIZE;

        return axisPoint(axis, 1.0).subtract(getAxisDirection(axis).multiply(tipSize/2));
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @param reach fraction of the length of the axis
    @return the point of the axis at that fraction of its length, in world
    space
    PRE: the transformation matrix of the gizmo has been set.
    */
    public Vector3Dd axisPoint(int axis, double reach)
    {
        checkAxis(axis);

        return getPosition().add(
            getAxisDirection(axis).multiply(reach*currentScale*AXIS_LENGTH));
    }

    /**
    @param group `XY_GROUP`, `YZ_GROUP` or `XZ_GROUP`
    @return the two axes (0, 1 or 2) whose plane contains the handles of the
    group
    */
    public static int[] bandAxes(int group)
    {
        return switch ( group ) {
            case XY_GROUP -> new int[] {0, 1};
            case YZ_GROUP -> new int[] {1, 2};
            case XZ_GROUP -> new int[] {0, 2};
            default -> throw new IllegalArgumentException("Invalid two-axis group: " +
                group + ". It must be XY_GROUP, YZ_GROUP or XZ_GROUP.");
        };
    }

    /**
    Gives the trapezoidal band of a two-axis handle: a quad contained in the
    plane of the two axes of the group, whose parallel sides join them at
    `BAND_INNER_REACH` and `BAND_OUTER_REACH` of their length.
    PRE: the transformation matrix of the gizmo has been set.

    @param group `XY_GROUP`, `YZ_GROUP` or `XZ_GROUP`
    @return the 4 vertices of the quad, in world space, in the order of a
    triangle strip (inner and outer point of the first axis, then the ones of
    the second one)
    */
    public Vector3Dd[] buildBandQuad(int group)
    {
        int[] axes = bandAxes(group);

        return new Vector3Dd[] {
            axisPoint(axes[0], BAND_INNER_REACH),
            axisPoint(axes[0], BAND_OUTER_REACH),
            axisPoint(axes[1], BAND_INNER_REACH),
            axisPoint(axes[1], BAND_OUTER_REACH)
        };
    }

    /**
    Gives the three triangles of the uniform (all-axis) handle: one in each of
    the XY, YZ and XZ planes, all of them with a vertex at the origin of the
    frame of the gizmo and the other two on the axes of their plane, at
    `BAND_INNER_REACH` of their length, so each triangle is flush with the
    inner side of the band of its plane.
    PRE: the transformation matrix of the gizmo has been set.

    @return the 9 vertices (3 consecutive per triangle), in world space
    */
    public Vector3Dd[] buildUniformTriangles()
    {
        Vector3Dd origin = getPosition();
        Vector3Dd[] vertices = new Vector3Dd[3*BAND_GROUPS.length];
        int index = 0;

        for ( int group : BAND_GROUPS ) {
            int[] axes = bandAxes(group);

            vertices[index++] = origin;
            vertices[index++] = axisPoint(axes[0], BAND_INNER_REACH);
            vertices[index++] = axisPoint(axes[1], BAND_INNER_REACH);
        }
        return vertices;
    }

    /**
    Recalculates the geometry of the elements of the gizmo (the cylinder and
    the cube of each axis) from its current transformation, apparent size and
    selection. The two-axis bands and the uniform handle are flat polygons,
    built on demand (see `buildBandQuad`, `buildUniformTriangles`).
    PRE: the transformation matrix of the gizmo has been set.
    */
    private void calculateGeometryState()
    {
        if ( T == null ) {
            return;
        }

        Matrix4x4d R = new Matrix4x4d(T).withoutTranslation();
        Vector3Dd origin = getPosition();
        double totalLength = currentScale*AXIS_LENGTH;
        double tipSize = currentScale*TIP_SIZE;
        double shaftLength = Math.max(0.0, totalLength - tipSize);

        double shaftRadius = getLineWidthInWorldUnits() / 2;
        shaftModel.setBaseRadius(shaftRadius);
        shaftModel.setTopRadius(shaftRadius);
        shaftModel.setHeight(shaftLength);
        tipModel.setSize(tipSize, tipSize, tipSize);

        for ( int axis = 0; axis < AXIS_COUNT; axis++ ) {
            SimpleMaterial material = createMaterial(getAxisDisplayColor(axis));
            Matrix4x4d eleR = R.multiply(axisTilt(axis));
            Matrix4x4d eleRi = new Matrix4x4d(eleR).invert();

            SimpleBody shaft = elementInstances.get(2*axis);

            shaft.setGeometry(shaftModel);
            shaft.setMaterial(material);
            shaft.setRotation(eleR);
            shaft.setRotationInverse(eleRi);
            shaft.setPosition(origin);

            SimpleBody tip = elementInstances.get(2*axis + 1);

            tip.setGeometry(tipModel);
            tip.setMaterial(material);
            tip.setRotation(eleR);
            tip.setRotationInverse(eleRi);
            tip.setPosition(getTipPosition(axis));
        }
    }

    private static SimpleMaterial createMaterial(ColorRgb c)
    {
        SimpleMaterial m = new SimpleMaterial();

        m = m.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m = m.withDiffuse(c);
        m = m.withSpecular(new ColorRgb(1, 1, 1));
        return m;
    }

    /**
    @param axis 0, 1 or 2
    @return the local rotation that takes the Z axis (the axis `shaftModel`
    and `tipModel` grow along) to the X, Y or Z axis of the frame
    */
    private static Matrix4x4d axisTilt(int axis)
    {
        return switch ( axis ) {
            case 0 -> new Matrix4x4d().axisRotation(Math.toRadians(90.0), 0, 1, 0);
            case 1 -> new Matrix4x4d().axisRotation(Math.toRadians(90.0), -1, 0, 0);
            default -> new Matrix4x4d();
        };
    }

    /**
    @return the nearest of the two hits of the ray with two triangles, or null
    if it hits neither
    */
    private static Double nearestOfTwoTriangles(Ray ray,
                                                Vector3Dd a1, Vector3Dd b1, Vector3Dd c1,
                                                Vector3Dd a2, Vector3Dd b2, Vector3Dd c2)
    {
        Double t1 = rayTriangleT(ray.getOrigin(), ray.getDirection(), a1, b1, c1);
        Double t2 = rayTriangleT(ray.getOrigin(), ray.getDirection(), a2, b2, c2);

        if ( t1 == null ) {
            return t2;
        }
        if ( t2 == null ) {
            return t1;
        }
        return Math.min(t1, t2);
    }

    /**
    Ray-triangle intersection (Moller-Trumbore), used to pick the flat
    handles, which are not backed by a `SimpleBody`.
    @return the distance along `dir` to the hit, or null if there is none
    */
    private static Double rayTriangleT(Vector3Dd origin, Vector3Dd dir,
                                       Vector3Dd a, Vector3Dd b, Vector3Dd c)
    {
        Vector3Dd edge1 = b.subtract(a);
        Vector3Dd edge2 = c.subtract(a);
        Vector3Dd h = dir.crossProduct(edge2);
        double det = edge1.dotProduct(h);

        if ( Math.abs(det) < VSDK.EPSILON ) {
            return null;
        }

        double invDet = 1.0/det;
        Vector3Dd s = origin.subtract(a);
        double u = invDet*s.dotProduct(h);

        if ( u < 0.0 || u > 1.0 ) {
            return null;
        }

        Vector3Dd q = s.crossProduct(edge1);
        double v = invDet*dir.dotProduct(q);

        if ( v < 0.0 || u + v > 1.0 ) {
            return null;
        }

        double t = invDet*edge2.dotProduct(q);

        return t > VSDK.EPSILON ? t : null;
    }

    private static Vector3Dd unitAxis(int axis)
    {
        return new Vector3Dd(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0);
    }

    private static void checkAxis(int axis)
    {
        if ( axis < 0 || axis >= AXIS_COUNT ) {
            throw new IllegalArgumentException("Invalid axis: " + axis +
                ". It must be 0, 1 or 2 for the X, Y or Z axis.");
        }
    }

    //= Numeric input =========================================================

    /**
    @return the input gizmo that shows (and lets the user type) the scale
    factors of this gizmo, updated with its current values
    */
    public InputGizmo getInputGizmo()
    {
        inputGizmo.setValue(0, scale.x());
        inputGizmo.setValue(1, scale.y());
        inputGizmo.setValue(2, scale.z());
        for ( int axis = 0; axis < AXIS_COUNT; axis++ ) {
            inputGizmo.setFieldHighlighted(axis, isAxisHighlighted(axis));
        }
        return inputGizmo;
    }

    //= Events ================================================================

    public boolean processMouseEvent(MouseEvent mouseEvent)
    {
        return false;
    }

    /**
    Lets the numeric input gizmo use the key first (digits, `-`, decimal
    point, TAB, BACKSPACE, arrows, and ENTER / ESC while editing); if it does
    not consume the key, `x`/`X`, `y`/`Y` and `z`/`Z` shrink/grow a single
    axis, and the arrow keys shrink/grow every axis uniformly.
    @param keyEvent key press
    @return true if the scale factors changed
    */
    public boolean processKeyPressedEvent(KeyEvent keyEvent)
    {
        if ( inputGizmo.consumesKey(keyEvent) ) {
            // Boxes not being edited must start from the current scale
            // factors, not from whatever they showed the last time they were
            // synced (see getInputGizmo)
            getInputGizmo();
            inputGizmo.processKeyPressedEvent(keyEvent);
            if ( inputGizmo.consumeCommit() ) {
                double[] values = inputGizmo.getValuesWithEdits();

                inputGizmo.cancelEditing();
                scale = new Vector3Dd(values[0], values[1], values[2]);
                return true;
            }
            return false;
        }

        char unicode_id = keyEvent.unicode_id;
        int keycode = keyEvent.keycode;
        double deltaMov = 1.1;
        boolean updateNeeded = false;
        Vector3Dd s = scale;

        if ( unicode_id != KeyEvent.KEY_NONE ) {
            switch ( unicode_id ) {
              case 'x':
                s = s.withX(s.x() / deltaMov);
                updateNeeded = true;
                break;
              case 'X':
                s = s.withX(s.x() * deltaMov);
                updateNeeded = true;
                break;
              case 'y':
                s = s.withY(s.y() / deltaMov);
                updateNeeded = true;
                break;
              case 'Y':
                s = s.withY(s.y() * deltaMov);
                updateNeeded = true;
                break;
              case 'z':
                s = s.withZ(s.z() / deltaMov);
                updateNeeded = true;
                break;
              case 'Z':
                s = s.withZ(s.z() * deltaMov);
                updateNeeded = true;
                break;
              default:
                break;
            }
        }
        else {
            switch ( keycode ) {
              case KeyEvent.KEY_UP:
              case KeyEvent.KEY_RIGHT:
                s = new Vector3Dd(s.x() * deltaMov, s.y() * deltaMov, s.z() * deltaMov);
                updateNeeded = true;
                break;
              case KeyEvent.KEY_LEFT:
              case KeyEvent.KEY_DOWN:
                s = new Vector3Dd(s.x() / deltaMov, s.y() / deltaMov, s.z() / deltaMov);
                updateNeeded = true;
                break;
              default:
                break;
            }
        }

        scale = s;
        return updateNeeded;
    }

    public boolean processKeyReleasedEvent(KeyEvent mouseEvent)
    {
        return false;
    }

    public boolean processMousePressedEvent(MouseEvent e)
    {
        return false;
    }

    public boolean processMouseReleasedEvent(MouseEvent e)
    {
        return false;
    }

    public boolean processMouseClickedEvent(MouseEvent e)
    {
        return false;
    }

    public boolean processMouseMovedEvent(MouseEvent e)
    {
        return false;
    }

    public boolean processMouseDraggedEvent(MouseEvent e)
    {
        return false;
    }

    public boolean processMouseWheelEvent(MouseEvent e)
    {
        return false;
    }
}
