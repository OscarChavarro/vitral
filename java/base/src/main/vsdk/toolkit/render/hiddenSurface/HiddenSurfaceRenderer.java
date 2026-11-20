//= References:                                                             =
//= [WEIL1977] Weiler, Kevin. Atherton, Peter. "Hidden surface removal      =
//=          using polygon area sorting". SIGGRAPH proceedings, 1977.       =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

package vsdk.toolkit.render.hiddenSurface;

// Java classes
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper.WeilerAthertonPolygonClipper;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.light.DirectionalLight;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.RenderingElement;

/**
An area of the x-y plane of the viewing space (a polygon with holes, maybe
with several main contours), with the measures used to sort and to clip it.
*/
class _WeilerAthertonArea extends RenderingElement
{
    final Polygon2D polygon;
    final double minX;
    final double minY;
    final double maxX;
    final double maxY;
    /// Area enclosed (holes discounted)
    final double area;
    /// Length of all the contours
    final double perimeter;

    _WeilerAthertonArea(Polygon2D polygon)
    {
        double[] box = HiddenSurfaceRenderer.boundingBox(polygon);
        this.polygon = polygon;
        this.minX = box[0];
        this.minY = box[1];
        this.maxX = box[2];
        this.maxY = box[3];
        this.area = HiddenSurfaceRenderer.enclosedArea(polygon);
        this.perimeter = HiddenSurfaceRenderer.perimeter(polygon);
    }

    boolean overlapsBox(_WeilerAthertonArea other)
    {
        return maxX >= other.minX && minX <= other.maxX &&
            maxY >= other.minY && minY <= other.maxY;
    }
}

/**
Original copy of an input polygon of [WEIL1977]: a front face of a solid,
transformed and clipped to the viewing space, with its plane in that space.
As noted in [WEIL1977].216, the clip template is always this copy of the
original polygon, not the pieces left of it after previous clips.
*/
class _WeilerAthertonSourcePolygon extends RenderingElement
{
    /// Area of the polygon on the x-y plane of the viewing space
    _WeilerAthertonArea template;
    /// Plane of the polygon in the viewing space, as depth = a*x + b*y + c,
    /// where a smaller depth is closer to the observer
    double a;
    double b;
    double c;
    /// Key of the preliminary depth sort: the nearest depth of the polygon
    double nearestDepth;
    SimpleBody body;
    _PolyhedralBoundedSolidFace face;
    InfinitePlane worldPlane;
    /// False if the near, far or window clips of the view removed part of
    /// the face
    boolean complete;

    double depthAt(double x, double y)
    {
        return a * x + b * y + c;
    }
}

/**
Part of a `_WeilerAthertonSourcePolygon` left by the x-y clips: the
elements of the inside and outside lists of [WEIL1977].216, figure 2.
*/
class _WeilerAthertonPiece extends RenderingElement
{
    final _WeilerAthertonArea area;
    final _WeilerAthertonSourcePolygon source;

    _WeilerAthertonPiece(_WeilerAthertonArea area,
                         _WeilerAthertonSourcePolygon source)
    {
        this.area = area;
        this.source = source;
    }
}

/**
Areas of a face of a solid reached by a light (the "surface detail of a
lighter shade" of [WEIL1977].218), in world coordinates: the visible
polygons of a view from the light, transformed back to the plane of their
source face.
*/
class _WeilerAthertonLitFace extends RenderingElement
{
    /// The whole face is visible from the light
    boolean whole;
    /// Otherwise, the lit regions: each one a list of contours (main
    /// contours and holes), each contour a list of points
    final List<List<List<Vector3Dd>>> regions =
        new ArrayList<List<List<Vector3Dd>>>();
}

/**
Uniform grid over the edges of a polygon, telling quickly whether a point is
near its border: the verification of the clips tests it at a number of
points that grows with the number of vertices.
*/
class _WeilerAthertonEdgeGrid extends RenderingElement
{
    private final double tolerance;
    private final double minX;
    private final double minY;
    private final double cellWidth;
    private final double cellHeight;
    private final int columns;
    private final int rows;
    /// For each cell, the edges {ax, ay, bx, by} near it
    private final List<List<double[]>> cells;

    _WeilerAthertonEdgeGrid(Polygon2D polygon, double tolerance)
    {
        double[] box = HiddenSurfaceRenderer.boundingBox(polygon);
        int edges = 0;

        for ( _Polygon2DContour loop : polygon.loops ) {
            edges += loop.vertices.size();
        }
        this.tolerance = tolerance;
        this.minX = box[0] - tolerance;
        this.minY = box[1] - tolerance;
        int side = Math.max(1, (int)Math.sqrt(edges));
        this.columns = side;
        this.rows = side;
        this.cellWidth = Math.max((box[2] + tolerance - minX) / columns,
            Double.MIN_NORMAL);
        this.cellHeight = Math.max((box[3] + tolerance - minY) / rows,
            Double.MIN_NORMAL);
        this.cells = new ArrayList<List<double[]>>(columns * rows);
        for ( int i = 0; i < columns * rows; i++ ) {
            cells.add(null);
        }
        for ( _Polygon2DContour loop : polygon.loops ) {
            int n = loop.vertices.size();
            for ( int i = 0; i < n; i++ ) {
                Vertex2D a = loop.vertices.get(i);
                Vertex2D b = loop.vertices.get((i + 1) % n);
                double[] edge = {a.x, a.y, b.x, b.y};
                int c0 = column(Math.min(a.x, b.x) - tolerance);
                int c1 = column(Math.max(a.x, b.x) + tolerance);
                int r0 = row(Math.min(a.y, b.y) - tolerance);
                int r1 = row(Math.max(a.y, b.y) + tolerance);
                for ( int r = r0; r <= r1; r++ ) {
                    for ( int c = c0; c <= c1; c++ ) {
                        List<double[]> cell = cells.get(r * columns + c);
                        if ( cell == null ) {
                            cell = new ArrayList<double[]>();
                            cells.set(r * columns + c, cell);
                        }
                        cell.add(edge);
                    }
                }
            }
        }
    }

    private int column(double x)
    {
        return Math.max(0, Math.min(columns - 1, (int)((x - minX) / cellWidth)));
    }

    private int row(double y)
    {
        return Math.max(0, Math.min(rows - 1, (int)((y - minY) / cellHeight)));
    }

    /**
    @return true if the point is at a distance from the border of the
    polygon not greater than the tolerance
    */
    boolean isNearBorder(double x, double y)
    {
        if ( x < minX || y < minY || x > minX + columns * cellWidth ||
             y > minY + rows * cellHeight ) {
            return false;
        }
        List<double[]> cell = cells.get(row(y) * columns + column(x));
        if ( cell == null ) {
            return false;
        }
        double tolerance2 = tolerance * tolerance;
        for ( double[] e : cell ) {
            double dx = e[2] - e[0];
            double dy = e[3] - e[1];
            double length2 = dx * dx + dy * dy;
            double t = length2 > 0.0 ? Math.max(0.0, Math.min(1.0,
                ((x - e[0]) * dx + (y - e[1]) * dy) / length2)) : 0.0;
            double ex = e[0] + t * dx - x;
            double ey = e[1] + t * dy - y;
            if ( ex * ex + ey * ey <= tolerance2 ) {
                return true;
            }
        }
        return false;
    }
}

/**
Part of a visible area of a face, with the lights that reach it.
*/
class _WeilerAthertonShadedPart extends RenderingElement
{
    final _WeilerAthertonArea area;
    final List<Light> lights;

    _WeilerAthertonShadedPart(_WeilerAthertonArea area, List<Light> lights)
    {
        this.area = area;
        this.lights = lights;
    }
}

