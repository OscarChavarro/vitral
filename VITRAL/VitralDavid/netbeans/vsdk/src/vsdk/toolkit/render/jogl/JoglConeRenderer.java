//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 18 2006 - Oscar Chavarro: Original base version                 =
//= - March 28 2006 - Oscar Chavarro: First complete version with cylinder  =
//=   case support                                                          =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Cone;

public class JoglConeRenderer {

    private static GLU glu = null;
    private static GLUquadric gluQuadric = null;

    private static void drawParts(GL gl, Cone cone)
    {
        double r1, r2, h;

        r1 = cone.getBaseRadius();
        r2 = cone.getTopRadius();
        h = cone.getHeight();

        glu.gluCylinder(gluQuadric, r1, r2, h, 16, 1);
        gl.glPushMatrix();
        gl.glRotated(180, 1, 0, 0);
        glu.gluDisk(gluQuadric, 0, r1, 16, 1);
        gl.glPopMatrix();

        if ( r2 > 0.0 ) {
            gl.glPushMatrix();
            gl.glTranslated(0, 0, h);
            glu.gluDisk(gluQuadric, 0, r1, 16, 1);
            gl.glPopMatrix();
        }

    }

    public static void draw(GL gl, Cone cone, Camera c, QualitySelection q)
    {
        if (glu == null) {
            glu = new GLU();
            gluQuadric = glu.gluNewQuadric();
        }

        // WARNING: Should be done here???
        gl.glEnable(gl.GL_CULL_FACE);
        gl.glCullFace(gl.GL_BACK);

        if ( q.isWiresSet() ) {
            gl.glLineWidth(1);
        }
        drawParts(gl, cone);

        if ( q.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, cone, q);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
