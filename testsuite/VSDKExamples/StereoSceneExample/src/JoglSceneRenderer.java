import javax.media.opengl.GL2;

import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;

public class JoglSceneRenderer
{
    public static void draw(GL2 gl, Scene s)
    {
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glDisable(gl.GL_LIGHTING);

        JoglCameraRenderer.activate(gl, s.activeCamera);

        //-----------------------------------------------------------------
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        int i;

	for ( i = 0; i < s.lights.size(); i++ ) {
            JoglLightRenderer.activate(gl, s.lights.get(i));
                                      
	}

	for ( i = 0; i < s.bodies.size(); i++ ) {
            JoglSimpleBodyRenderer.draw(gl, s.bodies.get(i),
                                        s.camera, s.qualitySelection);
	}

        //-----------------------------------------------------------------
    }
}
