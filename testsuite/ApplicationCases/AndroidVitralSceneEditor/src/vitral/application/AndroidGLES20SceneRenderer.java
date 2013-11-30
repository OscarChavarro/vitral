//===========================================================================

package vitral.application;

// Java classes
import java.util.ArrayList;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.androidgles20.AndroidGLES20SimpleBodyRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20CameraRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20LightRenderer;

public class AndroidGLES20SceneRenderer extends AndroidGLES20Renderer
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

    public static void draw(Scene s, RendererConfiguration r)
    {
        AndroidGLES20CameraRenderer.activate(s.camera);

        glMatrixMode(GL_MODELVIEW);

        for ( int i = 0; i < s.scene.getLights().size(); i++ ) {
            AndroidGLES20LightRenderer.draw(s.scene.getLights().get(i));
            AndroidGLES20LightRenderer.activate(s.scene.getLights().get(i));
        }
        
        glPushMatrix();
        drawBase(s, s.camera, r);
        glPopMatrix();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
