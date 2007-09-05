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

import java.util.ArrayList;
import java.util.Collections;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;

class _AppelEdgeSegment
{
    /// Distance from start to end with respect to line parameter
    public double t;
}

class _AppelEdgeCache
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

public class HiddenLineRenderer
{
    private static int
    computeQuantitativeInvisibility(ArrayList <SimpleBody> solids,
        Camera camera, _AppelEdgeCache edge)
    {
        int qi = 0;
        int i;

	for ( i = 0; i < solids.size(); i++ ) {
	    qi += solids.get(i).computeQuantitativeInvisibility(
		camera.getPosition(), edge.start);
	}

        System.out.println("Quantitative invisibility for line " + 
			   edge.start + " - " + edge.end + ": " + qi);
	return qi;
    }

    /**
    @todo handle Geometry's other than PolyhedralBoundedSolid by convertion.
    */
    private static void buildCache(ArrayList <SimpleBody> solids,
                                   SimpleBody body, 
                                   ArrayList <_AppelEdgeCache> cache,
                                   ArrayList <_AppelEdgeCache> contourCache,
                                   Camera camera)
    {
        Geometry g = body.getGeometry();
        if ( !(g instanceof PolyhedralBoundedSolid) ) {
            // Should not ignoreit, but export it to PolyhedralBoundedSolid!!!
            return;
        }
	else {
	    solids.add(body);
	}
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
            start = e.getStartingVertexIndex();
            end = e.getEndingVertexIndex();
            if ( start >= 0 && end >= 0 ) {
                Vector3D startPosition;
                Vector3D endPosition;
                Vector3D middle;
                Vector3D n;

                startPosition = solid.getVertexPosition(start);
                endPosition = solid.getVertexPosition(end);
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
        Vector3D m;
	double t = 0.1;
        int qi;

        qi = computeQuantitativeInvisibility(solids, camera, edge);
        if ( qi > 3 ) qi = 3;
        t = 0.05 + ((double)qi) * 0.3;

        m = edge.start.add(edge.d.multiply(t));
        outContourLineEndPoints.add(new Vector3D(edge.start));
        outContourLineEndPoints.add(new Vector3D(m));
        outVisibleLineEndPoints.add(new Vector3D(m));
        outVisibleLineEndPoints.add(new Vector3D(edge.end));

        int i;

        for ( i = 0; i < contourCache.size(); i++ ) {
	    ;
	}
    }

}


//===========================================================================
//= EOF                                                                     =
//===========================================================================
