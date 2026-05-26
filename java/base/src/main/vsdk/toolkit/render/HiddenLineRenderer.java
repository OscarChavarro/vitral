//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisivility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          meeting 1967.                                                  =

package vsdk.toolkit.render;

// Java classes
import java.util.ArrayList;
import java.util.Collections;

// VitralSDK classes
import java.util.List;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
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

class _AppelEdgeSegment extends RenderingElement implements Comparable <_AppelEdgeSegment>
{
    /// Distance from start to end with respect to line parameter
    public double t;
    public int deltaQI; // Relative change in quantitative invisibility

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
    /// `visibleEdgeForContourLine` contains an explicit reference to the
    /// planar surface marked as "S" on figure [APPE1967].5.
    public _PolyhedralBoundedSolidFace visibleEdgeForContourLine;

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
            else if ( dot > VSDK.EPSILON ) {
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

    private static int
    computeQuantitativeInvisibility(List <SimpleBody> solids,
        Camera camera, _AppelEdgeCache edge)
    {
        int qi = 0;
        int i;

        for ( i = 0; i < solids.size(); i++ ) {
            qi += solids.get(i).computeQuantitativeInvisibility(
                camera.getPosition(), edge.start.add(edge.d.multiply(10*VSDK.EPSILON)));
        }
        return qi;
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
                    f1 = isFaceVisibleFromCamera(face1, camera) >= 0;
                    f2 = isFaceVisibleFromCamera(face2, camera) >= 0;

                    //--------------------------------------------------------
                    materialLine = new _AppelEdgeCache();
                    materialLine.setStart(startPosition);
                    materialLine.setEnd(endPosition);
                    materialLine.d = endPosition.subtract(startPosition);
                    if ( l > 0 &&
                         Vector3Dd.distance(prevEnd, startPosition) < 
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
                        if ( f1 ) {
                            materialLine.visibleEdgeForContourLine = face1;
                        }
                        else {
                            materialLine.visibleEdgeForContourLine = face2;
                        }
                        contourCache.add(materialLine);
                    }
                    else {
                        // Visible non contour lines
                        materialLine.edgeType = _AppelEdgeCache.VISIBLE_LINE;
                    }
                    cache.add(materialLine);
                    //--------------------------------------------------------
                    prevEnd = Vector3Dd.copyOf(endPosition);
                    l++;
                }
            }
        }
    }

    /**
    This method takes an edge that is candidate to be visible, breaks it into
    segments and for each segment determines visibility. Visible segments
    are reported in `outVisibleContourLineEndPoints` and `outVisibleNonContourLineEndPoints`,
    and hidden segments are reported on `outHiddenLineEndPoints`.
    PRE:
      - Current edge is known to correspond to a material line (normal or
        contour)
      - `contourCache` contains the list of contour lines
    */
    private static void
    processLineToBeDrawn(
        List <SimpleBody> solids,
        _AppelEdgeCache inEdge,
        Camera inCamera,
        List <Vector3Dd> outVisibleContourLineEndPoints,
        List <Vector3Dd> outVisibleNonContourLineEndPoints,
        List <Vector3Dd> outHiddenLineEndPoints,
        List <_AppelEdgeCache> contourCache)
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
        Vector3Dd p = new Vector3Dd(); // Point "PP1" on figure 5 of [APPE1967]
        Vector3Dd n = new Vector3Dd();
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
                if ( planeHit != null ) {
                    segment = new _AppelEdgeSegment();
                    // Point PP2 lies on the current edge, so its parameter must
                    // come from the edge/plane intersection rather than from the
                    // contour line piercing the sweep triangle.
                    segment.t = planeHit.t() / inEdge.d.length();

                    // Determine the change in quantitative invisibility...
                    K = inEdge.start.add(inEdge.d.multiply(segment.t-2*VSDK.EPSILON));

                    // Project K on SP2
                    ray = ray.withOrigin(K);
                    ray = ray.withDirection(sp2c.subtract(K));
                    ray = ray.withDirection(ray.direction().normalized());
                    Ray contourHit = cl.visibleEdgeForContourLine.getContainingPlane().
                         doIntersection(ray);
                    if ( contourHit != null ) {
                        J = contourHit.origin().add(contourHit.direction().multiply(contourHit.t()));
                        pos = cl.visibleEdgeForContourLine.testPointInside(J, VSDK.EPSILON);
                        if ( pos == Geometry.INSIDE || pos == Geometry.LIMIT ) {
                            segment.deltaQI = 1;
                        }
                        else {
                            segment.deltaQI = -1;
                        }
                        segments.add(segment);
                    }
                }
            }
        }
        segment = new _AppelEdgeSegment();
        segment.t = 1;
        segments.add(segment);

        //- 3. Sort segment set -------------------------------------------
        Collections.sort(segments);

        // Erase null segments
        for ( i = 0; i < segments.size()-1; i++ ) {
            if ( segments.get(i).compareTo(segments.get(i+1)) == 0 ) {
                segments.remove(i);
                i--;
            }
        }

        //- 4. Determine visibility for each segment based on Q.I. --------
        Vector3Dd pos1, pos2;
        int qi;

        qi = computeQuantitativeInvisibility(solids, inCamera, inEdge);

        for ( i = 0; i < segments.size()-1; i++ ) {
            segment = segments.get(i);
            pos1 = inEdge.start.add(inEdge.d.multiply(segment.t));
            qi += segment.deltaQI;

            double val1 = segment.t;

            segment = segments.get(i+1);
            pos2 = inEdge.start.add(inEdge.d.multiply(segment.t));


            double val2 = segment.t;

            // This disables propagation of Q.I., making all slower!
            Vector3Dd posx = inEdge.start.add(inEdge.d.multiply((val2+val1)/2));
            qi = 0;
            for ( int solidIndex = 0; solidIndex < solids.size(); solidIndex++ ) {
                qi += solids.get(solidIndex).computeQuantitativeInvisibility(
                    inCamera.getPosition(), posx);
            }
            //

            if ( qi == 0 ) {
                if ( inEdge.edgeType == _AppelEdgeCache.CONTOUR_LINE ) {
                    outVisibleContourLineEndPoints.add(new Vector3Dd(pos1));
                    outVisibleContourLineEndPoints.add(new Vector3Dd(pos2));
                }
                else {
                    outVisibleNonContourLineEndPoints.add(new Vector3Dd(pos1));
                    outVisibleNonContourLineEndPoints.add(new Vector3Dd(pos2));
                }
            }
            else {
                outHiddenLineEndPoints.add(new Vector3Dd(pos1));
                outHiddenLineEndPoints.add(new Vector3Dd(pos2));
            }
        }

        //segments = null;
    }

    /**
    Given a viewing camera and a set of bodies, this method generates three
    sets of lines for visible/hidden line rendering, as described in
    paper [APPE1967] and section [FOLE1992].15.3.2. The calculated end line
    points are in 3D space and contains viewer's perception to respect to which
    line segments are visible (as part of the object contour or non-contour
    material lines) and which line segments are visible.
    */
    public static void executeAppelAlgorithm(
        List<SimpleBody> inSimpleBodyArray,
        Camera inCamera,
        List <Vector3Dd> outVisibleContourLineEndPoints,
        List <Vector3Dd> outVisibleNonContourLineEndPoints,
        List <Vector3Dd> outHiddenLineEndPoints)
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

        //-----------------------------------------------------------------
        _AppelEdgeCache edge;

        for ( i = 0; i < cache.size(); i++ ) {
            edge = cache.get(i);
            // Note that a "line to be drawn" is any line in the cache
            // not marked as a hidden line.
            switch ( edge.edgeType ) {
              case _AppelEdgeCache.HIDDEN_LINE:
                outHiddenLineEndPoints.add(new Vector3Dd(edge.start));
                outHiddenLineEndPoints.add(new Vector3Dd(edge.end));
                break;
              case _AppelEdgeCache.CONTOUR_LINE:
              case _AppelEdgeCache.VISIBLE_LINE:
                processLineToBeDrawn(
                    solids,
                    edge, inCamera, outVisibleContourLineEndPoints,
                    outVisibleNonContourLineEndPoints, outHiddenLineEndPoints,
                    contourCache);
                break;
              default: break;
            }
        }
        //-----------------------------------------------------------------
        //cache = null;
        //contourCache = null;
    }

}
