//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.render.jogl;

import java.util.ArrayList;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

public class JoglPolyhedralBoundedSolidFaceRenderer extends JoglRenderer
{
    private static GLU glu;
    private static _JoglPolygonTesselatorRoutines tesselatorProcessor;
    static {
        tesselatorProcessor = null;
    }

    private static double
    curveFactor(double param, double factor)
    {
        double percent = param / factor;
        return 0.1 * factor * Math.sin(0.5 * percent * Math.PI);
    }

    /**
    Invert is 1.0 for first loop, -1.0 for remaining loops
    */
    private static void
    drawHalfEdge(GL2 gl, _PolyhedralBoundedSolidHalfEdge he,
                 Vector3D startP, Vector3D endP, InfinitePlane loopPlane,
                 double invert)
    {
        // Algorithm parameters
        int N = 10;

        // Local variables
        Vector3D u, v;
        double factor, t;
        int i;
        Vector3D P, n;

        n = loopPlane.getNormal();
        v = endP.subtract(startP);
        factor = v.length() / 2;
        v = v.normalized();
        u = v.crossProduct(n);
        double delta = factor / ((double)N);

        gl.glPushMatrix();

        gl.glBegin(GL.GL_LINES);
            for ( i = 0, t = 0; i < N; i++, t += delta ) {
                P = startP.add(v.multiply(t).add(u.multiply(invert *
                    curveFactor(t, factor))));
                gl.glVertex3d(P.x(), P.y(), P.z());
                P = startP.add(v.multiply(t + delta).add(u.multiply(invert *
                    curveFactor(t + delta, factor))));
                gl.glVertex3d(P.x(), P.y(), P.z());
            }
        gl.glEnd();
        P = startP.add(v.multiply(factor).add(u.multiply(0)));
        Vector3D Pi = u.multiply(invert * factor * 0.1);
        gl.glTranslated(Pi.x(), Pi.y(), Pi.z());

        gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(P.x(), P.y(), P.z());
            P = startP.add(v.multiply(factor * 0.9).add(u.multiply(factor *
                0.05)));
            gl.glVertex3d(P.x(), P.y(), P.z());

            P = startP.add(v.multiply(factor).add(u.multiply(0)));
            gl.glVertex3d(P.x(), P.y(), P.z());
            P = startP.add(v.multiply(factor * 0.9).add(u.multiply((-factor) *
                0.05)));
            gl.glVertex3d(P.x(), P.y(), P.z());

        gl.glEnd();

        gl.glPopMatrix();
    }