/**
This class implements the hidden surface removal algorithm by polygon area
sorting of Weiler and Atherton [WEIL1977]: the hidden surface problem is
solved in polygon shaped windows of the image, taken from the input
polygons themselves, using the general 2D polygon clipper of the same paper
(`WeilerAthertonPolygonClipper`), which clips a concave polygon with holes
to the area of another concave polygon with holes.

The input are the faces of a set of `PolyhedralBoundedSolid`s. As required
by [WEIL1977].214, they are first transformed to the viewing space, where
the X and Y axes are parallel to the display surface and Z is parallel to
the line of sight: here, the normalized device coordinates of the camera
(with x and y scaled by `SCALE`), after clipping in homogeneous coordinates
[FOLE1992].6 against the near and far planes. The projective
transformation keeps planes as planes, so each face keeps a planar depth
function over its x-y area, and the depth of any point given by the 2D
clipper (that only computes x and y) is recovered from the plane of its
source face ("depth information can be preserved in all output",
[WEIL1977].217). The faces facing away from the observer are removed
beforehand (the "backplane removal" of [WEIL1977].221), and the faces are
clipped to the window of the view (the "viewbox clipping") with the same 2D
clipper.

The four steps of [WEIL1977].215 follow:
  a) A preliminary rough depth sort, by the nearest depth of each polygon.
  b) An x-y polygon area sort to the area of the currently most forward
     polygon: the remaining polygons are clipped to it, giving an inside
     list and an outside list (figure 2).
  c) A depth sort by removal of the polygons of the inside list located
     behind the current clip polygon.
  d) A conclusive depth sort by recursive subdivision when a polygon of the
     inside list is in front of the clip polygon, using it (its original
     copy) as the clip polygon and the inside list as input (figure 3). A
     stack of the polygons used as clip polygons detects the cyclic overlap
     (figure 4a), where no additional recursion is needed. Then the inside
     list is output and the process repeats with the outside list.

The output never overlaps on the x-y plane, as each visible area had all
the polygons behind it removed, so it solves the hidden lines as well as the
hidden surfaces [WEIL1977].215.

Two additions to [WEIL1977] make it usable with the solids of a scene:
  - Polygons that penetrate each other are not considered by [WEIL1977]
    (between two polygons in a window, one is entirely in front of the
    other). As solids of a scene may intersect, a piece of the inside list
    that lies in front of the clip polygon in part, and behind it in the
    other part, is divided by the projection of the intersection line of
    both planes (where their depth functions are equal), and only its front
    part is kept, so the steps c) and d) work as in [WEIL1977] with it.
  - The pieces have most of their vertices on the borders of previous clip
    polygons, the degenerate case of the clipper where "care is taken in
    placement of intersections where the subject and clip polygon contours
    are identical" [WEIL1977].218. Each clip is verified (the inside and
    outside areas must add up to the area of the subject, and sample points
    of the polygons must fall in the right part) and, if it fails, it is
    repeated with the clip polygon moved a distance far under the size of a
    pixel, which turns the degenerate contacts into regular ones. The strips
    thinner than `SLIVER_WIDTH` that such moves can leave are discarded. If
    every attempt fails, a convex clip polygon (the faces of boxes and the
    facets of spheres, the half planes of the penetrations) is solved by the
    half planes of its edges, as the Sutherland-Hodgman clipper that
    [WEIL1977].217 cites, and a concave one by the half planes of the convex
    trapezoids it is decomposed in.

Shadows are cast as in [WEIL1977].218 ("Shadowing"), taking advantage of the
output having the same form as the input: a hidden surface removed view of
the scene is produced from the position of each light, and its visible
polygons, transformed back to the original space, are the lit areas of their
source faces (surface detail of a lighter shade, [WEIL1977].219). Then the
hidden surface removed view from the observer is produced, and each visible
area is clipped with the same polygon clipper to the lit areas of its face,
for each light, so its parts know which lights reach them. A point light is
seen with one perspective view enclosing the scene or, if it is among the
solids, with the six views of the faces of a cube around it (slightly
overlapped); a directional light with an orthogonal view along its
direction. Ambient lights cast no
shadows.
*/
public class HiddenSurfaceRenderer extends RenderingElement
{
    /// Units of the viewing space for each unit of the normalized device
    /// coordinates: the tolerances of the 2D clipper are absolute (1e-4), so
    /// with this scale they are around 1e-5 pixels of a usual view
    private static final double SCALE = 1.0e4;
    /// Tolerance to compare depths in the viewing space (normalized device
    /// coordinates, from -1 to 1)
    private static final double DEPTH_EPSILON = 1.0e-10;
    /// Areas (in square units of the viewing space) under this are degenerate
    private static final double AREA_EPSILON = 1.0e-6;
    /// Contours thinner than this (area over half perimeter, in units of the
    /// viewing space) are degenerate strips
    private static final double SLIVER_WIDTH = 1.0e-3;
    /// Distance the clip polygon is moved in the first retry of a failed
    /// clip, in units of the viewing space; it grows in each retry
    private static final double RETRY_SHIFT = 1.0e-2;
    private static final int MAXIMUM_RETRIES = 6;
    /// Edges shorter than this (in units of the viewing space) are ignored by
    /// the clips by half planes
    private static final double MINIMUM_EDGE_LENGTH = 1.0e-6;

    private static final int BEHIND = 0;
    private static final int IN_FRONT = 1;
    private static final int PENETRATING = 2;

    /**
    A visible area of a face of a solid, output of the algorithm.
    */
    public static final class VisiblePolygon extends RenderingElement
    {
        /// Visible area, on the x-y plane of the normalized device coordinates
        /// of the camera (from <-1, -1> at the lower left corner of the view
        /// to <1, 1> at its upper right corner). Main contours are
        /// counterclockwise and holes clockwise.
        public final Polygon2D area;
        /// Body the face belongs to
        public final SimpleBody body;
        /// Face whose visible area is given
        public final _PolyhedralBoundedSolidFace face;
        /// Plane of the face in world coordinates, with an outward normal
        public final InfinitePlane worldPlane;
        /// Lights reaching the area (ambient lights included), or null if
        /// shadows were not computed
        public final List<Light> lights;

        VisiblePolygon(Polygon2D area, _WeilerAthertonSourcePolygon source,
                       List<Light> lights)
        {
            this.area = area;
            this.body = source.body;
            this.face = source.face;
            this.worldPlane = source.worldPlane;
            this.lights = lights;
        }
    }

    /// Relative margin of the views from the lights around the scene
    private static final double LIGHT_VIEW_MARGIN = 1.05;
    /// Largest half angle (radians) of the scene seen from a point light
    /// covered with one view; beyond it, six views of a cube are used
    private static final double MAXIMUM_LIGHT_HALF_ANGLE = Math.toRadians(55.0);
    /// Degrees added to the field of view of the six views of a cube
    private static final double CUBE_VIEW_OVERLAP = 2.0;
    /// Size (in pixels) of the views from the lights: only their aspect
    /// ratio matters
    private static final int LIGHT_VIEW_SIZE = 256;

    private final WeilerAthertonPolygonClipper clipper;
    private final ArrayList<_WeilerAthertonSourcePolygon> clipStack;
    /// Visible polygons, in the viewing space
    private final ArrayList<_WeilerAthertonPiece> output;

    private HiddenSurfaceRenderer()
    {
        clipper = new WeilerAthertonPolygonClipper();
        clipStack = new ArrayList<_WeilerAthertonSourcePolygon>();
        output = new ArrayList<_WeilerAthertonPiece>();
    }

    /**
    Solves the visible surfaces of a set of solids seen from a camera.
    @param solids bodies whose geometry is a `PolyhedralBoundedSolid` (other
    bodies are ignored)
    @param camera camera of the view; its viewport size must be already
    updated (it gives the aspect ratio)
    @return the visible areas of the faces of the solids, which do not
    overlap each other on the view
    */
    public static List<VisiblePolygon> executeWeilerAthertonAlgorithm(
        List<SimpleBody> solids, Camera camera)
    {
        HiddenSurfaceRenderer renderer = solve(solids, camera);
        List<VisiblePolygon> visible = new ArrayList<VisiblePolygon>();

        for ( _WeilerAthertonPiece piece : renderer.output ) {
            visible.add(new VisiblePolygon(toNormalizedDeviceCoordinates(
                piece.area.polygon), piece.source, null));
        }
        return visible;
    }

    /**
    Solves the visible surfaces of a set of solids seen from a camera, with
    the shadows cast by a set of lights, as in [WEIL1977].218 (see the class
    documentation).
    @param solids bodies whose geometry is a `PolyhedralBoundedSolid` (other
    bodies are ignored, and cast no shadows)
    @param camera camera of the view; its viewport size must be already
    updated (it gives the aspect ratio)
    @param lights lights of the scene
    @return the visible areas of the faces of the solids, which do not
    overlap each other on the view, each one with the lights that reach it
    */
    public static List<VisiblePolygon> executeWeilerAthertonAlgorithm(
        List<SimpleBody> solids, Camera camera, List<Light> lights)
    {
        HiddenSurfaceRenderer renderer = solve(solids, camera);
        List<Light> ambientLights = new ArrayList<Light>();
        List<Light> castingLights = new ArrayList<Light>();
        List<Map<SimpleBody, Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace>>>
            litFaces = new ArrayList<Map<SimpleBody,
                Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace>>>();
        double[] bounds = boundingSphere(solids);
        Matrix4x4d projection = camera.calculateProjectionMatrix();
        List<VisiblePolygon> visible = new ArrayList<VisiblePolygon>();

        // First pass: hidden surface removed views from the lights
        for ( Light light : lights ) {
            if ( light.isAmbient() ) {
                ambientLights.add(light);
            }
            else if ( bounds != null ) {
                castingLights.add(light);
                litFaces.add(litFacesFromLight(solids, light, bounds));
            }
        }

        // Second pass: the visible areas from the observer, divided by the
        // lit areas of their faces
        for ( _WeilerAthertonPiece piece : renderer.output ) {
            List<_WeilerAthertonShadedPart> parts =
                new ArrayList<_WeilerAthertonShadedPart>();
            parts.add(new _WeilerAthertonShadedPart(piece.area,
                new ArrayList<Light>(ambientLights)));
            for ( int k = 0; k < castingLights.size(); k++ ) {
                Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace> faces =
                    litFaces.get(k).get(piece.source.body);
                _WeilerAthertonLitFace lit = faces != null ?
                    faces.get(piece.source.face) : null;
                if ( lit != null ) {
                    parts = renderer.divideByLitArea(parts, lit,
                        castingLights.get(k), projection);
                }
            }
            for ( _WeilerAthertonShadedPart part : parts ) {
                visible.add(new VisiblePolygon(toNormalizedDeviceCoordinates(
                    part.area.polygon), piece.source, part.lights));
            }
        }
        return visible;
    }

