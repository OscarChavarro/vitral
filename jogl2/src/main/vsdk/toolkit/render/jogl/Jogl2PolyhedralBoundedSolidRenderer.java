//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;

public class Jogl2PolyhedralBoundedSolidRenderer extends Jogl2Renderer
{
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
            int start;
            int end;
            start = e.getStartingVertexId();
            end = e.getEndingVertexId();

            c = e.debugColor;
            if ( c.r >= 1 - VSDK.EPSILON &&
                 c.g >= 1 - VSDK.EPSILON &&
                 c.b >= 1 - VSDK.EPSILON ) {
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
                    gl.glVertex3d(startPosition.x(), startPosition.y(),
                                  startPosition.z());
                    gl.glVertex3d(endPosition.x(), endPosition.y(),
                                  endPosition.z());
                    gl.glEnd();
                }
            }
        }

    }

    public static void
    drawDebugVertices(GL2 gl,
                      PolyhedralBoundedSolid solid,
                      Camera camera,
                      RendererConfiguration quality)
    {
        Jogl2PolyhedralBoundedSolidVertexRenderer.drawDebugVertices(gl, solid,
            camera, quality);
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
        boolean f1;
        boolean f2;

        for ( i = 0; edgeIndex >= -1 && i < solid.edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = solid.edgesList.get(i);

            if ( i != edgeIndex && edgeIndex > -1 ) {
                continue;
            }

            int start;
            int end;
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
                    gl.glVertex3d(startPosition.x(), startPosition.y(),
                                  startPosition.z());
                    gl.glVertex3d(endPosition.x(), endPosition.y(),
                                  endPosition.z());
                    gl.glEnd();

                    //- If debugging a single edge, draw extra information ---
                    if ( edgeIndex > -1 ) {
                        // Draw containing face normals at the edge middle point
                        middle = startPosition.add(endPosition).multiply(0.5);
                        n = face1.getContainingPlane().getNormal();
                        gl.glLineWidth(1.0f);
                        gl.glColor3d(1, 1, 0);
                        gl.glBegin(GL.GL_LINES);
                            gl.glVertex3d(middle.x(), middle.y(), middle.z());
                            gl.glVertex3d(middle.x() + n.x() / 10,
                                          middle.y() + n.y() / 10,
                                          middle.z() + n.z() / 10);
                        gl.glEnd();
                        n = face2.getContainingPlane().getNormal();
                        gl.glLineWidth(1.0f);
                        gl.glColor3d(0, 1, 1);
                        gl.glBegin(GL.GL_LINES);
                            gl.glVertex3d(middle.x(), middle.y(), middle.z());
                            gl.glVertex3d(middle.x() + n.x() / 10,
                                          middle.y() + n.y() / 10,
                                          middle.z() + n.z() / 10);
                        gl.glEnd();

                        // Draw som sample points for quantitative invisibility
                        Vector3D d;
                        Vector3D p;
                        double l;
                        double t;
                        int qi;

                        d = endPosition.subtract(startPosition);
                        l = d.length();
                        d = d.normalized();

                        for ( t = VSDK.EPSILON;
                              isVisible && t < l;
                              t += (l / 20) ) {
                            p = startPosition.add(d.multiply(t));
                            qi = solid.computeQuantitativeInvisibility(
                                c.getPosition(), p);
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
                                gl.glVertex3d(p.x(), p.y(), p.z());
                            gl.glEnd();
                        }
                    }
                }
            }
        }
        gl.glPopAttrib();

    }

    public static void
    drawDebugFaceBoundary(GL2 gl, PolyhedralBoundedSolid solid, int faceIndex)
    {
        Jogl2PolyhedralBoundedSolidFaceRenderer.drawDebugFaceBoundary(gl, solid,
            faceIndex);
    }

    public static void
    drawDebugFace(GL2 gl, PolyhedralBoundedSolid solid, int faceIndex)
    {
        Jogl2PolyhedralBoundedSolidFaceRenderer.drawDebugFace(gl, solid,
            faceIndex);
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
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_NORMALIZE);

        //-----------------------------------------------------------------
        if ( quality.isSurfacesSet() ) {
            Jogl2GeometryRenderer.prepareSurfaceQuality(gl, quality);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            gl.glEnable(GL2GL3.GL_POLYGON_OFFSET_FILL);
            //gl.glPolygonOffset(1.0f, 1.0f);
            Jogl2PolyhedralBoundedSolidFaceRenderer.drawSurfaces(gl, solid);
        }
        if ( quality.isWiresSet() ) {
            drawEdges(gl, solid);
        }
        if ( quality.isPointsSet() ) {
            Jogl2PolyhedralBoundedSolidVertexRenderer.drawPoints(gl, solid);
        }
        if ( quality.isNormalsSet() ) {
            Jogl2PolyhedralBoundedSolidVertexRenderer.drawVertexNormals(gl,
                solid);
        }
        if ( quality.isBoundingVolumeSet() ) {
            Jogl2GeometryRenderer.drawMinMaxBox(gl, solid, quality);
        }
        if ( quality.isSelectionCornersSet() ) {
            Jogl2GeometryRenderer.drawSelectionCorners(gl, solid, quality);
        }

        //-----------------------------------------------------------------
    }
}
