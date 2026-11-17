package vsdk.toolkit.gui.gizmo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Tube;
import vsdk.toolkit.environment.geometry.volume.Volume;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

/**
A camera control gizmo, modeled after the ViewCube of Autodesk products: a
small cube, presented over a corner of a viewport (as a HUD), that always
shows the orientation of the camera of the viewport, and lets the user turn
that camera to a predefined view with a click, or orbit it by dragging.

Model
-----
The cube [-1, 1]^3 is divided, along each axis, in three bands: a corner band
at each side (of width `CORNER_FRACTION`) and a central band. Each of the 26
combinations (i, j, k) in {-1, 0, 1}^3, except (0, 0, 0), is one region of
the cube, modeled as a `Box`:
  - 6 face regions (one non zero component): the center of each face of the
    cube; clicking one gives an orthogonal view of that side.
  - 12 edge regions (two non zero components): long boxes along the edges;
    clicking one gives a three-fourths view between two sides.
  - 8 corner regions (three non zero components): small cubes at the
    vertices; clicking one gives a three-fourths view between three sides.
Regions are separated by small gaps (`GAP`), that mark them (and avoid
coincident faces between neighbors). Under the cube there is a compass, a
`Tube` around the vertical axis; dragging it turns the camera around Z.

The direction of a region is (i, j, k): the camera of its view looks from
that direction towards the center. Following the cameras of `Viewport` (Z
up), the faces are TOP (+Z), BOTTOM (-Z), FRONT (-Y), BACK (+Y), LEFT (-X)
and RIGHT (+X).

Presentation
------------
The gizmo is seen through its own orthogonal camera (`getHudCamera`), that
has the same orientation of the camera of the view (`setCamera`), and
occupies a square area of `getSizeInPixels()` pixels at the upper right
corner of the viewport (see `placeInViewport`). Pixel coordinates (`getHudX`,
`getHudY`, `pickRegion`...) have the origin at the upper left corner of the
viewport and y growing downwards. The parts (`getParts`) are bodies with materials that
reflect the state of the gizmo (`updatePartMaterials`): the region under the
cursor (`getVolatileSelection`) is highlighted, and the gizmo is translucent
when the cursor is not over it (`getInactiveOpacity`). Renderers of each
technology draw the parts with the HUD camera.

Interaction (see `CameraControlGizmoInteractionTechnique`) and the smooth
turn of the camera (see `CameraControlGizmoAnimation`) are platform
independent too.
*/
public class CameraControlGizmo extends Gizmo
{
    /// Not a region of the gizmo
    public static final int NO_REGION = -1;
    /// Number of regions of the cube
    public static final int NUMBER_OF_CUBE_REGIONS = 26;
    /// The compass ring under the cube
    public static final int COMPASS_REGION = NUMBER_OF_CUBE_REGIONS;
    /// Number of parts (bodies) of the gizmo: the regions and the compass
    public static final int NUMBER_OF_PARTS = NUMBER_OF_CUBE_REGIONS + 1;

    /// Kinds of regions
    public static final int KIND_NONE = 0;
    public static final int KIND_FACE = 1;
    public static final int KIND_EDGE = 2;
    public static final int KIND_CORNER = 3;
    public static final int KIND_COMPASS = 4;

    /// Width of the corner bands, as a fraction of the side of the cube
    public static final double CORNER_FRACTION = 0.15;
    /// Separation between neighbor regions, in units of the model
    public static final double GAP = 0.04;
    /// Radii and height of the compass, and the height of its base
    public static final double COMPASS_INTERNAL_RADIUS = 1.5;
    public static final double COMPASS_EXTERNAL_RADIUS = 1.8;
    public static final double COMPASS_HEIGHT = 0.08;
    public static final double COMPASS_BASE_Z = -1.0;

    /// Half of the side of the square of the model seen by the HUD camera
    public static final double HUD_HALF_EXTENT = 2.05;
    /// Distance from the center of the model to the HUD camera
    public static final double HUD_CAMERA_DISTANCE = 10.0;

