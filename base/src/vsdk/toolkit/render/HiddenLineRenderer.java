//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisivility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          meeting 1967.                                                  =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 5 2007 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render;

// Java classes
import java.util.ArrayList;
import java.util.Collections;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;

class _AppelEdgeSegment extends RenderingElement implements Comparable <_AppelEdgeSegment>
{
    /// Distance from start to end with respect to line parameter
    public double t;
    public int deltaQI; // Relative change in quantitative invisibility

    public int compareTo(_AppelEdgeSegment other)
    {
        if ( this.t < other.t ) return -1;
        else if ( this.t > other.t ) return 1;
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
    /// solid and with the same quantitative invisibility. Whe this happens,
    /// quantitative invisibility can be acumulated in the edge sequence,
    /// otherwise must be calculated.
    boolean onSequence;
    public Vector3D start;
    public Vector3D end;
    /// d = start - end
    public Vector3D d;
    /// `visibleEdgeForContourLine` contains an explicit reference to the
    /// planar surface marked as "S" on figure 5 on [APPE1967].
    public _PolyhedralBoundedSolidFace visibleEdgeForContourLine;

    public void setStart(Vector3D s)
    {
        start = new Vector3D(s);
    }
    public void setEnd(Vector3D e)
    {
        end = new Vector3D(e);
    }
}

/**
This class implements the Appel's algorithm for hidden line rendering. :)
*/
public class HiddenLineRenderer extends RenderingElement
{
    private static int
    computeQuantitativeInvisibility(ArrayList <SimpleBody> solids,
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

        g = g.exportToPolyhedralBoundedSolid();

        solids.add(body);
        PolyhedralBoundedSolid solid = (PolyhedralBoundedSolid)g;

        int i;
        long l = 0;

        _PolyhedralBoundedSolidFace face1;
        _PolyhedralBoundedSolidFace face2;
        boolean f1, f2;
        _AppelEdgeCache materialLine;
        Vector3D prevEnd = new Vector3D();

        for ( i = 0; i < solid.edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = solid.edgesList.get(i);

            int start, end;
            start = e.getStartingVertexId();
            end = e.getEndingVertexId();
            if ( start >= 0 && end >= 0 ) {
                Vector3D startPosition;
                Vector3D endPosition;
                Vector3D middle;
                Vector3D n;

                startPosition = e.leftHalf.startingVertex.position;
                endPosition = e.rightHalf.startingVertex.position;
                if ( startPosition != null && endPosition != null ) {
                    //--------------------------------------------------------
                    face1 = e.leftHalf.parentLoop.parentFace;
                    face2 = e.rightHalf.parentLoop.parentFace;
                    f1 = face1.isVisibleFrom(camera) >= 0;
                    f2 = face2.isVisibleFrom(camera) >= 0;

                    //--------------------------------------------------------
                    materialLine = new _AppelEdgeCache();
                    materialLine.setStart(startPosition);
                    materialLine.setEnd(endPosition);
                    materialLine.d = endPosition.substract(startPosition);
                    if ( l > 0 &&
                         VSDK.vectorDistance(prevEnd, startPosition) < 
                             VSDK.EPSILON ) {
                        materialLine.onSequence = true;
                    }
                    else {
                        materialLine.onSequence = false;
                    }
                    if ( !f1 && !f2 ) {
                        // Totally hidden lines
                        materialLine.edgeType = materialLine.HIDDEN_LINE;
                    }
                    else if ( f1 && !f2 || !f1 && f2 ) {
                        // Contour lines
                        materialLine.edgeType = materialLine.CONTOUR_LINE;
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
                        materialLine.edgeType = materialLine.VISIBLE_LINE;
                    }
                    cache.add(materialLine);
                    //--------------------------------------------------------
                    prevEnd.clone(endPosition);
                    l++;
                }
            }
        }
    }

