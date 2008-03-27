//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 26 2008 - Oscar Chavarro: Original base version                 =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//===========================================================================

package vsdk.toolkit.processing;

// Java classes
import java.util.ArrayList;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;

/**
This is a utility class containing operations for implementing the boundary
representation split methods over winged-edge data structures, as presented
at chapter [MANT1988].14.

This class offers just one public method, which is supposed to be called
from GeometricModeler class.
*/
public class PolyhedralBoundedSolidSplitter extends GeometricModeler
{
    /**
    Following variable `soov` ("set of ON-vertices") from program [MANT1988].1.
    */
    private static ArrayList <_PolyhedralBoundedSolidVertex> soov;

    /**
    Following variable `sone` ("set of null edges") from program [MANT1988].1.
    */
    private static ArrayList <_PolyhedralBoundedSolidEdge> sone;

    /**
    Following variable `sonf` ("set of null faces") from program [MANT1988].1.
    */
    private static ArrayList <_PolyhedralBoundedSolidFace> sonf;

    /**
    Implements function `addsoov` from program [MANT1988].14.2.
    */
    private static void addsoov(_PolyhedralBoundedSolidVertex v)
    {
        int i;

	for ( i = 0; i < soov.size(); i++ ) {
	    if ( soov.get(i) == v ) {
                return;
	    }
	}
        soov.add(v);
    }

    /**
    Implements solid splitting reduction step as indicated on section
    [MANT1988].14.4 and program [MANT1988].14.2.

    This method is responsible for generating the set of coplanar
    vertices of `inSolid` (with respect to `inSplittingPlane`) and store
    them on `soov` for later usage.

    This method subdivides all edges of `inSolid` that intersects
    `inSplittingPlane` at their intersection points.
    */
    private static void splitGenerate(PolyhedralBoundedSolid inSolid,
                                      InfinitePlane inSplittingPlane)
    {
        _PolyhedralBoundedSolidEdge e;
        _PolyhedralBoundedSolidHalfEdge he;
        _PolyhedralBoundedSolidVertex v1, v2;
        Vector3D p;
        double d1, d2, t;
        int s1, s2;
        int i;

        soov = new ArrayList <_PolyhedralBoundedSolidVertex>();
	for ( i = 0; i < inSolid.edgesList.size(); i++ ) {
	    e = inSolid.edgesList.get(i);
            v1 = e.rightHalf.startingVertex;
            v2 = e.leftHalf.startingVertex;
            d1 = inSplittingPlane.pointDistance(v1.position);
            d2 = inSplittingPlane.pointDistance(v2.position);
            s1 = inSolid.compareValue(d1, 0.0, VSDK.EPSILON);
            s2 = inSolid.compareValue(d2, 0.0, VSDK.EPSILON);
            if ( (s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1) ) {
                t = d1 / (d1 - d2);
                p = v1.position.add((v2.position.substract(v1.position)).multiply(t));
                he = e.leftHalf.next();
                inSolid.lmev(e.rightHalf, he, inSolid.getMaxVertexId()+1, p);
                addsoov(he.previous().startingVertex);
	    }
	    else {
                if ( s1 == 0 ) {
                    addsoov(v1);
		}
                if ( s2 == 0 ) {
                    addsoov(v2);
		}
	    }
	}
    }

    /**
    */
    private static void splitClassify(InfinitePlane inSplittingPlane)
    {
        System.out.println("splitClassify");
    }

    /**
    */
    private static void splitConnect()
    {
        System.out.println("splitConnect");
    }

    /**
    */
    private static void splitFinish(PolyhedralBoundedSolid inSolid,
                             ArrayList<PolyhedralBoundedSolid> outSolidsAbove,
                             ArrayList<PolyhedralBoundedSolid> outSolidsBelow)
    {
        System.out.println("splitFinish");
    }

    /**
    Given the input `inSolid` and the cutting plane `inSplittingPlane`,
    this method appends to the `outSolidsAbove` list the solids resulting
    from cutting the solid with the plane and resulting above the plane,
    similarly, `outSolidsBelow` will be appended with solid pieces
    resulting below the plane.

    Current macro-algorithm follows the strategy outlined on section
    [MANT1988].14.3 and program [MANT1988].14.1.
    */
    public static void split(
                      PolyhedralBoundedSolid inSolid,
                      InfinitePlane inSplittingPlane,
                      ArrayList<PolyhedralBoundedSolid> outSolidsAbove,
                      ArrayList<PolyhedralBoundedSolid> outSolidsBelow)
    {
        //-----------------------------------------------------------------
        sone = new ArrayList <_PolyhedralBoundedSolidEdge>();
        sonf = new ArrayList <_PolyhedralBoundedSolidFace>();

        //-----------------------------------------------------------------
        inSolid.validateModel();
        splitGenerate(inSolid, inSplittingPlane);
        splitClassify(inSplittingPlane);
        //if ( ??? ) {
            //VSDK.reportMessage(null, VSDK.FATAL_WARNING,
            //"PolyhedralBoundedSolidSplitter.split",
            //"Trying to build a halfedge from another, non-existing halfedge!");
            //return;
        //}
        splitConnect();
        splitFinish(inSolid, outSolidsAbove, outSolidsBelow);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
