//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 21 2007 - Oscar Chavarro: Original base version                 =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//===========================================================================

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;

public class PolyhedralBoundedSolidModelingTools
{
    /**    
    */
    public static PolyhedralBoundedSolid createBox(Vector3D boxSize)
    {
        PolyhedralBoundedSolid solid;

        Matrix4x4 R = new Matrix4x4();
        R.translation(0.55, 0.55, 0.55);

        Box b = new Box(boxSize);
        solid = b.exportToPolyhedralBoundedSolid();
        solid.applyTransformation(R);
        solid.validateModel();
        return solid;
    }

    /**    
    */
    public static PolyhedralBoundedSolid createSphere(double r)
    {
        PolyhedralBoundedSolid solid;

        Matrix4x4 R = new Matrix4x4();
        R.translation(0.55, 0.55, 0.55);

        Sphere s = new Sphere(r);
        solid = s.exportToPolyhedralBoundedSolid();
        solid.applyTransformation(R);
        solid.validateModel();
        return solid;
    }

    /**
    PRE:
    Works on the output of `createBox` method, for a box from <0.1, 0.1, 0.1>
    to <1, 1, 1>
    */
    public static void extrudeBox(PolyhedralBoundedSolid solid)
    {
        //- Cube modification to holed box --------------------------------
        solid.smev(6, 5, 9, new Vector3D(0.3, 0.3, 1));
        solid.kemr(6, 6, 5, 9, 9, 5);
        solid.smev(6, 9, 10, new Vector3D(0.8, 0.3, 1));
        solid.smev(6, 10, 11, new Vector3D(0.8, 0.8, 1));
        solid.smev(6, 11, 12, new Vector3D(0.3, 0.8, 1));
        solid.mef(6, 6, 9, 10, 12, 11, 7);

        //- Box extrusion -------------------------------------------------
        solid.smev(7, 9, 13, new Vector3D(0.3, 0.3, 0.1));
        solid.smev(7, 10, 14, new Vector3D(0.8, 0.3, 0.1));
        solid.mef(7, 7, 13, 9, 14, 10, 8);
        solid.smev(7, 11, 15, new Vector3D(0.8, 0.8, 0.1));
        solid.mef(7, 7, 14, 10, 15, 11, 9);
        solid.smev(7, 12, 16, new Vector3D(0.3, 0.8, 0.1));
        solid.mef(7, 7, 15, 11, 16, 12, 10);
        solid.mef(7, 7, 13, 14, 16, 12, 11);
        solid.validateModel();
    }

    /**
    This method implements the example presented in section [MANT1988].9.3,
    and figure [MANT1988].9.11.
    */
    public static PolyhedralBoundedSolid createHoledBox()
    {
        PolyhedralBoundedSolid solid;

        solid = PolyhedralBoundedSolidModelingTools.createBox(
                new Vector3D(0.9, 0.9, 0.9));
        PolyhedralBoundedSolidModelingTools.extrudeBox(solid);
        solid.kfmrhSameShell(2, 11);
        //R.translation(-0.55, -0.55, -0.55);
        //solid.applyTransformation(R);
        solid.validateModel();

        return solid;
    }

    /**
    This method implements the example presented in section [MANT1988].12.2,
    and figure [MANT1988].12.1.
    Generate an arc based on the radius and the coordinates of the center on
    the plane z=h. This method assumes that the first vertex of the arc
    already exists, so its identifier (vertexId) must be supplied.
    This method generates an approximation of a circular arc segment with
    `n` edges, centered at <cx, cy, h>, on the plane z=h, and with radius
    `rad`. The arc ranges from angle `phi1` to `phi2`, measured in degrees,
    where an angle of 0.0 degrees equals the x-axis and angles grow
    counterclockwise. The arc starts from existing vertex `vertexId` of face
    `faceId`.
    */
    public static void addArc(PolyhedralBoundedSolid solid,
        int faceId, int vertexId,
        double cx, double cy, double rad, double h, double phi1, double phi2,
        int n)
    {
        double x, y, angle, inc;
        int prev, i, nextVertexId;

        angle = Math.toRadians(phi1);
        inc = Math.toRadians(((phi2 - phi1) / ((double)n)));
        prev = vertexId;
        for ( i = 0; i < n; i++ ) {
            angle += inc;
            x = cx + rad * Math.cos(angle);
            y = cy + rad * Math.sin(angle);
            nextVertexId = solid.getMaxVertexId() + 1;
            solid.smev(faceId, prev, nextVertexId, new Vector3D(x, y, h));
            prev = nextVertexId;
        }
        solid.validateModel();
    }

    /**
    This method implements the example presented in section [MANT1988].12.2,
    and figure [MANT1988].12.2.
    */
    public static PolyhedralBoundedSolid createCircle(
        double cx, double cy, double rad, double h, int n)
    {
        PolyhedralBoundedSolid solid;

        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(cx + rad, cy, h), 1, 1);
        addArc(solid, 1, 1, cx, cy, rad, h, 0, 
            ((double)(n-1))*360.0/((double)n), n-1);
        solid.smef(1, n, 1, 2);
        solid.validateModel();
        return solid;
    }

    /**
    This method implements a generalized/extended version of the example
    presented in section [MANT1988].12.3.2, and figure [MANT1988].12.3. In the
    original example, a displacement vector is added (translated) to
    the extruded point. Current implementation allows translations, scales
    and rotations in terms of original face plane.
    */
    public static void translationalSweepExtrudeFace(
        PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidFace face,
        Matrix4x4 T)
    {
        _PolyhedralBoundedSolidLoop l;
        _PolyhedralBoundedSolidHalfEdge first, scan;
        _PolyhedralBoundedSolidVertex v;
        Vector3D newPos;
        int i;

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            l = face.boundariesList.get(i);
            first = l.boundaryStartHalfEdge;
            scan = first.next();
            v = scan.startingVertex;
            newPos = T.multiply(v.position);
            solid.lmev(scan, scan, solid.getMaxVertexId()+1, newPos);
            while ( scan != first ) {
                v = scan.next().startingVertex;
                newPos = T.multiply(v.position);
                solid.lmev(scan.next(), scan.next(), 
                    solid.getMaxVertexId()+1, newPos);
                solid.lmef(scan.previous(), scan.next().next(),
                    solid.getMaxFaceId()+1);
                scan = (scan.next().mirrorHalfEdge()).next();
            }
            solid.lmef(scan.previous(), scan.next().next(),
                solid.getMaxFaceId()+1);
        }
        solid.validateModel();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
