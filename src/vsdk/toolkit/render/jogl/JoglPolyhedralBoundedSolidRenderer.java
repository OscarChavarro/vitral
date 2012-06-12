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

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.media.RGBAImage;

public class JoglPolyhedralBoundedSolidRenderer extends JoglRenderer
{
    private static GLU glu;
    private static _JoglPolygonTesselatorRoutines tesselatorProcessor;
    static {
        tesselatorProcessor = null;
    }

    private static double
    curveFactor(double param, double factor)
    {
        double percent = param/factor;
        return 0.1 * factor * Math.sin(0.5*percent*Math.PI);
    }

    private static void
    drawHalfEdge(GL2 gl, _PolyhedralBoundedSolidHalfEdge he, 
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

        gl.glBegin(GL.GL_LINES);
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

        gl.glBegin(GL.GL_LINES);
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

    private static void setColor(GL2 gl, int i)
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
          default: r = .6; g = .5; b = .4; break;
        }
        gl.glColor3d(r, g, b);
    }

    private static void
    drawPoints(GL2 gl, PolyhedralBoundedSolid solid)
    {
        ColorRgb c;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for point
        gl.glColor3d(1, 0, 0);
        gl.glPointSize(2.0f);

        int i;
        for ( i = 0; i < solid.verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v = solid.verticesList.get(i);
            Vector3D p = v.position;

            c = v.debugColor;
            if ( c.r >= 1-VSDK.EPSILON &&
                 c.g <= VSDK.EPSILON &&
                 c.b <= VSDK.EPSILON ) {
                gl.glPointSize(5.0f);
            }
            else {
                gl.glPointSize(15.0f);
            }
            gl.glColor3d(c.r, c.g, c.b);

            gl.glBegin(GL.GL_POINTS);
                gl.glVertex3d(p.x, p.y, p.z);
            gl.glEnd();
        }
    }

    private static void
    drawEdges(GL2 gl, PolyhedralBoundedSolid solid)
    {
        int i;
        ColorRgb c;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glShadeModel(GL2.GL_FLAT);

        // Warning: Change with configured color for borders
        gl.glLineWidth(1.0f);
        gl.glColor3d(1, 1, 1);

        gl.glDisable(GL.GL_TEXTURE_2D);

        for ( i = 0; i < solid.edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = solid.edgesList.get(i);
            int start, end;
            start = e.getStartingVertexId();
            end = e.getEndingVertexId();

            c = e.debugColor;
            if ( c.r >= 1-VSDK.EPSILON &&
                 c.g >= 1-VSDK.EPSILON &&
                 c.b >= 1-VSDK.EPSILON ) {
                gl.glLineWidth(1.0f);
            }
            else {
                gl.glLineWidth(6f);
            }
            gl.glColor3d(c.r, c.g, c.b);

            if ( start >= 0 && end >= 0 ) {
                Vector3D startPosition;
                Vector3D endPosition;
                startPosition = e.rightHalf.startingVertex.position;
                endPosition = e.leftHalf.startingVertex.position;
                if ( startPosition != null && endPosition != null ) {
                    gl.glBegin(GL.GL_LINES);
                    gl.glVertex3d(startPosition.x, startPosition.y, 
                                  startPosition.z);
                    gl.glVertex3d(endPosition.x, endPosition.y, endPosition.z);
                    gl.glEnd();
                }
            }
        }

    }

    private static void drawTextureString3D(GL2 gl, RGBAImage i)
    {
        gl.glPushAttrib(GL2.GL_ENABLE_BIT);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        // Set texture parameters
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_GENERATE_MIPMAP,
            GL2.GL_TRUE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
            GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
            GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
            GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
            GL2.GL_CLAMP_TO_EDGE);

        // Calling this configuration with GL_BLEND here generates an error on
        // some Windows Vista machines with Intel graphics, as such on
        // Dell Inspiron 1525 laptop with Mobile Intel 965 (BIOS 1566).
        gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);

        //float c[] = {1f, 1f, 1f, 1f};
        //gl.glTexEnvfv(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_COLOR, c, 0);
        gl.glTranslated(1, 1, 0); // gl.glRasterPos3d(0, 0, 0);, check draw...
        JoglImageRenderer.draw(gl, i);
        gl.glPopAttrib();
    }

    public static void
    drawDebugVertices(GL2 gl, PolyhedralBoundedSolid solid, Camera camera)
    {
        //-----------------------------------------------------------------
        double minmax[] = solid.getMinMax();
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glColor3d(1, 0, 0);

        Vector3D midpoint = new Vector3D();
        midpoint.x = (minmax[3] + minmax[0]) / 2;
        midpoint.y = (minmax[4] + minmax[1]) / 2;
        midpoint.z = (minmax[5] + minmax[2]) / 2;

        camera.updateVectors();
        Vector3D u = camera.getLeft().multiply(-1.0);
        Vector3D v = camera.getUp();
        u.normalize();
        v.normalize();

        //-----------------------------------------------------------------
        int i;
        RGBAImage label;

        for ( i = 0; i < solid.verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex ve = solid.verticesList.get(i);
            Vector3D p = ve.position;

            // Draw original vertex point
            //gl.glPointSize(8.0f);
            //gl.glColor3d(1, 1, 0);
            //gl.glBegin(GL.GL_POINTS);
            //    gl.glVertex3d(p.x, p.y, p.z);
            //gl.glEnd();

            // Explode point with respect to camera
            double perspectiveDistance;
            Vector3D delta = p.substract(midpoint);
            delta.normalize();
            double projected_u = u.dotProduct(delta);
            double projected_v = v.dotProduct(delta);
            Vector3D projected = u.multiply(projected_u).add(
                v.multiply(projected_v));
            if ( projected.length() > VSDK.EPSILON ) {
                projected.normalize();
            }
            perspectiveDistance = VSDK.vectorDistance(camera.getPosition(), p);
            projected = projected.multiply(perspectiveDistance/80.0);

            // Bring point a little nearest towards the camera
            Vector3D ep = p.add(projected);
            Vector3D deltaView = camera.getPosition().substract(ep);
            deltaView.normalize();
            deltaView = deltaView.multiply(0.01);
            ep = ep.add(deltaView);

            // Draw exploded reference point for label
            //gl.glPointSize(4.0f);
            //gl.glColor3d(0.9, 0.9, 0.5);
            //gl.glBegin(GL.GL_POINTS);
            //    gl.glVertex3d(ep.x, ep.y, ep.z);
            //gl.glEnd();

            // Draw vertex id label
            label = AwtSystem.calculateLabelImage("" + ve.id,
                new ColorRgb(0, 0, 0));

            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glTranslated(ep.x, ep.y, ep.z);
            drawTextureString3D(gl, label);
            gl.glPopMatrix();
        }
    }

    public static void
    drawDebugEdges(GL2 gl, PolyhedralBoundedSolid solid, Camera c, int edgeIndex)
    {
        int i;

        gl.glPushAttrib(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_DEPTH_TEST);

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glShadeModel(GL2.GL_FLAT);

        gl.glDisable(GL.GL_TEXTURE_2D);

        _PolyhedralBoundedSolidFace face1;
        _PolyhedralBoundedSolidFace face2;
        boolean f1, f2;

        for ( i = 0; edgeIndex >= -1 && i < solid.edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = solid.edgesList.get(i);

            if ( i != edgeIndex && edgeIndex > -1 ) {
                continue;
            }

            int start, end;
            start = e.getStartingVertexId();
            end = e.getEndingVertexId();
            if ( start >= 0 && end >= 0 ) {
                Vector3D startPosition;
                Vector3D endPosition;
                Vector3D middle;
                Vector3D n;

                startPosition = e.rightHalf.startingVertex.position;
                endPosition = e.leftHalf.startingVertex.position;
                if ( startPosition != null && endPosition != null ) {
                    //- Prepare data for visible line determination ----------
                    face1 = e.leftHalf.parentLoop.parentFace;
                    face2 = e.rightHalf.parentLoop.parentFace;
                    f1 = face1.isVisibleFrom(c) >= 0;
                    f2 = face2.isVisibleFrom(c) >= 0;

                    //- Determine if line is hidden, visible or contour ------
                    boolean isVisible = false;
                    if ( !f1 && !f2 ) {
                        // Totally hidden lines
                        gl.glLineWidth(1.0f);
                        gl.glColor3d(0, 0, 0);
                    }
                    else if ( f1 && !f2 || !f1 && f2 ) {
                        // Contour lines
                        gl.glLineWidth(4.0f);
                        gl.glColor3d(1, 0, 0);
                        isVisible = true;
                    }
                    else {
                        // Visible non contour lines
                        gl.glLineWidth(2.0f);
                        gl.glColor3d(0.8, 0, 0);
                        isVisible = true;
                    }

                    //- Draw line --------------------------------------------
                    gl.glBegin(GL.GL_LINES);
                    gl.glVertex3d(startPosition.x, startPosition.y, 
                                  startPosition.z);
                    gl.glVertex3d(endPosition.x, endPosition.y, endPosition.z);
                    gl.glEnd();

                    //- If debugging a single edge, draw extra information ---
                    if ( edgeIndex > -1 ) {
                        // Draw containing face normals at the edge middle point
                        middle = startPosition.add(endPosition).multiply(0.5);
                        n = face1.containingPlane.getNormal();
                        gl.glLineWidth(1.0f);
                        gl.glColor3d(1, 1, 0);
                        gl.glBegin(GL.GL_LINES);
                            gl.glVertex3d(middle.x, middle.y, middle.z);
                            gl.glVertex3d(middle.x + n.x/10,
                                          middle.y + n.y/10,
                                          middle.z + n.z/10);
                        gl.glEnd();
                        n = face2.containingPlane.getNormal();
                        gl.glLineWidth(1.0f);
                        gl.glColor3d(0, 1, 1);
                        gl.glBegin(GL.GL_LINES);
                            gl.glVertex3d(middle.x, middle.y, middle.z);
                            gl.glVertex3d(middle.x + n.x/10,
                                          middle.y + n.y/10,
                                          middle.z + n.z/10);
                        gl.glEnd();

                        // Draw som sample points for quantitative invisibility
                        Vector3D d;
                        Vector3D p;
                        double l;
                        double t;
                        int qi;

                        d = endPosition.substract(startPosition);
                        l = d.length();
                        d.normalize();

                        for ( t = VSDK.EPSILON; isVisible && t < l; t += (l/20) ) {
                            p = startPosition.add(d.multiply(t));
                            qi = solid.computeQuantitativeInvisibility(c.getPosition(), p);
                            switch ( qi ) {
                              case 0:
                                gl.glPointSize(4);
                                gl.glColor3f(0, 1, 0);
                                break;
                              default:
                                gl.glPointSize(2);
                                gl.glColor3f(0, 0, 1);
                                break;
                            }
                            gl.glBegin(GL.GL_POINTS);
                                gl.glVertex3d(p.x, p.y, p.z);
                            gl.glEnd();
                        }
                    }
                }
            }
        }
        gl.glPopAttrib();

    }

    private static void
    drawVertexNormals(GL2 gl, PolyhedralBoundedSolid solid)
    {
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for vertex normals
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);
        int i, j;

        //-----------------------------------------------------------------
        Vector3D p0 = null, n;
        Vertex vertex = new Vertex(p0);

        gl.glBegin(GL.GL_LINES);
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            // Logic
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            if ( face.containingPlane != null ) {
                vertex.normal = face.containingPlane.getNormal();
            }
            else {
                continue;
            }

            // Face polygon processing via JOGL GLU tesselator
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;

                loop = face.boundariesList.get(j);
                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    continue;
		}
                heStart = he;
                do {
                    // Logic
                    he = he.next();
                    if ( he == null ) {
                        // Loop is not closed!
                        break;
                    }

                    // Draw polygon parts
                    vertex.position = he.startingVertex.position;
                    JoglGeometryRenderer.drawVertexNormal(gl, vertex);
                } while( he != heStart );
            }
        }
        gl.glEnd();

    }

    private static void
    drawSurfaces(GL2 gl, PolyhedralBoundedSolid solid)
    {
        int i, j;

        //- Prepare tesselator --------------------------------------------
        if ( tesselatorProcessor == null ) {
            tesselatorProcessor = 
                new _JoglPolygonTesselatorRoutines(gl, glu);
        }

        GLUtessellator tesselator;

        //- Draw solid faces one by one -----------------------------------
        gl.glCullFace(GL.GL_BACK);
        gl.glEnable(GL.GL_CULL_FACE);

        //-----------------------------------------------------------------
        Vector3D p0, n;
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            // Logic
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            if ( face.containingPlane != null ) {
                n = face.containingPlane.getNormal();
                gl.glNormal3d(n.x, n.y, n.z);
            }

            // Count used vertex for current face
            int totalNumberOfPoints = 0;

            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;
                loop = face.boundariesList.get(j);
                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    continue;
		}
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

            tesselator = GLU.gluNewTess();
            list = new double[totalNumberOfPoints][3];
            count = 0;
            GLU.gluTessCallback(tesselator,
                GLU.GLU_TESS_VERTEX, tesselatorProcessor);
            GLU.gluTessCallback(tesselator,
                GLU.GLU_TESS_BEGIN, tesselatorProcessor);
            GLU.gluTessCallback(tesselator,
                GLU.GLU_TESS_END, tesselatorProcessor);
            GLU.gluTessCallback(tesselator,
                GLU.GLU_TESS_ERROR, tesselatorProcessor);
            GLU.gluTessBeginPolygon(tesselator, null);

            // Face polygon generation via JOGL GLU tesselator
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;

                GLU.gluTessBeginContour(tesselator);

                loop = face.boundariesList.get(j);
                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    continue;
		}

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
                    GLU.gluTessVertex(tesselator, list[count], 0, list[count]);
                    count++;

                } while( he != heStart );

                GLU.gluTessEndContour(tesselator);
            }

            GLU.gluTessEndPolygon(tesselator);
            GLU.gluDeleteTess(tesselator);
        }
    }

    public static void
    drawDebugFaceBoundary(GL2 gl, PolyhedralBoundedSolid solid, int faceIndex)
    {
        int i, j;
        Vector3D startP, endP;
        Vector3D p0 = null, p1 = null;
        Vector3D p2 = null, a, b;
        Vector3D n;
        _PolyhedralBoundedSolidHalfEdge hePrev;
        InfinitePlane loopPlane;

        gl.glDisable(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth(1.0f);

        //- Draw face boundaries, one for each loop -----------------------
        for ( i = 0; faceIndex >= -1 && i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            //n = face.containingPlane.getNormal();

            if ( i != faceIndex && faceIndex > -1 ) {
                continue;
            }

            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;

                loop = face.boundariesList.get(j);
                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    continue;
		}

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
                    //loopPlane = face.containingPlane;

                    // Draw halfedges
                    if ( i == faceIndex ) {
                        gl.glLineWidth(2);
                    }
                    else {
                        gl.glLineWidth(1);
                    }
                    setColor(gl, i);
                    drawHalfEdge(gl, he, p0, p1, loopPlane);
                } while( he != heStart );
            }
        }
    }

    public static void
    drawDebugFace(GL2 gl, PolyhedralBoundedSolid solid, int faceIndex)
    {
        //- Draw face boundaries, one for each loop -----------------------
        int i, j;

        //- Prepare tesselator --------------------------------------------
        if ( tesselatorProcessor == null ) {
            glu = new GLU();
            tesselatorProcessor = 
                new _JoglPolygonTesselatorRoutines(gl, glu);
        }

        GLUtessellator tesselator;

        //- Draw solid faces one by one -----------------------------------
        gl.glCullFace(GL.GL_BACK);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glPolygonOffset(-1.0f, 1.0f);

        //-----------------------------------------------------------------
        Vector3D p0, n;
        for ( i = faceIndex;
              i >= 0 && i == faceIndex && i < solid.polygonsList.size();
              i++ ) {
            // Logic
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            if ( face.containingPlane != null ) {
                n = face.containingPlane.getNormal();
                gl.glNormal3d(n.x, n.y, n.z);
            }

            // Count used vertex for current face
            int totalNumberOfPoints = 0;

            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;
                loop = face.boundariesList.get(j);
                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    continue;
		}
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

            tesselator = GLU.gluNewTess();
            list = new double[totalNumberOfPoints][3];
            count = 0;
            GLU.gluTessCallback(tesselator,
                GLU.GLU_TESS_VERTEX, tesselatorProcessor);
            GLU.gluTessCallback(tesselator,
                GLU.GLU_TESS_BEGIN, tesselatorProcessor);
            GLU.gluTessCallback(tesselator,
                GLU.GLU_TESS_END, tesselatorProcessor);
            GLU.gluTessCallback(tesselator,
                GLU.GLU_TESS_ERROR, tesselatorProcessor);
            GLU.gluTessBeginPolygon(tesselator, null);

            // Face polygon generation via JOGL GLU tesselator
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;

                GLU.gluTessBeginContour(tesselator);

                loop = face.boundariesList.get(j);
                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    continue;
		}
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
                    GLU.gluTessVertex(tesselator, list[count], 0, list[count]);
                    count++;

                } while( he != heStart );

                GLU.gluTessEndContour(tesselator);
            }

            GLU.gluTessEndPolygon(tesselator);
            GLU.gluDeleteTess(tesselator);
        }
    }

    /**
    PRE: Solid has been previously validated. This implies for example that
    all faces are planar and has its containing plane equation calculated, so
    they are correctly formed geometrical and topological entities.
    */
    public static void
    draw(GL2 gl, PolyhedralBoundedSolid solid,
         Camera c, RendererConfiguration quality)
    {
        //-----------------------------------------------------------------
        //JoglCgRenderer.disableNvidiaCgProfiles();
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_NORMALIZE);

        //-----------------------------------------------------------------
        if ( quality.isSurfacesSet() ) {
            JoglGeometryRenderer.prepareSurfaceQuality(gl, quality);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            gl.glEnable(GL2GL3.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(1.0f, 1.0f);
            drawSurfaces(gl, solid);
        }
        if ( quality.isWiresSet() ) {
            drawEdges(gl, solid);
        }
        if ( quality.isPointsSet() ) {
            drawPoints(gl, solid);
        }
        if ( quality.isNormalsSet() ) {
            drawVertexNormals(gl, solid);
        }
        if ( quality.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, solid, quality);
        }
        if ( quality.isSelectionCornersSet() ) {
            JoglGeometryRenderer.drawSelectionCorners(gl, solid, quality);
        }

        //-----------------------------------------------------------------
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