    public static final int DEFAULT_BASE_SIZE_IN_PIXELS = 110;
    public static final int DEFAULT_BASE_MARGIN_IN_PIXELS = 6;
    public static final double DEFAULT_INACTIVE_OPACITY = 0.55;

    private static final String[] FACE_LABELS = {
        "LEFT", "RIGHT", "FRONT", "BACK", "BOTTOM", "TOP"
    };

    //= Model =============================================================
    /// Direction (i, j, k) of each region of the cube
    private final int[][] regionDirections;
    /// Bodies of the regions, then the compass
    private final ArrayList<SimpleBody> parts;
    /// Edges of the boundary representations of the parts, by part
    private float[][] edgeSegments;

    //= Presentation ======================================================
    private Camera camera;
    private final Camera hudCamera;
    private int baseSizeInPixels;
    private int baseMarginInPixels;
    private int sizeInPixels;
    private int marginInPixels;
    /// Upper left corner of the area of the gizmo, in pixels of the viewport
    private int hudX;
    private int hudY;
    private boolean visible;
    private double inactiveOpacity;
    private boolean active;
    private int volatileSelection;

    private ColorRgb faceColor;
    private ColorRgb edgeColor;
    private ColorRgb cornerColor;
    private ColorRgb compassColor;
    private ColorRgb highlightColor;
    private ColorRgb lineColor;
    private ColorRgb labelColor;

    public CameraControlGizmo()
    {
        regionDirections = new int[NUMBER_OF_CUBE_REGIONS][];
        parts = new ArrayList<SimpleBody>();
        edgeSegments = null;

        faceColor = new ColorRgb(0.86, 0.88, 0.92);
        edgeColor = new ColorRgb(0.74, 0.77, 0.82);
        cornerColor = new ColorRgb(0.66, 0.69, 0.75);
        compassColor = new ColorRgb(0.55, 0.57, 0.62);
        highlightColor = new ColorRgb(0.35, 0.62, 1.0);
        lineColor = new ColorRgb(0.15, 0.15, 0.2);
        labelColor = new ColorRgb(0.1, 0.1, 0.15);

        buildModel();

        camera = null;
        hudCamera = new Camera();
        hudCamera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        hudCamera.setOrthogonalZoom(1.0 / HUD_HALF_EXTENT);
        hudCamera.setNearPlaneDistance(0.1);
        hudCamera.setFarPlaneDistance(2 * HUD_CAMERA_DISTANCE);
        hudCamera.setName("Camera control gizmo");

        baseSizeInPixels = DEFAULT_BASE_SIZE_IN_PIXELS;
        baseMarginInPixels = DEFAULT_BASE_MARGIN_IN_PIXELS;
        sizeInPixels = baseSizeInPixels;
        marginInPixels = baseMarginInPixels;
        hudX = marginInPixels;
        hudY = marginInPixels;
        visible = true;
        inactiveOpacity = DEFAULT_INACTIVE_OPACITY;
        active = false;
        volatileSelection = NO_REGION;
        updateHudCamera();
        updatePartMaterials();
    }

    //= Model construction ================================================

    /**
    @return the interval [min, max] of the band `v` (-1, 0 or 1) of an axis,
    without the gaps towards its neighbor bands
    */
    private static double[] band(int v)
    {
        double c = 2 * CORNER_FRACTION;
        double g = GAP / 2;

        switch ( v ) {
          case -1:
            return new double[] {-1, -1 + c - g};
          case 1:
            return new double[] {1 - c + g, 1};
          default:
            return new double[] {-1 + c + g, 1 - c - g};
        }
    }

