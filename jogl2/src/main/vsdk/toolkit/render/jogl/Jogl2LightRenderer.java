package vsdk.toolkit.render.jogl;

// Java basic

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.LightType;

public class Jogl2LightRenderer extends Jogl2Renderer {

    public static int supportedLightsInOpenGL = 8;
    private static double scale = 1.0;
    
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
        float[] lightPosition=l.getPosition().exportToFloatArrayVector();
        float global_ambient[] = {0, 0, 0, 1};
        float global_twoside[] = {GL.GL_TRUE};  // WARNING: This is inefficient!
        int lightNumber = l.getId();

        if ( lightNumber >= supportedLightsInOpenGL || lightNumber < 0 ) {
            return;
        }

        lightPosition[3]=1.0f;
/*
        if ( l.getLightType() == LightType.DIRECTIONAL ) { // Why?
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

    public static void draw(GL2 gl, Light l)
    {
        Vector3D p = l.getPosition();
        ColorRgb c = l.getSpecular();
        double delta = 0.1;

        gl.glPushMatrix();
        gl.glTranslated(p.x(), p.y(), p.z());
        gl.glScaled(scale, scale, scale);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glLineWidth(2.0f);
        gl.glBegin(GL.GL_LINES);
            gl.glColor3d(c.r, c.g, c.b);
            gl.glVertex3d(0 - delta, 0, 0);
            gl.glVertex3d(0 + delta, 0, 0);
            gl.glVertex3d(0, 0 - delta, 0);
            gl.glVertex3d(0, 0 + delta, 0);
            gl.glVertex3d(0, 0, 0 - delta);
            gl.glVertex3d(0, 0, 0 + delta);
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

    /**
    @return the scale
    */
    public static double getScale() {
        return scale;
    }

    /**
    @param newScale the scale to set
    */
    public static void setScale(double newScale) {
        scale = newScale;
    }
    
}