    private static void setColor(GL2 gl, int i)
    {
        double r;
        double g;
        double b;
        switch ( i % 8 ) {
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

    private static boolean shouldDrawFaceAsBoundaryOnly(
        _PolyhedralBoundedSolidFace face)
    {
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext;
        ArrayList<Vector3D> points;

        numericContext = PolyhedralBoundedSolidNumericPolicy.forFace(face);
        points = PolyhedralBoundedSolidGeometricValidator.extractPointsFromFace(
            face);
        if ( points == null ||
             !PolyhedralBoundedSolidGeometricValidator.
                 validateFacePointsAreCoplanar(points, numericContext) ) {
            return true;
        }
        if ( faceArea(face) <= numericContext.bigEpsilon() *
             numericContext.bigEpsilon() ) {
            return true;
        }
        return hasCloseNonAdjacentEdges(face, numericContext);
    }

    private static double faceArea(_PolyhedralBoundedSolidFace face)
    {
        double area;
        int i;

        area = 0.0;
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop;
            _PolyhedralBoundedSolidHalfEdge he;
            _PolyhedralBoundedSolidHalfEdge heStart;
            Vector3D vectorArea;

            loop = face.boundariesList.get(i);
            he = loop.boundaryStartHalfEdge;
            if ( he == null ) {
                continue;
            }
            vectorArea = new Vector3D();
            heStart = he;
            do {
                _PolyhedralBoundedSolidHalfEdge next;
                next = he.next();
                if ( next == null ) {
                    break;
                }
                vectorArea = vectorArea.add(
                    he.startingVertex.position.crossProduct(
                        next.startingVertex.position));
                he = next;
            } while ( he != heStart );
            area += 0.5 * vectorArea.length();
        }
        return area;
    }

    private static boolean hasCloseNonAdjacentEdges(
        _PolyhedralBoundedSolidFace face,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        ArrayList<FaceSegment> segments;
        double tolerance;
        int i;
        int j;

        segments = collectFaceSegments(face);
        tolerance = Math.max(numericContext.bigEpsilon() * 10.0,
            numericContext.modelScale() * 1.0e-5);
        for ( i = 0; i < segments.size(); i++ ) {
            for ( j = i + 1; j < segments.size(); j++ ) {
                FaceSegment a = segments.get(i);
                FaceSegment b = segments.get(j);
                if ( a.sharesEndpointWith(b, numericContext.bigEpsilon()) ) {
                    continue;
                }
                if ( segmentDistance(a.start, a.end, b.start, b.end) <=
                     tolerance ) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ArrayList<FaceSegment> collectFaceSegments(
        _PolyhedralBoundedSolidFace face)
    {
        ArrayList<FaceSegment> segments;
        int i;

        segments = new ArrayList<FaceSegment>();
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop;
            _PolyhedralBoundedSolidHalfEdge he;
            _PolyhedralBoundedSolidHalfEdge heStart;

            loop = face.boundariesList.get(i);
            he = loop.boundaryStartHalfEdge;
            if ( he == null ) {
                continue;
            }
            heStart = he;
            do {
                _PolyhedralBoundedSolidHalfEdge next;
                next = he.next();
                if ( next == null ) {
                    break;
                }
                segments.add(new FaceSegment(he.startingVertex.position,
                    next.startingVertex.position));
                he = next;
            } while ( he != heStart );
        }
        return segments;
    }

    private static double segmentDistance(Vector3D p1, Vector3D q1,
                                          Vector3D p2, Vector3D q2)
    {
        Vector3D d1;
        Vector3D d2;
        Vector3D r;
        double a;
        double e;
        double f;
        double s;
        double t;
        double c;
        double b;
        double denom;

        d1 = q1.subtract(p1);
        d2 = q2.subtract(p2);
        r = p1.subtract(p2);
        a = d1.dotProduct(d1);
        e = d2.dotProduct(d2);
        f = d2.dotProduct(r);

        if ( a <= VSDK.EPSILON && e <= VSDK.EPSILON ) {
            return p1.subtract(p2).length();
        }
        if ( a <= VSDK.EPSILON ) {
            s = 0.0;
            t = clamp(f / e, 0.0, 1.0);
        }
        else {
            c = d1.dotProduct(r);
            if ( e <= VSDK.EPSILON ) {
                t = 0.0;
                s = clamp(-c / a, 0.0, 1.0);
            }
            else {
                b = d1.dotProduct(d2);
                denom = a * e - b * b;
                if ( denom != 0.0 ) {
                    s = clamp((b * f - c * e) / denom, 0.0, 1.0);
                }
                else {
                    s = 0.0;
                }
                t = (b * s + f) / e;
                if ( t < 0.0 ) {
                    t = 0.0;
                    s = clamp(-c / a, 0.0, 1.0);
                }
                else if ( t > 1.0 ) {
                    t = 1.0;
                    s = clamp((b - c) / a, 0.0, 1.0);
                }
            }
        }

        return p1.add(d1.multiply(s)).subtract(p2.add(d2.multiply(t))).length();
    }

    private static double clamp(double value, double min, double max)
    {
        if ( value < min ) {
            return min;
        }
        if ( value > max ) {
            return max;
        }
        return value;
    }

    private static void drawSuspiciousFaceBoundary(GL2 gl,
        _PolyhedralBoundedSolidFace face)
    {
        int i;

        gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT |
            GL2.GL_LINE_BIT | GL2.GL_LIGHTING_BIT | GL2.GL_TEXTURE_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glLineWidth(5.0f);
        gl.glColor3d(1.0, 1.0, 0.0);

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop;
            _PolyhedralBoundedSolidHalfEdge he;
            _PolyhedralBoundedSolidHalfEdge heStart;

            loop = face.boundariesList.get(i);
            he = loop.boundaryStartHalfEdge;
            if ( he == null ) {
                continue;
            }
            heStart = he;
            gl.glBegin(GL.GL_LINES);
            do {
                _PolyhedralBoundedSolidHalfEdge next;
                next = he.next();
                if ( next == null ) {
                    break;
                }
                gl.glVertex3d(he.startingVertex.position.x(),
                    he.startingVertex.position.y(),
                    he.startingVertex.position.z());
                gl.glVertex3d(next.startingVertex.position.x(),
                    next.startingVertex.position.y(),
                    next.startingVertex.position.z());
                he = next;
            } while ( he != heStart );
            gl.glEnd();
        }

        gl.glPopAttrib();
    }

    public static void
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
            if ( shouldDrawFaceAsBoundaryOnly(face) ) {
                drawSuspiciousFaceBoundary(gl, face);
                continue;
            }
            if ( face.getContainingPlane() != null ) {
                n = face.getContainingPlane().getNormal();
                gl.glNormal3d(n.x(), n.y(), n.z());
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

            tessellateFace(gl, face, totalNumberOfPoints);
        }
    }

    private static void tessellateFace(GL2 gl, _PolyhedralBoundedSolidFace face,
                                       int totalNumberOfPoints)
    {
        GLUtessellator tesselator;
        int count;
        int j;
        double list[][]; // JOGL GLU Tesselator needs a vertex memory
        Vector3D p0;

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
                list[count][0] = p0.x();
                list[count][1] = p0.y();
                list[count][2] = p0.z();
                GLU.gluTessVertex(tesselator, list[count], 0, list[count]);
                count++;

            } while( he != heStart );

            GLU.gluTessEndContour(tesselator);
        }

