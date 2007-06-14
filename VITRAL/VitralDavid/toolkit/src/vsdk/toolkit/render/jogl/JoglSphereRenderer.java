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
            gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
            {
                gl.glLineWidth(1);
                gl.glDisable(gl.GL_BLEND);
                gl.glDisable(gl.GL_LIGHTING);
                gl.glPushMatrix();
                {
                    glut.glutWireSphere(s.getRadius(), 20, 10);
                }
                gl.glPopMatrix();
            }
            gl.glPopAttrib();
        }

        if ( q.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, s, q);
    }

    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