    private void buildModel()
    {
        int region = 0;

        for ( int i = -1; i <= 1; i++ ) {
            for ( int j = -1; j <= 1; j++ ) {
                for ( int k = -1; k <= 1; k++ ) {
                    if ( i == 0 && j == 0 && k == 0 ) {
                        continue;
                    }
                    regionDirections[region] = new int[] {i, j, k};
                    double[] bx = band(i);
                    double[] by = band(j);
                    double[] bz = band(k);
                    // Centered boxes, placed by the position of the part
                    // (renderers tessellate boxes centered at the origin)
                    Vector3Dd half = new Vector3Dd((bx[1] - bx[0]) / 2,
                        (by[1] - by[0]) / 2, (bz[1] - bz[0]) / 2);
                    Box box = new Box(half.multiply(-1), half);
                    parts.add(createPart(box, new Vector3Dd(
                        (bx[0] + bx[1]) / 2, (by[0] + by[1]) / 2,
                        (bz[0] + bz[1]) / 2), regionName(region)));
                    region++;
                }
            }
        }

        Tube compass = new Tube(COMPASS_INTERNAL_RADIUS,
            COMPASS_EXTERNAL_RADIUS, COMPASS_HEIGHT);
        parts.add(createPart(compass, new Vector3Dd(0, 0, COMPASS_BASE_Z),
            "Compass"));
    }

    private static SimpleBody createPart(vsdk.toolkit.environment.geometry.Geometry geometry,
        Vector3Dd position, String name)
    {
        SimpleBody body = new SimpleBody();

        body.setGeometry(geometry);
        body.setPosition(position);
        body.setRotation(new Matrix4x4d());
        body.setRotationInverse(new Matrix4x4d());
        body.setScale(new Vector3Dd(1, 1, 1));
        body.setMaterial(new SimpleMaterial());
        body.setName(name);
        return body;
    }

    //= Model queries =====================================================

    /**
    @return the bodies of the gizmo: index `r` is the body of region `r`, and
    the last one (`COMPASS_REGION`) is the compass
    */
    public List<SimpleBody> getParts()
    {
        return Collections.unmodifiableList(parts);
    }

    /// Sides of the polygons approximating the circles of the compass
    public static final int COMPASS_SLICES = 24;

    /**
    @param part index of a part (a region or `COMPASS_REGION`)
    @return the boundary representation of the geometry of the part, in the
    space of the part (its body places it with its position)
    */
    public PolyhedralBoundedSolid exportPartToPolyhedralBoundedSolid(int part)
    {
        SimpleBody body = parts.get(part);
        PolyhedralBoundedSolid brep;

        if ( body.getGeometry() instanceof Tube tube ) {
            brep = tube.exportToPolyhedralBoundedSolid(COMPASS_SLICES);
        }
        else {
            brep = ((Volume)body.getGeometry()).exportToPolyhedralBoundedSolid();
        }
        return brep;
    }

    /**
    The edges of the boundary representations of the parts, for renderers
    that draw the gizmo with lines over its surfaces (the edges of their
    triangle meshes would also show the diagonals of the faces).
    @param part index of a part (a region or `COMPASS_REGION`)
    @return x, y, z of the two ends of each edge, in the space of the gizmo
    */
    public float[] getEdgeSegments(int part)
    {
        if ( edgeSegments == null ) {
            edgeSegments = new float[NUMBER_OF_PARTS][];
            for ( int p = 0; p < NUMBER_OF_PARTS; p++ ) {
                PolyhedralBoundedSolid brep = exportPartToPolyhedralBoundedSolid(p);
                Vector3Dd offset = parts.get(p).getPosition();
                int n = brep.getEdgesList().size();
                float[] positions = new float[6 * n];
                for ( int e = 0; e < n; e++ ) {
                    _PolyhedralBoundedSolidEdge edge = brep.getEdgesList().get(e);
                    Vector3Dd a = edge.rightHalf.startingVertex.position.add(offset);
                    Vector3Dd b = edge.leftHalf.startingVertex.position.add(offset);
                    positions[6 * e] = (float)a.x();
                    positions[6 * e + 1] = (float)a.y();
                    positions[6 * e + 2] = (float)a.z();
                    positions[6 * e + 3] = (float)b.x();
                    positions[6 * e + 4] = (float)b.y();
                    positions[6 * e + 5] = (float)b.z();
                }
                edgeSegments[p] = positions;
            }
        }
        return edgeSegments[part].clone();
    }

    /**
    @param region a region of the cube
    @return its direction (i, j, k), each component -1, 0 or 1
    */
    public int[] getRegionDirectionComponents(int region)
    {
        return regionDirections[region].clone();
    }

