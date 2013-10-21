//===========================================================================

package vsdk.toolkit.render.joglcg;

// Java basic
import java.util.ArrayList;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import com.jogamp.opengl.cg.CgGL;
import com.jogamp.opengl.cg.CGprogram;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Light;

public class JoglCgLightRenderer extends JoglCgRenderer {

    public static int supportedLightsInOpenGL = 8;

    public static void deactivate(GL2 gl, Light l)
    {
        int lightNumber = l.getId();

        if ( lightNumber >= supportedLightsInOpenGL || lightNumber < 0 ) {
            return;
        }
        gl.glDisable(GL2.GL_LIGHT0 + lightNumber);
    }

    public static void activate(GL2 gl, Light l)
    {
        //-----------------------------------------------------------------
        if ( nvidiaCgAutomaticMode ) {
            ArrayList<CGprogram> allVertexShaders;
            ArrayList<CGprogram> allTextureShaders;
            allVertexShaders = getAllVertexShaders();
            allTextureShaders = getAllPixelShaders();

            int i;

            for ( i = 0; i < allVertexShaders.size(); i++ ) {
                activateNvidiaGpuParameters(gl, l,
                                            allVertexShaders.get(i),
                                            allTextureShaders.get(i));
            }
        }

        //-----------------------------------------------------------------
        float[] lightPosition=l.getPosition().exportToFloatArrayVect();
        float global_ambient[] = {0, 0, 0, 1};
        float global_twoside[] = {GL.GL_TRUE};  // WARNING: This is inefficient!
        int lightNumber = l.getId();

        if ( lightNumber >= supportedLightsInOpenGL || lightNumber < 0 ) {
            return;
        }

        lightPosition[3]=1.0f;
/*
        if ( l.getLightType() == Light.DIRECTIONAL ) { // Why?
            lightPosition[3]=0;
        }
*/
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, global_ambient, 0);   // OJO! Esta
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_TWO_SIDE, global_twoside, 0);  // cableado!
        gl.glLightModeli(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, GL.GL_TRUE); // OJO: ?
        gl.glEnable(GL2.GL_LIGHTING);  // Is it right to have this here and re-set all the time?
        gl.glEnable(GL2.GL_LIGHT0 + lightNumber);

        gl.glLightfv(GL2.GL_LIGHT0 + lightNumber, GL2.GL_POSITION, lightPosition, 0);
        gl.glLightfv(GL2.GL_LIGHT0 + lightNumber, GL2.GL_AMBIENT, l.getAmbient().exportToFloatArrayVect(), 0);
        gl.glLightfv(GL2.GL_LIGHT0 + lightNumber, GL2.GL_DIFFUSE, l.getDiffuse().exportToFloatArrayVect(), 0);
        gl.glLightfv(GL2.GL_LIGHT0 + lightNumber, GL2.GL_SPECULAR, l.getSpecular().exportToFloatArrayVect(), 0);
        
/*
        gl.glLightf(GL2.GL_LIGHT0 + lightNumber, GL2.GL_CONSTANT_ATTENUATION, constantAtenuation);
        gl.glLightf(GL2.GL_LIGHT0 + lightNumber, GL2.GL_LINEAR_ATTENUATION, linearAtenuation);
        gl.glLightf(GL2.GL_LIGHT0 + lightNumber, GL2.GL_QUADRATIC_ATTENUATION, quadricAtenuation);
*/
        gl.glPopMatrix();
    }

    public static void activateNvidiaGpuParameters(GL2 gl, Light light,
        CGprogram vertexShader, CGprogram pixelShader)
    {
        Vector3D lp = light.getPosition();
        double lpos[] = {lp.x, lp.y, lp.z};
        double lightColor[] = {1.0, 1.0, 1.0};

        CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
            vertexShader, "lightPositionGlobal"), lpos, 0);
        CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
            pixelShader, "lightColor"), lightColor, 0);
    }

    public static void draw(GL2 gl, Light l)
    {
        Vector3D p = l.getPosition();
        ColorRgb c = l.getSpecular();
        double delta = 0.1;

        gl.glPushMatrix();
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glLineWidth(2.0f);
        gl.glBegin(GL.GL_LINES);
            gl.glColor3d(c.r, c.g, c.b);
            gl.glVertex3d(p.x - delta, p.y, p.z);
            gl.glVertex3d(p.x + delta, p.y, p.z);
            gl.glVertex3d(p.x, p.y - delta, p.z);
            gl.glVertex3d(p.x, p.y + delta, p.z);
            gl.glVertex3d(p.x, p.y, p.z - delta);
            gl.glVertex3d(p.x, p.y, p.z + delta);
        gl.glEnd();
        gl.glPopMatrix();
    }

    public static void turnOffAllLights(GL2 gl)
    {
        int i;

        for ( i = 0; i < supportedLightsInOpenGL; i++ ) {
            gl.glDisable(GL2.GL_LIGHT0 + i);
        }
    }
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