    /**
    Given a viewing camera and a set of bodies, this method generates three
    sets of lines for visible line rendering, as described in [APPE1967] and
    [FOLE1992].15.3.2. The lines are in 3D space and contains viewer's
    perception to respect to which line segments are visible (as part of the
    object contour or normal) and which line segments are visible.
    */
    public static void executeAppelAlgorithm(
        ArrayList <SimpleBody> inSimpleBodyArray,
        Camera inCamera,
        ArrayList <Vector3D> outContourLineEndPoints,
        ArrayList <Vector3D> outVisibleLineEndPoints,
        ArrayList <Vector3D> outHiddenLineEndPoints)
    {
        //-----------------------------------------------------------------
        ArrayList <_AppelEdgeCache> cache = new ArrayList <_AppelEdgeCache>();
        ArrayList <_AppelEdgeCache> contourCache = new ArrayList <_AppelEdgeCache>();
        int i;

        //-----------------------------------------------------------------
        ArrayList <SimpleBody> solids = new ArrayList <SimpleBody>();
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
                outHiddenLineEndPoints.add(new Vector3D(edge.start));
                outHiddenLineEndPoints.add(new Vector3D(edge.end));
                break;
              case _AppelEdgeCache.CONTOUR_LINE:
              case _AppelEdgeCache.VISIBLE_LINE:
                processLineToBeDrawn(
                    solids,
                    edge, inCamera, outContourLineEndPoints,
                    outVisibleLineEndPoints, outHiddenLineEndPoints,
                    contourCache);
                break;
              default: break;
            }
        }
        //-----------------------------------------------------------------
        cache = null;
        contourCache = null;
    }

    private static void
    processLineToBeDrawn(
        ArrayList <SimpleBody> solids,
        _AppelEdgeCache edge,
        Camera camera,
        ArrayList <Vector3D> outContourLineEndPoints,
        ArrayList <Vector3D> outVisibleLineEndPoints,
        ArrayList <Vector3D> outHiddenLineEndPoints,
        ArrayList <_AppelEdgeCache> contourCache)
    {
        //- Compute the sweep plane triangle ------------------------------
        // Defines plane "SP1" on figure 5 of [APPE1967]
        Vector3D sp1a, sp1b, sp1c;

        sp1a = edge.start;
        sp1b = edge.end;
        sp1c = camera.getPosition();

        //-----------------------------------------------------------------
        // Defines plane "SP2" on figure 5 of [APPE1967]
        Vector3D sp2a, sp2b, sp2c;
        Vector3D K; // Preceding point "K" on figure 5 of [APPE1967]
        Vector3D J; // "K" projected on "SP2"
        Ray ray = new Ray(new Vector3D(), new Vector3D());
        double t0;
        int i;
        int pos;
        _AppelEdgeCache cl;          // Line "CL" on figure 5 of [APPE1967]
        Vector3D p = new Vector3D(); // Point "PP1" on figure 5 of [APPE1967]
        Vector3D n = new Vector3D();
        ArrayList<_AppelEdgeSegment> segments;
        _AppelEdgeSegment segment;
        InfinitePlane plane;

        segments = new ArrayList<_AppelEdgeSegment>();
        segment = new _AppelEdgeSegment();
        segment.t = 0;
        segment.deltaQI = 0;
        segments.add(segment);
        sp2c = camera.getPosition();

        for ( i = 0; i < contourCache.size(); i++ ) {
            cl = contourCache.get(i);
            ray.origin.clone(cl.start.add(cl.d.multiply(VSDK.EPSILON)));
            ray.direction.clone(cl.d);
            t0 = ray.direction.length() - 2*VSDK.EPSILON;
            ray.direction.normalize();
            if (
             Geometry.doIntersectionWithTriangle(ray, sp1a, sp1b, sp1c, p, n) &&
             ray.t < t0
            ) {
                // The breaking point in the current testing edge corresponding
                // to the passing contour is the piercing point where the
                // edge intersects with the contour's sweeping plane.
                sp2a = cl.start;
                sp2b = cl.end;
                plane = new InfinitePlane(sp2a, sp2b, sp2c);
                ray.origin.clone(edge.start);
                ray.direction.clone(edge.d);
                ray.direction.normalize();
                if ( plane.doIntersection(ray) ) {
                    segment = new _AppelEdgeSegment();
                    segment.t = ray.t / edge.d.length(); // Point "PP2"

                    // Determine the change in quantitative invisibility...
                    K = edge.start.add(edge.d.multiply(segment.t-2*VSDK.EPSILON));

                    // Project K on SP2
                    ray.origin.clone(K);
                    ray.direction = sp2c.substract(K);
                    ray.direction.normalize();
                    if ( cl.visibleEdgeForContourLine.containingPlane.
                         doIntersection(ray) ) {
                        J = ray.origin.add(ray.direction.multiply(ray.t));
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

        //-----------------------------------------------------------------
        Collections.sort(segments);

        //-----------------------------------------------------------------
        Vector3D pos1, pos2;
        int qi;

        qi = computeQuantitativeInvisibility(solids, camera, edge);

        for ( i = 0; i < segments.size()-1; i++ ) {
            segment = segments.get(i);
            qi += segment.deltaQI;
            pos1 = edge.start.add(edge.d.multiply(segment.t));
            segment = segments.get(i+1);
            pos2 = edge.start.add(edge.d.multiply(segment.t));
            if ( qi == 0 ) {
                if ( edge.edgeType == edge.CONTOUR_LINE ) {
                    outContourLineEndPoints.add(new Vector3D(pos1));
                    outContourLineEndPoints.add(new Vector3D(pos2));
                }
                else {
                    outVisibleLineEndPoints.add(new Vector3D(pos1));
                    outVisibleLineEndPoints.add(new Vector3D(pos2));
                }
            }
            else {
                outHiddenLineEndPoints.add(new Vector3D(pos1));
                outHiddenLineEndPoints.add(new Vector3D(pos2));
            }
        }
        segments = null;

    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
