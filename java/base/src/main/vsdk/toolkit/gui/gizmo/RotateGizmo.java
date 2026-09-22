package vsdk.toolkit.gui.gizmo;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

/**
Gizmo to specify the orientation of an object, made of three rings, one around
each axis of the frame of the gizmo (in the style of the rotation gizmo of
3D Studio Max, and following the structure of `TranslateGizmo`).

- Geometric model: each ring has an internal model based on a `Torus` (a
  `SimpleBody` of the ring), used to interact with the mouse: the tube of the
  torus is a little thicker than the drawn ring, so it is easy to point at.
- Size: like `TranslateGizmo`, the gizmo keeps an apparent size in pixels
  from its camera, and its base size and line widths are designed for legacy
  resolutions and scaled for the one of the screen (see `applyScale`).
- Drawing: rings are drawn as strips of quads facing the camera (see
  `buildRingStrip`), so each ring can have its own width in pixels.
- Selection: the ring under the cursor is the volatile selection and the one
  clicked is the persistent selection (see `RotateGizmoInteractionTechnique`).
  Rings are drawn with the color of their axis, unless they are the current
  selection (see `getCurrentSelection`): then they are yellow.
- Rotation arc: while a ring is being dragged, the gizmo keeps the arc the
  rotation has swept (see `setArc`), to be drawn as a translucent sector with
  the color of its ring (never yellow), and labeled with its angle.
- Numeric input: the angles (in degrees, with two decimals) of the orientation
  of the gizmo are shown and edited with a nested `InputGizmo`. They are the
  angles of the rotations around the X, Y and Z axes, applied in that
  order, that give the orientation (see `extractAnglesInDegrees`).

The transformation matrix of the gizmo has the orientation and the position
of the frame the rings belong to.
*/
public class RotateGizmo extends Gizmo {
    public static final int NULL_GROUP = 0;
    public static final int X_RING_GROUP = 1;
    public static final int Y_RING_GROUP = 2;
    public static final int Z_RING_GROUP = 3;

    /// Number of rings, and of the values of the input gizmo
    public static final int RING_COUNT = 3;

    /// Size and line width designed for legacy resolutions
    public static final int DEFAULT_APPARENT_SIZE_IN_PIXELS = 100;
    public static final double DEFAULT_LINE_WIDTH = 2.0;

    /// Decimals of the angles of the input gizmo, and maximum digits of the
    /// integer part (-180 to 180)
    public static final int ANGLE_DECIMALS = 2;
    public static final int ANGLE_INTEGER_DIGITS = 3;

    /// Number of straight pieces of the strip of a ring
    public static final int RING_SEGMENTS = 96;

    /// Maximum angle of each triangle of the sector that shows the rotation arc
    public static final double ARC_STEP_IN_DEGREES = 3.0;
    /// Distance of the label of the rotation arc from the center of the gizmo,
    /// relative to the radius of the rings
    private static final double ARC_LABEL_DISTANCE = 1.2;

    /// Radius of the rings, and tolerance to point at them with the cursor,
    /// relative to the apparent size of the gizmo (1.0 is the size in pixels)
    private static final double RING_RADIUS = 0.8;
    private static final double PICK_TOLERANCE = 0.06;
    private static final double MAX_TUBE_TO_RING_RADIUS_RATIO = 0.5;

    /// Rotation of the local frame of each torus (whose axis is Z) so its
    /// axis is the axis of its ring
    private static final double[][] RING_TILT = {
        {Math.toRadians(90.0), 0, 1, 0},
        {Math.toRadians(90.0), -1, 0, 0},
        {0, 0, 0, 1}
    };

    private Matrix4x4d T;
    private Camera camera;

    /// Geometric model based in primitive instancing: one torus for each ring
    private final Torus[] ringModels;
    private final SimpleBody[] ringInstances;
    private final ColorRgb[] axisColors;

    /// Apparent size (in legacy resolution pixels) the user has chosen
    private int baseApparentSizeInPixels;
    /// Apparent size in pixels of the screen currently in use
    private int apparentSizeInPixels;
    private final double[] baseRingLineWidths;
    private final double[] ringLineWidths;
    private double currentScale;

    /// Rotation arc: ring dragged (-1 if none), the axes of its plane (fixed
    /// while dragging) and the angles, in radians, where the arc starts and
    /// how much it goes on (negative for the other way)
    private int arcRing;
    private Vector3Dd arcU;
    private Vector3Dd arcV;
    private double arcStartAngle;
    private double arcSweep;

