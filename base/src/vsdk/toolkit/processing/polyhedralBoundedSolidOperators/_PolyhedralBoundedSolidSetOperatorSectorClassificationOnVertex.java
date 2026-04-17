package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidOperator;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;

/**
This class is used to store vertex / halfedge neigborhood information for the
vertex/vertex classifier as proposed on section [MANT1988].15.5. and program
[MANT1988].15.6.
*/
public class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex
    extends PolyhedralBoundedSolidOperator
    implements Comparable<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>
{
    public _PolyhedralBoundedSolidHalfEdge he;
    public Vector3D ref1;
    public Vector3D ref2;
    public Vector3D ref12;
    public Vector3D referenceLine;
    public Vector3D referenceU;
    public Vector3D referenceV;
    public boolean wide;

    public _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex()
    {
        referenceLine = null;
        referenceU = null;
        referenceV = null;
    }

    public double getAngle()
    {
        if ( referenceLine == null || referenceU == null || referenceV == null ) {
            return -1000;
        }

        double x, y;
        double an;
        Vector3D a = ref1;

        if ( PolyhedralBoundedSolidSetOperator.colinearVectorsWithDirection(ref1, referenceLine) ) {
            a = ref2;
        }

        Vector3D u, v;

        u = new Vector3D(referenceU);
        u.normalize();
        v = new Vector3D(referenceV);
        v.normalize();
        a.normalize();

        x = a.dotProduct(u);
        y = a.dotProduct(v);

        an = Math.acos(x);
        if ( y < 0 ) an *= -1;

        return an;
    }

    public String toString()
    {
        String msg;

        msg = "R1: " + ref1 + " R2: " + ref2 + " HE " + he.startingVertex.id + "/" + he.next().startingVertex.id + (wide?"(W)":"(nw)");

        return msg;
    }

    public int compareTo(_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex other)
    {
        double a, b;

        a = this.getAngle();
        b = other.getAngle();

        if ( a > b) return 1;
        if ( a < b) return -1;
        return 0;
    }
}