    /**
    Runs the hidden surface removal of [WEIL1977] for a view.
    @return the renderer, with the visible polygons in its output
    */
    private static HiddenSurfaceRenderer solve(List<SimpleBody> solids,
                                               Camera camera)
    {
        HiddenSurfaceRenderer renderer = new HiddenSurfaceRenderer();
        List<_WeilerAthertonSourcePolygon> sources;
        List<_WeilerAthertonPiece> polygonList;
        int i;

        sources = renderer.buildInputPolygons(solids, camera);

        // Step a) of [WEIL1977].215: preliminary rough depth sort, from the
        // polygons closest to the observer to those furthest away
        Collections.sort(sources, (p, q) ->
            Double.compare(p.nearestDepth, q.nearestDepth));

        polygonList = new ArrayList<_WeilerAthertonPiece>();
        for ( i = 0; i < sources.size(); i++ ) {
            polygonList.add(new _WeilerAthertonPiece(sources.get(i).template,
                sources.get(i)));
        }

        // [WEIL1977].216: the process is continued until the outside list
        // is exhausted
        while ( !polygonList.isEmpty() ) {
            polygonList = renderer.subdivide(polygonList.get(0).source,
                polygonList);
        }
        return renderer;
    }

    //= Shadows ===========================================================

    /**
    Shadowing pass of [WEIL1977].218 for one light: "a hidden surface removed
    view of a scene from the position of the light source", whose visible
    polygons are "transformed back to the original space" as the lit areas
    of their source faces.
    @param bounds bounding sphere of the solids, as {x, y, z, radius}
    @return the lit areas of the faces reached by the light, by body and face
    */
    private static Map<SimpleBody, Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace>>
    litFacesFromLight(List<SimpleBody> solids, Light light, double[] bounds)
    {
        Map<SimpleBody, Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace>> lit =
            new IdentityHashMap<SimpleBody,
                Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace>>();

        // A light inside a solid sees only its inner side, the back of its
        // faces: it reaches nothing outside of it
        if ( !(light instanceof DirectionalLight) &&
             isInsideASolid(solids, light.getPosition()) ) {
            return lit;
        }
        for ( Camera camera : lightCameras(light, bounds) ) {
            HiddenSurfaceRenderer renderer = solve(solids, camera);
            Matrix4x4d inverse = camera.calculateProjectionMatrix().inverse();
            Map<_WeilerAthertonSourcePolygon, double[]> visibleAreas =
                new IdentityHashMap<_WeilerAthertonSourcePolygon, double[]>();

            for ( _WeilerAthertonPiece piece : renderer.output ) {
                double[] sum = visibleAreas.get(piece.source);
                if ( sum == null ) {
                    sum = new double[1];
                    visibleAreas.put(piece.source, sum);
                }
                sum[0] += piece.area.area;
            }
            for ( _WeilerAthertonPiece piece : renderer.output ) {
                _WeilerAthertonSourcePolygon source = piece.source;
                Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace> faces =
                    lit.get(source.body);
                if ( faces == null ) {
                    faces = new IdentityHashMap<_PolyhedralBoundedSolidFace,
                        _WeilerAthertonLitFace>();
                    lit.put(source.body, faces);
                }
                _WeilerAthertonLitFace litFace = faces.get(source.face);
                if ( litFace == null ) {
                    litFace = new _WeilerAthertonLitFace();
                    faces.put(source.face, litFace);
                }
                if ( litFace.whole ) {
                    continue;
                }
                // A face entirely in the view and not hidden at all is lit
                // whole: its visible pieces add up to its area
                if ( source.complete && visibleAreas.get(source)[0] >=
                     source.template.area * (1.0 - 1.0e-9) - AREA_EPSILON ) {
                    litFace.whole = true;
                    litFace.regions.clear();
                    continue;
                }
                List<List<Vector3Dd>> region = new ArrayList<List<Vector3Dd>>();
                for ( _Polygon2DContour loop : piece.area.polygon.loops ) {
                    List<Vector3Dd> contour = new ArrayList<Vector3Dd>();
                    for ( Vertex2D v : loop.vertices ) {
                        Vector3Dd p = unprojectToPlane(inverse, v.x / SCALE,
                            v.y / SCALE, source.worldPlane);
                        if ( p != null ) {
                            contour.add(p);
                        }
                    }
                    if ( contour.size() >= 3 ) {
                        region.add(contour);
                    }
                }
                if ( !region.isEmpty() ) {
                    litFace.regions.add(region);
                }
            }
        }
        return lit;
    }

    private static boolean isInsideASolid(List<SimpleBody> solids, Vector3Dd p)
    {
        for ( SimpleBody body : solids ) {
            if ( body.getGeometry() instanceof PolyhedralBoundedSolid brep &&
                 brep.doContainmentTest(body.getTransformationMatrix().inverse()
                     .multiply(p), VSDK.EPSILON) == Geometry.INSIDE ) {
                return true;
            }
        }
        return false;
    }

    /**
    @return the point of a plane seen at a point of the x-y plane of the
    normalized device coordinates of a view, or null if the line of sight
    is parallel to the plane
    */
    private static Vector3Dd unprojectToPlane(Matrix4x4d inverseProjection,
                                              double x, double y,
                                              InfinitePlane plane)
    {
        Vector4Dd a = inverseProjection.multiply(
            new Vector4Dd(x, y, -0.5, 1.0)).dividedByW();
        Vector4Dd b = inverseProjection.multiply(
            new Vector4Dd(x, y, 0.5, 1.0)).dividedByW();
        Vector3Dd p = new Vector3Dd(a.x(), a.y(), a.z());
        Vector3Dd direction = new Vector3Dd(b.x() - a.x(), b.y() - a.y(),
            b.z() - a.z());
        Vector3Dd n = plane.getNormal();
        double denominator = n.dotProduct(direction);

        if ( Math.abs(denominator) <= 1.0e-300 ) {
            return null;
        }
        double t = -(n.dotProduct(p) + plane.getD()) / denominator;
        return p.add(direction.multiply(t));
    }

    /**
    Views from a light enclosing the scene: one perspective view, or the six
    views of the faces of a cube when the light is among the solids, for a
    point (or spot) light, and one orthogonal view for a directional light.
    @param bounds bounding sphere of the solids, as {x, y, z, radius}
    */
    private static List<Camera> lightCameras(Light light, double[] bounds)
    {
        List<Camera> cameras = new ArrayList<Camera>();
        Vector3Dd center = new Vector3Dd(bounds[0], bounds[1], bounds[2]);
        double radius = bounds[3] * LIGHT_VIEW_MARGIN + VSDK.EPSILON;

        if ( light instanceof DirectionalLight ) {
            // The position of a directional light is its direction
            Vector3Dd direction = light.getPosition().normalized();
            Camera camera = lightCamera(center.subtract(
                direction.multiply(2.0 * radius)), direction);
            camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
            camera.setOrthogonalZoom(1.0 / radius);
            camera.setNearPlaneDistance(0.5 * radius);
            camera.setFarPlaneDistance(3.5 * radius);
            camera.updateViewportResize(LIGHT_VIEW_SIZE, LIGHT_VIEW_SIZE);
            cameras.add(camera);
            return cameras;
        }

        Vector3Dd position = light.getPosition();
        Vector3Dd toCenter = center.subtract(position);
        double distance = toCenter.length();
        if ( distance > radius &&
             Math.asin(radius / distance) <= MAXIMUM_LIGHT_HALF_ANGLE ) {
            Camera camera = lightCamera(position, toCenter.normalized());
            camera.setFov(2.0 * Math.toDegrees(Math.asin(radius / distance)) *
                LIGHT_VIEW_MARGIN);
            camera.setNearPlaneDistance(0.5 * (distance - radius));
            camera.setFarPlaneDistance(2.0 * (distance + radius));
            cameras.add(camera);
        }
        else {
            Vector3Dd[] directions = {
                new Vector3Dd(1.0, 0.0, 0.0), new Vector3Dd(-1.0, 0.0, 0.0),
                new Vector3Dd(0.0, 1.0, 0.0), new Vector3Dd(0.0, -1.0, 0.0),
                new Vector3Dd(0.0, 0.0, 1.0), new Vector3Dd(0.0, 0.0, -1.0)
            };
            // The views overlap a little, so no lit area is lost on their
            // common borders (the lit regions of a face may overlap: see
            // `divideByLitArea`)
            for ( Vector3Dd direction : directions ) {
                Camera camera = lightCamera(position, direction);
                camera.setFov(90.0 + CUBE_VIEW_OVERLAP);
                camera.setNearPlaneDistance(1.0e-3 * (distance + radius));
                camera.setFarPlaneDistance(2.0 * (distance + radius));
                cameras.add(camera);
            }
        }
        for ( Camera camera : cameras ) {
            camera.updateViewportResize(LIGHT_VIEW_SIZE, LIGHT_VIEW_SIZE);
        }
        return cameras;
    }

    /**
    @return a camera at a position looking along a direction
    */
    private static Camera lightCamera(Vector3Dd position, Vector3Dd front)
    {
        Camera camera = new Camera();
        Vector3Dd upHint = Math.abs(front.z()) < 0.9 ?
            new Vector3Dd(0.0, 0.0, 1.0) : new Vector3Dd(1.0, 0.0, 0.0);
        Vector3Dd left = upHint.crossProduct(front).normalized();
        Vector3Dd up = front.crossProduct(left).normalized();

        camera.setPosition(position);
        camera.setRotation(new Matrix4x4d()
            .withVal(0, 0, front.x()).withVal(0, 1, left.x()).withVal(0, 2, up.x())
            .withVal(1, 0, front.y()).withVal(1, 1, left.y()).withVal(1, 2, up.y())
            .withVal(2, 0, front.z()).withVal(2, 1, left.z()).withVal(2, 2, up.z()));
        camera.updateViewportResize(LIGHT_VIEW_SIZE, LIGHT_VIEW_SIZE);
        return camera;
    }

