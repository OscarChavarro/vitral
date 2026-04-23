package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL2;

import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.environment.FixedBackground;
import vsdk.toolkit.environment.SimpleBackground;

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
