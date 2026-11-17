#ifndef __CAMERA_CONTROL_GIZMO__
#define __CAMERA_CONTROL_GIZMO__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/gizmo/Gizmo.h"

class Geometry;
class PolyhedralBoundedSolid;
class Ray;
class SimpleBody;
class Viewport;
class ViewportElementScaler;

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
viewport and y growing downwards. The parts (`getParts`) are bodies with
materials that reflect the state of the gizmo (`updatePartMaterials`): the
region under the cursor (`getVolatileSelection`) is highlighted, and the
gizmo is translucent when the cursor is not over it (`getInactiveOpacity`).
Renderers of each technology draw the parts with the HUD camera.

Interaction (see `CameraControlGizmoInteractionTechnique`) and the smooth
turn of the camera (see `CameraControlGizmoAnimation`) are platform
independent too.
*/
class CameraControlGizmo : public Gizmo {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    enum {
        /// Not a region of the gizmo
        NO_REGION = -1,
        /// Number of regions of the cube
        NUMBER_OF_CUBE_REGIONS = 26,
        /// The compass ring under the cube
        COMPASS_REGION = 26,
        /// Number of parts (bodies) of the gizmo: the regions and the compass
        NUMBER_OF_PARTS = 27
    };

    /// Kinds of regions
    enum {
        KIND_NONE = 0,
        KIND_FACE = 1,
        KIND_EDGE = 2,
        KIND_CORNER = 3,
        KIND_COMPASS = 4
    };

    /// Width of the corner bands, as a fraction of the side of the cube
    static const double CORNER_FRACTION;
    /// Separation between neighbor regions, in units of the model
    static const double GAP;
    /// Radii and height of the compass, and the height of its base
    static const double COMPASS_INTERNAL_RADIUS;
    static const double COMPASS_EXTERNAL_RADIUS;
    static const double COMPASS_HEIGHT;
    static const double COMPASS_BASE_Z;

    /// Half of the side of the square of the model seen by the HUD camera
    static const double HUD_HALF_EXTENT;
    /// Distance from the center of the model to the HUD camera
    static const double HUD_CAMERA_DISTANCE;

    enum {
        DEFAULT_BASE_SIZE_IN_PIXELS = 110,
        DEFAULT_BASE_MARGIN_IN_PIXELS = 6
    };
    static const double DEFAULT_INACTIVE_OPACITY;

    /// Sides of the polygons approximating the circles of the compass
    enum { COMPASS_SLICES = 24 };

private:
    //= Model ===========================================================
    /// Direction (i, j, k) of each region of the cube
    int regionDirections[NUMBER_OF_CUBE_REGIONS][3];
    /// Bodies of the regions, then the compass (owned)
    ArrayList<SimpleBody*> parts;
    /// Edges of the boundary representations of the parts, by part (null
    /// until they are first asked for)
    ArrayList<float>* edgeSegments;
    ArrayList<float>* createEdgeSegments();

    //= Presentation ====================================================
    Camera* camera;
    Camera hudCamera;
    int baseSizeInPixels;
    int baseMarginInPixels;
    int sizeInPixels;
    int marginInPixels;
    /// Upper left corner of the area of the gizmo, in pixels of the viewport
    int hudX;
    int hudY;
    BOOLEAN visible;
    double inactiveOpacity;
    BOOLEAN active;
    int volatileSelection;

    ColorRgb faceColor;
    ColorRgb edgeColor;
    ColorRgb cornerColor;
    ColorRgb compassColor;
    ColorRgb highlightColor;
    ColorRgb lineColor;
    ColorRgb labelColor;

    //= Model construction ==============================================

    /**
    @param out receives the interval [min, max] of the band `v` (-1, 0 or 1)
    of an axis, without the gaps towards its neighbor bands
    */
    static void band(int v, double out[2]);
    void buildModel();
    static SimpleBody* createPart(Geometry* geometry, const Vector3Dd& position,
        const String& name);