    /**
    @return the bounding sphere of the vertices of the solids, as {x, y, z,
    radius}, or null if they have no vertices
    */
    private static double[] boundingSphere(List<SimpleBody> solids)
    {
        double[] box = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
                        -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        List<Vector3Dd> points = new ArrayList<Vector3Dd>();

        for ( SimpleBody body : solids ) {
            if ( !(body.getGeometry() instanceof PolyhedralBoundedSolid brep) ) {
                continue;
            }
            Matrix4x4d transform = body.getTransformationMatrix();
            for ( int i = 0; i < brep.getPolygonsList().size(); i++ ) {
                _PolyhedralBoundedSolidFace face = brep.getPolygonsList().get(i);
                for ( int j = 0; j < face.boundariesList.size(); j++ ) {
                    _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
                    _PolyhedralBoundedSolidHalfEdge he =
                        loop != null ? loop.boundaryStartHalfEdge : null;
                    _PolyhedralBoundedSolidHalfEdge heStart = he;
                    if ( he == null ) {
                        continue;
                    }
                    do {
                        if ( he == null || he.startingVertex == null ) {
                            break;
                        }
                        Vector3Dd p = transform.multiply(he.startingVertex.position);
                        points.add(p);
                        box[0] = Math.min(box[0], p.x());
                        box[1] = Math.min(box[1], p.y());
                        box[2] = Math.min(box[2], p.z());
                        box[3] = Math.max(box[3], p.x());
                        box[4] = Math.max(box[4], p.y());
                        box[5] = Math.max(box[5], p.z());
                        he = he.next();
                    } while ( he != heStart );
                }
            }
        }
        if ( points.isEmpty() ) {
            return null;
        }
        Vector3Dd center = new Vector3Dd((box[0] + box[3]) / 2.0,
            (box[1] + box[4]) / 2.0, (box[2] + box[5]) / 2.0);
        double radius = 0.0;
        for ( Vector3Dd p : points ) {
            radius = Math.max(radius, p.subtract(center).length());
        }
        return new double[] {center.x(), center.y(), center.z(), radius};
    }

    /**
    Divides the parts of a visible area by the lit areas of its face for a
    light: the parts inside them get the light. The lit areas are projected
    to the viewing space of the observer and used as clip polygons.
    */
    private List<_WeilerAthertonShadedPart> divideByLitArea(
        List<_WeilerAthertonShadedPart> parts, _WeilerAthertonLitFace lit,
        Light light, Matrix4x4d projection)
    {
        List<_WeilerAthertonShadedPart> result =
            new ArrayList<_WeilerAthertonShadedPart>();

        if ( lit.whole ) {
            for ( _WeilerAthertonShadedPart part : parts ) {
                result.add(new _WeilerAthertonShadedPart(part.area,
                    withLight(part.lights, light)));
            }
            return result;
        }

        List<_WeilerAthertonArea> regions = new ArrayList<_WeilerAthertonArea>();
        for ( List<List<Vector3Dd>> region : lit.regions ) {
            _WeilerAthertonArea area = projectRegion(region, projection);
            if ( area != null ) {
                regions.add(area);
            }
        }
        for ( _WeilerAthertonShadedPart part : parts ) {
            // Each lit region takes its part of what the previous ones left,
            // so regions that overlap (from the views of a cube) are counted
            // once
            List<_WeilerAthertonArea> remaining = new ArrayList<_WeilerAthertonArea>();
            List<_WeilerAthertonArea> litAreas = new ArrayList<_WeilerAthertonArea>();
            remaining.add(part.area);
            for ( _WeilerAthertonArea region : regions ) {
                List<_WeilerAthertonArea> next = new ArrayList<_WeilerAthertonArea>();
                for ( _WeilerAthertonArea area : remaining ) {
                    if ( !area.overlapsBox(region) ) {
                        next.add(area);
                        continue;
                    }
                    verifiedClip(region, area, litAreas, next);
                }
                remaining = next;
            }
            if ( !litAreas.isEmpty() ) {
                List<Light> lights = withLight(part.lights, light);
                for ( _WeilerAthertonArea area : litAreas ) {
                    result.add(new _WeilerAthertonShadedPart(area, lights));
                }
            }
            for ( _WeilerAthertonArea area : remaining ) {
                result.add(new _WeilerAthertonShadedPart(area, part.lights));
            }
        }
        return result;
    }

    private static List<Light> withLight(List<Light> lights, Light light)
    {
        List<Light> result = new ArrayList<Light>(lights);
        result.add(light);
        return result;
    }

    /**
    Projects a lit region (in world coordinates) to the viewing space of the
    observer, clipped against its near and far planes.
    @return the region, with its main contours counterclockwise and its holes
    clockwise, or null if nothing of it is left
    */
    private static _WeilerAthertonArea projectRegion(
        List<List<Vector3Dd>> region, Matrix4x4d projection)
    {
        Polygon2D polygon = new Polygon2D();
        polygon.loops.clear();

        for ( List<Vector3Dd> contour : region ) {
            List<Vector4Dd> clipLoop = new ArrayList<Vector4Dd>();
            for ( Vector3Dd p : contour ) {
                clipLoop.add(projection.multiply(new Vector4Dd(p)));
            }
            clipLoop = clipAgainstPlane(clipLoop, 1.0);
            clipLoop = clipAgainstPlane(clipLoop, -1.0);
            if ( clipLoop.size() < 3 ) {
                continue;
            }
            List<double[]> loop = new ArrayList<double[]>();
            for ( Vector4Dd p : clipLoop ) {
                Vector4Dd ndc = p.dividedByW();
                loop.add(new double[] {SCALE * ndc.x(), SCALE * ndc.y()});
            }
            if ( Math.abs(signedArea(loop)) > AREA_EPSILON ) {
                addLoop(polygon, loop);
            }
        }
        if ( polygon.loops.isEmpty() ) {
            return null;
        }
        // The light and the observer see the same side of the face, so the
        // orientation is kept; it is checked anyway
        if ( enclosedArea(polygon) < 0.0 ) {
            for ( _Polygon2DContour loop : polygon.loops ) {
                Collections.reverse(loop.vertices);
            }
        }
        _WeilerAthertonArea area = new _WeilerAthertonArea(polygon);
        return area.area > AREA_EPSILON ? area : null;
    }

    private static Polygon2D toNormalizedDeviceCoordinates(Polygon2D polygon)
    {
        Polygon2D ndc = new Polygon2D();
        ndc.loops.clear();
        for ( _Polygon2DContour loop : polygon.loops ) {
            ndc.nextLoop();
            for ( Vertex2D v : loop.vertices ) {
                ndc.addVertex(v.x / SCALE, v.y / SCALE);
            }
        }
        return ndc;
    }

    //= Hidden surface algorithm ==========================================

    /**
    Solves the hidden surface problem in the area of a clip polygon, as in
    [WEIL1977].215-216 (steps b, c and d), outputting the visible polygons of
    that area.
    @param clipPolygon source polygon whose original copy is the clip
    template
    @param polygonList polygons to sort
    @return the outside list: the parts of the polygons outside of the clip
    polygon, still to be solved
    */
    private List<_WeilerAthertonPiece> subdivide(
        _WeilerAthertonSourcePolygon clipPolygon,
        List<_WeilerAthertonPiece> polygonList)
    {
        List<_WeilerAthertonPiece> insideList = new ArrayList<_WeilerAthertonPiece>();
        List<_WeilerAthertonPiece> outsideList = new ArrayList<_WeilerAthertonPiece>();
        int i;

        // Step b): x-y polygon area sort to the area of the clip polygon
        for ( i = 0; i < polygonList.size(); i++ ) {
            clipPiece(clipPolygon, polygonList.get(i), insideList, outsideList);
        }

        // Step c): removal of the polygons behind the clip polygon
        insideList = removePolygonsBehind(clipPolygon, insideList);

        // Step d): a polygon in front of the clip polygon reveals an error in
        // the initial depth sort: the region is subdivided recursively with
        // the offending polygon as clip polygon and the inside list as input
        // (figure 3). If it is on the stack, it is a cyclic overlap
        // (figure 4a), and all the material behind it was already removed.
        while ( true ) {
            _WeilerAthertonPiece offending = null;
            for ( i = 0; i < insideList.size(); i++ ) {
                _WeilerAthertonPiece piece = insideList.get(i);
                if ( piece.source != clipPolygon &&
                     !clipStack.contains(piece.source) ) {
                    offending = piece;
                    break;
                }
            }
            if ( offending == null ) {
                break;
            }
            clipStack.add(clipPolygon);
            insideList = subdivide(offending.source, insideList);
            clipStack.remove(clipStack.size() - 1);
        }

        // The inside list is displayed: first the parts of the clip polygon,
        // then the parts of the cyclic overlaps, which are in front of it
        for ( i = 0; i < insideList.size(); i++ ) {
            if ( insideList.get(i).source == clipPolygon ) {
                display(insideList.get(i));
            }
        }
        for ( i = 0; i < insideList.size(); i++ ) {
            if ( insideList.get(i).source != clipPolygon ) {
                display(insideList.get(i));
            }
        }
        return outsideList;
    }

