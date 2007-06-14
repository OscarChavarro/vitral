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

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.

    @return Approximate number of triangles. If non-triangles primitives like
    quads are rendered, this counts as the corresponding number of triangles.
    1D and 0D primitives are not counted.

    @todo Implement triangle count!
    */
    public static int draw(GL gl, Sphere s, Camera c, QualitySelection q)
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

        if ( q.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, s, q);
    }

        return 0;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
