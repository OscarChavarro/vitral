//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 14 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;
import com.sun.opengl.util.GLUT;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Sphere;

public class JoglSphereRenderer extends JoglRenderer {

    private static GLUT glut = null;

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void draw(GL gl, Sphere s, Camera c, QualitySelection q)
    {
        draw(gl, s, c, q, 20, 10);
    }

    public static void draw(GL gl, Sphere s, Camera c, QualitySelection q,
                int slices, int stacks)
    {
        gl.glShadeModel(gl.GL_SMOOTH);

        if (glut == null) {
            glut = new GLUT();
        }
        if ( q.isSurfacesSet() ) {
            VSDK.acumulatePrimitiveCount(VSDK.TRIANGLE, 2*slices*stacks);
            glut.glutSolidSphere(s.getRadius(), slices, stacks);
        }

        if ( q.isWiresSet() ) {
            gl.glLineWidth(1);
            VSDK.acumulatePrimitiveCount(VSDK.TRIANGLE, 2*slices*stacks);
            glut.glutWireSphere(s.getRadius(), slices, stacks);
        }

        if ( q.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, s, q);
    }
        if ( q.isSelectionCornersSet() ) {
            JoglGeometryRenderer.drawSelectionCorners(gl, s, q);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