    /**
    Clips a polygon to the area of the original copy of the clip polygon,
    placing its parts on the inside and outside lists.
    */
    private void clipPiece(_WeilerAthertonSourcePolygon clipPolygon,
                           _WeilerAthertonPiece piece,
                           List<_WeilerAthertonPiece> insideList,
                           List<_WeilerAthertonPiece> outsideList)
    {
        // The parts of a polygon are inside its own original copy
        if ( piece.source == clipPolygon ) {
            insideList.add(piece);
            return;
        }
        // Areas that do not overlap need no clip
        if ( !piece.area.overlapsBox(clipPolygon.template) ) {
            outsideList.add(piece);
            return;
        }

        List<_WeilerAthertonArea> inside = new ArrayList<_WeilerAthertonArea>();
        List<_WeilerAthertonArea> outside = new ArrayList<_WeilerAthertonArea>();
        verifiedClip(clipPolygon.template, piece.area, inside, outside);
        for ( _WeilerAthertonArea area : inside ) {
            insideList.add(new _WeilerAthertonPiece(area, piece.source));
        }
        for ( _WeilerAthertonArea area : outside ) {
            outsideList.add(new _WeilerAthertonPiece(area, piece.source));
        }
    }

    /**
    Step c) of [WEIL1977].216: the polygons of the inside list located behind
    the clip polygon are hidden from view. A polygon partially in front of
    it (penetrating it) is divided, and only its front part is kept.
    @return the new inside list
    */
    private List<_WeilerAthertonPiece> removePolygonsBehind(
        _WeilerAthertonSourcePolygon clipPolygon,
        List<_WeilerAthertonPiece> insideList)
    {
        List<_WeilerAthertonPiece> remaining = new ArrayList<_WeilerAthertonPiece>();
        int i;

        for ( i = 0; i < insideList.size(); i++ ) {
            _WeilerAthertonPiece piece = insideList.get(i);
            if ( piece.source == clipPolygon ) {
                remaining.add(piece);
                continue;
            }
            switch ( classifyDepth(clipPolygon, piece) ) {
              case IN_FRONT:
                remaining.add(piece);
                break;
              case PENETRATING:
                addFrontPart(clipPolygon, piece, remaining);
                break;
              case BEHIND:
              default:
                break;
            }
        }
        return remaining;
    }

    /**
    The difference between the depth of a piece and the depth of the clip
    polygon is an affine function over the x-y plane (both are planes in the
    viewing space), so its extremes over the piece are at its vertices.
    @return BEHIND if no part of the piece is in front of the clip polygon
    (including the coplanar case), IN_FRONT if no part is behind it, or
    PENETRATING otherwise
    */
    private static int classifyDepth(_WeilerAthertonSourcePolygon clipPolygon,
                                     _WeilerAthertonPiece piece)
    {
        boolean front = false;
        boolean behind = false;

        for ( _Polygon2DContour loop : piece.area.polygon.loops ) {
            for ( Vertex2D v : loop.vertices ) {
                double difference = piece.source.depthAt(v.x, v.y) -
                    clipPolygon.depthAt(v.x, v.y);
                if ( difference < -DEPTH_EPSILON ) {
                    front = true;
                }
                else if ( difference > DEPTH_EPSILON ) {
                    behind = true;
                }
            }
        }
        if ( front && behind ) {
            return PENETRATING;
        }
        return front ? IN_FRONT : BEHIND;
    }

    /**
    Divides a piece penetrating the clip polygon by the projection of the
    intersection of their planes, where their depths are equal, keeping
    only the part in front of the clip polygon. The half plane in front is
    taken as a convex polygon covering the piece, to which the piece is
    clipped.
    */
    private void addFrontPart(_WeilerAthertonSourcePolygon clipPolygon,
                              _WeilerAthertonPiece piece,
                              List<_WeilerAthertonPiece> remaining)
    {
        // Points with da*x + db*y + dc < 0 are in front of the clip polygon
        double da = piece.source.a - clipPolygon.a;
        double db = piece.source.b - clipPolygon.b;
        double dc = piece.source.c - clipPolygon.c;
        _WeilerAthertonArea area = piece.area;
        double margin = 1.0 + 0.01 * ((area.maxX - area.minX) +
            (area.maxY - area.minY));
        double[][] box = {
            {area.minX - margin, area.minY - margin},
            {area.maxX + margin, area.minY - margin},
            {area.maxX + margin, area.maxY + margin},
            {area.minX - margin, area.maxY + margin}
        };
        List<double[]> halfPlane = new ArrayList<double[]>();
        int i;

        // Sutherland-Hodgman clip of the (convex) box by the line
        for ( i = 0; i < box.length; i++ ) {
            double[] p = box[i];
            double[] q = box[(i + 1) % box.length];
            double fp = da * p[0] + db * p[1] + dc;
            double fq = da * q[0] + db * q[1] + dc;
            if ( fp <= 0.0 ) {
                halfPlane.add(p);
            }
            if ( (fp < 0.0 && fq > 0.0) || (fp > 0.0 && fq < 0.0) ) {
                double t = fp / (fp - fq);
                halfPlane.add(new double[] {p[0] + t * (q[0] - p[0]),
                                            p[1] + t * (q[1] - p[1])});
            }
        }
        if ( halfPlane.size() < 3 ) {
            return;
        }

        Polygon2D frontRegion = new Polygon2D();
        for ( double[] p : halfPlane ) {
            frontRegion.addVertex(p[0], p[1]);
        }
        List<_WeilerAthertonArea> front = new ArrayList<_WeilerAthertonArea>();
        List<_WeilerAthertonArea> back = new ArrayList<_WeilerAthertonArea>();
        verifiedClip(new _WeilerAthertonArea(frontRegion), area, front, back);
        for ( _WeilerAthertonArea part : front ) {
            remaining.add(new _WeilerAthertonPiece(part, piece.source));
        }
    }

    private void display(_WeilerAthertonPiece piece)
    {
        output.add(piece);
    }

    //= Verified clips ====================================================

    /**
    Clips a subject polygon to the area of a clip polygon with
    `WeilerAthertonPolygonClipper`, verifying the result (see the class
    documentation): if it is not consistent, the clip is repeated with the
    clip polygon slightly moved. If no attempt succeeds, the clip is solved
    by the half planes of the clip polygon (`clipByConvexPolygon`), or of
    the convex trapezoids of a concave one (`trapezoids`).
    @param clip clip polygon
    @param subject polygon to clip
    @param inside receives the parts of the subject inside the clip polygon
    @param outside receives the parts of the subject outside of it
    */
    private void verifiedClip(_WeilerAthertonArea clip,
                              _WeilerAthertonArea subject,
                              List<_WeilerAthertonArea> inside,
                              List<_WeilerAthertonArea> outside)
    {
        int attempt;

        for ( attempt = 0; attempt <= MAXIMUM_RETRIES; attempt++ ) {
            double shift = attempt * RETRY_SHIFT;
            // Golden angle: the directions of the moves do not repeat
            double angle = attempt * 2.399963229728653;
            Polygon2D clipPolygon = attempt == 0 ? clip.polygon :
                translated(clip.polygon, shift * Math.cos(angle),
                           shift * Math.sin(angle));
            Polygon2D insidePolygon = new Polygon2D();
            Polygon2D outsidePolygon = new Polygon2D();

            clipper.clipPolygons(clipPolygon, subject.polygon, insidePolygon,
                outsidePolygon);
            if ( clipper.isLastClipAbandoned() ) {
                continue;
            }
            Polygon2D insideClean = withoutDegenerateContours(insidePolygon);
            Polygon2D outsideClean = withoutDegenerateContours(outsidePolygon);
            if ( isConsistentClip(clip, clipPolygon, subject, insideClean,
                                  outsideClean, shift) ) {
                addWithoutSlivers(inside, insideClean);
                addWithoutSlivers(outside, outsideClean);
                return;
            }
        }

        // The clipper could not solve the contacts: a convex clip polygon
        // (the usual case) is solved exactly by half planes instead, and a
        // concave one by the half planes of its trapezoids
        if ( isConvex(clip.polygon) ) {
            clipByConvexPolygon(clip.polygon, subject, inside, outside);
            return;
        }
        List<_WeilerAthertonArea> remaining = new ArrayList<_WeilerAthertonArea>();
        remaining.add(subject);
        for ( Polygon2D trapezoid : trapezoids(clip.polygon) ) {
            List<_WeilerAthertonArea> next = new ArrayList<_WeilerAthertonArea>();
            _WeilerAthertonArea trapezoidArea = new _WeilerAthertonArea(trapezoid);
            for ( _WeilerAthertonArea area : remaining ) {
                if ( area.overlapsBox(trapezoidArea) ) {
                    clipByConvexPolygon(trapezoid, area, inside, next);
                }
                else {
                    next.add(area);
                }
            }
            remaining = next;
        }
        outside.addAll(remaining);
    }

