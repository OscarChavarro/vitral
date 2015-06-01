//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 15 2005 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL2;

import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.environment.FixedBackground;
import vsdk.toolkit.environment.SimpleBackground;

public class JoglBackgroundRenderer extends JoglRenderer 
{
    public static void draw(GL2 gl, Background background)
    {
        if ( background instanceof CubemapBackground ) {
            JoglCubemapBackgroundRenderer.draw(gl, (CubemapBackground)background);
        }
        else if ( background instanceof FixedBackground ) {
            JoglFixedBackgroundRenderer.draw(gl, (FixedBackground)background);
        }
        else if ( background instanceof SimpleBackground ) {
            JoglSimpleBackgroundRenderer.draw(gl, (SimpleBackground)background);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
