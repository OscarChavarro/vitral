package vsdk.toolkit.gui.gizmo;

// Java basic classes
import java.util.ArrayList;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.processing.CurveModeler;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

public class TranslateGizmo extends Gizmo {
    /// Internal transformation state
    private Matrix4x4d T;
    private Camera camera;

    /// Geometric model based in primitive instancing: primitive concretions
    private final Arrow arrowModel;
    private final Cone cylinderModel;
    private final Box boxModel;
    private final Cone coneModel;

    /// Geometric model based in primitive instancing: primitive instances
    /// This list is always of size 12, and its elements follow the order 
    /// indicated in the values of the *_ELEMENT constants of this class.
    ArrayList<SimpleBody> elementInstances;
    ArrayList<SimpleBody> elementInstances3dsmax;

    /// Internal element selection state
    public static final int X_AXIS_ELEMENT = 1;
    public static final int Y_AXIS_ELEMENT = 2;
    public static final int Z_AXIS_ELEMENT = 3;
    public static final int XYY_SEGMENT_ELEMENT = 4;
    public static final int XYX_SEGMENT_ELEMENT = 5;
    public static final int YZZ_SEGMENT_ELEMENT = 6;
    public static final int YZY_SEGMENT_ELEMENT = 7;
    public static final int XZZ_SEGMENT_ELEMENT = 8;
    public static final int XZX_SEGMENT_ELEMENT = 9;
    public static final int XY_BOX_ELEMENT = 10;
    public static final int YZ_BOX_ELEMENT = 11;
    public static final int XZ_BOX_ELEMENT = 12;

    public static final int NULL_GROUP = 0;
    public static final int X_AXIS_GROUP = 1;
    public static final int Y_AXIS_GROUP = 2;
    public static final int Z_AXIS_GROUP = 3;
    public static final int XY_PLANE_GROUP = 4;
    public static final int YZ_PLANE_GROUP = 5;
    public static final int XZ_PLANE_GROUP = 6;

    private static final double SEGMENT_LENGTH = 0.32;
    private static final double SEGMENT_WIDTH = 0.02;
    private static final double BOX_SIDE = 0.3;
    private static final double BOX_HEIGHT = 0.01;
    private static final double ARROW_LENGTH = 1.0;

    /// Size and line width designed for legacy resolutions
    public static final int DEFAULT_APPARENT_SIZE_IN_PIXELS = 100;
    public static final double DEFAULT_LINE_WIDTH = 1.0;

    /// Apparent size (in legacy resolution pixels) the user has chosen
    private int baseApparentSizeInPixels;
    /// Apparent size in pixels of the screen currently in use
    private int apparentSizeInPixels;
    /// Width, in pixels, of the lines of the gizmo
    private double lineWidth;

    /// Interaction state
    private final InputGizmo inputGizmo;
    private int persistentSelection;
    private int volatileSelection;

    private boolean selectedResizing;
    private double currentScale;

    public TranslateGizmo(Camera cam)
    {
        baseApparentSizeInPixels = DEFAULT_APPARENT_SIZE_IN_PIXELS;
        apparentSizeInPixels = DEFAULT_APPARENT_SIZE_IN_PIXELS;
        lineWidth = DEFAULT_LINE_WIDTH;
        inputGizmo = new InputGizmo(3);

        ReferenceFrameGizmo axisColors = new ReferenceFrameGizmo();

        for ( int axis = 0; axis < 3; axis++ ) {
            inputGizmo.setFieldColor(axis, axisColors.getAxisColor(axis));
        }
        persistentSelection = X_AXIS_GROUP;
        volatileSelection = NULL_GROUP;

        // Total arrow length = 0.2 empty + 0.5 base + 0.3 head
        arrowModel = new Arrow(0.5* ARROW_LENGTH, 0.3* ARROW_LENGTH, 0.025, 0.05);
        cylinderModel = new Cone(SEGMENT_WIDTH, SEGMENT_WIDTH, SEGMENT_LENGTH);
        boxModel = new Box(BOX_SIDE, BOX_SIDE, BOX_HEIGHT);
        coneModel = new Cone(0.05, 0, 0.3* ARROW_LENGTH);

        elementInstances = new ArrayList<>();
        for ( int i = 0; i < 12; i++ ) {
            SimpleBody r = new SimpleBody();
            elementInstances.add(r);
        }

        elementInstances3dsmax = new ArrayList<>();
        for ( int i = 0; i < 15; i++ ) {
            SimpleBody r = new SimpleBody();
            elementInstances3dsmax.add(r);
        }

        setCamera(cam);
        selectedResizing = true;
        currentScale = 1.0;
    }