    // Not copied: the parts are owned
    CameraControlGizmo(const CameraControlGizmo&);
    CameraControlGizmo& operator=(const CameraControlGizmo&);

public:
    CameraControlGizmo();
    virtual ~CameraControlGizmo();

    //= Model queries ===================================================

    /**
    @return the bodies of the gizmo: index `r` is the body of region `r`, and
    the last one (`COMPASS_REGION`) is the compass
    */
    const ArrayList<SimpleBody*>& getParts() const;

    /**
    @param part index of a part (a region or `COMPASS_REGION`)
    @return the boundary representation of the geometry of the part, in the
    space of the part (its body places it with its position), owned by the
    caller
    */
    PolyhedralBoundedSolid* exportPartToPolyhedralBoundedSolid(int part) const;

    /**
    The edges of the boundary representations of the parts, for renderers
    that draw the gizmo with lines over its surfaces (the edges of their
    triangle meshes would also show the diagonals of the faces).
    @param part index of a part (a region or `COMPASS_REGION`)
    @return x, y, z of the two ends of each edge, in the space of the gizmo
    */
    ArrayList<float> getEdgeSegments(int part);

    /**
    @param region a region of the cube
    @param out receives its direction (i, j, k), each component -1, 0 or 1
    */
    void getRegionDirectionComponents(int region, int out[3]) const;

    /**
    @param region a region of the cube
    @return the unit vector from the center of the cube towards the region
    */
    Vector3Dd getRegionDirection(int region) const;

    /**
    @param i component along X (-1, 0 or 1)
    @param j component along Y (-1, 0 or 1)
    @param k component along Z (-1, 0 or 1)
    @return the region with that direction, or `NO_REGION` for (0, 0, 0)
    */
    int findRegion(int i, int j, int k) const;

    /**
    @param region a region, `COMPASS_REGION` or `NO_REGION`
    @return one of the `KIND_*` constants
    */
    int getRegionKind(int region) const;

    /**
    @param region a face region
    @return its label (i.e. "TOP"), or an empty string (Java: null) if it is
    not a face
    */
    String getFaceLabel(int region) const;

    /**
    @return a readable name of a region (i.e. "TOP FRONT RIGHT")
    */
    String regionName(int region) const;

    /**
    @return the regions that are faces of the cube
    */
    ArrayList<int> getFaceRegions() const;

    /**
    @param region a face region
    @return the point of the model where its label goes (the center of the
    face, on the surface of the cube)
    */
    Vector3Dd getFaceLabelPosition(int region) const;

    //= Views ===========================================================

    /**
    Builds a camera rotation (as given by `Camera::getRotation()`: columns
    front, left and up) looking along `front`, with the up direction as near
    as possible to `upHint`.
    C++ port note: Java returns the rotation, or null if `upHint` is
    parallel to `front`; here the result tells it and `out` receives the
    rotation.
    @return false if `upHint` is parallel to `front` (`out` is not changed)
    */
    static BOOLEAN createCameraRotation(const Vector3Dd& front,
        const Vector3Dd& upHint, Matrix4x4d& out);

    /**
    Calculates the orientation of the camera for the view of a region: it
    looks at the center from the direction of the region, keeping the scene
    upright (Z up). For the TOP and BOTTOM faces, where Z can not be up, up is
    the horizontal axis (+-X or +-Y) nearest to where the current camera was
    looking (or to its up direction), so the turn is the smallest one.
    @param region a region of the cube
    @param currentRotation rotation of the camera now (columns front, left,
    up)
    @param out receives the rotation of the camera for the view of the
    region
    @return false if it could not be calculated (Java: null)
    */
    BOOLEAN calculateViewRotation(int region, const Matrix4x4d& currentRotation,
        Matrix4x4d& out) const;

    //= Presentation ====================================================

    /**
    @param camera camera of the view the gizmo shows and controls (not
    owned)
    */
    void setCamera(Camera* camera);
    Camera* getCamera() const;

