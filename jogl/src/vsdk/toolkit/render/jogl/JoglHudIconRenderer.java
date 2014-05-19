//===========================================================================
package vsdk.toolkit.render.jogl;

// Java basic classes
import java.util.ArrayList;

// JOGL classes
import javax.media.opengl.GL2;

// VSDK classes
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.gui.HudIcon;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;

/**
*/
public class JoglHudIconRenderer {
    private static final Image xxx;
    static {
        xxx = new RGBImage();
        xxx.init(8, 8);
        xxx.createTestPattern();
    }
    
    public static void activateDefaultTextureParameters(GL2 gl)
    {
        gl.glTexParameterf(GL2.GL_TEXTURE_2D, 
            GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameterf(GL2.GL_TEXTURE_2D,
                GL2.GL_TEXTURE_MAG_FILTER,
                GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
                GL2.GL_REPEAT);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
                GL2.GL_REPEAT);
    }

    public static void drawImage(GL2 gl, Image img, Camera c, int x, int y)
    {
        double fx, fy;
        double dx, dy;

        if ( img == null ) {
            return;
        }
        
        fx = (((double)img.getXSize()) * 2.0) / 
             ((double)c.getViewportXSize());

        fy = (((double)img.getYSize()) * 2.0) / 
             ((double)c.getViewportYSize());

        dx = ((double)img.getXSize() + x) / 
            ((double)c.getViewportXSize());

        dy = ((double)img.getYSize() + y) / 
            ((double)c.getViewportYSize());

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslated(-1.0 + dx, 1.0 - dy, 0);
        gl.glScaled(fx, fy, 1.0);
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        JoglImageRenderer.activate(gl, img);
        activateDefaultTextureParameters(gl);
        drawUnitSquare(gl);
    }

    public static void draw(GL2 gl, ArrayList<HudIcon> hudIcons, Camera camera) 
    {
        int i;
        
        for ( i = 0; i < hudIcons.size(); i++ ) {
            HudIcon icon = hudIcons.get(i);
            drawImage(gl, icon.getImage(), camera, icon.getX(), icon.getY());
        }
    }    

    private static void drawUnitSquare(GL2 gl) {
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glColor3d(1, 1, 1);
        gl.glBegin(GL2.GL_QUADS);
            gl.glTexCoord2d(0, 0);
            gl.glVertex2d(-0.5, -0.5);
            
            gl.glTexCoord2d(1, 0);
            gl.glVertex2d(0.5, -0.5);
            
            gl.glTexCoord2d(1, 1);
            gl.glVertex2d(0.5, 0.5);
            
            gl.glTexCoord2d(0, 1);
            gl.glVertex2d(-0.5, 0.5);
        gl.glEnd();

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