    /**
    Decomposes a polygon (with holes, filled by the even-odd rule) in convex
    trapezoids that do not overlap: between each pair of consecutive x
    coordinates of its vertices, the edges crossing that vertical slab,
    sorted from bottom to top, bound the polygon by pairs.
    @return the trapezoids, counterclockwise and without repeated vertices
    */
    private static List<Polygon2D> trapezoids(Polygon2D polygon)
    {
        List<Polygon2D> result = new ArrayList<Polygon2D>();
        List<double[]> edges = new ArrayList<double[]>();
        List<Double> xs = new ArrayList<Double>();

        for ( _Polygon2DContour loop : polygon.loops ) {
            int n = loop.vertices.size();
            for ( int i = 0; i < n; i++ ) {
                Vertex2D p = loop.vertices.get(i);
                Vertex2D q = loop.vertices.get((i + 1) % n);
                xs.add(p.x);
                if ( p.x != q.x ) {
                    edges.add(p.x < q.x ? new double[] {p.x, p.y, q.x, q.y} :
                                          new double[] {q.x, q.y, p.x, p.y});
                }
            }
        }
        Collections.sort(xs);
        for ( int i = 0; i + 1 < xs.size(); i++ ) {
            double x0 = xs.get(i);
            double x1 = xs.get(i + 1);
            if ( x1 <= x0 ) {
                continue;
            }
            double xm = (x0 + x1) / 2.0;
            List<double[]> crossing = new ArrayList<double[]>();
            for ( double[] e : edges ) {
                if ( e[0] <= x0 && e[2] >= x1 ) {
                    double slope = (e[3] - e[1]) / (e[2] - e[0]);
                    crossing.add(new double[] {e[1] + slope * (x0 - e[0]),
                        e[1] + slope * (x1 - e[0]), e[1] + slope * (xm - e[0])});
                }
            }
            crossing.sort((a, b) -> Double.compare(a[2], b[2]));
            for ( int j = 0; j + 1 < crossing.size(); j += 2 ) {
                double[] low = crossing.get(j);
                double[] high = crossing.get(j + 1);
                List<double[]> corners = new ArrayList<double[]>();
                addDistinct(corners, x0, low[0]);
                addDistinct(corners, x1, low[1]);
                addDistinct(corners, x1, high[1]);
                addDistinct(corners, x0, high[0]);
                if ( corners.size() >= 3 &&
                     Math.abs(signedArea(corners)) > AREA_EPSILON ) {
                    Polygon2D trapezoid = new Polygon2D();
                    trapezoid.loops.clear();
                    addLoop(trapezoid, corners);
                    result.add(trapezoid);
                }
            }
        }
        return result;
    }

    private static void addDistinct(List<double[]> points, double x, double y)
    {
        if ( !points.isEmpty() ) {
            double[] last = points.get(points.size() - 1);
            double[] first = points.get(0);
            if ( Math.hypot(last[0] - x, last[1] - y) <= MINIMUM_EDGE_LENGTH ||
                 Math.hypot(first[0] - x, first[1] - y) <= MINIMUM_EDGE_LENGTH ) {
                return;
            }
        }
        points.add(new double[] {x, y});
    }

    /**
    Clips a polygon to a convex one, by the half planes of its edges
    (Sutherland-Hodgman, [FOLE1992].3.14.1, for each contour): the inside part
    is the subject within all the half planes, and the outside part is the
    union of the disjoint parts outside of each edge and inside of the
    previous ones. The parts may have degenerate edges along the clip edges,
    harmless for their areas.
    @param clip a convex counterclockwise polygon (see `isConvex`)
    */
    private static void clipByConvexPolygon(Polygon2D clip,
                                            _WeilerAthertonArea subject,
                                            List<_WeilerAthertonArea> inside,
                                            List<_WeilerAthertonArea> outside)
    {
        List<Vertex2D> vertices = clip.loops.get(0).vertices;
        Polygon2D remaining = subject.polygon;
        int n = vertices.size();

        for ( int i = 0; i < n && remaining != null; i++ ) {
            Vertex2D p = vertices.get(i);
            Vertex2D q = vertices.get((i + 1) % n);
            // An edge of (almost) no length gives no direction: its half
            // plane would cut at random
            if ( Math.hypot(q.x - p.x, q.y - p.y) <= MINIMUM_EDGE_LENGTH ) {
                continue;
            }
            // Inside is at the left of the edge: a*x + b*y + c >= 0
            double a = -(q.y - p.y);
            double b = q.x - p.x;
            double c = -(a * p.x + b * p.y);
            Polygon2D out = clipByHalfPlane(remaining, -a, -b, -c);
            if ( out != null ) {
                addWithoutSlivers(outside, out);
            }
            remaining = clipByHalfPlane(remaining, a, b, c);
        }
        if ( remaining != null ) {
            addWithoutSlivers(inside, remaining);
        }
    }

    /**
    @return the part of a polygon where a*x + b*y + c >= 0 (each contour
    clipped by Sutherland-Hodgman), or null if nothing is left
    */
    private static Polygon2D clipByHalfPlane(Polygon2D polygon, double a,
                                             double b, double c)
    {
        Polygon2D result = new Polygon2D();
        result.loops.clear();
        for ( _Polygon2DContour loop : polygon.loops ) {
            List<double[]> clipped = new ArrayList<double[]>();
            int n = loop.vertices.size();
            for ( int i = 0; i < n; i++ ) {
                Vertex2D p = loop.vertices.get(i);
                Vertex2D q = loop.vertices.get((i + 1) % n);
                double fp = a * p.x + b * p.y + c;
                double fq = a * q.x + b * q.y + c;
                if ( fp >= 0.0 ) {
                    clipped.add(new double[] {p.x, p.y});
                }
                if ( (fp >= 0.0) != (fq >= 0.0) ) {
                    double t = fp / (fp - fq);
                    clipped.add(new double[] {p.x + t * (q.x - p.x),
                                              p.y + t * (q.y - p.y)});
                }
            }
            if ( clipped.size() >= 3 ) {
                addLoop(result, clipped);
            }
        }
        return withoutDegenerateContours(result);
    }

    /**
    @return true if the polygon has one contour, counterclockwise and convex
    */
    private static boolean isConvex(Polygon2D polygon)
    {
        if ( polygon.loops.size() != 1 ) {
            return false;
        }
        List<Vertex2D> vertices = polygon.loops.get(0).vertices;
        int n = vertices.size();
        if ( n < 3 || signedArea(polygon.loops.get(0)) <= 0.0 ) {
            return false;
        }
        for ( int i = 0; i < n; i++ ) {
            Vertex2D p = vertices.get(i);
            Vertex2D q = vertices.get((i + 1) % n);
            Vertex2D r = vertices.get((i + 2) % n);
            double cross = (q.x - p.x) * (r.y - q.y) - (q.y - p.y) * (r.x - q.x);
            double scale = Math.hypot(q.x - p.x, q.y - p.y) *
                Math.hypot(r.x - q.x, r.y - q.y);
            if ( cross < -1.0e-9 * scale ) {
                return false;
            }
        }
        return true;
    }

    /**
    @return true if the inside and outside parts of a clip add up to the
    subject and are within the expected bounds
    */
    private static boolean isConsistentClip(_WeilerAthertonArea clip,
                                            Polygon2D clipPolygon,
                                            _WeilerAthertonArea subject,
                                            Polygon2D inside,
                                            Polygon2D outside,
                                            double shift)
    {
        double insideArea = inside != null ? enclosedArea(inside) : 0.0;
        double outsideArea = outside != null ? enclosedArea(outside) : 0.0;
        double length = shift + SLIVER_WIDTH;
        double tolerance = 1.0e-7 * subject.area + AREA_EPSILON +
            4.0 * length * (subject.perimeter + 2.0 *
                ((subject.maxX - subject.minX) + (subject.maxY - subject.minY)));

        if ( insideArea < -tolerance || outsideArea < -tolerance ) {
            return false;
        }
        if ( Math.abs(insideArea + outsideArea - subject.area) > tolerance ) {
            return false;
        }
        if ( insideArea > Math.min(subject.area, clip.area) + tolerance ) {
            return false;
        }
        if ( inside != null ) {
            double[] box = boundingBox(inside);
            double slack = 2.0 * length;
            if ( box[0] < Math.max(subject.minX, clip.minX) - slack ||
                 box[1] < Math.max(subject.minY, clip.minY) - slack ||
                 box[2] > Math.min(subject.maxX, clip.maxX) + slack ||
                 box[3] > Math.min(subject.maxY, clip.maxY) + slack ) {
                return false;
            }
        }
        if ( outside != null ) {
            double[] box = boundingBox(outside);
            double slack = 2.0 * length;
            if ( box[0] < subject.minX - slack || box[1] < subject.minY - slack ||
                 box[2] > subject.maxX + slack || box[3] > subject.maxY + slack ) {
                return false;
            }
        }
        // At sample points of the four polygons, far from all their
        // borders, a point is in the inside part if and only if it is in the
        // subject and in the clip polygon, and it is in the outside part if
        // and only if it is in the subject and not in the clip polygon. It
        // fails when the clipper misses contacts of the contours: i.e. a
        // piece inscribed in the clip polygon (all its vertices on its
        // border), or a contour of the subject identical to the clip polygon
        double slack = 2.0 * length;
        List<double[]> samples = samplePoints(clipPolygon);
        samples.addAll(samplePoints(subject.polygon));
        if ( inside != null ) {
            samples.addAll(samplePoints(inside));
        }
        if ( outside != null ) {
            samples.addAll(samplePoints(outside));
        }
        _WeilerAthertonEdgeGrid clipBorder =
            new _WeilerAthertonEdgeGrid(clipPolygon, slack);
        _WeilerAthertonEdgeGrid subjectBorder =
            new _WeilerAthertonEdgeGrid(subject.polygon, slack);
        _WeilerAthertonEdgeGrid insideBorder = inside != null ?
            new _WeilerAthertonEdgeGrid(inside, slack) : null;
        _WeilerAthertonEdgeGrid outsideBorder = outside != null ?
            new _WeilerAthertonEdgeGrid(outside, slack) : null;
        for ( double[] point : samples ) {
            double x = point[0];
            double y = point[1];
            if ( clipBorder.isNearBorder(x, y) ||
                 subjectBorder.isNearBorder(x, y) ) {
                continue;
            }
            boolean inSubject = contains(subject.polygon, x, y);
            boolean inClip = contains(clipPolygon, x, y);
            if ( !isOnExpectedSide(inside, insideBorder, x, y,
                                   inSubject && inClip) ||
                 !isOnExpectedSide(outside, outsideBorder, x, y,
                                   inSubject && !inClip) ) {
                return false;
            }
        }
        return true;
    }