    /// Interaction state
    private final InputGizmo inputGizmo;
    private int persistentSelection;
    private int volatileSelection;

    /**
    @param cam camera the gizmo is seen from
    */
    public RotateGizmo(Camera cam)
    {
        T = new Matrix4x4d();
        baseApparentSizeInPixels = DEFAULT_APPARENT_SIZE_IN_PIXELS;
        apparentSizeInPixels = DEFAULT_APPARENT_SIZE_IN_PIXELS;
        currentScale = 1.0;
        persistentSelection = NULL_GROUP;
        volatileSelection = NULL_GROUP;
        arcRing = -1;

        ReferenceFrameGizmo axisColorSource = new ReferenceFrameGizmo();

        axisColors = new ColorRgb[RING_COUNT];
        ringModels = new Torus[RING_COUNT];
        ringInstances = new SimpleBody[RING_COUNT];
        baseRingLineWidths = new double[RING_COUNT];
        ringLineWidths = new double[RING_COUNT];
        inputGizmo = new InputGizmo(RING_COUNT, ANGLE_DECIMALS, ANGLE_INTEGER_DIGITS,
            InputGizmoValueChangeRules.forRotation());

        for ( int ring = 0; ring < RING_COUNT; ring++ ) {
            axisColors[ring] = axisColorSource.getAxisColor(ring);
            inputGizmo.setFieldColor(ring, axisColors[ring]);
            ringModels[ring] = new Torus(RING_RADIUS, PICK_TOLERANCE);
            ringInstances[ring] = new SimpleBody();
            ringInstances[ring].setGeometry(ringModels[ring]);
            baseRingLineWidths[ring] = DEFAULT_LINE_WIDTH;
            ringLineWidths[ring] = DEFAULT_LINE_WIDTH;
        }
        setCamera(cam);
    }

