//===========================================================================

package vsdk.toolkit.render.joglcg;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import com.jogamp.opengl.cg.CgGL;
import com.jogamp.opengl.cg.CGparameter;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.render.joglcg.JoglCgRenderer;
import vsdk.toolkit.render.joglcg.JoglCgGeometryRenderer;

public class JoglCgSphereRenderer extends JoglCgRenderer {

    /**
    @todo check this method for efficiency improvement
    */
    private static void
    sphereNormal(Vector3D n, double theta, double phi)
    {
        n.x = Math.cos(phi) * Math.cos(theta);
        n.y = -Math.cos(phi) * Math.sin(theta);
        n.z = Math.sin(phi);
    }

    /**
    @todo check this method for efficiency improvement
    */
    private static void
    sphereTangent(Vector3D t, double theta, double phi)
    {
        t.x = Math.sin(theta);
        t.y = Math.cos(theta);
        t.z = 0;
    }

    /**
    @todo check this method for efficiency improvement
    */
    private static void
    sphereBinormal(Vector3D b, double theta, double phi)
    {
        b.x = -Math.sin(phi)*Math.cos(theta);
        b.y = Math.sin(phi)*Math.sin(theta);
        b.z = Math.cos(phi)*Math.cos(theta)*Math.cos(theta) + Math.cos(phi)*Math.sin(theta)*Math.sin(theta);
    }

    /**
    Warning: Change with configured color for vertex normals, tangents and
    binormals
    */
    private static void drawVertexNormals(GL2 gl, Sphere sphere, int slices, int stacks) {
        JoglCgRenderer.disableNvidiaCgProfiles();
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glLineWidth(1.0f);


        Vector3D n = new Vector3D();
        Vector3D p = new Vector3D();
        Vector3D T = new Vector3D();
        Vector3D b = new Vector3D();

        gl.glBegin(GL.GL_LINES);
        for( int i = 0; i < stacks; i++ ) {
            double t1 = i/(stacks-1.f);
            double phi1 = Math.PI*t1 - Math.PI/2;
    
            for( int j = 0; j < slices; j++ ) {
                double s = j/(slices-1.f);
                double theta = 2*Math.PI*s;

                sphereNormal(n, theta, phi1);
                sphere.spherePosition(p, theta, phi1);
                sphereTangent(T, theta, phi1);
                sphereBinormal(b, theta, phi1);

                gl.glColor3d(1, 1, 0);
                gl.glVertex3d(p.x, p.y, p.z);
                gl.glVertex3d(p.x+n.x/10.0, p.y+n.y/10.0, p.z+n.z/10.0);

                gl.glColor3d(0.9, 0.5, 0.5);
                gl.glVertex3d(p.x, p.y, p.z);
                gl.glVertex3d(p.x+T.x/20.0, p.y+T.y/20.0, p.z+T.z/20.0);

                gl.glColor3d(0.5, 0.9, 0.5);
                gl.glVertex3d(p.x, p.y, p.z);
                gl.glVertex3d(p.x+b.x/20.0, p.y+b.y/20.0, p.z+b.z/20.0);
            }
        }
        gl.glEnd();
    }

    private static void drawPoints(GL2 gl, Sphere sphere, int slices, int stacks) {
        JoglCgRenderer.disableNvidiaCgProfiles();
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glColor3d(1, 0, 0);
        gl.glPointSize(2.0f);

        Vector3D p = new Vector3D();

        gl.glBegin(GL.GL_POINTS);
        for( int i = 0; i < stacks; i++ ) {
            double t1 = i/(stacks-1.f);
            double phi1 = Math.PI*t1 - Math.PI/2;
    
            for( int j = 0; j < slices; j++ ) {
                double s = j/(slices-1.f);
                double theta = 2*Math.PI*s;
                sphere.spherePosition(p, theta, phi1);
                gl.glVertex3d(p.x, p.y, p.z);
            }
        }
        gl.glEnd();
    }

