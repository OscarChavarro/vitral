package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL2;

import vsdk.toolkit.environment.light.Light;

public class Jogl4LightRenderer extends Jogl4Renderer
{
    public static void activate(GL2 gl, Light light)
    {
        Jogl2LightRenderer.activate(gl, light);
    }

    public static void draw(GL2 gl, Light light)
    {
        Jogl2LightRenderer.draw(gl, light);
    }
}
