//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisivility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          meeting 1967.                                                  =

package vsdk.toolkit.render.hiddenLine;

// Java classes
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.Intersection;
import vsdk.toolkit.environment.geometry.element.Triangle;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.Volume;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.media.Calligraphic2DBuffer;
import vsdk.toolkit.render.RenderingElement;

class _AppelEdgeSegment extends RenderingElement implements Comparable <_AppelEdgeSegment>
{
    /// Distance from start to end with respect to line parameter
    public double t;
    /// Change in quantitative invisibility when the edge crosses this boundary,
    /// computed at detection from Appel's image-space side rule. Coincident
    /// crossings are merged by SUMMING their deltaQI so none is lost. 0 for the
    /// synthetic t=0 / t=1 bounds.
    public int deltaQI;

    @Override
    public int compareTo(_AppelEdgeSegment other)
    {
        if ( this.t < other.t - VSDK.EPSILON ) return -1;
        else if ( this.t > other.t + VSDK.EPSILON ) return 1;
        return 0;
    }
}

class _AppelEdgeCache extends RenderingElement
{
    public static final int HIDDEN_LINE = 0;
    public static final int VISIBLE_LINE = 1;
    public static final int CONTOUR_LINE = 2;

    public int edgeType;
    /// True if current line starts on the end of a previous one in the same
    /// solid and with the same quantitative invisibility. When this happens,
    /// quantitative invisibility can be acumulated in the edge sequence,
    /// otherwise must be calculated.
    boolean onSequence;
    public Vector3Dd start;
    public Vector3Dd end;
    /// d = start - end
    public Vector3Dd d;
    public SimpleBody ownerBody;
    /// `visibleEdgeForContourLine` contains an explicit reference to the
    /// planar surface marked as "S" on figure [APPE1967].5.
    public _PolyhedralBoundedSolidFace visibleEdgeForContourLine;
    public SimpleBody visibleEdgeBody;
    /// World point a small step INTO the visible face S from the contour edge,
    /// along the in-plane inward normal (cross(outwardNormal, edgeDir) in loop
    /// winding). It marks which side of the contour line S lies on, robustly for
    /// non-convex faces (a local step, unlike the face centroid which can fall on
    /// the wrong side of the edge for a concave face). Used by the image-space
    /// deltaQI side rule.
    public Vector3Dd visibleFaceInteriorRef;
    public _PolyhedralBoundedSolidFace leftFace;
    public _PolyhedralBoundedSolidFace rightFace;
    public int edgeIndex;

    public void setStart(Vector3Dd s)
    {
        start = new Vector3Dd(s);
    }

    public void setEnd(Vector3Dd e)
    {
        end = new Vector3Dd(e);
    }
}

/**
This class implements the Appel's algorithm for hidden line rendering. :)
*/
public class HiddenLineRenderer extends RenderingElement
{
    /// Diagnostic: when >= 0, processLineToBeDrawn logs the contour-crossing /
    /// segment-splitting decisions for the cached edge with this edgeIndex.
    public static int DEBUG_EDGE_INDEX = -1;

    private static void debugSplit(int edgeIndex, String message)
    {
        if ( edgeIndex == DEBUG_EDGE_INDEX ) {
            System.out.println("[AppelSplit e" + edgeIndex + "] " + message);
        }
    }

    public static final class AppelAlgorithmDump extends RenderingElement
    {
        public final ArrayList<AppelEdgeDump> edges = new ArrayList<AppelEdgeDump>();
    }

    public static final class AppelEdgeDump extends RenderingElement
    {
        public int edgeIndex;
        public int edgeType;
        public String edgeTypeName;
        public int face1Id;
        public int face2Id;
        public Vector3Dd start;
        public Vector3Dd end;
        public int initialQuantitativeInvisibility;
        public final ArrayList<AppelSegmentDump> segments =
            new ArrayList<AppelSegmentDump>();
        public final ArrayList<AppelEventDump> events =
            new ArrayList<AppelEventDump>();
    }

    public static final class AppelSegmentDump extends RenderingElement
    {
        public double tStart;
        public double tEnd;
        public Vector3Dd start;
        public Vector3Dd end;
        public Vector3Dd midpoint;
        public int midpointQuantitativeInvisibility;
        public final ArrayList<Integer> midpointContributorFaceIds =
            new ArrayList<Integer>();
        public String classification;
    }

    public static final class AppelEventDump extends RenderingElement
    {
        public double t;
        public int deltaQI;
        public int contourEdgeIndex;
        public int visibleFaceId;
    }

