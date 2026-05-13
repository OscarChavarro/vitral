package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL2;

import vsdk.toolkit.environment.background.Background;
import vsdk.toolkit.environment.background.CubemapBackground;
import vsdk.toolkit.environment.background.FixedBackground;
import vsdk.toolkit.environment.background.SimpleBackground;

public class Jogl2BackgroundRenderer extends Jogl2Renderer 
{
    public static void draw(GL2 gl, Background background)
    {
        if ( background instanceof CubemapBackground ) {
            Jogl2CubemapBackgroundRenderer.draw(gl, (CubemapBackground)background);
        }
        else if ( background instanceof FixedBackground ) {
            Jogl2FixedBackgroundRenderer.draw(gl, (FixedBackground)background);
        }
        else if ( background instanceof SimpleBackground ) {
            Jogl2SimpleBackgroundRenderer.draw(gl, (SimpleBackground)background);
        }
    }
}