    /**
    @param region a region of the cube
    @return the unit vector from the center of the cube towards the region
    */
    public Vector3Dd getRegionDirection(int region)
    {
        int[] d = regionDirections[region];
        return new Vector3Dd(d[0], d[1], d[2]).normalized();
    }

    /**
    @param i component along X (-1, 0 or 1)
    @param j component along Y (-1, 0 or 1)
    @param k component along Z (-1, 0 or 1)
    @return the region with that direction, or `NO_REGION` for (0, 0, 0)
    */
    public int findRegion(int i, int j, int k)
    {
        for ( int r = 0; r < NUMBER_OF_CUBE_REGIONS; r++ ) {
            int[] d = regionDirections[r];
            if ( d[0] == i && d[1] == j && d[2] == k ) {
                return r;
            }
        }
        return NO_REGION;
    }

    /**
    @param region a region, `COMPASS_REGION` or `NO_REGION`
    @return one of the `KIND_*` constants
    */
    public int getRegionKind(int region)
    {
        if ( region == COMPASS_REGION ) {
            return KIND_COMPASS;
        }
        if ( region < 0 || region >= NUMBER_OF_CUBE_REGIONS ) {
            return KIND_NONE;
        }
        int[] d = regionDirections[region];
        int nonZero = Math.abs(d[0]) + Math.abs(d[1]) + Math.abs(d[2]);
        switch ( nonZero ) {
          case 1:
            return KIND_FACE;
          case 2:
            return KIND_EDGE;
          default:
            return KIND_CORNER;
        }
    }

    /**
    @param region a face region
    @return its label (i.e. "TOP"), or null if it is not a face
    */
    public String getFaceLabel(int region)
    {
        if ( getRegionKind(region) != KIND_FACE ) {
            return null;
        }
        int[] d = regionDirections[region];
        for ( int axis = 0; axis < 3; axis++ ) {
            if ( d[axis] != 0 ) {
                return FACE_LABELS[2 * axis + (d[axis] > 0 ? 1 : 0)];
            }
        }
        return null;
    }

    /**
    @return a readable name of a region (i.e. "TOP FRONT RIGHT")
    */
    public String regionName(int region)
    {
        if ( region == COMPASS_REGION ) {
            return "Compass";
        }
        if ( region < 0 || region >= NUMBER_OF_CUBE_REGIONS ) {
            return "None";
        }
        int[] d = regionDirections[region];
        StringBuilder sb = new StringBuilder();
        // Vertical first, as in "TOP FRONT RIGHT"
        int[] order = {2, 1, 0};
        for ( int axis : order ) {
            if ( d[axis] != 0 ) {
                if ( sb.length() > 0 ) {
                    sb.append(' ');
                }
                sb.append(FACE_LABELS[2 * axis + (d[axis] > 0 ? 1 : 0)]);
            }
        }
        return sb.toString();
    }

    /**
    @return the regions that are faces of the cube
    */
    public int[] getFaceRegions()
    {
        int[] faces = new int[6];
        int n = 0;
        for ( int r = 0; r < NUMBER_OF_CUBE_REGIONS; r++ ) {
            if ( getRegionKind(r) == KIND_FACE ) {
                faces[n++] = r;
            }
        }
        return faces;
    }

    /**
    @param region a face region
    @return the point of the model where its label goes (the center of the
    face, on the surface of the cube)
    */
    public Vector3Dd getFaceLabelPosition(int region)
    {
        int[] d = regionDirections[region];
        return new Vector3Dd(d[0], d[1], d[2]);
    }

    //= Views =============================================================

