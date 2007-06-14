//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 18 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import com.sun.opengl.util.GLUT;

import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Cone;

public class JoglConeRenderer {

    private static GLU glu = null;
    private static GLUT glut = null;
    private static GLUquadric gluQuadric = null;

    public static void draw(GL gl, Cone cone, Camera c, QualitySelection q)
    {
        if (glu == null) {
            glu = new GLU();
            gluQuadric = glu.gluNewQuadric();
        }

        if (glut == null) {
            glut = new GLUT();
        }

        if ( q.isSurfacesSet() ) {
        glut.glutSolidCone(cone.getBaseRadius(), cone.getHeight(), 20, 3);
            glu.gluDisk(gluQuadric, 0, cone.getBaseRadius(), 20, 1);
        }

        if ( q.isWiresSet() ) {
            gl.glLineWidth(1);
        glut.glutWireCone(cone.getBaseRadius(), cone.getHeight(), 20, 3);
            glu.gluDisk(gluQuadric, 0, cone.getBaseRadius(), 20, 1);
        }

        if ( q.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, cone, q);
    }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
