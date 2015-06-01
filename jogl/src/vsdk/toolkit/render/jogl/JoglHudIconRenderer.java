//===========================================================================
package vsdk.toolkit.render.jogl;

// Java basic classes
import java.io.File;
import java.util.ArrayList;

// JOGL classes
import com.jogamp.opengl.GL2;

// VSDK classes
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.gui.HudIcon;
import vsdk.toolkit.io.image.ImagePersistence;

/**
 */
public class JoglHudIconRenderer extends JoglRenderer {

    public static void activateDefaultTextureParameters(GL2 gl) {
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

    public static void draw(GL2 gl, ArrayList<HudIcon> hudIcons, Camera camera) {
        int i;

        for (i = 0; i < hudIcons.size(); i++) {
            HudIcon icon = hudIcons.get(i);
            drawImageOn2DWindow(gl, icon.getImage(), camera, icon.getX(), icon.getY());
        }
    }

    public static void draw(GL2 gl, HudIcon icon, Camera camera, int index) {
        try {
            switch (index) {
                case 1:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero02.png")), camera, icon.getX(), icon.getY());
                    break;
                case 2:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero03.png")), camera, icon.getX(), icon.getY());
                    break;
                case 3:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero04.png")), camera, icon.getX(), icon.getY());
                    break;
                case 4:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero05.png")), camera, icon.getX(), icon.getY());
                    break;
                case 5:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero06.png")), camera, icon.getX(), icon.getY());
                    break;
                case 6:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero07.png")), camera, icon.getX(), icon.getY());
                    break;
                case 7:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero08.png")), camera, icon.getX(), icon.getY());
                    break;
                case 8:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero09.png")), camera, icon.getX(), icon.getY());
                    break;
                case 9:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero10.png")), camera, icon.getX(), icon.getY());
                    break;
                case 10:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero10.png")), camera, icon.getX(), icon.getY());
                    break;
                case 11:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero11.png")), camera, icon.getX(), icon.getY());
                    break;
                case 12:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero13.png")), camera, icon.getX(), icon.getY());
                    break;
                default:
                    drawImageOn2DWindow(gl, ImagePersistence.importRGBA(new File("/usr/local/mosquioids/etc/billboards/Numero01.png")), camera, icon.getX(), icon.getY());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