    /**
    Generates the relevant JOGL/OpenGL + Cg primitives for a sphere's vertex.
    */
    private static void
    drawVertex(GL2 gl, double theta, double phi, double s, double t,
               Vector3D P, Vector3D N, Vector3D T, Vector3D B, Sphere sphere)
    {
        //- If inside a Cg schema, pass non standard OpenGL parameters ----
        if ( renderingWithNvidiaGpu() ) {
            CGparameter tangentParam;
            CGparameter binormalParam;
            double vectorarray[];

            sphereTangent(T, theta, phi);
            tangentParam = accessNvidiaGpuVertexParameter("TObject");
            if ( tangentParam != null ) {
                vectorarray = T.exportToDoubleArrayVect();
                CgGL.cgGLSetParameter3dv(tangentParam, vectorarray, 0);
            }

            sphereBinormal(B, theta, phi);
            binormalParam = accessNvidiaGpuVertexParameter("BObject");
            if ( binormalParam != null ) {
                B.x = 1;
                B.y = 0;
                B.z = 0;
                vectorarray = B.exportToDoubleArrayVect();
                CgGL.cgGLSetParameter3dv(binormalParam, vectorarray, 0);
            }
        }

        //- Pass standard vertex parameters to OpenGL ---------------------
        sphereNormal(N, theta, phi);
        gl.glTexCoord2d(1.0-s, t);
        gl.glNormal3d(N.x, N.y, N.z);

        //- Execute vertex -----------------------------------------------
        sphere.spherePosition(P, theta, phi);
        gl.glVertex3d(P.x, P.y, P.z);
    }

    private static void
    drawSphereElements(GL2 gl, Sphere sphere, int slices, int stacks)
    {
        VSDK.acumulatePrimitiveCount(VSDK.QUAD, slices*stacks);
        VSDK.acumulatePrimitiveCount(VSDK.QUAD_STRIP, stacks);

        //-----------------------------------------------------------------
        Vector3D P = new Vector3D(); // Vertex position
        Vector3D N = new Vector3D(); // Vertex normal
        Vector3D T = new Vector3D(); // Vertex tangent
        Vector3D B = new Vector3D(); // Vertex binormal

        int i;
        int j;
        double t1;
        double t2;
        double phi1;
        double phi2;
        double s;
        double theta;

        //- Draw main sphere body -----------------------------------------
        for( i = 0; i < stacks - 2; i++ ) {
            t1 = i/(stacks-1.f);
            t2 = (i+1)/(stacks-1.f);
            phi1 = Math.PI*t1 - Math.PI/2;
            phi2 = Math.PI*t2 - Math.PI/2;
    
            gl.glBegin(GL2.GL_QUAD_STRIP);
            for( j = 0; j < slices; j++ ) {
                s = j/(slices-1.f);
                theta = 2*Math.PI*s;
                drawVertex(gl, theta, phi1, s, t1, P, N, T, B, sphere);
                drawVertex(gl, theta, phi2, s, t2, P, N, T, B, sphere);
            }
            gl.glEnd();
        }

        //- Draw sphere upper cap -----------------------------------------
        // Cap need to be painted in inverse order to allow for correct
        // rendering in the case of flat shading in OpenGL/JOGL
        t1 = i/(stacks-1.f);
        t2 = (i+1)/(stacks-1.f);
        phi1 = Math.PI*t1 - Math.PI/2;
        phi2 = Math.PI*t2 - Math.PI/2;
    
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for( j = slices-1; j >= 0; j-- ) {
            s = j/(slices-1.f);
            theta = 2*Math.PI*s;
            drawVertex(gl, theta, phi2, s, t2, P, N, T, B, sphere);
            drawVertex(gl, theta, phi1, s, t1, P, N, T, B, sphere);
        }
        gl.glEnd();

    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void draw(GL2 gl, Sphere s, Camera c, RendererConfiguration q)
    {
        draw(gl, s, c, q, 20, 10);
    }

    public static void draw(GL2 gl, Sphere s, Camera c, RendererConfiguration q,
                            int slices, int stacks)
    {
        JoglCgGeometryRenderer.activateShaders(gl, s, c);
        if ( q.isSurfacesSet() ) {
            JoglCgGeometryRenderer.prepareSurfaceQuality(gl, q);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            gl.glEnable(GL2GL3.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(1.0f, 1.0f);
            drawSphereElements(gl, s, slices, stacks);
        }
        if ( q.isWiresSet() ) {
            JoglCgRenderer.disableNvidiaCgProfiles();
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glShadeModel(GL2.GL_FLAT);

            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
            gl.glDisable(GL2GL3.GL_POLYGON_OFFSET_LINE);
            gl.glLineWidth(1.0f);

            // Warning: Change with configured color for borders
            gl.glColor3d(1, 1, 1);
            gl.glDisable(GL.GL_TEXTURE_2D);

            drawSphereElements(gl, s, slices, stacks);
        }

        if ( q.isPointsSet() ) {
            drawPoints(gl, s, slices, stacks);
        }
        if ( q.isNormalsSet() ) {
            drawVertexNormals(gl, s, slices, stacks);
        }
        if ( q.isBoundingVolumeSet() ) {
            JoglCgGeometryRenderer.drawMinMaxBox(gl, s, q);
        }
        if ( q.isSelectionCornersSet() ) {
            JoglCgGeometryRenderer.drawSelectionCorners(gl, s, q);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
