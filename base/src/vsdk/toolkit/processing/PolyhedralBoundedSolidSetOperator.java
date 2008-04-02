//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 1 2008 - Oscar Chavarro: Original base version                  =
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
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;

class _PolyhedralBoundedSolidSetOperatorVertexVertex extends GeometricModeler
{
    public _PolyhedralBoundedSolidVertex va;
    public _PolyhedralBoundedSolidVertex vb;
}

class _PolyhedralBoundedSolidSetOperatorVertexFace extends GeometricModeler
{
    public _PolyhedralBoundedSolidVertex v;
    public _PolyhedralBoundedSolidFace f;
}

public class PolyhedralBoundedSolidSetOperator extends GeometricModeler
{
    /**
    Following variable `sonvv` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv;

    /**
    Following variable `sonva` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva;

    /**
    Following variable `sonea` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidEdge> sonea;

    /**
    Following variable `soneb` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidEdge> soneb;

    /**
    Following variable `sonfa` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidFace> sonfa;

    /**
    Following variable `sonfb` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidFace> sonfb;

    /**
    Following program [MANT1988].15.1.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        sonvv = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex>();
        sonva = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>();
        sonea = new ArrayList<_PolyhedralBoundedSolidEdge>();
        soneb = new ArrayList<_PolyhedralBoundedSolidEdge>();
        sonfa = new ArrayList<_PolyhedralBoundedSolidFace>();
        sonfb = new ArrayList<_PolyhedralBoundedSolidFace>();

        _PolyhedralBoundedSolidFace f;
        PolyhedralBoundedSolid res = new PolyhedralBoundedSolid();

	System.out.println("HUY");
        
        return res;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
