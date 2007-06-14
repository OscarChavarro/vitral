//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 17 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.Light;
import javax.media.opengl.GL;

public class JoglLightRenderer {

    public static int supportedLightsInOpenGL = 8;

    public static void activate(GL gl, Light l)
    {
        float[] lightPosition=l.getPosition().toFloatVect();
        float global_ambient[] = {0, 0, 0, 1};
        float global_twoside[] = {gl.GL_TRUE};  // WARNING: This is inefficient!
        int lightNumber = l.getId();

        if ( lightNumber >= supportedLightsInOpenGL || lightNumber < 0 ) {
            return;
    }

/*
        if ( l.getLightType() == Light.DIRECTIONAL ) { // Why?
            lightPosition[3]=0;
        }
*/

        gl.glLightModelfv(gl.GL_LIGHT_MODEL_AMBIENT, global_ambient, 0);   // OJO! Esta
        gl.glLightModelfv(gl.GL_LIGHT_MODEL_TWO_SIDE, global_twoside, 0);  // cableado!
        gl.glLightModeli(gl.GL_LIGHT_MODEL_LOCAL_VIEWER, gl.GL_TRUE); // OJO: ?
        gl.glEnable(gl.GL_LIGHTING);  // Is it right to have this here and re-set all the time?
        gl.glEnable(gl.GL_LIGHT0 + lightNumber);

        gl.glLightfv(gl.GL_LIGHT0 + lightNumber, gl.GL_POSITION, lightPosition, 0);
        gl.glLightfv(gl.GL_LIGHT0 + lightNumber, gl.GL_AMBIENT, l.getAmbient().toFloatVect(), 0);
        gl.glLightfv(gl.GL_LIGHT0 + lightNumber, gl.GL_DIFFUSE, l.getDiffuse().toFloatVect(), 0);
        gl.glLightfv(gl.GL_LIGHT0 + lightNumber, gl.GL_SPECULAR, l.getSpecular().toFloatVect(), 0);
        
        if(l.usesAtenuation())
        {
            gl.glLightf(gl.GL_LIGHT0 + lightNumber, gl.GL_CONSTANT_ATTENUATION, (float)l.getConstantAtenuation());
            gl.glLightf(gl.GL_LIGHT0 + lightNumber, gl.GL_LINEAR_ATTENUATION, (float)l.getLinearAtenuation());
            gl.glLightf(gl.GL_LIGHT0 + lightNumber, gl.GL_QUADRATIC_ATTENUATION, (float)l.getQuadricAtenuation());
        }
    }

    public static void draw(GL gl, Light l)
    {
        Vector3D p = l.getPosition();
        ColorRgb c = l.getSpecular();
        double delta = 0.1;

        gl.glPushMatrix();
        gl.glDisable(gl.GL_LIGHTING);
        gl.glLineWidth(2.0f);
        gl.glBegin(gl.GL_LINES);
            gl.glColor3d(c.r, c.g, c.g);
            gl.glVertex3d(p.x - delta, p.y, p.z);
            gl.glVertex3d(p.x + delta, p.y, p.z);
            gl.glVertex3d(p.x, p.y - delta, p.z);
            gl.glVertex3d(p.x, p.y + delta, p.z);
            gl.glVertex3d(p.x, p.y, p.z - delta);
            gl.glVertex3d(p.x, p.y, p.z + delta);
        gl.glEnd();
        gl.glPopMatrix();
    }

    public static void turnOffAllLights(GL gl)
    {
        int i;

    for ( i = 0; i < supportedLightsInOpenGL; i++ ) {
        gl.glDisable(gl.GL_LIGHT0 + i);
    }
    }
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
