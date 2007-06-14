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
        return 0.1 * factor * Math.sin(0.5*percent*Math.PI);
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
        factor = v.length()/2;
        v.normalize();
        u = v.crossProduct(n);
        double delta = factor/((double)N);

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
/*
        gl.glTranslated(P.x, P.y, P.z);
        gl.glRotated(20, n.x, n.y, n.z);
        gl.glTranslated(-P.x, -P.y, -P.z);
*/
        Vector3D Pi = u.multiply(factor*0.1);
        gl.glTranslated(Pi.x, Pi.y, Pi.z);

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

    private static void setColor(GL gl, int i)
    {
        double r = 0.0, g = 0.0, b = 0.0;
        switch ( i%8 ) {
          case 0:  r = 1; g = 0; b = 0; break;
          case 1:  r = 0; g = 1; b = 0; break;
          case 2:  r = 0; g = 0; b = 1; break;
          case 3:  r = 0; g = 1; b = 1; break;
          case 4:  r = 1; g = 0; b = 1; break;
          case 5:  r = 0.5; g = 0; b = 0; break;
          case 6:  r = 0; g = 0.5; b = 0; break;
          default: r = 1; g = 1; b = 1; break;
        }
        gl.glColor3d(r, g, b);
    }

    public static void
    draw(GL gl, PolyhedralBoundedSolid solid,
         Camera c, RendererConfiguration q, int faceIndex)
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
        Vector3D p0 = null, p1 = null, p2 = null, n, a, b;
        _PolyhedralBoundedSolidHalfEdge hePrev;
        InfinitePlane loopPlane;

        //-----------------------------------------------------------------
        System.out.println("= PRINTING THE MAIN SOLID STRUCTURE ===========================================");
        System.out.println("Solid with " + 
            solid.verticesList.size() + " vertices:");
        for ( i = 0; i < solid.verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v;
            v = solid.verticesList.get(i);
            System.out.println("  - " + v);
        }

        System.out.println("Solid with " +
                           solid.edgesList.size() + " edges:");
        for ( i = 0; i < solid.edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = solid.edgesList.get(i);
            System.out.println("  - Edge " + i + ": " + e);
        }
        System.out.println("Solid with " + 
            solid.polygonsList.size() + " faces:");

        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            System.out.println("  - " + face);
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;

                System.out.println("    . Loop " + j + ", with halfedges: ");
                loop = face.boundariesList.get(j);


                System.out.println("HeID | StartVertex | End Vertex | nccw He | pccwHe");
                System.out.println("-----+-------------+------------+---------+-------");

                he = loop.boundaryStartHalfEdge;
                heStart = he;
                do {
                    he = he.next();
                    if ( he == null ) {
                        // Loop is not closed!
                        System.out.println("  - (not closed loop)");
                        break;
                    }

                    System.out.printf("%4d | %11d | %10d | %7d | %6d",
                        he.id, he.startingVertex.id,
                        he.next().startingVertex.id,
                        he.next().id, he.previous().id);
                    System.out.println("");

                } while( he != heStart );
            }
        }

        //-----------------------------------------------------------------
        loopPlane =
            new InfinitePlane(new Vector3D(0, 0, 1), new Vector3D(0, 0, 0));
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);

            if ( i != faceIndex ) {
                //continue;
            }

            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;

                loop = face.boundariesList.get(j);
                he = loop.boundaryStartHalfEdge;
                heStart = he;
                do {
                    // Logic
                    he = he.next();
                    if ( he == null ) {
                        // Loop is not closed!
                        break;
                    }

                    // Draw halfedges
                    if ( i == faceIndex ) {
                        gl.glLineWidth(2);
                    }
                    else {
                        gl.glLineWidth(1);
                    }
                    setColor(gl, i);
                    drawHalfEdge(gl, he, he.startingVertex.position,
                                 he.next().startingVertex.position,
                                 loopPlane) ;

                    // Draw edges (repeated!, this could be done better)
                    gl.glLineWidth(1);
            gl.glColor3d(0, 0, 0);
            gl.glBegin(gl.GL_LINES);
            gl.glVertex3d(he.startingVertex.position.x,
                  he.startingVertex.position.y,
                  he.startingVertex.position.z);
            gl.glVertex3d(he.next().startingVertex.position.x,
                  he.next().startingVertex.position.y,
                  he.next().startingVertex.position.z);
            gl.glEnd();
                } while( he != heStart );
            }
        }
        //-----------------------------------------------------------------
/*
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);

            if ( i == faceIndex ) {
                gl.glLineWidth(2);
            }
            else {
                gl.glLineWidth(1);
                continue;
            }

            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                loop = face.boundariesList.get(j);
                _PolyhedralBoundedSolidHalfEdge he, heStart;


                loopPlane = new InfinitePlane(new Vector3D(0, 0, 1), new Vector3D(0, 0, 0));
                //- First pass: determine face plane -------------------------
                int k = 0;
                he = loop.boundaryStartHalfEdge;
                heStart = he;
                do {
                    //-----
                    switch ( k ) {
                      case 0: p0 = he.startingVertex.position; break;
                      case 1: p1 = he.startingVertex.position; break;
                      case 2: p2 = he.startingVertex.position; break;
                    }
                    //-----
                    he = he.nextSameLoop();
                    if ( he == null ) {
                        // Loop is not closed!
                        System.out.println("  - (not closed loop)");
                        break;
                    }
                    System.out.println("      . HE " + he.id);
                    k++;
                } while( he != heStart );
                if ( k >= 3 ) {
                    a = p1.substract(p0);    a.normalize();
                    b = p2.substract(p0);    b.normalize();
                    n = a.crossProduct(b);   n.normalize();
                    loopPlane = new InfinitePlane(n, p0);
                }

                //- Second pass: draw face -----------------------------------
                he = loop.boundaryStartHalfEdge;
                heStart = he;
                hePrev = null;
                do {
                    //-----
                    he = he.nextSameLoop();
                    if ( he == null ) {
                        // Loop is not closed!
                        break;
                    }
                    if ( hePrev != null ) {
                        drawHalfEdge(gl, hePrev,
                                     hePrev.startingVertex.position,
                                     he.startingVertex.position, loopPlane);
                    }
                    hePrev = he;
                    k++;
                } while( he != heStart );
                if ( he != null ) {
                    he = he.nextSameLoop();
                    drawHalfEdge(gl, hePrev,
                                 hePrev.startingVertex.position,
                                 he.startingVertex.position, loopPlane);
                }
            }
        }

        //-----------------------------------------------------------------
        loopPlane = new InfinitePlane(new Vector3D(0, 0, 1), new Vector3D(0, 0, 0));
        for ( i = 0; i < solid.edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = solid.edgesList.get(i);
            startP = e.leftHalf.startingVertex.position;
            endP = e.rightHalf.startingVertex.position;
            //gl.glLineWidth(1.0f);
            //drawHalfEdge(gl, e.leftHalf, startP, endP, loopPlane);

            //if ( selectedHe == e.rightHalf ) {
            //    gl.glLineWidth(2.0f);
            //  }
            //  else {
            //    gl.glLineWidth(1.0f);
            //}
            //drawHalfEdge(gl, e.rightHalf, endP, startP, loopPlane);
            gl.glLineWidth(3.0f);
            gl.glColor3d(0, 0, 0);
            gl.glBegin(gl.GL_LINES);
                gl.glVertex3d(startP.x, startP.y, startP.z);
                gl.glVertex3d(endP.x, endP.y, endP.z);
            gl.glEnd();
        }
*/
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