    /**
    @param part a part of the clip result (null if empty)
    @param border index of the edges of the part
    @param expected true if the point must be in the part
    @return true if the point is in the part as expected, or it is too close
    to its border to tell
    */
    private static boolean isOnExpectedSide(Polygon2D part,
                                            _WeilerAthertonEdgeGrid border,
                                            double x, double y,
                                            boolean expected)
    {
        if ( part == null ) {
            return !expected;
        }
        return contains(part, x, y) == expected || border.isNearBorder(x, y);
    }

    /**
    @return sample points of a polygon, as {x, y} pairs: its vertices, the
    middle points of its edges, and the centroids of the triangles at its
    vertices (inside the polygon at its ears)
    */
    private static List<double[]> samplePoints(Polygon2D polygon)
    {
        List<double[]> points = new ArrayList<double[]>();
        for ( _Polygon2DContour loop : polygon.loops ) {
            int n = loop.vertices.size();
            for ( int i = 0; i < n; i++ ) {
                Vertex2D p = loop.vertices.get((i + n - 1) % n);
                Vertex2D q = loop.vertices.get(i);
                Vertex2D r = loop.vertices.get((i + 1) % n);
                points.add(new double[] {q.x, q.y});
                points.add(new double[] {(q.x + r.x) / 2.0, (q.y + r.y) / 2.0});
                points.add(new double[] {(p.x + q.x + r.x) / 3.0,
                                         (p.y + q.y + r.y) / 3.0});
            }
        }
        return points;
    }

    /**
    Adds the parts of a clip result to a list, discarding the contours that
    are degenerate strips (see `SLIVER_WIDTH`).
    */
    private static void addWithoutSlivers(List<_WeilerAthertonArea> list,
                                          Polygon2D polygon)
    {
        if ( polygon == null ) {
            return;
        }
        Polygon2D clean = new Polygon2D();
        boolean empty = true;
        clean.loops.clear();
        for ( _Polygon2DContour loop : polygon.loops ) {
            double area = Math.abs(signedArea(loop));
            double halfPerimeter = perimeter(loop) / 2.0;
            if ( halfPerimeter <= 0.0 || area / halfPerimeter < SLIVER_WIDTH ) {
                continue;
            }
            copyLoop(clean, loop);
            empty = false;
        }
        if ( !empty ) {
            _WeilerAthertonArea area = new _WeilerAthertonArea(clean);
            if ( area.area > AREA_EPSILON ) {
                list.add(area);
            }
        }
    }

    //= Input polygons ====================================================

    /**
    Transforms the faces of the solids to the viewing space, removing the
    faces facing away from the observer and clipping them to the view.
    */
    private List<_WeilerAthertonSourcePolygon> buildInputPolygons(
        List<SimpleBody> solids, Camera camera)
    {
        List<_WeilerAthertonSourcePolygon> sources =
            new ArrayList<_WeilerAthertonSourcePolygon>();
        Matrix4x4d projection = camera.calculateProjectionMatrix();
        double depthSign = depthSign(camera, projection);
        Polygon2D windowPolygon = new Polygon2D();
        int i;

        windowPolygon.addVertex(-SCALE, -SCALE);
        windowPolygon.addVertex(SCALE, -SCALE);
        windowPolygon.addVertex(SCALE, SCALE);
        windowPolygon.addVertex(-SCALE, SCALE);
        _WeilerAthertonArea window = new _WeilerAthertonArea(windowPolygon);

        for ( SimpleBody body : solids ) {
            Geometry geometry = body.getGeometry();
            if ( !(geometry instanceof PolyhedralBoundedSolid brep) ) {
                continue;
            }
            Matrix4x4d transform = body.getTransformationMatrix();
            for ( i = 0; i < brep.getPolygonsList().size(); i++ ) {
                _PolyhedralBoundedSolidFace face = brep.getPolygonsList().get(i);
                _WeilerAthertonSourcePolygon source = buildInputPolygon(face,
                    body, transform, camera, projection, depthSign, window);
                if ( source != null ) {
                    sources.add(source);
                }
            }
        }
        return sources;
    }

    private _WeilerAthertonSourcePolygon buildInputPolygon(
        _PolyhedralBoundedSolidFace face, SimpleBody body,
        Matrix4x4d transform, Camera camera, Matrix4x4d projection,
        double depthSign, _WeilerAthertonArea window)
    {
        InfinitePlane worldPlane = worldContainingPlane(face, body, transform);
        if ( worldPlane == null || !isFrontFace(worldPlane, camera) ) {
            return null;
        }

        // Projection of each loop, clipped in homogeneous coordinates against
        // the near and far planes
        List<List<double[]>> loops = new ArrayList<List<double[]>>();
        boolean complete = true;
        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            List<Vector4Dd> clipLoop = new ArrayList<Vector4Dd>();
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge heStart = he;
            do {
                if ( he == null || he.startingVertex == null ) {
                    break;
                }
                clipLoop.add(projection.multiply(new Vector4Dd(
                    transform.multiply(he.startingVertex.position))));
                he = he.next();
            } while ( he != heStart );

            for ( Vector4Dd p : clipLoop ) {
                if ( p.z() + p.w() < 0.0 || p.w() - p.z() < 0.0 ) {
                    complete = false;
                }
            }
            clipLoop = clipAgainstPlane(clipLoop, 1.0);
            clipLoop = clipAgainstPlane(clipLoop, -1.0);
            if ( clipLoop.size() < 3 ) {
                complete = false;
                continue;
            }
            List<double[]> projectedLoop = new ArrayList<double[]>();
            for ( Vector4Dd p : clipLoop ) {
                Vector4Dd ndc = p.dividedByW();
                projectedLoop.add(new double[] {SCALE * ndc.x(), SCALE * ndc.y(),
                                                depthSign * ndc.z()});
            }
            if ( Math.abs(signedArea(projectedLoop)) > AREA_EPSILON ) {
                loops.add(projectedLoop);
            }
        }
        if ( loops.isEmpty() ) {
            return null;
        }

        // The polygon clipper follows main contours counterclockwise and
        // holes clockwise: the main contour is the one of largest area
        int outer = 0;
        for ( int i = 1; i < loops.size(); i++ ) {
            if ( Math.abs(signedArea(loops.get(i))) >
                 Math.abs(signedArea(loops.get(outer))) ) {
                outer = i;
            }
        }
        for ( int i = 0; i < loops.size(); i++ ) {
            double area = signedArea(loops.get(i));
            if ( (i == outer && area < 0.0) || (i != outer && area > 0.0) ) {
                Collections.reverse(loops.get(i));
            }
        }

        _WeilerAthertonSourcePolygon source = new _WeilerAthertonSourcePolygon();
        if ( !fitDepthPlane(loops.get(outer), source) ) {
            // Seen edge-on
            return null;
        }

        Polygon2D projected = new Polygon2D();
        projected.loops.clear();
        addLoop(projected, loops.get(outer));
        for ( int i = 0; i < loops.size(); i++ ) {
            if ( i != outer ) {
                addLoop(projected, loops.get(i));
            }
        }

        // Clip to the window of the view
        _WeilerAthertonArea template = new _WeilerAthertonArea(projected);
        if ( !template.overlapsBox(window) ) {
            return null;
        }
        if ( template.minX < window.minX || template.maxX > window.maxX ||
             template.minY < window.minY || template.maxY > window.maxY ) {
            List<_WeilerAthertonArea> inside = new ArrayList<_WeilerAthertonArea>();
            List<_WeilerAthertonArea> outside = new ArrayList<_WeilerAthertonArea>();
            verifiedClip(window, template, inside, outside);
            if ( inside.isEmpty() ) {
                return null;
            }
            template = merged(inside);
            complete = false;
        }

