//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 29 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Arrow;

public class JoglArrowRenderer extends JoglRenderer {

    private static GLU glu = null;
    private static GLUquadric gluQuadric = null;

    private static void drawParts(GL gl, Arrow arrow)
    {
        double r1, r2, h1, h2;

        h1 = arrow.getBaseLength();
        h2 = arrow.getHeadLength();
        r1 = arrow.getBaseRadius();
        r2 = arrow.getHeadRadius();

        glu.gluCylinder(gluQuadric, r1, r1, h1, 16, 1);
        gl.glPushMatrix();
        gl.glRotated(180, 1, 0, 0);
        glu.gluDisk(gluQuadric, 0, r1, 16, 1);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslated(0, 0, h1);
        glu.gluCylinder(gluQuadric, r2, 0, h2, 16, 1);
        gl.glRotated(180, 1, 0, 0);
        glu.gluDisk(gluQuadric, r1, r2, 16, 1);

        gl.glPopMatrix();
    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void draw(GL gl, Arrow arrow, Camera c, QualitySelection q)
    {
        if (glu == null) {
            glu = new GLU();
            gluQuadric = glu.gluNewQuadric();
        }

        if ( q.isWiresSet() ) {
            gl.glLineWidth(1);
        }
        drawParts(gl, arrow);

        if ( q.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, arrow, q);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