    /**
    @return the orthogonal camera the gizmo is seen through, oriented as the
    camera of the view, with a viewport of the size of the area of the gizmo
    */
    Camera* getHudCamera();
    const Camera* getHudCamera() const;

    /**
    Orients the HUD camera as the camera of the view, looking at the center
    of the model.
    */
    void updateHudCamera();

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
    void placeInViewport(int viewportSizeX, int titleAreaEndX,
        int titleAreaEndY, const ViewportElementScaler* scaler);

    /**
    Makes the gizmo show and control the camera of a viewport, placed at its
    upper right corner, avoiding its title (see `Viewport::getTitleAreaSizeX`).
    Renderers and interaction techniques must place the gizmo the same way.
    @param viewport viewport where the gizmo is presented
    @param scaler scaler of the viewport set, or null to use base sizes
    */
    void placeInViewport(const Viewport* viewport,
        const ViewportElementScaler* scaler);

    int getBaseSizeInPixels() const;
    void setBaseSizeInPixels(int baseSizeInPixels);

    /** @return side of the square area of the gizmo, in pixels */
    int getSizeInPixels() const;

    /** @return left of the area of the gizmo, in pixels of the viewport */
    int getHudX() const;

    /** @return top of the area of the gizmo, in pixels of the viewport (y
    grows downwards) */
    int getHudY() const;

    /**
    @param x horizontal pixel of the viewport
    @param y vertical pixel of the viewport (from the top)
    @return true if the pixel is in the area of the gizmo
    */
    BOOLEAN containsPixel(int x, int y) const;

    BOOLEAN isVisible() const;
    void setVisible(BOOLEAN visible);

    /**
    Projects a point of the model to the viewport.
    @param out receives {x, y}, the pixel of the viewport (y from the top)
    */
    void projectToViewport(const Vector3Dd& p, double out[2]) const;

    /**
    @param region a face region
    @return the cosine of the angle between the outward normal of the face
    and the direction towards the viewer (positive when the face is seen)
    */
    double faceVisibility(int region) const;

    //= Picking =========================================================

    /**
    Finds the part of the gizmo under a pixel.
    @param x horizontal pixel of the viewport
    @param y vertical pixel of the viewport (from the top)
    @return the region (or `COMPASS_REGION`) nearest to the viewer under the
    pixel, or `NO_REGION`
    */
    int pickRegion(int x, int y) const;

    /**
    @param ray ray in the space of the model of the gizmo
    @return the region nearest to the origin of the ray hit by it, or
    `NO_REGION`
    */
    int pickRegion(const Ray& ray) const;

    /**
    Intersects the line of sight of a pixel with the plane of the compass.
    @param x horizontal pixel of the area of the gizmo (0 at its left)
    @param y vertical pixel of the area of the gizmo (0 at its top)
    @param viewCamera camera that sees the gizmo (i.e. a copy of the HUD
    camera taken when a gesture started)
    @return the angle, around Z, of the point of the plane of the compass
    under the pixel, or NaN if the plane is seen almost edge on
    */
    static double compassAngleAt(int x, int y, const Camera& viewCamera);

    //= State and its materials =========================================

    /**
    @return true if the cursor is over the gizmo (it is opaque then)
    */
    BOOLEAN isActive() const;
    void setActive(BOOLEAN active);

    /**
    @return the region under the cursor, `COMPASS_REGION`, or `NO_REGION`
    */
    int getVolatileSelection() const;
    void setVolatileSelection(int region);

    double getInactiveOpacity() const;
    void setInactiveOpacity(double inactiveOpacity);

    /** @return opacity of the gizmo in its current state */
    double getOpacity() const;

    ColorRgb getLineColor() const;
    ColorRgb getLabelColor() const;
    ColorRgb getHighlightColor() const;

    /**
    @param region a region or `COMPASS_REGION`
    @return the color of the part of the region, for the current state
    */
    ColorRgb getPartColor(int region) const;

    /**
    Gives each part a material with the color and opacity it has in the
    current state of the gizmo.
    */
    void updatePartMaterials();
};

#endif