        GLU.gluTessEndPolygon(tesselator);
        GLU.gluDeleteTess(tesselator);
    }

    private static final class FaceSegment
    {
        private final Vector3D start;
        private final Vector3D end;

        private FaceSegment(Vector3D start, Vector3D end)
        {
            this.start = start;
            this.end = end;
        }

        private boolean sharesEndpointWith(FaceSegment other, double tolerance)
        {
            return start.subtract(other.start).length() <= tolerance ||
                start.subtract(other.end).length() <= tolerance ||
                end.subtract(other.start).length() <= tolerance ||
                end.subtract(other.end).length() <= tolerance;
        }
    }

    public static void
    drawDebugFaceBoundary(GL2 gl, PolyhedralBoundedSolid solid, int faceIndex)
    {
        int i, j;
        Vector3D startP, endP;
        Vector3D p0;
        Vector3D p1;
        Vector3D p2;
        Vector3D a;
        Vector3D b;
        Vector3D n;
        InfinitePlane loopPlane;

        gl.glDisable(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth(1.0f);

        //- Draw face boundaries, one for each loop -----------------------
        for ( i = 0; faceIndex >= -1 && i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            //n = face.getContainingPlane().getNormal();

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
                    a = p1.subtract(p0);    a = a.normalized();
                    b = p2.subtract(p0);    b = b.normalized();
                    n = a.crossProduct(b);   n = n.normalized();
                    loopPlane = new InfinitePlane(n, p0);
                    //loopPlane = face.getContainingPlane();

                    // Draw halfedges
                    if ( i == faceIndex ) {
                        gl.glLineWidth(2);
                    }
                    else {
                        gl.glLineWidth(1);
                    }
                    setColor(gl, i);
                    if ( j == 0 ) {
                        drawHalfEdge(gl, he, p0, p1, loopPlane, 1.0);
                    }
                    else {
                        drawHalfEdge(gl, he, p0, p1, loopPlane, -1.0);
                    }
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

        //- Draw solid faces one by one -----------------------------------
        gl.glCullFace(GL.GL_BACK);
        gl.glEnable(GL.GL_CULL_FACE);
        //gl.glPolygonOffset(-1.0f, 1.0f);

        //-----------------------------------------------------------------
        Vector3D n;
        for ( i = faceIndex;
              i >= 0 && i == faceIndex && i < solid.polygonsList.size();
              i++ ) {
            // Logic
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            if ( shouldDrawFaceAsBoundaryOnly(face) ) {
                drawSuspiciousFaceBoundary(gl, face);
                continue;
            }
            if ( face.getContainingPlane() != null ) {
                n = face.getContainingPlane().getNormal();
                gl.glNormal3d(n.x(), n.y(), n.z());
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

            tessellateFace(gl, face, totalNumberOfPoints);
        }
    }
}
