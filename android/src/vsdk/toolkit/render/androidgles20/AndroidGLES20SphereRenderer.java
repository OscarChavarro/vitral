//===========================================================================

package vsdk.toolkit.render.androidgles20;

// Java basic classes
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

// Android classes
import android.opengl.GLES20;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;

public class AndroidGLES20SphereRenderer extends AndroidGLES20Renderer
{
    private static int defaultSlices = 20;
    private static int defaultStacks = 10;

    private static void drawPoints(Sphere sphere, int slices, int stacks,
        RendererConfiguration q)
    {
        //-----------------------------------------------------------------
        int n;
        n = stacks * slices;

        float vertexDataArray[] = new float[n*8];

        //-----------------------------------------------------------------
        Vector3D p = new Vector3D();
        ColorRgb c = q.getWireColor();

        int index = 0;

        for( int i = 0; i < stacks; i++ ) {
            double t1 = ((double)i)/((double)(stacks)-1.0);
            double phi1 = Math.PI * t1 - Math.PI / 2;
    
            for( int j = 0; j < slices; j++ ) {
                double s = ((double)j) / (((double)slices)-1.0);
                double theta = 2 * Math.PI * s;

                sphere.spherePosition(p, theta, phi1);

                vertexDataArray[index] = (float)p.x;    index++;
                vertexDataArray[index] = (float)p.y;    index++;
                vertexDataArray[index] = (float)p.z;    index++;
                vertexDataArray[index] = (float)c.r;    index++;
                vertexDataArray[index] = (float)c.g;    index++;
                vertexDataArray[index] = (float)c.b;    index++;
                vertexDataArray[index] = 0.0f;          index++; // u
                vertexDataArray[index] = 0.0f;          index++; // v
            }
        }

        //-----------------------------------------------------------------
        FloatBuffer verticesBufferedArray;

        verticesBufferedArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
            ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);

