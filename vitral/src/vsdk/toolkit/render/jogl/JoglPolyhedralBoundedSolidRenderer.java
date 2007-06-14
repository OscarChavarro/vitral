//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - January 3 2007 - Oscar Chavarro: Original base version                =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL clases
import javax.media.opengl.GL;
import com.sun.opengl.cg.CgGL;
import com.sun.opengl.cg.CGparameter;

// VitralSDK classes
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;

public class JoglPolyhedralBoundedSolidRenderer extends JoglRenderer
{

    private static double
    curveFactor(double param, double factor)
    {
        double percent = param/factor;
        return 0.1 * factor * Math.sin(percent*Math.PI);
    }

    public static void
    drawHalfEdge(GL gl, _PolyhedralBoundedSolidHalfEdge he, 
                 Vector3D startP, Vector3D endP, InfinitePlane loopPlane)
    {
        // Algorithm parameters
        int N = 10;

        // Local variables
        Vector3D u, v;
        double factor, t;
        int i;
        Vector3D P, n;

        n = loopPlane.getNormal();
        v = endP.substract(startP);
        factor = v.length();
        v.normalize();
        u = v.crossProduct(n);
        double delta = factor/((double)N);

        if ( he.parentEdge == null ) {
            gl.glColor3d(1, 1, 1);
          }
          else if ( he.parentEdge.rightHalf == he ) {
            gl.glColor3d(1, 0, 0);
          }
          else {
            gl.glColor3d(0, 0, 1);
          }
        ;

        gl.glPushMatrix();

        gl.glBegin(gl.GL_LINES);
            for ( i = 0, t = 0; i < N; i++, t += delta ) {
                P = startP.add(v.multiply(t).add(u.multiply(curveFactor(t, factor))));
                gl.glVertex3d(P.x, P.y, P.z);
                P = startP.add(v.multiply(t+delta).add(u.multiply(curveFactor(t+delta, factor))));
                gl.glVertex3d(P.x, P.y, P.z);
            }
        gl.glEnd();
        P = startP.add(v.multiply(factor).add(u.multiply(0)));
        gl.glTranslated(P.x, P.y, P.z);
        gl.glRotated(20, n.x, n.y, n.z);
        gl.glTranslated(-P.x, -P.y, -P.z);
        gl.glBegin(gl.GL_LINES);
            gl.glVertex3d(P.x, P.y, P.z);
            P = startP.add(v.multiply(factor*0.9).add(u.multiply(factor*0.05)));
            gl.glVertex3d(P.x, P.y, P.z);

            P = startP.add(v.multiply(factor).add(u.multiply(0)));
            gl.glVertex3d(P.x, P.y, P.z);
            P = startP.add(v.multiply(factor*0.9).add(u.multiply(-factor*0.05)));
            gl.glVertex3d(P.x, P.y, P.z);

        gl.glEnd();

        gl.glPopMatrix();
    }

    public static void
    draw(GL gl, PolyhedralBoundedSolid solid,
         Camera c, RendererConfiguration q)
    {
        int i, j;

        JoglRenderer.disableNvidiaCgProfiles();
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glColor3d(0, 0, 0);
        gl.glPointSize(8.0f);
        gl.glLineWidth(1.0f);

        gl.glBegin(gl.GL_POINTS);
        for ( i = 0; i < solid.verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v = solid.verticesList.get(i);
            Vector3D p = v.position;
            gl.glVertex3d(p.x, p.y, p.z);
        }
        gl.glEnd();

        Vector3D startP, endP;
        Vector3D p0, p1, p2, n, a, b;
        _PolyhedralBoundedSolidHalfEdge startHe;


        System.out.println("= PRINTING MAIN SOLID STRUCTURE ===========================================");
        System.out.println("Solid with " + 
            solid.polygonsList.size() + " faces:");
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            System.out.println("  - Face " + i + ": " + face);
        }

        System.out.println("Solid with " + 
            solid.halfEdgesList.size() + " half edges:");
        for ( i = 0; i < solid.halfEdgesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge he = solid.halfEdgesList.get(i);
            System.out.println("  - HalfEdge " + i + ": " + he);
        }

        System.out.println("- Printing drawing steps --------------------------------------------------");
        System.out.println("Solid with " + solid.polygonsList.size() + " faces");

        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);

            System.out.println("  - Face " + i + ", " + face.boundariesList.size() + " loops:");

            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                System.out.println("    . Loop " + j + ":");
                _PolyhedralBoundedSolidLoop loop;
                loop = face.boundariesList.get(j);
                _PolyhedralBoundedSolidHalfEdge headHe, he;
                headHe = loop.boundaryStartHalfEdge;

                //- Loop preprocessing ----------------------------------------
                // Count number of halfedges in current loop, and determine
                // plane equation

                int k = 0;
                he = headHe;
                do {
                    startP = he.startingVertex.position;
                    he = he.next();  k++;
                    System.out.println("      P " + k + ": " + startP);
                } while ( he != headHe );

                p0 = he.startingVertex.position;
                InfinitePlane loopPlane = new InfinitePlane(new Vector3D(0, 0, 1), p0);

                if ( k >= 3 ) {
                    p1 = he.next().startingVertex.position;
                    p2 = he.next().next().startingVertex.position;
                    a = p1.substract(p0);
                    b = p2.substract(p0);
                    a.normalize();
                    b.normalize();
                    n = a.crossProduct(b);
                    n.normalize();
                    loopPlane = new InfinitePlane(n, p0);
                }

                //- Half edge painting ----------------------------------------
                he = headHe;
                do {
                    startP = he.startingVertex.position;
                    startHe = he;
                    he = he.next();
                    endP = he.startingVertex.position;
                    drawHalfEdge(gl, startHe, startP, endP, loopPlane);
                } while ( he != headHe );

            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
