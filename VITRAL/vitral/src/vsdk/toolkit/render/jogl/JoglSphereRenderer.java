//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 14 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;
import com.sun.opengl.util.GLUT;

import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Sphere;

public class JoglSphereRenderer {

    private static GLUT glut = null;

    public static void draw(GL gl, Sphere s, Camera c, QualitySelection q)
    {
        if (glut == null) {
            glut = new GLUT();
        }
        if ( q.isSurfacesSet() ) {
            glut.glutSolidSphere(s.getRadius(), 20, 10);
        }

        if ( q.isWiresSet() ) {
            gl.glLineWidth(1);
            glut.glutWireSphere(s.getRadius(), 20, 10);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
