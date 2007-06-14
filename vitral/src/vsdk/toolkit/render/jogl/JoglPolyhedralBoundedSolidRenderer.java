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
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
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
    private static GLU glu;
    private static _JoglPolygonTesselatorRoutines tesselatorProcessor;
    static {
        glu = null;
        tesselatorProcessor = null;
    }

    private static double
    curveFactor(double param, double factor)
    {
        double percent = param/factor;
        return 0.1 * factor * Math.sin(0.5*percent*Math.PI);
    }

    private static void
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
          default: r = .5; g = .5; b = .5; break;
        }
        gl.glColor3d(r, g, b);
    }

    public static void
    draw(GL gl, PolyhedralBoundedSolid solid,
         Camera c, RendererConfiguration q, int faceIndex)
    {
        //-----------------------------------------------------------------
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

        //- Draw face boundaries, one for each loop -----------------------
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

                    // Calculate containing plane equation for current edge
                    p0 = he.startingVertex.position;
                    p1 = he.next().startingVertex.position;
                    p2 = he.next().next().startingVertex.position;
                    a = p1.substract(p0);    a.normalize();
                    b = p2.substract(p0);    b.normalize();
                    n = a.crossProduct(b);   n.normalize();
                    loopPlane = new InfinitePlane(n, p0);

                    // Draw halfedges
                    if ( i == faceIndex ) {
                        gl.glLineWidth(2);
                    }
                    else {
                        gl.glLineWidth(1);
                    }
                    setColor(gl, i);
                    drawHalfEdge(gl, he, p0, p1, loopPlane);

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

        //- Prepare tesselator --------------------------------------------
        //double list[] = new double [3];
        if ( tesselatorProcessor == null ) {
            glu = new GLU();
            tesselatorProcessor = 
                new _JoglPolygonTesselatorRoutines(gl, glu);
        }

        GLUtessellator tesselator;

        //- Draw solid faces one by one -----------------------------------
        gl.glCullFace(gl.GL_BACK);
        gl.glEnable(gl.GL_CULL_FACE);

        //-----------------------------------------------------------------
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            // Logic
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            //if ( i != faceIndex ) continue;
            setColor(gl, i);

            // Count used vertex for current face
            int totalNumberOfPoints = 0;

            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;
                loop = face.boundariesList.get(j);
                he = loop.boundaryStartHalfEdge;
                heStart = he;
                do {
                    // Logic
                    he = he.next();
                    if ( he == null  ) {
                        // Loop is not closed!
                        break;
                    }
                    // Counting
                    totalNumberOfPoints++;
                } while( he != heStart );
            }

            // Tesselator preparation for current face
            int count;
            double list[][]; // JOGL GLU Tesselator needs a vertex memory

            tesselator = glu.gluNewTess();
            list = new double[totalNumberOfPoints][3];
            count = 0;
            glu.gluTessCallback(tesselator,
                glu.GLU_TESS_VERTEX, tesselatorProcessor);
            glu.gluTessCallback(tesselator,
                glu.GLU_TESS_BEGIN, tesselatorProcessor);
            glu.gluTessCallback(tesselator,
                glu.GLU_TESS_END, tesselatorProcessor);
            glu.gluTessCallback(tesselator,
                glu.GLU_TESS_ERROR, tesselatorProcessor);
            glu.gluTessBeginPolygon(tesselator, null);

            // Face polygon generation via JOGL GLU tesselator
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;

                glu.gluTessBeginContour(tesselator);

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

                    // Draw polygon parts
                    p0 = he.startingVertex.position;
                    list[count][0] = p0.x;
                    list[count][1] = p0.y;
                    list[count][2] = p0.z;
                    glu.gluTessVertex(tesselator, list[count], 0, list[count]);
                    count++;

                } while( he != heStart );

                glu.gluTessEndContour(tesselator);
            }

            glu.gluTessEndPolygon(tesselator);
            glu.gluDeleteTess(tesselator);
        }

        //-----------------------------------------------------------------
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