    public int getApparentSizeInPixels()
    {
        return apparentSizeInPixels;
    }

    public void setApparentSizeInPixels(int du)
    {
        apparentSizeInPixels = du;
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
    @return the width, in pixels, of the lines that draw the gizmo
    */
    public double getLineWidth()
    {
        return lineWidth;
    }

    /**
    @param lineWidth width, in pixels, of the lines that draw the gizmo; not
    positive values are ignored
    */
    public void setLineWidth(double lineWidth)
    {
        if ( lineWidth > 0.0 ) {
            this.lineWidth = lineWidth;
        }
    }

    /**
    Sets the apparent size and the line width of the gizmo to the values
    that make it look proportional to the screen resolution: the base values
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
        setLineWidth(scaler.scaleLength(DEFAULT_LINE_WIDTH));
    }

    /**
    @return the width of the lines of the gizmo, converted from pixels to
    world units, as seen from its camera at its current apparent size
    */
    public double getLineWidthInWorldUnits()
    {
        return lineWidth * currentScale / apparentSizeInPixels;
    }

    /**
    Gives the straight lines drawn by the gizmo: the shaft of each axis arrow
    (from the gap around the origin up to the base of its head) and the
    segments that delimit the plane handles. Lines hidden because they point
    to the viewer of an orthogonal camera are not included, and the color of
    the lines of the selected group is yellow.
    PRE: the transformation matrix of the gizmo has been set.

    @return the lines of the gizmo, in world space
    */
    public ArrayList<TranslateGizmoLineSegment> getLineSegments()
    {
        ArrayList<TranslateGizmoLineSegment> segments = new ArrayList<TranslateGizmoLineSegment>();
        Vector3Dd zAxis = new Vector3Dd(0, 0, 1);

        for ( SimpleBody element : elementInstances ) {
            Geometry g = element.getGeometry();
            double length;

            if ( g == arrowModel ) {
                length = currentScale*0.5* ARROW_LENGTH;
            }
            else if ( g == cylinderModel ) {
                length = currentScale* SEGMENT_LENGTH;
            }
            else {
                continue;
            }

            Vector3Dd start = element.getPosition();
            Vector3Dd end = start.add(element.getRotation().multiply(zAxis).multiply(length));

            segments.add(new TranslateGizmoLineSegment(start, end, element.getMaterial().getDiffuse()));
        }
        return segments;
    }

    /**
    Builds the geometry to draw a line of the gizmo as a line with thickness
    seen from the camera of the gizmo: a triangle strip (a rectangle of 4
    vertices, in strip order) in world space, facing the camera. Its width is
    `getLineWidth()` pixels, and it is extended half of that width at both
    ends (square caps), so lines that meet at a corner have no gaps.

    @param segment line to draw, as given by `getLineSegments()`
    @return the 4 vertices of the triangle strip, or null if the line has no
    length
    */
    public Vector3Dd[] buildLineStrip(TranslateGizmoLineSegment segment)
    {
        Vector3Dd direction = segment.end().subtract(segment.start());
        double length = direction.length();

        if ( length < VSDK.EPSILON ) {
            return null;
        }
        direction = direction.multiply(1/length);

        // Direction from the eye to the line, to face the camera
        Vector3Dd view;

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            view = camera.getFront();
        }
        else {
            view = segment.start().add(segment.end()).multiply(0.5).subtract(camera.getPosition());
        }

        Vector3Dd side = direction.crossProduct(view);

        if ( side.length() < VSDK.EPSILON ) {
            // Line pointing to the viewer: any direction in screen is valid
            side = camera.getUp();
        }

        double halfWidth = getLineWidthInWorldUnits()/2;
        side = side.normalized().multiply(halfWidth);

        Vector3Dd start = segment.start().subtract(direction.multiply(halfWidth));
        Vector3Dd end = segment.end().add(direction.multiply(halfWidth));

        return new Vector3Dd[] {
            start.add(side),
            start.subtract(side),
            end.add(side),
            end.subtract(side)
        };
    }

