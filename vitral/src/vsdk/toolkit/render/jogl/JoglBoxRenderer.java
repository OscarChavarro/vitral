//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 20 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import com.sun.opengl.util.GLUT;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Box;

public class JoglBoxRenderer extends JoglRenderer {

    private static GLUT glut = null;

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void draw(GL gl, Box box, Camera c, QualitySelection q)
    {
        if (glut == null) {
            glut = new GLUT();
        }

        Vector3D size = box.getSize();

        gl.glPushMatrix();
        gl.glScaled(size.x, size.y, size.z);
        if ( q.isSurfacesSet() ) {
        glut.glutSolidCube(1);
        }

        if ( q.isWiresSet() ) {
            gl.glLineWidth(1);
        glut.glutWireCube(1);
        }
        gl.glPopMatrix();

        if ( q.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, box, q);
    }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
