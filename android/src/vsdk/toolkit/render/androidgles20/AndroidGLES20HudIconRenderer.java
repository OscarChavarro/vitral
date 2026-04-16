package vsdk.toolkit.render.androidgles20;

// Java basic classes
import java.util.ArrayList;

// VSDK classes
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.gui.HudIcon;

/**
*/
public class AndroidGLES20HudIconRenderer extends AndroidGLES20Renderer {
    public static void draw(ArrayList<HudIcon> hudIcons, Camera camera)
    {
        int i;
        
        for ( i = 0; i < hudIcons.size(); i++ ) {
            HudIcon icon = hudIcons.get(i);
            drawImage(icon.getImage(), camera, icon.getX(), icon.getY(), true);
        }

    }
}