    public static int isFaceVisibleFromCamera(
        _PolyhedralBoundedSolidFace face,
        Camera camera)
    {
        Vector3Dd iv = new Vector3Dd(1, 0, 0);
        Vector3Dd viewingVector = camera.getRotation().multiply(iv);
        Vector3Dd n = face.getContainingPlane().getNormal().normalized();
        double dot;

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            viewingVector = viewingVector.normalized();
            dot = n.dotProduct(viewingVector);
            if ( dot > VSDK.EPSILON ) {
                return -1;
            }
            else if ( dot < -VSDK.EPSILON ) {
                return 1;
            }
            else {
                return 0;
            }
        }

        Vector3Dd cameraPosition = camera.getPosition();
        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge heStart = he;

            do {
                he = he.next();
                if ( he == null ) {
                    // Loop is not closed.
                    break;
                }
                Vector3Dd p = he.startingVertex.position;
                Vector3Dd t = p.subtract(cameraPosition).multiply(-1).normalized();
                if ( t.dotProduct(n) > 0.0 ) {
                    return 1;
                }
            } while ( he != heStart );
        }
        return -1;
    }

    private static Vector3Dd transformToWorld(SimpleBody body, Vector3Dd localPoint)
    {
        if ( body == null ) {
            return localPoint;
        }
        return body.getTransformationMatrix().multiply(localPoint);
    }

    private static Vector3Dd transformToLocal(SimpleBody body, Vector3Dd worldPoint)
    {
        if ( body == null ) {
            return worldPoint;
        }

        Vector3Dd translatedPoint = worldPoint.subtract(body.getPosition());
        Vector3Dd rotatedPoint = body.getRotationInverse().multiply(translatedPoint);
        Vector3Dd scale = body.getScale();

        return new Vector3Dd(
            Math.abs(scale.x()) > VSDK.EPSILON ? rotatedPoint.x() / scale.x() : 0.0,
            Math.abs(scale.y()) > VSDK.EPSILON ? rotatedPoint.y() / scale.y() : 0.0,
            Math.abs(scale.z()) > VSDK.EPSILON ? rotatedPoint.z() / scale.z() : 0.0);
    }

    /**
    World-space containing plane of a face, with a reliably OUTWARD normal.

    The orientation is taken from the face's maintained plane
    (`getContainingPlane()`, a Newell fit over the whole boundary loop), not
    from the cross product of the first three loop vertices: the latter flips to
    an inward normal at a reflex (concave) corner, which mislabels front/back
    faces and the occluding face of a contour. The outward object-space normal
    is mapped to world space with the body's normal transform (orientation
    preserving), and an arbitrary loop vertex provides the in-plane point.

    @param face face whose world plane is requested
    @param body body the face belongs to (null = identity transform)
    @return the world-space plane with an outward normal, or null if undefined
    */
    private static InfinitePlane getWorldContainingPlane(
        _PolyhedralBoundedSolidFace face,
        SimpleBody body)
    {
        if ( face == null ) {
            return null;
        }

        InfinitePlane localPlane = face.getContainingPlane();
        if ( localPlane == null ) {
            return null;
        }

        Vector3Dd pointOnFace = null;
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

        Vector3Dd worldPoint = transformToWorld(body, pointOnFace);
        Vector3Dd worldNormal = body == null ?
            localPlane.getNormal() :
            body.transformNormalToWorld(localPlane.getNormal());
        if ( worldNormal.length() <= VSDK.EPSILON ) {
            return null;
        }
        return new InfinitePlane(worldNormal.normalized(), worldPoint);
    }

    private static int isFaceVisibleFromCameraTransformed(
        _PolyhedralBoundedSolidFace face,
        SimpleBody body,
        Camera camera)
    {
        if ( face == null || camera == null ) {
            return 0;
        }

        Vector3Dd iv = new Vector3Dd(1, 0, 0);
        Vector3Dd viewingVector = camera.getRotation().multiply(iv);
        InfinitePlane plane = getWorldContainingPlane(face, body);
        if ( plane == null ) {
            return 0;
        }
        Vector3Dd n = plane.getNormal().normalized();

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            viewingVector = viewingVector.normalized();
            double dot = n.dotProduct(viewingVector);
            if ( dot > VSDK.EPSILON ) {
                return -1;
            }
            else if ( dot < -VSDK.EPSILON ) {
                return 1;
            }
            return 0;
        }

        Vector3Dd cameraPosition = camera.getPosition();
        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge heStart = he;
            do {
                he = he.next();
                if ( he == null || he.startingVertex == null ) {
                    break;
                }
                Vector3Dd p = transformToWorld(body, he.startingVertex.position);
                Vector3Dd t = p.subtract(cameraPosition).multiply(-1).normalized();
                if ( t.dotProduct(n) > 0.0 ) {
                    return 1;
                }
            } while ( he != heStart );
        }
        return -1;
    }

    /**
    Given a set of solids, this method computes the "edge cache": a list
    of edges, where every edge gets one of three classifications:
    HIDDEN_LINE, VISIBLE_LINE or CONTOUR_LINE.
    Edges are classified according to the visibility of its participating
    faces. Note that current implementation supposes that every given
    edge is shared by exactly two planar surfaces, and that no pair of
    edges intersects. This assumption is implied here by first converting
    the solid to a polyhedral bounded representation (BREP). As VitralSDK
    BREP ensures that assumptions, current implementation is solid with
    that data structure.

    Original [APPE1967] paper makes no assumption on data representation as
    long that the representation is able to answer the question of what pair
    or surfaces share an edge, so this Vitral SDK implementation is more
    restrictive that the one of the original paper, but it is expected to me
    also more robust.
    */
    private static void buildCache(ArrayList <SimpleBody> solids,
                                   SimpleBody body, 
                                   ArrayList <_AppelEdgeCache> cache,
                                   ArrayList <_AppelEdgeCache> contourCache,
                                   Camera camera)
    {
        Geometry g = body.getGeometry();

        if ( g == null ) {
            return;
        }

        if ( !(g instanceof Volume) ) {
            return;
        }
        g = ((Volume)g).exportToPolyhedralBoundedSolid();
        if ( g == null ) {
            return;
        }

        solids.add(body);
        PolyhedralBoundedSolid solid = (PolyhedralBoundedSolid)g;

        int i;
        long l = 0;

        _PolyhedralBoundedSolidFace face1;
        _PolyhedralBoundedSolidFace face2;
        boolean f1, f2;
        _AppelEdgeCache materialLine;
        Vector3Dd prevEnd = new Vector3Dd();

        for ( i = 0; i < solid.getEdgesList().size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = solid.getEdgesList().get(i);

            int start, end;
            start = e.getStartingVertexId();
            end = e.getEndingVertexId();
            if ( start >= 0 && end >= 0 ) {
                Vector3Dd startPosition;
                Vector3Dd endPosition;

                startPosition = e.leftHalf.startingVertex.position;
                endPosition = e.rightHalf.startingVertex.position;
                if ( startPosition != null && endPosition != null ) {
                    //--------------------------------------------------------
                    face1 = e.leftHalf.parentLoop.parentFace;
                    face2 = e.rightHalf.parentLoop.parentFace;
                    f1 = isFaceVisibleFromCameraTransformed(face1, body, camera) >= 0;
                    f2 = isFaceVisibleFromCameraTransformed(face2, body, camera) >= 0;

                    //--------------------------------------------------------
                    materialLine = new _AppelEdgeCache();
                    materialLine.setStart(transformToWorld(body, startPosition));
                    materialLine.setEnd(transformToWorld(body, endPosition));
                    materialLine.d = materialLine.end.subtract(materialLine.start);
                    materialLine.ownerBody = body;
                    materialLine.leftFace = face1;
                    materialLine.rightFace = face2;
                    materialLine.edgeIndex = i;
                    if ( l > 0 &&
                         Vector3Dd.distance(prevEnd, materialLine.start) <
                             VSDK.EPSILON ) {
                        materialLine.onSequence = true;
                    }
                    else {
                        materialLine.onSequence = false;
                    }
                    if ( !f1 && !f2 ) {
                        // Totally hidden lines
                        materialLine.edgeType = _AppelEdgeCache.HIDDEN_LINE;
                    }
                    else if ( f1 && !f2 || !f1 && f2 ) {
                        // Contour lines
                        materialLine.edgeType = _AppelEdgeCache.CONTOUR_LINE;
                        _PolyhedralBoundedSolidHalfEdge visibleHalf;
                        if ( f1 ) {
                            materialLine.visibleEdgeForContourLine = face1;
                            visibleHalf = e.leftHalf;
                        }
                        else {
                            materialLine.visibleEdgeForContourLine = face2;
                            visibleHalf = e.rightHalf;
                        }
                        materialLine.visibleEdgeBody = body;
                        materialLine.visibleFaceInteriorRef =
                            contourFaceInteriorRef(
                                materialLine.visibleEdgeForContourLine,
                                visibleHalf, body);
                        contourCache.add(materialLine);
                    }
                    else {
                        // Visible non contour lines
                        materialLine.edgeType = _AppelEdgeCache.VISIBLE_LINE;
                    }
                    cache.add(materialLine);
                    //--------------------------------------------------------
                    prevEnd = Vector3Dd.copyOf(materialLine.end);
                    l++;
                }
            }
        }
    }

    private static int computeMidpointQuantitativeInvisibility(
        List<SimpleBody> solids,
        Camera camera,
        Vector3Dd midpoint)
    {
        // Single source of truth: delegate to the per-body kernel QI
        // (SimpleBody -> PolyhedralBoundedSolid.computeQuantitativeInvisibility),
        // which already resolves grazing lines of sight robustly. The previous
        // world-space reimplementation that lived here diverged from the kernel
        // (different LIMIT handling and normal source) and was removed so both
        // the visibility decision and the diagnostics share one computation.
        int qi = 0;
        for ( int i = 0; i < solids.size(); i++ ) {
            qi += solids.get(i).computeQuantitativeInvisibility(
                camera.getPosition(), midpoint);
        }
        return qi;
    }

    private static boolean isUnitInterval(double t)
    {
        return t >= VSDK.EPSILON && t <= 1.0 - VSDK.EPSILON;
    }

    public static String edgeTypeName(int edgeType)
    {
        return switch ( edgeType ) {
          case _AppelEdgeCache.HIDDEN_LINE -> "hidden";
          case _AppelEdgeCache.CONTOUR_LINE -> "contour";
          case _AppelEdgeCache.VISIBLE_LINE -> "visible";
          default -> "unknown";
        };
    }

    private static AppelEdgeDump createEdgeDump(_AppelEdgeCache edge)
    {
        AppelEdgeDump dump = new AppelEdgeDump();
        dump.edgeIndex = edge.edgeIndex;
        dump.edgeType = edge.edgeType;
        dump.edgeTypeName = edgeTypeName(edge.edgeType);
        dump.face1Id = edge.leftFace != null ? edge.leftFace.id : -1;
        dump.face2Id = edge.rightFace != null ? edge.rightFace.id : -1;
        dump.start = new Vector3Dd(edge.start);
        dump.end = new Vector3Dd(edge.end);
        return dump;
    }

    /**
    This method takes an edge that is candidate to be visible, breaks it into
    segments and for each segment determines visibility. Visible segments
    are reported in `outVisibleContourLineSet` and
    `outVisibleNonContourLineSet`, and hidden segments are reported on
    `outHiddenLineSet`.
    PRE:
      - Current edge is known to correspond to a material line (normal or
        contour)
      - `contourCache` contains the list of contour lines
    */
    private static final double[][] CLIP_PLANES = new double[][] {
        { 1.0, 0.0, 0.0, 1.0 },
        { -1.0, 0.0, 0.0, 1.0 },
        { 0.0, 1.0, 0.0, 1.0 },
        { 0.0, -1.0, 0.0, 1.0 },
        { 0.0, 0.0, 1.0, 1.0 },
        { 0.0, 0.0, -1.0, 1.0 }
    };

    private static double evaluateClipPlane(double[] plane, Vector4Dd point)
    {
        return plane[0] * point.x() + plane[1] * point.y() +
            plane[2] * point.z() + plane[3] * point.w();
    }

    private static Vector4Dd interpolate(Vector4Dd start,
                                         Vector4Dd end,
                                         double t)
    {
        return start.multiply(1.0 - t).add(end.multiply(t));
    }

    private static Vector4Dd[] clipLineToClipVolume(Vector4Dd start,
                                                    Vector4Dd end)
    {
        Vector4Dd clippedStart = start;
        Vector4Dd clippedEnd = end;

        for ( int i = 0; i < CLIP_PLANES.length; i++ ) {
            double[] plane = CLIP_PLANES[i];
            double d0 = evaluateClipPlane(plane, clippedStart);
            double d1 = evaluateClipPlane(plane, clippedEnd);

            if ( d0 < 0.0 && d1 < 0.0 ) {
                return null;
            }
            if ( d0 < 0.0 || d1 < 0.0 ) {
                double denominator = d0 - d1;
                if ( Math.abs(denominator) < VSDK.EPSILON ) {
                    return null;
                }
                double t = d0 / denominator;
                Vector4Dd intersection = interpolate(clippedStart, clippedEnd,
                    t);
                if ( d0 < 0.0 ) {
                    clippedStart = intersection;
                }
                else {
                    clippedEnd = intersection;
                }
            }
        }
        return new Vector4Dd[] { clippedStart, clippedEnd };
    }

    private static void addProjectedLine(Calligraphic2DBuffer lineSet,
                                         Vector3Dd point0,
                                         Vector3Dd point1,
                                         Camera camera)
    {
        Vector4Dd clip0 = camera.calculateProjectionMatrix().multiply(
            new Vector4Dd(point0));
        Vector4Dd clip1 = camera.calculateProjectionMatrix().multiply(
            new Vector4Dd(point1));
        Vector4Dd[] clipped = clipLineToClipVolume(clip0, clip1);
        if ( clipped == null ) {
            return;
        }

        Vector4Dd ndc0 = clipped[0].dividedByW();
        Vector4Dd ndc1 = clipped[1].dividedByW();
        lineSet.add2DLine(ndc0.x(), ndc0.y(), ndc1.x(), ndc1.y());
    }

    /**
    Projects a world point to 2D normalized device coordinates, or null if the
    point is at/behind the eye plane (w &lt;= 0) where the projection is invalid.

    @param point world point
    @param camera viewing camera
    @return {x, y} in NDC, or null
    */
    private static double[] projectToNdc(Vector3Dd point, Camera camera)
    {
        Vector4Dd clip = camera.calculateProjectionMatrix().multiply(
            new Vector4Dd(point));
        if ( clip.w() <= VSDK.EPSILON ) {
            return null;
        }
        return new double[] { clip.x() / clip.w(), clip.y() / clip.w() };
    }

    /** Sign of which side of the 2D line (a -&gt; b) the point p lies on. */
    private static double sideOfLine2D(double[] a, double[] b, double[] p)
    {
        return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]);
    }

    /**
    World point a small step into the interior of face S across its edge,
    measured by the in-plane inward normal at that edge (cross of the outward
    face normal with the edge direction in loop winding). Local, so it is on the
    correct side even for a concave S. Returns null if it cannot be built.

    @param face the visible face S
    @param visibleHalf the half-edge of S lying on the contour edge
    @param body owning body
    @return world interior reference point, or null
    */
    private static Vector3Dd contourFaceInteriorRef(
        _PolyhedralBoundedSolidFace face,
        _PolyhedralBoundedSolidHalfEdge visibleHalf, SimpleBody body)
    {
        if ( face == null || visibleHalf == null ||
             visibleHalf.startingVertex == null || visibleHalf.next() == null ||
             visibleHalf.next().startingVertex == null ) {
            return null;
        }
        InfinitePlane plane = face.getContainingPlane();
        if ( plane == null ) {
            return null;
        }
        Vector3Dd v1 = visibleHalf.startingVertex.position;
        Vector3Dd v2 = visibleHalf.next().startingVertex.position;
        Vector3Dd edgeDir = v2.subtract(v1);
        double edgeLength = edgeDir.length();
        if ( edgeLength <= VSDK.EPSILON ) {
            return null;
        }
        Vector3Dd inward = plane.getNormal().normalized().crossProduct(edgeDir);
        if ( inward.length() <= VSDK.EPSILON ) {
            return null;
        }
        inward = inward.normalized();
        Vector3Dd midpoint = v1.add(v2).multiply(0.5);
        Vector3Dd interiorLocal = midpoint.add(inward.multiply(0.25 * edgeLength));
        return transformToWorld(body, interiorLocal);
    }

    /**
    Appel's image-space change in quantitative invisibility when the processed
    edge crosses contour `cl` (known to pass in front of the edge). Crossing the
    contour line in the image, the edge moves from one side of it to the other;
    Q.I. increases by one if it moves onto the side where the contour's
    front-facing face projects (the edge goes behind that face) and decreases by
    one otherwise. All inputs are taken far from the crossing (edge endpoints and
    the face centroid), so the sign is free of silhouette-grazing degeneracy and
    does not depend on which face actually lies deepest behind the edge.

    @param inEdge the edge being classified
    @param cl the crossing contour edge (its front face is cl.visibleEdgeForContourLine)
    @param camera viewing camera
    @return +1, -1, or 0 when the configuration is degenerate/unprojectable
    */
    private static int contourCrossingDeltaQI(_AppelEdgeCache inEdge,
        _AppelEdgeCache cl, Camera camera)
    {
        Vector3Dd interiorRef = cl.visibleFaceInteriorRef;
        if ( interiorRef == null ) {
            return 0;
        }
        double[] clStart = projectToNdc(cl.start, camera);
        double[] clEnd = projectToNdc(cl.end, camera);
        double[] edgeEnd = projectToNdc(inEdge.end, camera);
        double[] faceSide = projectToNdc(interiorRef, camera);
        if ( clStart == null || clEnd == null || edgeEnd == null ||
             faceSide == null ) {
            return 0;
        }
        double edgeEndSide = sideOfLine2D(clStart, clEnd, edgeEnd);
        double faceCentroidSide = sideOfLine2D(clStart, clEnd, faceSide);
        if ( Math.abs(edgeEndSide) < VSDK.EPSILON ||
             Math.abs(faceCentroidSide) < VSDK.EPSILON ) {
            return 0;
        }
        // The edge start (s=0) and end (s=1) lie on opposite sides of the
        // contour line, so the end's side is "after the crossing". If that side
        // matches the front face's side, the edge has moved behind the face.
        return (edgeEndSide > 0.0) == (faceCentroidSide > 0.0) ? 1 : -1;
    }

    private static void
    processLineToBeDrawn(
        List <SimpleBody> solids,
        _AppelEdgeCache inEdge,
        Camera inCamera,
        Calligraphic2DBuffer outVisibleContourLineSet,
        Calligraphic2DBuffer outVisibleNonContourLineSet,
        Calligraphic2DBuffer outHiddenLineSet,
        List <_AppelEdgeCache> contourCache,
        AppelEdgeDump edgeDump)
    {
        //- 1. Compute the sweep plane triangle ---------------------------
        // Defines plane "SP1" on figure [APPE1967].5.
        Vector3Dd sp1a, sp1b, sp1c;

        sp1a = inEdge.start;
        sp1b = inEdge.end;
        sp1c = inCamera.getPosition();

        //- 2. Break current edge into segments ---------------------------
        // Defines plane "SP2" on figure [APPE1967].5.
        Vector3Dd sp2a, sp2b, sp2c;
        Vector3Dd K; // Preceding point "K" on figure [APPE1967].5.
        Vector3Dd J; // "K" projected on "SP2"
        Ray ray = new Ray(new Vector3Dd(), new Vector3Dd());
        double t0;
        int i;
        int pos;
        _AppelEdgeCache cl;          // Line "CL" on figure 5 of [APPE1967]
        ArrayList<_AppelEdgeSegment> segments;
        _AppelEdgeSegment segment;
        InfinitePlane plane;

        segments = new ArrayList<_AppelEdgeSegment>();
        segment = new _AppelEdgeSegment();
        segment.t = 0;
        segment.deltaQI = 0;
        segments.add(segment);
        sp2c = inCamera.getPosition();

        for ( i = 0; i < contourCache.size(); i++ ) {
            cl = contourCache.get(i);
            if ( cl == inEdge ) {
                // Do not break an edge with itself.
                continue;
            }
            ray = ray.withOrigin(cl.start.add(cl.d.multiply(3*VSDK.EPSILON)));
            ray = ray.withDirection(cl.d);
            t0 = ray.direction().length() - 6*VSDK.EPSILON;
            ray = ray.withDirection(ray.direction().normalized());
            Intersection hit =
                Triangle.doIntersectionWithTriangle(ray, sp1a, sp1b, sp1c);
            if ( inEdge.edgeIndex == DEBUG_EDGE_INDEX && hit != null ) {
                debugSplit(inEdge.edgeIndex, "contour cl.edgeIndex=" +
                    cl.edgeIndex + " sweepHit t=" +
                    String.format("%.5f", hit.t) + " t0=" +
                    String.format("%.5f", t0) + " accepted=" + (hit.t < t0));
            }
            if (
             hit != null &&
             hit.t < t0
            ) {
                // The breaking point in the current testing edge corresponding
                // to the passing contour is the piercing point where the
                // edge intersects with the contour's sweeping plane.
                sp2a = cl.start;
                sp2b = cl.end;
                plane = new InfinitePlane(sp2a, sp2b, sp2c);
                ray = ray.withOrigin(inEdge.start);
                ray = ray.withDirection(inEdge.d.normalized());
                Ray planeHit = plane.doIntersection(ray);
                if ( planeHit == null ) {
                    debugSplit(inEdge.edgeIndex, "  cl=" + cl.edgeIndex +
                        " DISCARDED: edge/SP2 plane intersection null");
                    continue;
                }
                segment = new _AppelEdgeSegment();
                // Point PP2 lies on the current edge, so its parameter must
                // come from the edge/plane intersection rather than from the
                // contour line piercing the sweep triangle.
                segment.t = planeHit.t() / inEdge.d.length();
                if ( !isUnitInterval(segment.t) ) {
                    debugSplit(inEdge.edgeIndex, "  cl=" + cl.edgeIndex +
                        " DISCARDED: split t=" + String.format("%.5f",
                        segment.t) + " outside unit interval");
                    continue;
                }

                // A contour line crosses this edge in the image at parameter
                // segment.t, so the edge is split there: the quantitative
                // invisibility can only change where the edge crosses a contour.
                // The +/-1 change is decided now by Appel's image-space side rule
                // (robust, no occlusion sampling); coincident crossings are
                // summed in step 3.
                // deltaQI feeds the diagnostic incremental-Q.I. dump only; skip
                // its image-space projection work on the rendering path.
                segment.deltaQI = edgeDump != null ?
                    contourCrossingDeltaQI(inEdge, cl, inCamera) : 0;
                debugSplit(inEdge.edgeIndex, "  cl=" + cl.edgeIndex +
                    " ADDED split t=" + String.format("%.5f", segment.t) +
                    " deltaQI=" + segment.deltaQI);
                segments.add(segment);
            }
        }
        segment = new _AppelEdgeSegment();
        segment.t = 1;
        segments.add(segment);

        //- 3. Sort segment set -------------------------------------------
        Collections.sort(segments);

        // Merge coincident boundaries: when two crossings fall within tolerance
        // they delimit a zero-length (undrawable) sub-segment, but each still
        // carries a real change in quantitative invisibility. Rather than drop
        // one (which would lose its increment), SUM their deltaQI into the kept
        // boundary so step 4 accounts for every crossing.
        for ( i = 0; i < segments.size()-1; i++ ) {
            if ( segments.get(i).compareTo(segments.get(i+1)) == 0 ) {
                segments.get(i+1).deltaQI += segments.get(i).deltaQI;
                segments.remove(i);
                i--;
            }
        }

        //- 4. Determine visibility by incremental [APPE1967] propagation ---
        // The contour crossings (step 2) partition the edge into sub-segments of
        // UNIFORM visibility: the quantitative invisibility changes ONLY where
        // the edge crosses a contour, and there by the +/-1 already computed from
        // the image-space side rule (step 2/3). So the Q.I. is seeded once by a
        // single kernel ray cast on the first sub-segment, then propagated by
        // accumulating each crossing's deltaQI. The per-sub-segment midpoint
        // kernel Q.I. is still recorded in the dump as an independent cross-check.
        Vector3Dd pos1, pos2;
        int qi = 0;

        // The propagation seed is a diagnostic-only quantity (classification uses
        // each sub-segment's own midpoint Q.I.), so the seed ray cast runs only
        // when collecting the dump.
        if ( edgeDump != null ) {
            double seedMidT = (segments.get(0).t + segments.get(1).t) / 2.0;
            qi = computeMidpointQuantitativeInvisibility(solids, inCamera,
                inEdge.start.add(inEdge.d.multiply(seedMidT)));
            edgeDump.initialQuantitativeInvisibility = qi;
        }

        for ( i = 0; i < segments.size()-1; i++ ) {
            double val1 = segments.get(i).t;
            double val2 = segments.get(i+1).t;
            pos1 = inEdge.start.add(inEdge.d.multiply(val1));
            pos2 = inEdge.start.add(inEdge.d.multiply(val2));
            Vector3Dd posx = inEdge.start.add(inEdge.d.multiply((val1+val2)/2));

            // Propagate the incremental Q.I. (diagnostic): accumulate the
            // crossing at the left boundary (segments.get(0)'s deltaQI is 0, so
            // the first sub-segment keeps the seed).
            qi += segments.get(i).deltaQI;

            // Classification AUTHORITY is the robust per-sub-segment kernel Q.I.
            // at the (non-degenerate) midpoint. The incrementally propagated `qi`
            // is recorded in the dump for comparison, but is not used to draw:
            // the cheap per-crossing deltaQI (image-space side rule) is not yet
            // robust on concave faces, so until it matches the kernel everywhere
            // the kernel midpoint Q.I. drives rendering.
            int midpointQi = computeMidpointQuantitativeInvisibility(
                solids, inCamera, posx);

            if ( i > 0 && edgeDump != null ) {
                AppelEventDump event = new AppelEventDump();
                event.t = val1;
                event.deltaQI = segments.get(i).deltaQI;
                event.contourEdgeIndex = -1;
                event.visibleFaceId = -1;
                edgeDump.events.add(event);
            }

            if ( midpointQi == 0 ) {
                if ( inEdge.edgeType == _AppelEdgeCache.CONTOUR_LINE ) {
                    addProjectedLine(outVisibleContourLineSet, pos1, pos2,
                        inCamera);
                }
                else {
                    addProjectedLine(outVisibleNonContourLineSet, pos1, pos2,
                        inCamera);
                }
            }
            else {
                addProjectedLine(outHiddenLineSet, pos1, pos2, inCamera);
            }

            if ( edgeDump != null ) {
                AppelSegmentDump segmentDump = new AppelSegmentDump();
                segmentDump.tStart = val1;
                segmentDump.tEnd = val2;
                segmentDump.start = new Vector3Dd(pos1);
                segmentDump.end = new Vector3Dd(pos2);
                segmentDump.midpoint = new Vector3Dd(posx);
                segmentDump.midpointQuantitativeInvisibility = midpointQi;
                segmentDump.classification = midpointQi == 0 ? "visible" : "hidden";
                edgeDump.segments.add(segmentDump);
            }
        }

        //segments = null;
    }

    /**
    Given a viewing camera and a set of bodies, this method generates three
    sets of lines for visible/hidden line rendering, as described in
    paper [APPE1967] and section [FOLE1992].15.3.2. The resulting line sets
    are projected to 2D calligraphic buffers and separated into visible
    contour, visible non-contour and hidden segments.
    */
    public static void executeAppelAlgorithm(
        List<SimpleBody> inSimpleBodyArray,
        Camera inCamera,
        Calligraphic2DBuffer outVisibleContourLineSet,
        Calligraphic2DBuffer outVisibleNonContourLineSet,
        Calligraphic2DBuffer outHiddenLineSet)
    {
        // Rendering path: do not collect the per-edge diagnostic dump, which
        // also lets each edge skip the diagnostic-only seed Q.I. sample and the
        // image-space deltaQI projection.
        runAppelAlgorithm(inSimpleBodyArray, inCamera, outVisibleContourLineSet,
            outVisibleNonContourLineSet, outHiddenLineSet, false);
    }

    public static AppelAlgorithmDump executeAppelAlgorithmWithDiagnostics(
        List<SimpleBody> inSimpleBodyArray,
        Camera inCamera,
        Calligraphic2DBuffer outVisibleContourLineSet,
        Calligraphic2DBuffer outVisibleNonContourLineSet,
        Calligraphic2DBuffer outHiddenLineSet)
    {
        return runAppelAlgorithm(inSimpleBodyArray, inCamera,
            outVisibleContourLineSet, outVisibleNonContourLineSet,
            outHiddenLineSet, true);
    }

    private static AppelAlgorithmDump runAppelAlgorithm(
        List<SimpleBody> inSimpleBodyArray,
        Camera inCamera,
        Calligraphic2DBuffer outVisibleContourLineSet,
        Calligraphic2DBuffer outVisibleNonContourLineSet,
        Calligraphic2DBuffer outHiddenLineSet,
        boolean collectDiagnostics)
    {
        //-----------------------------------------------------------------
        ArrayList <_AppelEdgeCache> cache;
        ArrayList <_AppelEdgeCache> contourCache;

        cache = new ArrayList <_AppelEdgeCache>();
        contourCache = new ArrayList <_AppelEdgeCache>();

        //-----------------------------------------------------------------
        ArrayList <SimpleBody> solids;
        int i;

        solids = new ArrayList <SimpleBody>();

        for ( i = 0; i < inSimpleBodyArray.size(); i++ ) {
            buildCache(solids, inSimpleBodyArray.get(i), cache, contourCache, inCamera);
        }

        // Snapshot each solid's face planes and tolerance once: every edge below
        // issues many quantitative-invisibility samples against these unchanged
        // solids, so caching avoids recomputing each face plane per ray per
        // sample (the dominant cost on dense models such as the kurlanderBowl).
        for ( i = 0; i < solids.size(); i++ ) {
            Geometry geometry = solids.get(i).getGeometry();
            if ( geometry instanceof PolyhedralBoundedSolid ) {
                ((PolyhedralBoundedSolid) geometry).beginVisibilityQueries();
            }
        }

        //-----------------------------------------------------------------
        _AppelEdgeCache edge;
        AppelAlgorithmDump dump = new AppelAlgorithmDump();

        try {
        for ( i = 0; i < cache.size(); i++ ) {
            edge = cache.get(i);
            AppelEdgeDump edgeDump =
                collectDiagnostics ? createEdgeDump(edge) : null;
            // Every cached edge is resolved through the quantitative
            // invisibility test, including those whose two adjacent faces are
            // both back-facing (edgeType HIDDEN_LINE). Such an edge must NOT be
            // assumed hidden: the "both faces back-facing => invisible"
            // shortcut is only sound for convex bodies. On a concave solid
            // (e.g. the kurlanderBowl) a both-back-facing edge can be genuinely
            // visible, so its visibility is determined by QI like any other
            // line, consistent with [APPE1967] which makes no convexity
            // assumption.
            switch ( edge.edgeType ) {
              case _AppelEdgeCache.HIDDEN_LINE:
              case _AppelEdgeCache.CONTOUR_LINE:
              case _AppelEdgeCache.VISIBLE_LINE:
                processLineToBeDrawn(
                    solids,
                    edge, inCamera, outVisibleContourLineSet,
                    outVisibleNonContourLineSet, outHiddenLineSet,
                    contourCache, edgeDump);
                break;
              default: break;
            }
            if ( edgeDump != null ) {
                dump.edges.add(edgeDump);
            }
        }
        }
        finally {
            for ( i = 0; i < solids.size(); i++ ) {
                Geometry geometry = solids.get(i).getGeometry();
                if ( geometry instanceof PolyhedralBoundedSolid ) {
                    ((PolyhedralBoundedSolid) geometry).endVisibilityQueries();
                }
            }
        }
        //-----------------------------------------------------------------
        //cache = null;
        //contourCache = null;
        return dump;
    }

}
