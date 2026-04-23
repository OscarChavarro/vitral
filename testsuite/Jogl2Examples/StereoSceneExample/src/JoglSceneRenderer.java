import com.jogamp.opengl.GL2;

import vsdk.toolkit.render.jogl.Jogl2CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl2SimpleBodyRenderer;
import vsdk.toolkit.render.jogl.Jogl2LightRenderer;

public class JoglSceneRenderer
{
    public static void draw(GL2 gl, Scene s)
    {
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glDisable(gl.GL_LIGHTING);

        Jogl2CameraRenderer.activate(gl, s.activeCamera);

        //-----------------------------------------------------------------
        int i;

        for ( i = 0; i < s.lights.size(); i++ ) {
            Jogl2LightRenderer.activate(gl, s.lights.get(i));
                                      
        }

        for ( i = 0; i < s.bodies.size(); i++ ) {
            Jogl2SimpleBodyRenderer.draw(gl, s.bodies.get(i),
                                        s.camera, s.qualitySelection);
        }

        //-----------------------------------------------------------------
    }
}