    /**
    Builds a camera rotation (as given by `Camera.getRotation()`: columns
    front, left and up) looking along `front`, with the up direction as near
    as possible to `upHint`.
    @return the rotation, or null if `upHint` is parallel to `front`
    */
    public static Matrix4x4d createCameraRotation(Vector3Dd front, Vector3Dd upHint)
    {
        Vector3Dd f = front.normalized();
        Vector3Dd left = upHint.crossProduct(f);

        if ( left.length() < VSDK.EPSILON ) {
            return null;
        }
        left = left.normalized();
        Vector3Dd up = f.crossProduct(left).normalized();

        return new Matrix4x4d()
            .withVal(0, 0, f.x()).withVal(0, 1, left.x()).withVal(0, 2, up.x())
            .withVal(1, 0, f.y()).withVal(1, 1, left.y()).withVal(1, 2, up.y())
            .withVal(2, 0, f.z()).withVal(2, 1, left.z()).withVal(2, 2, up.z());
    }

    /**
    Calculates the orientation of the camera for the view of a region: it
    looks at the center from the direction of the region, keeping the scene
    upright (Z up). For the TOP and BOTTOM faces, where Z can not be up, up is
    the horizontal axis (+-X or +-Y) nearest to where the current camera was
    looking (or to its up direction), so the turn is the smallest one.
    @param region a region of the cube
    @param currentRotation rotation of the camera now (columns front, left,
    up)
    @return the rotation of the camera for the view of the region
    */
    public Matrix4x4d calculateViewRotation(int region, Matrix4x4d currentRotation)
    {
        Vector3Dd front = getRegionDirection(region).multiply(-1);
        int[] d = regionDirections[region];

        if ( d[0] != 0 || d[1] != 0 ) {
            return createCameraRotation(front, new Vector3Dd(0, 0, 1));
        }

        Vector3Dd currentFront = new Vector3Dd(currentRotation.get(0, 0),
            currentRotation.get(1, 0), currentRotation.get(2, 0));
        Vector3Dd currentUp = new Vector3Dd(currentRotation.get(0, 2),
            currentRotation.get(1, 2), currentRotation.get(2, 2));
        // Tilting down to look at the TOP, what was ahead goes up on the
        // screen; tilting up to look at the BOTTOM, it goes down
        Vector3Dd hint = currentUp.add(currentFront.multiply(d[2]));
        Vector3Dd[] candidates = {
            new Vector3Dd(1, 0, 0), new Vector3Dd(-1, 0, 0),
            new Vector3Dd(0, 1, 0), new Vector3Dd(0, -1, 0)
        };
        Vector3Dd best = candidates[2];
        double bestDot = Double.NEGATIVE_INFINITY;

        for ( Vector3Dd c : candidates ) {
            double dot = c.dotProduct(hint);
            if ( dot > bestDot + VSDK.EPSILON ) {
                bestDot = dot;
                best = c;
            }
        }
        return createCameraRotation(front, best);
    }

    //= Presentation ======================================================

    /**
    @param camera camera of the view the gizmo shows and controls
    */
    public void setCamera(Camera camera)
    {
        this.camera = camera;
        updateHudCamera();
    }

    public Camera getCamera()
    {
        return camera;
    }

    /**
    @return the orthogonal camera the gizmo is seen through, oriented as the
    camera of the view, with a viewport of the size of the area of the gizmo
    */
    public Camera getHudCamera()
    {
        return hudCamera;
    }

    /**
    Orients the HUD camera as the camera of the view, looking at the center
    of the model.
    */
    public void updateHudCamera()
    {
        Matrix4x4d rotation = (camera != null) ? camera.getRotation() :
            createCameraRotation(new Vector3Dd(1, 1, -1), new Vector3Dd(0, 0, 1));
        Vector3Dd front = new Vector3Dd(rotation.get(0, 0), rotation.get(1, 0),
            rotation.get(2, 0)).normalized();

        hudCamera.setRotation(rotation);
        hudCamera.setPosition(front.multiply(-HUD_CAMERA_DISTANCE));
        hudCamera.updateViewportResize(sizeInPixels, sizeInPixels);
        hudCamera.updateVectors();
    }

