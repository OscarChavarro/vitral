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
    Following variable `sonvb` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb;

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
    Procedure `updmaxnames` functionality is described on section
    [MANT1988].15.4. This method increments the face and vertex
    identifiers of `solidToUpdate` so that they do not overlap with
    `referenceSolid` identifiers.
    */
    private static void updmaxnames(PolyhedralBoundedSolid solidToUpdate,
                             PolyhedralBoundedSolid referenceSolid)
    {
        _PolyhedralBoundedSolidVertex v;
        _PolyhedralBoundedSolidFace f;
        int i;

        for ( i = 0; i < solidToUpdate.verticesList.size(); i++ ) {
            v = solidToUpdate.verticesList.get(i);
            v.id += referenceSolid.getMaxVertexId()+1;
        }

        for ( i = 0; i < solidToUpdate.polygonsList.size(); i++ ) {
            f = solidToUpdate.polygonsList.get(i);
            f.id += referenceSolid.getMaxFaceId()+1;
        }
    }

    private static void doSetOpGenerate(
        _PolyhedralBoundedSolidEdge e,
        _PolyhedralBoundedSolidFace f,
        int BvsA)
    {
        _PolyhedralBoundedSolidVertex v1, v2;
        double d1, d2, t;
        Vector3D p;
        int s1, s2, cont;

        v1 = e.rightHalf.startingVertex;
        v2 = e.leftHalf.startingVertex;
        d1 = f.containingPlane.pointDistance(v1.position);
        d2 = f.containingPlane.pointDistance(v2.position);
        s1 = PolyhedralBoundedSolid.compareValue(d1, 0.0, VSDK.EPSILON);
        s2 = PolyhedralBoundedSolid.compareValue(d2, 0.0, VSDK.EPSILON);

        if ( (s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1) ) {
            t = d1 / (d1 - d2);
            p = v1.position.add(
                    (v2.position.substract(
                         v1.position)).multiply(t));
            cont = f.testPointInside(p, VSDK.EPSILON);
            if ( cont < 0 ) {
                f.parentSolid.lmev(e.rightHalf, e.leftHalf.next(),
                                   f.parentSolid.getMaxVertexId()+1, p);
                //sonvv();
                System.out.println("Something rare is going on!");
                //processEdge(e.rightHalf.previous().parentEdge, f.parentSolid, BvsA);
            }
        }
        else {
            if ( s1 == 0 ) {
                //doVertexOnFace(v1, f, BvsA);
            }
            if ( s2 == 0 ) {
                //doVertexOnFace(v2, f, BvsA);
            }
        }
    }

    /**
    Following program [MANT1988].15.2.
    */
    private static void processEdge(_PolyhedralBoundedSolidEdge e,
                                    PolyhedralBoundedSolid s,
                                    int BvsA)
    {
        _PolyhedralBoundedSolidFace f;
        int i;

        for ( i = 0; i < s.polygonsList.size(); i++ ) {
            f = s.polygonsList.get(i);
            doSetOpGenerate(e, f, BvsA);
        }
    }

    /**
    Following program [MANT1988].15.2.
    */
    private static void setOpGenerate(PolyhedralBoundedSolid inSolidA,
                                      PolyhedralBoundedSolid inSolidB)
    {
        _PolyhedralBoundedSolidEdge e;

        sonvv = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex>();
        sonva = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>();
        sonvb = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>();

        int i;

        for ( i = 0; i < inSolidA.edgesList.size(); i++ ) {
            e = inSolidA.edgesList.get(i);
            processEdge(e, inSolidB, 0);
        }
        for ( i = 0; i < inSolidB.edgesList.size(); i++ ) {
            e = inSolidB.edgesList.get(i);
            processEdge(e, inSolidA, 1);
        }
    }

    /**
    Following program [MANT1988].15.5.
    */
    private static void setOpClassify(int op)
    {
        System.out.println("setOpClassify");
    }

    /**
    Following program [MANT1988].15.14.
    */
    private static void setOpConnect()
    {
        System.out.println("setOpConnect");
    }

    /**
    Following program [MANT1988].15.15.
    */
    private static void setOpFinish(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op
    )
    {
        System.out.println("setOpFinish");
    }

    /**
    Following program [MANT1988].15.1.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        //-----------------------------------------------------------------
        PolyhedralBoundedSolid res = new PolyhedralBoundedSolid();

        sonea = new ArrayList<_PolyhedralBoundedSolidEdge>();
        soneb = new ArrayList<_PolyhedralBoundedSolidEdge>();
        sonfa = new ArrayList<_PolyhedralBoundedSolidFace>();
        sonfb = new ArrayList<_PolyhedralBoundedSolidFace>();

        //-----------------------------------------------------------------
        inSolidA.validateModel();
        inSolidB.validateModel();
        updmaxnames(inSolidB, inSolidA);
        //setOpGenerate(inSolidA, inSolidB);
        setOpClassify(op);
        if ( sonea.size() == 0 ) {
            // No intersections found
            System.out.println("Empty sonea.");
            return inSolidA;
        }
        setOpConnect();
        setOpFinish(inSolidA, inSolidB, res, op);

        return res;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