    //= Camera, size and line widths ======================================

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
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the width, in pixels, of the strip that draws the ring
    */
    public double getRingLineWidth(int ring)
    {
        checkRing(ring);
        return ringLineWidths[ring];
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @param lineWidth width, in pixels, of the strip that draws the ring; not
    positive values are ignored
    */
    public void setRingLineWidth(int ring, double lineWidth)
    {
        checkRing(ring);
        if ( lineWidth > 0.0 ) {
            ringLineWidths[ring] = lineWidth;
        }
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the width the ring is designed to have in legacy resolutions, in
    pixels, before scaling it for the resolution of the screen
    */
    public double getBaseRingLineWidth(int ring)
    {
        checkRing(ring);
        return baseRingLineWidths[ring];
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @param lineWidth width of the ring in legacy resolutions, in pixels; not
    positive values are ignored. It is used the next time `applyScale` is called
    */
    public void setBaseRingLineWidth(int ring, double lineWidth)
    {
        checkRing(ring);
        if ( lineWidth > 0.0 ) {
            baseRingLineWidths[ring] = lineWidth;
        }
    }

    /**
    Sets the apparent size and the line widths of the gizmo to the values that
    make it look proportional to the screen resolution: the base values
    (designed for legacy resolutions) multiplied by the scale of the given
    scaler. The new size is used by the gizmo the next time its
    transformation is set.

    @param scaler scaler informed of the resolution of the screen
    */
    public void applyScale(ViewportElementScaler scaler)
    {
        if ( scaler == null ) {
            return;
        }
        setApparentSizeInPixels(scaler.scaleSize(baseApparentSizeInPixels));
        for ( int ring = 0; ring < RING_COUNT; ring++ ) {
            ringLineWidths[ring] = scaler.scaleLength(baseRingLineWidths[ring]);
        }
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
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the width of the strip of the ring, converted from pixels to world
    units, as seen from its camera at its current apparent size
    */
    public double getRingLineWidthInWorldUnits(int ring)
    {
        checkRing(ring);
        return ringLineWidths[ring] * currentScale / apparentSizeInPixels;
    }

    /**
    @return the radius of the rings, in world units, as seen from its camera
    at its current apparent size
    */
    public double getRingRadius()
    {
        return RING_RADIUS * currentScale;
    }

    //= Transformation ====================================================

    public Vector3Dd getPosition()
    {
        return T.extractTranslation();
    }

    public void setPosition(Vector3Dd p)
    {
        setTransformationMatrix(T.withTranslation(p));
    }

    /**
    Sets the frame of the gizmo and recalculates the geometry of its rings.
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
    Recalculates the geometry of the rings of the gizmo from its current
    transformation, apparent size and camera.
    */
    public void updateGeometryState()
    {
        updateScale();

        double ringRadius = getRingRadius();
        Matrix4x4d rotation = new Matrix4x4d(T).withoutTranslation();
        Vector3Dd position = getPosition();

        for ( int ring = 0; ring < RING_COUNT; ring++ ) {
            // The tube covers the width of the drawn ring, at least
            double tubeRadius = Math.max(PICK_TOLERANCE * currentScale,
                getRingLineWidthInWorldUnits(ring) / 2);

            ringModels[ring].setMajorRadius(ringRadius);
            ringModels[ring].setMinorRadius(
                Math.min(tubeRadius, ringRadius * MAX_TUBE_TO_RING_RADIUS_RATIO));

            double[] tilt = RING_TILT[ring];
            Matrix4x4d ringRotation = rotation.multiply(
                new Matrix4x4d().axisRotation(tilt[0], tilt[1], tilt[2], tilt[3]));

            ringInstances[ring].setRotation(ringRotation);
            ringInstances[ring].setPosition(position);
        }
    }

    /**
    Calculates the scale that makes the gizmo look as big as its apparent
    size, from its camera. It is kept as it was if the size in pixels can not
    be measured.
    */
    private void updateScale()
    {
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

    //= Geometry of the rings =============================================

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the internal model of the ring, whose axis is the local Z axis of
    its instance (see `getRingInstance`)
    */
    public Torus getRingModel(int ring)
    {
        checkRing(ring);
        return ringModels[ring];
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the instance of the model of the ring, with its position and
    orientation in world space; it is used to point at the ring with the mouse
    */
    public SimpleBody getRingInstance(int ring)
    {
        checkRing(ring);
        return ringInstances[ring];
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the direction, in world space, of the axis of the ring
    */
    public Vector3Dd getAxisDirection(int ring)
    {
        checkRing(ring);

        Matrix4x4d rotation = new Matrix4x4d(T).withoutTranslation();

        return rotation.multiply(unitAxis(ring)).normalized();
    }

    /**
    Builds the geometry to draw a ring as a strip of quads facing the camera of
    the gizmo: a triangle strip in world space, that follows the ring with the
    width in pixels of the ring (see `getRingLineWidth`) at every vertex.
    The strip is closed, that is, it ends where it starts.
    PRE: the transformation matrix of the gizmo has been set.

    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the vertices of the triangle strip: an alternating pair (one at
    each side of the ring) for each of the `RING_SEGMENTS + 1` positions
    */
    public Vector3Dd[] buildRingStrip(int ring)
    {
        checkRing(ring);

        Vector3Dd center = getPosition();
        Vector3Dd axis = getAxisDirection(ring);
        Matrix4x4d rotation = new Matrix4x4d(T).withoutTranslation();
        // The plane of the ring is spanned by the next two axes (X: YZ,
        // Y: ZX, Z: XY), so the ring goes around its axis counterclockwise
        Vector3Dd u = rotation.multiply(unitAxis((ring + 1) % RING_COUNT)).normalized();
        Vector3Dd v = rotation.multiply(unitAxis((ring + 2) % RING_COUNT)).normalized();
        double radius = getRingRadius();
        double halfWidth = getRingLineWidthInWorldUnits(ring) / 2;
        Vector3Dd[] strip = new Vector3Dd[2 * (RING_SEGMENTS + 1)];

        for ( int i = 0; i <= RING_SEGMENTS; i++ ) {
            double angle = 2 * Math.PI * (i % RING_SEGMENTS) / RING_SEGMENTS;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vector3Dd point = center.add(u.multiply(radius * cos)).add(v.multiply(radius * sin));
            Vector3Dd tangent = v.multiply(cos).subtract(u.multiply(sin));
            Vector3Dd view;

            if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
                view = camera.getFront();
            }
            else {
                view = point.subtract(camera.getPosition());
            }

            Vector3Dd side = tangent.crossProduct(view);

            if ( side.length() < VSDK.EPSILON ) {
                // Ring pointing to the viewer: any direction across the ring is valid
                side = axis;
            }
            side = side.normalized().multiply(halfWidth);
            strip[2*i] = point.add(side);
            strip[2*i + 1] = point.subtract(side);
        }
        return strip;
    }

    private static Vector3Dd unitAxis(int axis)
    {
        return new Vector3Dd(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0);
    }

    //= Selection =========================================================

    /**
    Finds the ring a ray points at, using the torus models of the rings.
    PRE: the transformation matrix of the gizmo has been set.

    @param ray ray, in world space, i.e. the one the cursor defines
    @return the `*_RING_GROUP` constant of the nearest ring hit by the ray, or
    `NULL_GROUP` if it hits none
    */
    public int pickRing(Ray ray)
    {
        double nearestDistance = Double.MAX_VALUE;
        int nearestRing = NULL_GROUP;

        for ( int ring = 0; ring < RING_COUNT; ring++ ) {
            Ray hit = ringInstances[ring].doIntersectionFirstHit(ray.withT(Double.MAX_VALUE));

            if ( hit != null && hit.getT() < nearestDistance ) {
                nearestDistance = hit.getT();
                nearestRing = groupOfRing(ring);
            }
        }
        return nearestRing;
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the `*_RING_GROUP` constant of the ring
    */
    public static int groupOfRing(int ring)
    {
        checkRing(ring);
        return ring + 1;
    }

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
    @return the group (one of the `*_GROUP` constants) that is highlighted: the
    one under the cursor, or the chosen one if the cursor is not over any group
    */
    public int getCurrentSelection()
    {
        if ( volatileSelection == NULL_GROUP ) {
            return persistentSelection;
        }
        return volatileSelection;
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return true if the ring is the current selection, so it is drawn yellow
    */
    public boolean isRingHighlighted(int ring)
    {
        checkRing(ring);
        return getCurrentSelection() == groupOfRing(ring);
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the color the ring must be drawn with: the one of its axis, or
    yellow if it is highlighted
    */
    public ColorRgb getRingColor(int ring)
    {
        checkRing(ring);
        return isRingHighlighted(ring) ? InputGizmo.HIGHLIGHT_COLOR : axisColors[ring];
    }

    //= Rotation arc ======================================================

    /**
    Sets the arc of the rotation being done around a ring, so it is shown.
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @param u direction, in world space, where the angles are measured from
    @param v direction, in world space, perpendicular to `u` and to the
    axis of the ring, so angles grow from `u` towards it
    @param startAngle angle, in radians, where the arc starts
    @param sweep angle, in radians, the arc goes on from its start (negative
    for the other way)
    */
    public void setArc(int ring, Vector3Dd u, Vector3Dd v, double startAngle, double sweep)
    {
        checkRing(ring);
        arcRing = ring;
        arcU = u;
        arcV = v;
        arcStartAngle = startAngle;
        arcSweep = sweep;
    }

    /**
    Stops showing the rotation arc.
    */
    public void clearArc()
    {
        arcRing = -1;
    }

    /**
    @return true if there is a rotation arc to show
    */
    public boolean isArcVisible()
    {
        return arcRing >= 0;
    }

    /**
    @return the ring the arc belongs to (0, 1 or 2), or -1 if there is no arc
    */
    public int getArcRing()
    {
        return arcRing;
    }

    /**
    @return the angle the arc goes on, in radians (negative for the other way)
    */
    public double getArcSweep()
    {
        return arcSweep;
    }

    /**
    @return the angle the arc goes on, in degrees (negative for the other way)
    */
    public double getArcSweepInDegrees()
    {
        return Math.toDegrees(arcSweep);
    }

    /**
    @return the color to draw the arc with: the one of its axis, whether or
    not the ring is highlighted
    */
    public ColorRgb getArcColor()
    {
        return axisColors[Math.max(0, arcRing)];
    }

    /**
    Builds the geometry to draw the arc as a translucent sector: a triangle
    fan in world space, with the center of the gizmo as its first vertex and
    then the points of the arc at the radius of the rings, from its start.
    An arc of more than a turn is shown as a whole disc.

    @return the vertices of the triangle fan, or an empty array if there is
    no arc to show, or it has no angle yet
    */
    public Vector3Dd[] buildArcFan()
    {
        if ( !isArcVisible() || Math.abs(arcSweep) < VSDK.EPSILON ) {
            return new Vector3Dd[0];
        }

        double sweep = Math.max(-2 * Math.PI, Math.min(2 * Math.PI, arcSweep));
        int segments = Math.max(1, (int)Math.ceil(Math.abs(Math.toDegrees(sweep)) / ARC_STEP_IN_DEGREES));
        Vector3Dd[] fan = new Vector3Dd[segments + 2];

        fan[0] = getPosition();
        for ( int i = 0; i <= segments; i++ ) {
            fan[i + 1] = pointOfArc(arcStartAngle + sweep * i / segments, 1.0);
        }
        return fan;
    }

    /**
    @return the point, in world space, where the label with the angle of the
    arc must be anchored: just outside the ring, at the middle of the arc; or
    null if there is no arc to show
    */
    public Vector3Dd getArcLabelPosition()
    {
        if ( !isArcVisible() ) {
            return null;
        }
        return pointOfArc(arcStartAngle + arcSweep / 2, ARC_LABEL_DISTANCE);
    }

    private Vector3Dd pointOfArc(double angle, double radiusFactor)
    {
        double radius = getRingRadius() * radiusFactor;

        return getPosition().add(arcU.multiply(Math.cos(angle) * radius))
            .add(arcV.multiply(Math.sin(angle) * radius));
    }

    //= Numeric input =====================================================

    /**
    @return the input gizmo that shows (and lets the user type) the angles, in
    degrees, of the orientation of this gizmo, updated with its current
    orientation and highlighted rings
    */
    public InputGizmo getInputGizmo()
    {
        double[] angles = extractAnglesInDegrees(T);

        for ( int ring = 0; ring < RING_COUNT; ring++ ) {
            inputGizmo.setValue(ring, angles[ring]);
            inputGizmo.setFieldHighlighted(ring, isRingHighlighted(ring));
        }
        return inputGizmo;
    }

    /**
    Builds the rotation given by three angles: a rotation around the X axis,
    then one around the Y axis and then one around the Z axis, all of them
    around the axes of the world.

    @param xDegrees rotation around the X axis, in degrees
    @param yDegrees rotation around the Y axis, in degrees
    @param zDegrees rotation around the Z axis, in degrees
    @return the rotation matrix
    */
    public static Matrix4x4d createRotationFromAnglesInDegrees(double xDegrees,
                                                              double yDegrees,
                                                              double zDegrees)
    {
        Matrix4x4d rx = new Matrix4x4d().axisRotation(Math.toRadians(xDegrees), 1, 0, 0);
        Matrix4x4d ry = new Matrix4x4d().axisRotation(Math.toRadians(yDegrees), 0, 1, 0);
        Matrix4x4d rz = new Matrix4x4d().axisRotation(Math.toRadians(zDegrees), 0, 0, 1);

        return rz.multiply(ry.multiply(rx));
    }

    /**
    Inverse of `createRotationFromAnglesInDegrees`: the angles of the rotation
    around the X, Y and Z axes that give an orientation. The angle around Y is
    in [-90, 90] and the other ones are in (-180, 180], so an orientation may
    be given with angles different from the ones that were used to create it.
    When the Y rotation is +-90 degrees there are infinite solutions (gimbal
    lock) and the rotation around Z is taken as 0.

    @param rotation rotation matrix (its translation is ignored)
    @return the angles, in degrees, around the X, Y and Z axes
    */
    public static double[] extractAnglesInDegrees(Matrix4x4d rotation)
    {
        double sinY = Math.max(-1.0, Math.min(1.0, -rotation.get(2, 0)));
        double y = Math.asin(sinY);
        double x;
        double z;

        if ( Math.abs(sinY) < 1.0 - 1.0e-9 ) {
            x = Math.atan2(rotation.get(2, 1), rotation.get(2, 2));
            z = Math.atan2(rotation.get(1, 0), rotation.get(0, 0));
        }
        else {
            x = Math.atan2(-rotation.get(1, 2), rotation.get(1, 1));
            z = 0;
        }
        return new double[] {Math.toDegrees(x), Math.toDegrees(y), Math.toDegrees(z)};
    }

    private static void checkRing(int ring)
    {
        if ( ring < 0 || ring >= RING_COUNT ) {
            throw new IllegalArgumentException("Invalid ring: " + ring +
                ". It must be 0, 1 or 2 for the X, Y or Z axis.");
        }
    }
}