    /**
    Places the gizmo at the upper right corner of a viewport, with the size
    following the resolution of the screen. The title of the viewport is at
    its upper left corner: if the viewport is so narrow that the gizmo would
    cover the title, it is moved down, under the title.
    @param viewportSizeX width of the viewport, in pixels
    @param titleAreaEndX first pixel to the right of the title area (0 if
    there is no title)
    @param titleAreaEndY first pixel under the title area (0 if there is no
    title)
    @param scaler scaler of the viewport set, or null to use base sizes
    */
    public void placeInViewport(int viewportSizeX, int titleAreaEndX,
                                int titleAreaEndY, ViewportElementScaler scaler)
    {
        if ( scaler != null ) {
            sizeInPixels = Math.max(16, scaler.scaleSize(baseSizeInPixels));
            marginInPixels = scaler.scaleSize(baseMarginInPixels);
        }
        else {
            sizeInPixels = baseSizeInPixels;
            marginInPixels = baseMarginInPixels;
        }
        hudX = viewportSizeX - marginInPixels - sizeInPixels;
        hudY = marginInPixels;
        if ( hudX < titleAreaEndX + marginInPixels ) {
            hudY = titleAreaEndY + marginInPixels;
        }
        updateHudCamera();
    }

    /**
    Makes the gizmo show and control the camera of a viewport, placed at its
    upper right corner, avoiding its title (see `Viewport.getTitleAreaSizeX`).
    Renderers and interaction techniques must place the gizmo the same way.
    @param viewport viewport where the gizmo is presented
    @param scaler scaler of the viewport set, or null to use base sizes
    */
    public void placeInViewport(Viewport viewport, ViewportElementScaler scaler)
    {
        camera = viewport.getActiveCamera();
        placeInViewport(viewport.getPixelSizeX(),
            viewport.getTitleAreaStartX() + viewport.getTitleAreaSizeX(),
            viewport.getTitleAreaStartY() + viewport.getTitleAreaSizeY(),
            scaler);
    }

    public int getBaseSizeInPixels()
    {
        return baseSizeInPixels;
    }

    public void setBaseSizeInPixels(int baseSizeInPixels)
    {
        this.baseSizeInPixels = baseSizeInPixels;
    }

    /** @return side of the square area of the gizmo, in pixels */
    public int getSizeInPixels()
    {
        return sizeInPixels;
    }

    /** @return left of the area of the gizmo, in pixels of the viewport */
    public int getHudX()
    {
        return hudX;
    }

    /** @return top of the area of the gizmo, in pixels of the viewport (y
    grows downwards) */
    public int getHudY()
    {
        return hudY;
    }

    /**
    @param x horizontal pixel of the viewport
    @param y vertical pixel of the viewport (from the top)
    @return true if the pixel is in the area of the gizmo
    */
    public boolean containsPixel(int x, int y)
    {
        return visible && x >= hudX && x < hudX + sizeInPixels &&
            y >= hudY && y < hudY + sizeInPixels;
    }

