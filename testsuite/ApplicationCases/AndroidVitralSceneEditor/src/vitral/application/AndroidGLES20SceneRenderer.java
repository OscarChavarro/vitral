//===========================================================================

package vitral.application;

// Java classes
import java.util.ArrayList;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.androidgles20.AndroidGLES20SimpleBodyRenderer;

public class AndroidGLES20SceneRenderer
{
    private static void drawBase(Scene s, Camera c, RendererConfiguration r)
    {
        int i;
        ArrayList<SimpleBody> bodies = s.scene.getSimpleBodies();
        SimpleBody gi;
        RendererConfiguration r2 = r.clone();

        for ( i = 0; i < bodies.size(); i++ ) {
            gi = bodies.get(i);
	    if ( i == s.selectedObjectIndex ) {
                r2.setSelectionCorners(true);
	    }
	    else {
                r2.setSelectionCorners(false);
	    }
	    AndroidGLES20SimpleBodyRenderer.draw(gi, c, r2);
	}
    }

    public static void draw(Scene s, Camera c, RendererConfiguration r)
    {
        drawBase(s, c, r);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
