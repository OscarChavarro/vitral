//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.render.jogl;

import java.util.ArrayList;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.media.RGBAImage;

public class JoglPolyhedralBoundedSolidVertexRenderer extends JoglRenderer
{
    private static final double LABEL_GROUPING_EPSILON_FACTOR = 1000.0;
    private static final double LABEL_GROUPING_CAMERA_PLANE_FACTOR = 0.005;

    public static void
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
            if ( c.r >= 1 - VSDK.EPSILON &&
                 c.g <= VSDK.EPSILON &&
                 c.b <= VSDK.EPSILON ) {
                gl.glPointSize(5.0f);
            }
            else {
                gl.glPointSize(15.0f);
            }
            gl.glColor3d(c.r, c.g, c.b);

            gl.glBegin(GL.GL_POINTS);
                gl.glVertex3d(p.x(), p.y(), p.z());
            gl.glEnd();
        }
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
        midpoint = midpoint.withX((minmax[3] + minmax[0]) / 2);
        midpoint = midpoint.withY((minmax[4] + minmax[1]) / 2);
        midpoint = midpoint.withZ((minmax[5] + minmax[2]) / 2);

        camera.updateVectors();
        Vector3D u = camera.getLeft().multiply(-1.0);
        Vector3D v = camera.getUp();
        u = u.normalized();
        v = v.normalized();

        //-----------------------------------------------------------------
        int i;
        ArrayList<VertexLabelGroup> vertexGroups;
        RGBAImage label;

        vertexGroups = buildVertexGroups(solid, camera, midpoint, u, v);

        for ( i = 0; i < vertexGroups.size(); i++ ) {
            VertexLabelGroup group = vertexGroups.get(i);
            Vector3D ep = group.labelPosition;

            // Draw original vertex point
            //gl.glPointSize(8.0f);
            //gl.glColor3d(1, 1, 0);
            //gl.glBegin(GL.GL_POINTS);
            //    gl.glVertex3d(p.x(), p.y(), p.z());
            //gl.glEnd();

            // Draw exploded reference point for label
            //gl.glPointSize(4.0f);
            //gl.glColor3d(0.9, 0.9, 0.5);
            //gl.glBegin(GL.GL_POINTS);
            //    gl.glVertex3d(ep.x(), ep.y(), ep.z());
            //gl.glEnd();

            // Draw vertex id label
            label = AwtSystem.calculateLabelImage(
                buildVertexIdsLabel(group.vertices), new ColorRgb(0, 0, 0));

            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glTranslated(ep.x(), ep.y(), ep.z());
            drawTextureString3D(gl, label);
            gl.glPopMatrix();
        }
    }

    private static Vector3D
    computeLabelPosition(_PolyhedralBoundedSolidVertex vertex,
                         Camera camera,
                         Vector3D midpoint,
                         Vector3D u,
                         Vector3D v)
    {
        Vector3D p;
        double perspectiveDistance;
        Vector3D delta;
        double projectedU;
        double projectedV;
        Vector3D projected;
        Vector3D ep;
        Vector3D deltaView;

        p = vertex.position;
        delta = p.subtract(midpoint);
        delta = delta.normalized();
        projectedU = u.dotProduct(delta);
        projectedV = v.dotProduct(delta);
        projected = u.multiply(projectedU).add(v.multiply(projectedV));
        if ( projected.length() > VSDK.EPSILON ) {
            projected = projected.normalized();
        }
        perspectiveDistance = VSDK.vectorDistance(camera.getPosition(), p);
        projected = projected.multiply(perspectiveDistance / 80.0);

        ep = p.add(projected);
        deltaView = camera.getPosition().subtract(ep);
        deltaView = deltaView.normalized();
        deltaView = deltaView.multiply(0.01);
        return ep.add(deltaView);
    }

    private static ArrayList<VertexLabelGroup>
    buildVertexGroups(PolyhedralBoundedSolid solid,
                      Camera camera,
                      Vector3D midpoint,
                      Vector3D u,
                      Vector3D v)
    {
        ArrayList<VertexLabelGroup> vertexGroups;
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext;
        double spatialTolerance;
        double cameraPlaneTolerance;
        int i;

        vertexGroups = new ArrayList<VertexLabelGroup>();
        numericContext = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        spatialTolerance = numericContext.bigEpsilon() *
            LABEL_GROUPING_EPSILON_FACTOR;
        cameraPlaneTolerance = Math.max(spatialTolerance,
            numericContext.modelScale() * LABEL_GROUPING_CAMERA_PLANE_FACTOR);

        for ( i = 0; i < solid.verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex = solid.verticesList.get(i);
            Vector3D labelPosition = computeLabelPosition(vertex, camera,
                midpoint, u, v);
            VertexLabelGroup group;

            group = findVertexGroup(vertexGroups, vertex, labelPosition,
                spatialTolerance, cameraPlaneTolerance, u, v);
            if ( group == null ) {
                group = new VertexLabelGroup(vertex, labelPosition);
                vertexGroups.add(group);
            }
            else {
                group.add(vertex, labelPosition);
            }
        }
        return vertexGroups;
    }

    private static VertexLabelGroup
    findVertexGroup(ArrayList<VertexLabelGroup> groups,
                    _PolyhedralBoundedSolidVertex vertex,
                    Vector3D labelPosition,
                    double spatialTolerance,
                    double cameraPlaneTolerance,
                    Vector3D u,
                    Vector3D v)
    {
        int i;

        for ( i = 0; i < groups.size(); i++ ) {
            VertexLabelGroup group = groups.get(i);
            if ( group.containsCloseVertex(vertex, labelPosition,
                 spatialTolerance, cameraPlaneTolerance, u, v) ) {
                return group;
            }
        }
        return null;
    }

    private static double
    distanceSquared(Vector3D a, Vector3D b)
    {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();

        return dx * dx + dy * dy + dz * dz;
    }

    private static double
    cameraPlaneDistanceSquared(Vector3D a, Vector3D b, Vector3D u, Vector3D v)
    {
        Vector3D delta;
        double du;
        double dv;

        delta = a.subtract(b);
        du = u.dotProduct(delta);
        dv = v.dotProduct(delta);
        return du * du + dv * dv;
    }

    private static final class VertexLabelGroup
    {
        private final ArrayList<_PolyhedralBoundedSolidVertex> vertices;
        private final ArrayList<Vector3D> labelPositions;
        private final Vector3D labelPosition;

        private VertexLabelGroup(_PolyhedralBoundedSolidVertex vertex,
                                 Vector3D labelPosition)
        {
            vertices = new ArrayList<_PolyhedralBoundedSolidVertex>();
            labelPositions = new ArrayList<Vector3D>();
            this.labelPosition = labelPosition;
            add(vertex, labelPosition);
        }

        private void add(_PolyhedralBoundedSolidVertex vertex,
                         Vector3D labelPosition)
        {
            vertices.add(vertex);
            labelPositions.add(labelPosition);
        }

        private boolean containsCloseVertex(_PolyhedralBoundedSolidVertex vertex,
                                            Vector3D labelPosition,
                                            double spatialTolerance,
                                            double cameraPlaneTolerance,
                                            Vector3D u,
                                            Vector3D v)
        {
            double spatialToleranceSquared;
            double cameraPlaneToleranceSquared;
            int i;

            spatialToleranceSquared = spatialTolerance * spatialTolerance;
            cameraPlaneToleranceSquared = cameraPlaneTolerance *
                cameraPlaneTolerance;
            for ( i = 0; i < vertices.size(); i++ ) {
                if ( distanceSquared(vertices.get(i).position,
                     vertex.position) <= spatialToleranceSquared ) {
                    return true;
                }
                if ( cameraPlaneDistanceSquared(labelPositions.get(i),
                     labelPosition, u, v) <= cameraPlaneToleranceSquared ) {
                    return true;
                }
            }
            return false;
        }
    }

    private static String
    buildVertexIdsLabel(ArrayList<_PolyhedralBoundedSolidVertex> group)
    {
        StringBuilder label;
        int i;

        label = new StringBuilder();
        for ( i = 0; i < group.size(); i++ ) {
            if ( i > 0 ) {
                label.append(", ");
            }
            label.append(group.get(i).id);
        }
        return label.toString();
    }

    public static void
    drawVertexNormals(GL2 gl, PolyhedralBoundedSolid solid)
    {
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for vertex normals
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);
        int i, j;

        //-----------------------------------------------------------------
        Vector3D p0 = null;
        Vertex vertex = new Vertex(p0);

        gl.glBegin(GL.GL_LINES);
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            // Logic
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            if ( face.getContainingPlane() != null ) {
                vertex.normal = face.getContainingPlane().getNormal();
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
        gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE,
            GL2.GL_REPLACE);

        //float c[] = {1f, 1f, 1f, 1f};
        //gl.glTexEnvfv(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_COLOR, c, 0);
        gl.glTranslated(1, 1, 0); // gl.glRasterPos3d(0, 0, 0);, check draw...
        JoglImageRenderer.draw(gl, i);
        gl.glPopAttrib();
    }
}