    public boolean isVisible()
    {
        return visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    /**
    Projects a point of the model to the viewport.
    @return {x, y} pixel of the viewport (y from the top)
    */
    public double[] projectToViewport(Vector3Dd p)
    {
        Vector4Dd clip = hudCamera.calculateProjectionMatrix().multiply(new Vector4Dd(p));
        Vector4Dd ndc = clip.dividedByW();

        return new double[] {
            hudX + (ndc.x() + 1.0) / 2.0 * sizeInPixels,
            hudY + (1.0 - ndc.y()) / 2.0 * sizeInPixels
        };
    }

    /**
    @param region a face region
    @return the cosine of the angle between the outward normal of the face
    and the direction towards the viewer (positive when the face is seen)
    */
    public double faceVisibility(int region)
    {
        Vector3Dd towardsViewer = hudCamera.getFront().multiply(-1).normalized();
        return getRegionDirection(region).dotProduct(towardsViewer);
    }

    //= Picking ===========================================================

    /**
    Finds the part of the gizmo under a pixel.
    @param x horizontal pixel of the viewport
    @param y vertical pixel of the viewport (from the top)
    @return the region (or `COMPASS_REGION`) nearest to the viewer under the
    pixel, or `NO_REGION`
    */
    public int pickRegion(int x, int y)
    {
        if ( !containsPixel(x, y) ) {
            return NO_REGION;
        }
        Ray ray = hudCamera.generateRay(x - hudX, y - hudY);
        return pickRegion(ray);
    }

    /**
    @param ray ray in the space of the model of the gizmo
    @return the region nearest to the origin of the ray hit by it, or
    `NO_REGION`
    */
    public int pickRegion(Ray ray)
    {
        int nearest = NO_REGION;
        double nearestT = Double.POSITIVE_INFINITY;

        for ( int r = 0; r < parts.size(); r++ ) {
            RayHit hit = new RayHit(RayHit.DETAIL_NONE, true);
            if ( parts.get(r).doIntersectionFirstHit(ray, hit) ) {
                double t = hit.getRay() != null ? hit.getRay().getT() :
                    hit.getHitDistance();
                if ( t < nearestT ) {
                    nearestT = t;
                    nearest = r;
                }
            }
        }
        return nearest;
    }

    /**
    Intersects the line of sight of a pixel with the plane of the compass.
    @param x horizontal pixel of the area of the gizmo (0 at its left)
    @param y vertical pixel of the area of the gizmo (0 at its top)
    @param viewCamera camera that sees the gizmo (i.e. a copy of the HUD
    camera taken when a gesture started)
    @return the angle, around Z, of the point of the plane of the compass
    under the pixel, or NaN if the plane is seen almost edge on
    */
    public static double compassAngleAt(int x, int y, Camera viewCamera)
    {
        Ray ray = viewCamera.generateRay(x, y);
        Vector3Dd d = ray.getDirection();
        if ( Math.abs(d.normalized().z()) < 0.1 ) {
            return Double.NaN;
        }
        double zc = COMPASS_BASE_Z + COMPASS_HEIGHT / 2;
        double t = (zc - ray.getOrigin().z()) / d.z();
        Vector3Dd p = ray.getOrigin().add(d.multiply(t));
        return Math.atan2(p.y(), p.x());
    }

    //= State and its materials ===========================================

    /**
    @return true if the cursor is over the gizmo (it is opaque then)
    */
    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        if ( this.active != active ) {
            this.active = active;
            updatePartMaterials();
        }
    }

    /**
    @return the region under the cursor, `COMPASS_REGION`, or `NO_REGION`
    */
    public int getVolatileSelection()
    {
        return volatileSelection;
    }

    public void setVolatileSelection(int region)
    {
        if ( volatileSelection != region ) {
            volatileSelection = region;
            updatePartMaterials();
        }
    }

    public double getInactiveOpacity()
    {
        return inactiveOpacity;
    }

    public void setInactiveOpacity(double inactiveOpacity)
    {
        this.inactiveOpacity = inactiveOpacity;
        updatePartMaterials();
    }

    /** @return opacity of the gizmo in its current state */
    public double getOpacity()
    {
        return active ? 1.0 : inactiveOpacity;
    }

    public ColorRgb getLineColor()
    {
        return lineColor;
    }

    public ColorRgb getLabelColor()
    {
        return labelColor;
    }

    public ColorRgb getHighlightColor()
    {
        return highlightColor;
    }

    /**
    @param region a region or `COMPASS_REGION`
    @return the color of the part of the region, for the current state
    */
    public ColorRgb getPartColor(int region)
    {
        if ( region == volatileSelection ) {
            return highlightColor;
        }
        switch ( getRegionKind(region) ) {
          case KIND_FACE:
            return faceColor;
          case KIND_EDGE:
            return edgeColor;
          case KIND_CORNER:
            return cornerColor;
          default:
            return compassColor;
        }
    }

    /**
    Gives each part a material with the color and opacity it has in the
    current state of the gizmo.
    */
    public final void updatePartMaterials()
    {
        double opacity = getOpacity();

        for ( int r = 0; r < parts.size(); r++ ) {
            ColorRgb c = getPartColor(r);
            SimpleMaterial m = new SimpleMaterial()
                .withAmbient(c.multiply(0.45))
                .withDiffuse(c)
                .withSpecular(new ColorRgb(0.15, 0.15, 0.15))
                .withPhongExponent(20)
                .withOpacity(opacity);
            parts.get(r).setMaterial(m);
        }
    }
}
