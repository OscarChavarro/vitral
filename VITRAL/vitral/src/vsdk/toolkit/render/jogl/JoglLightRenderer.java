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
        int lightNumber = l.getId();

    System.out.println("Activo la luz " + lightNumber);

        if ( lightNumber >= supportedLightsInOpenGL ) {
            return;
    }

        if ( l.getLightType() == Light.DIRECTIONAL ) {
            lightPosition[3]=0;
        }

        gl.glLightfv(gl.GL_LIGHT0 + lightNumber, gl.GL_POSITION, lightPosition, 0);
        gl.glLightfv(gl.GL_LIGHT0 + lightNumber, gl.GL_AMBIENT, l.getAmbient().toFloatVect(), 0);
        gl.glLightfv(gl.GL_LIGHT0 + lightNumber, gl.GL_DIFFUSE, l.getDiffuse().toFloatVect(), 0);
        gl.glLightfv(gl.GL_LIGHT0 + lightNumber, gl.GL_SPECULAR, l.getSpecular().toFloatVect(), 0);
        
/*
        gl.glLightf(gl.GL_LIGHT0 + lightNumber, gl.GL_CONSTANT_ATTENUATION, constantAtenuation);
        gl.glLightf(gl.GL_LIGHT0 + lightNumber, gl.GL_LINEAR_ATTENUATION, linearAtenuation);
        gl.glLightf(gl.GL_LIGHT0 + lightNumber, gl.GL_QUADRATIC_ATTENUATION, quadricAtenuation);
*/
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
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
