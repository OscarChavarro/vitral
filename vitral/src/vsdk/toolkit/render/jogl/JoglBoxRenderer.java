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

public class JoglBoxRenderer {

    private static GLUT glut = null;

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.

    @return Approximate number of triangles. If non-triangles primitives like
    quads are rendered, this counts as the corresponding number of triangles.
    1D and 0D primitives are not counted.
    */
    public static int draw(GL gl, Box box, Camera c, QualitySelection q)
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

        return 12;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