        source.template = template;
        source.nearestDepth = Double.MAX_VALUE;
        for ( _Polygon2DContour loop : template.polygon.loops ) {
            for ( Vertex2D v : loop.vertices ) {
                source.nearestDepth = Math.min(source.nearestDepth,
                    source.depthAt(v.x, v.y));
            }
        }
        source.body = body;
        source.face = face;
        source.worldPlane = worldPlane;
        source.complete = complete;
        return source;
    }

    /**
    Sutherland-Hodgman clip of a loop in homogeneous coordinates against the
    near (side = 1: z >= -w) or far (side = -1: z <= w) plane.
    */
    private static List<Vector4Dd> clipAgainstPlane(List<Vector4Dd> loop,
                                                    double side)
    {
        List<Vector4Dd> result = new ArrayList<Vector4Dd>();
        int n = loop.size();
        int i;

        for ( i = 0; i < n; i++ ) {
            Vector4Dd p = loop.get(i);
            Vector4Dd q = loop.get((i + 1) % n);
            double dp = side * p.z() + p.w();
            double dq = side * q.z() + q.w();
            if ( dp >= 0.0 ) {
                result.add(p);
            }
            if ( (dp >= 0.0) != (dq >= 0.0) ) {
                double t = dp / (dp - dq);
                result.add(p.multiply(1.0 - t).add(q.multiply(t)));
            }
        }
        return result;
    }

    /**
    Fits the depth plane of a projected loop with Newell's method (robust for
    concave loops), as depth = a*x + b*y + c.
    @return false if the plane is parallel to the line of sight
    */
    private static boolean fitDepthPlane(List<double[]> loop,
                                         _WeilerAthertonSourcePolygon source)
    {
        double nx = 0.0;
        double ny = 0.0;
        double nz = 0.0;
        double cx = 0.0;
        double cy = 0.0;
        double cz = 0.0;
        int n = loop.size();
        int i;

        for ( i = 0; i < n; i++ ) {
            double[] p = loop.get(i);
            double[] q = loop.get((i + 1) % n);
            nx += (p[1] - q[1]) * (p[2] + q[2]);
            ny += (p[2] - q[2]) * (p[0] + q[0]);
            nz += (p[0] - q[0]) * (p[1] + q[1]);
            cx += p[0];
            cy += p[1];
            cz += p[2];
        }
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if ( length <= VSDK.EPSILON * VSDK.EPSILON ||
             Math.abs(nz) <= 1.0e-12 * length ) {
            return false;
        }
        cx /= n;
        cy /= n;
        cz /= n;
        source.a = -nx / nz;
        source.b = -ny / nz;
        source.c = cz - source.a * cx - source.b * cy;
        return true;
    }

    /**
    @return 1 if the depth of the normalized device coordinates grows away
    from the observer, -1 otherwise, so the depths used by the algorithm
    are always smaller closer to the observer
    */
    private static double depthSign(Camera camera, Matrix4x4d projection)
    {
        Vector3Dd front = camera.getFront().normalized();
        double near = camera.getNearPlaneDistance();
        double far = camera.getFarPlaneDistance();
        Vector3Dd p0 = camera.getPosition().add(front.multiply(
            near + 0.25 * (far - near)));
        Vector3Dd p1 = camera.getPosition().add(front.multiply(
            near + 0.75 * (far - near)));
        double z0 = projection.multiply(new Vector4Dd(p0)).dividedByW().z();
        double z1 = projection.multiply(new Vector4Dd(p1)).dividedByW().z();
        return z1 >= z0 ? 1.0 : -1.0;
    }

    /**
    Backplane removal: a face of a closed solid is seen only from the side
    its outward normal points to.
    */
    private static boolean isFrontFace(InfinitePlane worldPlane, Camera camera)
    {
        Vector3Dd n = worldPlane.getNormal();
        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            return n.dotProduct(camera.getFront()) < 0.0;
        }
        // Signed distance from the plane to the eye
        double distance = n.x() * camera.getPosition().x() +
            n.y() * camera.getPosition().y() +
            n.z() * camera.getPosition().z() + worldPlane.getD();
        return distance > 0.0;
    }

    /**
    @return the plane of a face in world coordinates, with its outward
    normal (from the maintained plane of the face, a Newell fit, not from
    three vertices that could be at a reflex corner), or null if undefined
    */
    private static InfinitePlane worldContainingPlane(
        _PolyhedralBoundedSolidFace face, SimpleBody body, Matrix4x4d transform)
    {
        InfinitePlane localPlane = face.getContainingPlane();
        Vector3Dd pointOnFace = null;

        if ( localPlane == null ) {
            return null;
        }
        for ( int i = 0; i < face.boundariesList.size() && pointOnFace == null;
              i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge he =
                loop != null ? loop.boundaryStartHalfEdge : null;
            if ( he != null && he.startingVertex != null ) {
                pointOnFace = he.startingVertex.position;
            }
        }
        if ( pointOnFace == null ) {
            return null;
        }
        Vector3Dd worldNormal = body.transformNormalToWorld(localPlane.getNormal());
        if ( worldNormal.length() <= VSDK.EPSILON ) {
            return null;
        }
        return new InfinitePlane(worldNormal.normalized(),
            transform.multiply(pointOnFace));
    }

    //= 2D polygon utilities ==============================================

    private static void addLoop(Polygon2D polygon, List<double[]> loop)
    {
        polygon.nextLoop();
        for ( double[] p : loop ) {
            polygon.addVertex(p[0], p[1]);
        }
    }

    private static void copyLoop(Polygon2D polygon, _Polygon2DContour loop)
    {
        polygon.nextLoop();
        for ( Vertex2D v : loop.vertices ) {
            polygon.addVertex(v.x, v.y);
        }
    }

    private static Polygon2D translated(Polygon2D polygon, double dx, double dy)
    {
        Polygon2D result = new Polygon2D();
        result.loops.clear();
        for ( _Polygon2DContour loop : polygon.loops ) {
            result.nextLoop();
            for ( Vertex2D v : loop.vertices ) {
                result.addVertex(v.x + dx, v.y + dy);
            }
        }
        return result;
    }

    /**
    @return one area with all the contours of several areas (that do not
    overlap)
    */
    private static _WeilerAthertonArea merged(List<_WeilerAthertonArea> areas)
    {
        if ( areas.size() == 1 ) {
            return areas.get(0);
        }
        Polygon2D result = new Polygon2D();
        result.loops.clear();
        for ( _WeilerAthertonArea area : areas ) {
            for ( _Polygon2DContour loop : area.polygon.loops ) {
                copyLoop(result, loop);
            }
        }
        return new _WeilerAthertonArea(result);
    }

    private static double signedArea(List<double[]> loop)
    {
        double area = 0.0;
        int n = loop.size();
        for ( int i = 0; i < n; i++ ) {
            double[] p = loop.get(i);
            double[] q = loop.get((i + 1) % n);
            area += p[0] * q[1] - q[0] * p[1];
        }
        return area / 2.0;
    }

    static double signedArea(_Polygon2DContour loop)
    {
        double area = 0.0;
        int n = loop.vertices.size();
        for ( int i = 0; i < n; i++ ) {
            Vertex2D p = loop.vertices.get(i);
            Vertex2D q = loop.vertices.get((i + 1) % n);
            area += p.x * q.y - q.x * p.y;
        }
        return area / 2.0;
    }

    static double perimeter(_Polygon2DContour loop)
    {
        double length = 0.0;
        int n = loop.vertices.size();
        for ( int i = 0; i < n; i++ ) {
            Vertex2D p = loop.vertices.get(i);
            Vertex2D q = loop.vertices.get((i + 1) % n);
            length += Math.hypot(q.x - p.x, q.y - p.y);
        }
        return length;
    }

    static double perimeter(Polygon2D polygon)
    {
        double length = 0.0;
        for ( _Polygon2DContour loop : polygon.loops ) {
            length += perimeter(loop);
        }
        return length;
    }

    /**
    @return the area enclosed by the contours of a polygon: its main contours
    are counterclockwise (positive area) and its holes clockwise (negative
    area), as the clipper keeps them
    */
    static double enclosedArea(Polygon2D polygon)
    {
        double area = 0.0;
        for ( _Polygon2DContour loop : polygon.loops ) {
            area += signedArea(loop);
        }
        return area;
    }

    /**
    @return a copy of a clip result without its degenerate contours (with
    less than three vertices or with no area), or null if no contour is left
    */
    static Polygon2D withoutDegenerateContours(Polygon2D polygon)
    {
        Polygon2D clean = new Polygon2D();
        boolean empty = true;

        clean.loops.clear();
        for ( _Polygon2DContour loop : polygon.loops ) {
            if ( loop.vertices.size() < 3 ||
                 Math.abs(signedArea(loop)) <= AREA_EPSILON ) {
                continue;
            }
            copyLoop(clean, loop);
            empty = false;
        }
        return empty ? null : clean;
    }

    /**
    @return true if the point is inside the polygon, for the even-odd rule
    */
    static boolean contains(Polygon2D polygon, double x, double y)
    {
        boolean inside = false;
        for ( _Polygon2DContour loop : polygon.loops ) {
            if ( loop.vertices.size() >= 3 && containsInLoop(loop, x, y) ) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static boolean containsInLoop(_Polygon2DContour loop, double x,
                                          double y)
    {
        boolean inside = false;
        int n = loop.vertices.size();
        for ( int i = 0, j = n - 1; i < n; j = i++ ) {
            Vertex2D a = loop.vertices.get(i);
            Vertex2D b = loop.vertices.get(j);
            if ( (a.y > y) != (b.y > y) &&
                 x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x ) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
    @return {minX, minY, maxX, maxY} of the vertices of a polygon
    */
    static double[] boundingBox(Polygon2D polygon)
    {
        double[] box = {Double.MAX_VALUE, Double.MAX_VALUE,
                        -Double.MAX_VALUE, -Double.MAX_VALUE};
        for ( _Polygon2DContour loop : polygon.loops ) {
            for ( Vertex2D v : loop.vertices ) {
                box[0] = Math.min(box[0], v.x);
                box[1] = Math.min(box[1], v.y);
                box[2] = Math.max(box[2], v.x);
                box[3] = Math.max(box[3], v.y);
            }
        }
        return box;
    }
}