        //drawVertices3Position2Uv(verticesBufferedArray, GLES20.GL_POINTS,
        //                       n, vertexSizeInBytes);
        drawVertices3Position3Color2Uv(verticesBufferedArray, GLES20.GL_POINTS,
                                 n);
    }

    private static void drawWires(Sphere sphere, int slices, int stacks,
        RendererConfiguration q)
    {
        //-----------------------------------------------------------------
        Vector3D p = new Vector3D();
        ColorRgb c = q.getWireColor();

        for( int i = 1; i < stacks - 1; i++ ) {
            //------------------------------------------------------------
            double t1 = ((double)i)/((double)(stacks)-1.0);
            double phi1 = Math.PI * t1 - Math.PI / 2;
    
            int index = 0;
            float vertexDataArray[] = new float[slices*8];

            for( int j = 0; j < slices; j++ ) {
                double s = ((double)j) / (((double)slices)-1.0);
                double theta = 2 * Math.PI * s;

                sphere.spherePosition(p, theta, phi1);

                vertexDataArray[index] = (float)p.x;    index++;
                vertexDataArray[index] = (float)p.y;    index++;
                vertexDataArray[index] = (float)p.z;    index++;
                vertexDataArray[index] = (float)c.r;    index++;
                vertexDataArray[index] = (float)c.g;    index++;
                vertexDataArray[index] = (float)c.b;    index++;
                vertexDataArray[index] = 0.0f;          index++; // u
                vertexDataArray[index] = 0.0f;          index++; // v
            }
            //------------------------------------------------------------
            FloatBuffer verticesBufferedArray;

            verticesBufferedArray = ByteBuffer.allocateDirect(
                vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
            verticesBufferedArray.put(vertexDataArray);
            drawVertices3Position3Color2Uv(verticesBufferedArray, 
                GLES20.GL_LINE_STRIP, slices);
        }

        for( int j = 0; j < slices; j++ ) {
            //------------------------------------------------------------    
            int index = 0;
            float vertexDataArray[] = new float[stacks*8];

            double s = ((double)j) / (((double)slices)-1.0);
            double theta = 2 * Math.PI * s;

            for( int i = 0; i < stacks; i++ ) {
                double t1 = ((double)i)/((double)(stacks)-1.0);
                double phi1 = Math.PI * t1 - Math.PI / 2;

                sphere.spherePosition(p, theta, phi1);

                vertexDataArray[index] = (float)p.x;    index++;
                vertexDataArray[index] = (float)p.y;    index++;
                vertexDataArray[index] = (float)p.z;    index++;
                vertexDataArray[index] = (float)c.r;    index++;
                vertexDataArray[index] = (float)c.g;    index++;
                vertexDataArray[index] = (float)c.b;    index++;
                vertexDataArray[index] = 0.0f;          index++; // u
                vertexDataArray[index] = 0.0f;          index++; // v
            }
            //------------------------------------------------------------
            FloatBuffer verticesBufferedArray;

            verticesBufferedArray = ByteBuffer.allocateDirect(
                vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
            verticesBufferedArray.put(vertexDataArray);
            drawVertices3Position3Color2Uv(verticesBufferedArray, 
                GLES20.GL_LINE_STRIP, stacks);
        }

    }

    public static void draw(Sphere s, Camera c, RendererConfiguration q)
    {
        draw(s, c, q, defaultSlices, defaultStacks);
    }

    public static void setDefaultSlicesStacks(int sl, int st)
    {
        defaultSlices = sl;
        defaultStacks = st;
    }

    /**
    Generates the relevant OpenGLES primitives for a sphere's vertex.
    */
    private static void
    drawVertex(double theta, double phi, double s, double t,
               Vector3D P, Vector3D N, Vector3D T, Vector3D B, Sphere sphere,
               float vertexDataArray[], int index, RendererConfiguration q)
    {
        //- Execute vertex -----------------------------------------------
        // glVertex3d(P.x, P.y, P.z);
        sphere.spherePosition(P, theta, phi);
        vertexDataArray[index] = (float)P.x;    index++;
        vertexDataArray[index] = (float)P.y;    index++;
        vertexDataArray[index] = (float)P.z;    index++;

        // glColor3d(c.r, c.g, c.b);
        vertexDataArray[index] = (float)1.0;    index++;
        vertexDataArray[index] = (float)1.0;    index++;
        vertexDataArray[index] = (float)1.0;    index++;

        //gl.glNormal3d(N.x, N.y, N.z);
        vertexDataArray[index] = (float)N.x;    index++;
        vertexDataArray[index] = (float)N.y;    index++;
        vertexDataArray[index] = (float)N.z;    index++;

        // glTexCoord2d(1.0-s, t);
        vertexDataArray[index] = (float)(1.0-s);index++;
        vertexDataArray[index] = (float)(t);    index++;

        //- If inside a Cg schema, pass non standard OpenGL parameters ----
        //CGparameter tangentParam;
        //CGparameter binormalParam;
        //double vectorarray[];

        sphere.sphereTangent(T, theta, phi);
        //tangentParam = accessNvidiaGpuVertexParameter("TObject");
        //if ( tangentParam != null ) {
        //    vectorarray = T.exportToDoubleArrayVect();
        //    CgGL.cgGLSetParameter3dv(tangentParam, vectorarray, 0);
        //}

        sphere.sphereBinormal(B, theta, phi);
        //binormalParam = accessNvidiaGpuVertexParameter("BObject");
        //if ( binormalParam != null ) {
        //    B.x = 1;
        //    B.y = 0;
        //    B.z = 0;
        //    vectorarray = B.exportToDoubleArrayVect();
        //    CgGL.cgGLSetParameter3dv(binormalParam, vectorarray, 0);
        //}
    }

    private static void
    drawSurfacesSmooth(Sphere sphere, int slices, int stacks, 
        RendererConfiguration q)
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
        float vertexDataArray[];
        int vertexFloatElements = 11;
        int index;
        FloatBuffer verticesBufferedArray;

        //- Draw main sphere body -----------------------------------------
        for( i = 0; i < stacks - 2; i++ ) {
            //-------------------------------------------------------------
            t1 = i/(stacks-1.f);
            t2 = (i+1)/(stacks-1.f);
            phi1 = Math.PI*t1 - Math.PI/2;
            phi2 = Math.PI*t2 - Math.PI/2;
    
            vertexDataArray = new float[slices*2*vertexFloatElements];
            index = 0;

            for( j = 0; j < slices; j++ ) {
                s = j/(slices-1.f);
                theta = 2*Math.PI*s;

                sphere.sphereNormal(N, theta, phi1);
                drawVertex(theta, phi1, s, t1, P, N, T, B, 
                           sphere, vertexDataArray, index, q);
                index += vertexFloatElements;

                sphere.sphereNormal(N, theta, phi2);
                drawVertex(theta, phi2, s, t2, P, N, T, B, 
                           sphere, vertexDataArray, index, q);
                index += vertexFloatElements;
            }

            //-------------------------------------------------------------
            verticesBufferedArray = ByteBuffer.allocateDirect(
                vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
            verticesBufferedArray.put(vertexDataArray);

            drawVertices3Position3Color3Normal2Uv(verticesBufferedArray, 
                GLES20.GL_TRIANGLE_STRIP, slices*2);

        }

        //- Draw sphere upper cap -----------------------------------------
        // Cap need to be painted in inverse order to allow for correct
        // rendering in the case of flat shading in OpenGL/JOGL
        t1 = i/(stacks-1.f);
        t2 = (i+1)/(stacks-1.f);
        phi1 = Math.PI*t1 - Math.PI/2;
        phi2 = Math.PI*t2 - Math.PI/2;
    
        vertexDataArray = new float[slices*2*vertexFloatElements];
        index = 0;

        //-------------------------------------------------------------
        for( j = slices-1; j >= 0; j-- ) {
            s = j/(slices-1.f);
            theta = 2*Math.PI*s;

            sphere.sphereNormal(N, theta, phi2);
            drawVertex(theta, phi2, s, t2, P, N, T, B, 
                sphere, vertexDataArray, index, q);
            index += vertexFloatElements;

            sphere.sphereNormal(N, theta, phi1);
            drawVertex(theta, phi1, s, t1, P, N, T, B, 
                sphere, vertexDataArray, index, q);
            index += vertexFloatElements;
        }

        //-------------------------------------------------------------
        verticesBufferedArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
            ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);
        drawVertices3Position3Color3Normal2Uv(verticesBufferedArray, 
            GLES20.GL_TRIANGLE_STRIP, slices*2);

    }

    /**
    Current version has a little error in normal managament at the cap.
    Rendering results are strange. Pending to check this.
    */
    private static void
    drawSurfacesFlat(Sphere sphere, int slices, int stacks, RendererConfiguration q)
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
        double s1;
        double s2;
        double theta1;
        double theta2;
        float vertexDataArray[];
        int vertexFloatElements = 11;
        int index;

        //- Draw main sphere body -----------------------------------------
        for( i = 0; i < stacks - 2; i++ ) {
            //-------------------------------------------------------------
            t1 = i/(stacks-1.f);
            t2 = (i+1)/(stacks-1.f);
            phi1 = Math.PI*t1 - Math.PI/2;
            phi2 = Math.PI*t2 - Math.PI/2;
    
            vertexDataArray = new float[slices*6*vertexFloatElements];
            index = 0;

            for( j = 0; j < slices; j++ ) {
                s1 = j/(slices-1.f);
                s2 = (j+1)/(slices-1.f);
                theta1 = 2*Math.PI*s1;
                theta2 = 2*Math.PI*s2;

                sphere.sphereNormal(N, (theta2+theta1)/2, (phi2+phi1)/2);
                drawVertex(theta1, phi1, s1, t1, P, N, T, B, 
                           sphere, vertexDataArray, index, q);
                index += vertexFloatElements;

                sphere.sphereNormal(N, (theta2+theta1)/2, (phi2+phi1)/2);
                drawVertex(theta1, phi2, s1, t2, P, N, T, B, 
                           sphere, vertexDataArray, index, q);
                index += vertexFloatElements;

                sphere.sphereNormal(N, (theta2+theta1)/2, (phi2+phi1)/2);
                drawVertex(theta2, phi1, s2, t1, P, N, T, B, 
                           sphere, vertexDataArray, index, q);
                index += vertexFloatElements;

                sphere.sphereNormal(N, (theta2+theta1)/2, (phi2+phi1)/2);
                drawVertex(theta1, phi2, s1, t2, P, N, T, B, 
                           sphere, vertexDataArray, index, q);
                index += vertexFloatElements;

                sphere.sphereNormal(N, (theta2+theta1)/2, (phi2+phi1)/2);
                drawVertex(theta2, phi2, s2, t2, P, N, T, B, 
                           sphere, vertexDataArray, index, q);
                index += vertexFloatElements;

                sphere.sphereNormal(N, (theta2+theta1)/2, (phi2+phi1)/2);
                drawVertex(theta2, phi1, s2, t1, P, N, T, B, 
                           sphere, vertexDataArray, index, q);
                index += vertexFloatElements;
            }

            //-------------------------------------------------------------
            FloatBuffer verticesBufferedArray;

            verticesBufferedArray = ByteBuffer.allocateDirect(
                vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
            verticesBufferedArray.put(vertexDataArray);

            drawVertices3Position3Color3Normal2Uv(verticesBufferedArray, 
                GLES20.GL_TRIANGLES, slices*6);

        }

        //- Draw sphere upper cap -----------------------------------------
        // Cap need to be painted in inverse order to allow for correct
        // rendering in the case of flat shading in OpenGL/JOGL
        t1 = i/(stacks-1.f);
        t2 = (i+1)/(stacks-1.f);
        phi1 = Math.PI*t1 - Math.PI/2;
        phi2 = Math.PI*t2 - Math.PI/2;
    
        vertexDataArray = new float[slices*2*vertexFloatElements];
        index = 0;

        //-------------------------------------------------------------
        for( j = slices-1; j >= 0; j-- ) {
            s1 = j/(slices-1.f);
            s2 = (j+1)/(slices-1.f);
            theta1 = 2*Math.PI*s1;
            theta2 = 2*Math.PI*s2;

            sphere.sphereNormal(N, (theta2+theta1)/2, (phi2+phi1)/2);
            drawVertex(theta1, phi2, s1, t2, P, N, T, B, 
                sphere, vertexDataArray, index, q);
            index += vertexFloatElements;

            sphere.sphereNormal(N, (theta2+theta1)/2, (phi2+phi1)/2);
            drawVertex(theta1, phi1, s1, t1, P, N, T, B, 
                sphere, vertexDataArray, index, q);
            index += vertexFloatElements;
        }

        //-------------------------------------------------------------
        FloatBuffer verticesBufferedArray;
        verticesBufferedArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
            ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);
        drawVertices3Position3Color3Normal2Uv(verticesBufferedArray, 
            GLES20.GL_TRIANGLE_STRIP, slices*2);

    }

    public static void draw(Sphere s, Camera c, RendererConfiguration q,
                            int slices, int stacks)
    {
        RendererConfiguration qcopy;
        
        if ( q.isPointsSet() ) {
            glDisable(GL_TEXTURE_2D);
            setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
            double r = s.getRadius();
            if ( q.isSurfacesSet() ) s.setRadius(r * 1.01);
            qcopy = q.clone();
            qcopy.setUseVertexColors(true);
            qcopy.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
            setRendererConfiguration(qcopy);
            drawPoints(s, slices, stacks, qcopy);
            s.setRadius(r);
        }
        
        if ( q.isWiresSet() ) {
            glDisable(GL_TEXTURE_2D);
            qcopy = q.clone();
            qcopy.setUseVertexColors(true);
            qcopy.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
            double r = s.getRadius();
            if ( q.isSurfacesSet() ) s.setRadius(r * 1.01);
            setRendererConfiguration(qcopy);
            drawWires(s, slices, stacks, q);
            s.setRadius(r);
        }
        
        if ( q.isSurfacesSet() ) {
            if ( q.isTextureSet() ) {
                glEnable(GL_TEXTURE_2D);
            }
            else {
                glDisable(GL_TEXTURE_2D);
            }

            setRendererConfiguration(q);
            if ( q.getShadingType() == RendererConfiguration.SHADING_TYPE_FLAT ) {
                drawSurfacesFlat(s, slices, stacks, q);
            }
            else {
                drawSurfacesSmooth(s, slices, stacks, q);
            }
        }
        
        if ( q.isBoundingVolumeSet() ) {
            qcopy = q.clone();
            qcopy.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
            setRendererConfiguration(qcopy);
            AndroidGLES20GeometryRenderer.drawMinMaxBox(s, q);
        }
        
        if ( q.isSelectionCornersSet() ) {
            qcopy = q.clone();
            qcopy.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
            setRendererConfiguration(qcopy);
            AndroidGLES20GeometryRenderer.drawSelectionCorners(s, q);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