    /**
    @return the factor applied to the gizmo geometry so it keeps its apparent
    size in pixels, as seen from its camera
    */
    public double getCurrentScale()
    {
        return currentScale;
    }

    public final void setCamera(Camera cam)
    {
        camera = cam;
    }

    public Camera getCamera()
    {
        return camera;
    }

    public ArrayList<SimpleBody> getElements()
    {
        return elementInstances;
    }

    public ArrayList<SimpleBody> getElements3dsmax()
    {
        int i;
        SimpleBody r, o, r2;
        Geometry g;
        SimpleMaterial red = createMaterial(0.78, 0, 0);
        SimpleMaterial green = createMaterial(0, 0.61, 0);
        SimpleMaterial blue = createMaterial(0, 0, 0.76);

        Matrix4x4d R = new Matrix4x4d(T).withoutTranslation();
        Matrix4x4d subR = new Matrix4x4d();
        Matrix4x4d eleR, eleRi;
        Vector3Dd subP;
        Vector3Dd eleP;

        coneModel.setBaseRadius(currentScale*0.05);
        coneModel.setHeight(currentScale*0.3* ARROW_LENGTH);
        boxModel.setSize(currentScale*(BOX_SIDE+0.025), currentScale*(BOX_SIDE+0.025), currentScale*BOX_HEIGHT);

        //-----------------------------------------------------------------
        ParametricCurve lineModel;
        ParametricCurve segmentModel;

        lineModel = CurveModeler.createLine(0, 0, 0,
            0, 0, currentScale*0.7);

        segmentModel = CurveModeler.createLine(0, 0, 0,
            0, 0, currentScale* SEGMENT_LENGTH);

        //-----------------------------------------------------------------
        for ( i = 0; i < elementInstances.size(); i++ ) {
            r = elementInstances3dsmax.get(i);
            o = elementInstances.get(i);

            r.setMaterial(o.getMaterial()); 
            r.setRotation(o.getRotation());
            r.setRotationInverse(o.getRotationInverse());

            g = o.getGeometry();
            if ( g != null && g instanceof Arrow ) {
                r.setGeometry(coneModel);
                switch ( i ) {
                  case 0:
                    // Rotation
                    subR = subR.axisRotation(Math.toRadians(90.0), 0, 1, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, currentScale*0.7* ARROW_LENGTH);
                    eleP = eleR.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                    r.setMaterial(red); 
                    break;
                  case 1:
                    // Rotation
                    subR = subR.axisRotation(Math.toRadians(90.0), -1, 0, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, currentScale*0.7* ARROW_LENGTH);
                    eleP = eleR.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                    r.setMaterial(green); 
                    break;
                  case 2:
                    // Rotation
                    subR = new Matrix4x4d();
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, currentScale*0.7* ARROW_LENGTH);
                    eleP = eleR.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                    r.setMaterial(blue); 
                    break;
                }

                r2 = elementInstances3dsmax.get(i+12);
                r2.setMaterial(o.getMaterial()); 
                r2.setRotation(o.getRotation());
                r2.setRotationInverse(o.getRotationInverse());
                r2.setPosition(o.getPosition());
                r2.setGeometry(lineModel);
            }
            else if ( g != null && g instanceof Cone ) {
                r.setPosition(o.getPosition());
                r.setGeometry(segmentModel);
            }
            else {
                r.setPosition(o.getPosition());
                r.setGeometry(g);
            }
        }

