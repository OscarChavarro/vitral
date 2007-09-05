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
    /**
    @todo handle Geometry's other than PolyhedralBoundedSolid by convertion.
    */
    private static void buildCache(SimpleBody body, 
                                   ArrayList <_AppelEdgeCache> cache,
                                   ArrayList <_AppelEdgeCache> contourCache,
                                   Camera camera)
    {
        Geometry g = body.getGeometry();
        if ( !(g instanceof PolyhedralBoundedSolid) ) {
            // Should not ignoreit, but export it to PolyhedralBoundedSolid!!!
            return;
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
        for ( i = 0; i < inSimpleBodyArray.size(); i++ ) {
            buildCache(inSimpleBodyArray.get(i), cache, contourCache, inCamera);
        }

        //-----------------------------------------------------------------
        _AppelEdgeCache edge;

        for ( i = 0; i < cache.size(); i++ ) {
            edge = cache.get(i);
            switch ( edge.edgeType ) {
              case _AppelEdgeCache.HIDDEN_LINE:
                outHiddenLineEndPoints.add(new Vector3D(edge.start));
                outHiddenLineEndPoints.add(new Vector3D(edge.end));
                break;
              case _AppelEdgeCache.CONTOUR_LINE:
              case _AppelEdgeCache.VISIBLE_LINE:
                processQuantitativeInvisibility(
                    edge, inCamera, outContourLineEndPoints,
                    outVisibleLineEndPoints, outHiddenLineEndPoints,
                    cache, contourCache);
                break;
              default: break;
            }
        }
        //-----------------------------------------------------------------
        cache = null;
        contourCache = null;
    }

    private static void
    processQuantitativeInvisibility(
        _AppelEdgeCache edge,
        Camera inCamera,
        ArrayList <Vector3D> outContourLineEndPoints,
        ArrayList <Vector3D> outVisibleLineEndPoints,
        ArrayList <Vector3D> outHiddenLineEndPoints,
        ArrayList <_AppelEdgeCache> cache,
        ArrayList <_AppelEdgeCache> contourCache)
    {
        Vector3D m = edge.end.add(edge.start).multiply(0.5);
        outContourLineEndPoints.add(new Vector3D(edge.start));
        outContourLineEndPoints.add(new Vector3D(m));
        outVisibleLineEndPoints.add(new Vector3D(m));
        outVisibleLineEndPoints.add(new Vector3D(edge.end));
    }

}


//===========================================================================
//= EOF                                                                     =
//===========================================================================
