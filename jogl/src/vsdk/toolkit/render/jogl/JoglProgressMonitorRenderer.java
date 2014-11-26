//===========================================================================
package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL2;

// VSDK classes
import vsdk.toolkit.gui.ProgressMonitor;

/**
*/
public class JoglProgressMonitorRenderer extends JoglRenderer {

    private static void drawFrame(
        GL2 gl,
        double x0, double y0, double xSize, double ySize,
        double xBorder, double yBorder)
    {
        gl.glBegin(GL2.GL_QUADS);
            gl.glVertex3d(x0 + xBorder, y0 + yBorder, 0);
            gl.glVertex3d(x0 + xSize - xBorder, y0 + yBorder, 0);
            gl.glVertex3d(x0 + xSize - xBorder, y0 + ySize - yBorder, 0);
            gl.glVertex3d(x0 + xBorder, y0 + ySize - yBorder, 0);
        gl.glEnd();
    }

    public static void draw(
        GL2 gl, ProgressMonitor monitor,
        double x0, double y0, double xSize, double ySize)
    {
        gl.glPushAttrib(GL2.GL_DEPTH_BITS);
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glPushMatrix();
        gl.glColor3d(0, 0, 0);
        drawFrame(gl, x0, y0, xSize, ySize, 0.0, 0.0);
        gl.glColor3d(0.6, 0.6, 0.6);
        drawFrame(gl, x0, y0, xSize, ySize, 0.05, 0.05);

        gl.glColor3d(0.0, 1.0, 0.0);
        drawFrame(gl, x0 + 0.05, y0 + 0.05, (xSize - 2*0.05)*monitor.getCurrentPercent()/100.0, ySize - 2*0.05, 0, 0);

        gl.glPopMatrix();
        gl.glPopAttrib();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
