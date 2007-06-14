//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 18 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;
import com.sun.opengl.util.GLUT;

import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Cone;

public class JoglConeRenderer {

    private static GLUT glut = null;

    public static void draw(GL gl, Cone cone, Camera c, QualitySelection q)
    {
        if (glut == null) {
            glut = new GLUT();
        }

        gl.glPushMatrix();
        gl.glTranslated(0, 0, -cone.getHeight()/2);
        if ( q.isSurfacesSet() ) {
        glut.glutSolidCone(cone.getBaseRadius(), cone.getHeight(), 20, 3);
        }

        if ( q.isWiresSet() ) {
            gl.glLineWidth(1);
        glut.glutWireCone(cone.getBaseRadius(), cone.getHeight(), 20, 3);
        }
        gl.glPopMatrix();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