        return elementInstances3dsmax;
    }

    private SimpleMaterial createMaterial(double r, double g, double b)
    {
        SimpleMaterial m = new SimpleMaterial();

        m = m.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m = m.withDiffuse(new ColorRgb(r, g, b));
        m = m.withSpecular(new ColorRgb(1, 1, 1));
        return m;
    }

    /**
    This method updates the data structure contained in the `elementInstances`
    array starting from the given parameters.
    - translation is the position of the center of the gizmo
    - rotation is the rotation matrix containing the orientation of the gizmo
    - if autosize is false, initialdu, initialdv and camera parameters are not
      used, and the gizmo doesn't change its current size. If autosize is true,
      the gizmo size is changed such as from the current camera, the gizmo
      projection fit a 2D area of initialdu * initialdv pixels.
    - modelType must be one of the following values: MODEL_FOR_GRAVITY or
      MODEL_FOR_DISPLAY. Depending on this value the size of current
      geometric elements could change.
      @param translation
      @param rotation
      @param autosize
      @param initialdu
      @param camera
    */
    public void calculateGeometryState(Vector3Dd translation, Matrix4x4d rotation,
                                       boolean autosize, int initialdu,
                                       Camera camera)
    {
        //-----------------------------------------------------------------
        int i;
        SimpleMaterial red = createMaterial(0.78, 0, 0);
        SimpleMaterial green = createMaterial(0, 0.61, 0);
        SimpleMaterial blue = createMaterial(0, 0, 0.76);
        SimpleMaterial yellow = createMaterial(1, 1, 0);
        SimpleMaterial yellowTransparent = createMaterial(1, 1, 0);

        yellowTransparent = yellowTransparent.withOpacity(0.2);

        int currentSelection;
        if ( volatileSelection == NULL_GROUP ) {
            currentSelection = persistentSelection;
        }
        else {
            currentSelection = volatileSelection;
        }

        Matrix4x4d R = new Matrix4x4d(T).withoutTranslation();
        Matrix4x4d subR = new Matrix4x4d();
        Matrix4x4d eleR, eleRi;
        Vector3Dd subP;
        Vector3Dd eleP;

        camera.updateVectors();

        if ( selectedResizing ) {
            Vector3Dd p = getPosition();
            Vector3Dd right = camera.getLeft().multiply(-1);

            right = right.normalized();
            Vector3Dd a = camera.projectPointUsingRayMethod(p);
            Vector3Dd b = camera.projectPointUsingRayMethod(p.add(right));

            // Keeps the last scale if the size in pixels can not be measured
            if ( a != null && b != null ) {
                double factor = Vector3Dd.distance(a, b);

                if ( factor > VSDK.EPSILON ) {
                    currentScale = ((double)initialdu)/factor;
                }
            }
        }
        double scale = currentScale;

        arrowModel.setBaseLength(scale*0.5* ARROW_LENGTH);
        arrowModel.setHeadLength(scale*0.3* ARROW_LENGTH);
        arrowModel.setBaseRadius(scale*0.025);
        arrowModel.setHeadRadius(scale*0.05);
        cylinderModel.setBaseRadius(scale*SEGMENT_WIDTH);
        cylinderModel.setTopRadius(scale*SEGMENT_WIDTH);
        cylinderModel.setHeight(scale* SEGMENT_LENGTH);
        boxModel.setSize(scale*BOX_SIDE, scale*BOX_SIDE, scale*BOX_HEIGHT);

        //-----------------------------------------------------------------
        Vector3Dd front = camera.getFront();
        Vector3Dd axisI = new Vector3Dd(1, 0, 0);
        Vector3Dd axisJ = new Vector3Dd(0, 1, 0);
        Vector3Dd axisK = new Vector3Dd(0, 0, 1);
        boolean orthogonalCamera;
        boolean iPar;
        boolean jPar;
        boolean kPar;

        orthogonalCamera = (camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL);
        iPar = (Math.abs(front.dotProduct(axisI)) > 1.0-VSDK.EPSILON);
        jPar = (Math.abs(front.dotProduct(axisJ)) > 1.0-VSDK.EPSILON);
        kPar = (Math.abs(front.dotProduct(axisK)) > 1.0-VSDK.EPSILON);

        int index;
        for ( i = 0, index = 1;
              index <= 12 && i < elementInstances.size();
              index++, i++ ) {
            SimpleBody r = elementInstances.get(i);
            r.setGeometry(null);
            switch ( index ) {
              case X_AXIS_ELEMENT:
                if ( !(orthogonalCamera && iPar) ) {
                    // Basic model
                    r.setGeometry(arrowModel);
                    if ( currentSelection == X_AXIS_GROUP ||
                         currentSelection == XY_PLANE_GROUP ||
                         currentSelection == XZ_PLANE_GROUP ) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(red);
                    }
                    // Rotation
                    subR = subR.axisRotation(Math.toRadians(90.0), 0, 1, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, scale*0.2* ARROW_LENGTH);
                    eleP = eleR.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
              case Y_AXIS_ELEMENT:
                  if ( !(orthogonalCamera && jPar) ) {
                    // Basic model
                    r.setGeometry(arrowModel);
                    if ( currentSelection == Y_AXIS_GROUP ||
                         currentSelection == XY_PLANE_GROUP ||
                         currentSelection == YZ_PLANE_GROUP ) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(green);
                    }
                    // Rotation
                    subR = subR.axisRotation(Math.toRadians(90.0), -1, 0, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, scale*0.2* ARROW_LENGTH);
                    eleP = eleR.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
              case Z_AXIS_ELEMENT:
                if ( !(orthogonalCamera && kPar) ) {
                    // Basic model
                    r.setGeometry(arrowModel);
                    if ( currentSelection == Z_AXIS_GROUP ||
                         currentSelection == YZ_PLANE_GROUP ||
                         currentSelection == XZ_PLANE_GROUP ) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(blue);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, scale*0.2* ARROW_LENGTH);
                    eleP = eleR.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
              case XYY_SEGMENT_ELEMENT:
                if ( !(orthogonalCamera && (iPar || jPar)) ) {
                    // Basic model
                    r.setGeometry(cylinderModel);
                    if ( currentSelection == XY_PLANE_GROUP ) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(green);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(Math.toRadians(90.0), 0, 1, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, scale* SEGMENT_LENGTH, 0);
                    eleP = eleR.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
              case XYX_SEGMENT_ELEMENT:
                if ( !(orthogonalCamera && (iPar || jPar)) ) {
                    // Basic model
                    r.setGeometry(cylinderModel);
                    if ( currentSelection == XY_PLANE_GROUP ) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(red);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(Math.toRadians(90.0), -1, 0, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(scale* SEGMENT_LENGTH, 0, 0);
                    eleP = eleR.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
              case YZZ_SEGMENT_ELEMENT:
                // Basic model
                if ( !(orthogonalCamera && (jPar || kPar)) ) {
                    r.setGeometry(cylinderModel);
                    if ( currentSelection == YZ_PLANE_GROUP ) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(blue);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(Math.toRadians(90.0), -1, 0, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, scale* SEGMENT_LENGTH);
                    eleP = R.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
              case YZY_SEGMENT_ELEMENT:
                if ( !(orthogonalCamera && (jPar || kPar)) ) {
                    // Basic model
                    r.setGeometry(cylinderModel);
                    if ( currentSelection == YZ_PLANE_GROUP ) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(green);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, scale* SEGMENT_LENGTH, 0);
                    eleP = R.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
              case XZZ_SEGMENT_ELEMENT:
                if ( !(orthogonalCamera && (iPar || kPar)) ) {
                    // Basic model
                    r.setGeometry(cylinderModel);
                    if ( currentSelection == XZ_PLANE_GROUP ) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(blue);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(Math.toRadians(90.0), 0, 1, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, scale* SEGMENT_LENGTH);
                    eleP = R.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
              case XZX_SEGMENT_ELEMENT:
                if ( !(orthogonalCamera && (iPar || kPar)) ) {
                    // Basic model
                    r.setGeometry(cylinderModel);
                    if ( currentSelection == XZ_PLANE_GROUP ) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(red);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(scale* SEGMENT_LENGTH, 0, 0);
                    eleP = R.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
              case XY_BOX_ELEMENT:
                if ( !(orthogonalCamera && (iPar || jPar)) ) {
                    // Basic model
                    r.setGeometry(null);
                    if ( currentSelection != XY_PLANE_GROUP ) {
                        break;
                    }
                    r.setGeometry(boxModel);
                    r.setMaterial(yellowTransparent); 
                    // Rotation
                    subR = new Matrix4x4d();
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(scale*BOX_SIDE/2, scale*BOX_SIDE/2, 0);
                    eleP = R.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
              case YZ_BOX_ELEMENT:
                if ( !(orthogonalCamera && (jPar || kPar)) ) {
                    // Basic model
                    r.setGeometry(null);
                    if ( currentSelection != YZ_PLANE_GROUP ) {
                        break;
                    }
                    r.setGeometry(boxModel);
                    r.setMaterial(yellowTransparent); 
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(Math.toRadians(90.0), 0, 1, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, scale*BOX_SIDE/2, scale*BOX_SIDE/2);
                    eleP = R.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
              case XZ_BOX_ELEMENT:
                if ( !(orthogonalCamera && (iPar || kPar)) ) {
                    // Basic model
                    r.setGeometry(null);
                    if ( currentSelection != XZ_PLANE_GROUP ) {
                        break;
                    }
                    r.setGeometry(boxModel);
                    r.setMaterial(yellowTransparent); 
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(Math.toRadians(90.0), 1, 0, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(scale*BOX_SIDE/2, 0, scale*BOX_SIDE/2);
                    eleP = R.multiply(subP).add(getPosition());
                    r.setPosition(eleP);
                }
                break;
            }
        }
    }

    public Vector3Dd getPosition()
    {
        return T.extractTranslation();
    }

    public void setPosition(Vector3Dd p)
    {
        T = T.withTranslation(p);
    }

    public void setTransformationMatrix(Matrix4x4d T)
    {
        this.T = T;

        Matrix4x4d R = new Matrix4x4d(T).withoutTranslation();
        calculateGeometryState(getPosition(), 
                               R, selectedResizing, apparentSizeInPixels,
                               camera);
    }

    public Matrix4x4d getTransformationMatrix()
    {
        return T;
    }

    /**
    Recalculates the geometry of the elements of the gizmo from its current
    transformation, apparent size, resizing state, selection and camera.
    PRE: the transformation matrix of the gizmo has been set.
    */
    public void updateGeometryState()
    {
        Matrix4x4d R = new Matrix4x4d(T).withoutTranslation();

        calculateGeometryState(getPosition(), R, selectedResizing,
            apparentSizeInPixels, camera);
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
    @return the input gizmo that shows (and lets the user type) the
    coordinates of this gizmo, updated with its current position and highlighted
    axes
    */
    public InputGizmo getInputGizmo()
    {
        if ( T != null ) {
            Vector3Dd position = getPosition();

            inputGizmo.setValue(0, position.x());
            inputGizmo.setValue(1, position.y());
            inputGizmo.setValue(2, position.z());
        }
        for ( int axis = 0; axis < 3; axis++ ) {
            inputGizmo.setFieldHighlighted(axis, isAxisHighlighted(axis));
        }
        return inputGizmo;
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return true if the axis is highlighted (drawn yellow) because the group
    currently selected moves along it: the axis itself, or a plane that
    contains it
    */
    public boolean isAxisHighlighted(int axis)
    {
        int currentSelection = getCurrentSelection();

        return switch ( axis ) {
            case 0 -> currentSelection == X_AXIS_GROUP ||
                currentSelection == XY_PLANE_GROUP ||
                currentSelection == XZ_PLANE_GROUP;
            case 1 -> currentSelection == Y_AXIS_GROUP ||
                currentSelection == XY_PLANE_GROUP ||
                currentSelection == YZ_PLANE_GROUP;
            case 2 -> currentSelection == Z_AXIS_GROUP ||
                currentSelection == YZ_PLANE_GROUP ||
                currentSelection == XZ_PLANE_GROUP;
            default -> false;
        };
    }

    /**
    @return true if the size of the gizmo is recalculated to keep its apparent
    size in pixels, false if it is kept while it is being dragged
    */
    public boolean isSelectedResizing()
    {
        return selectedResizing;
    }

    /**
    @param selectedResizing true if the size of the gizmo must be recalculated
    to keep its apparent size in pixels
    */
    public void setSelectedResizing(boolean selectedResizing)
    {
        this.selectedResizing = selectedResizing;
    }
}
