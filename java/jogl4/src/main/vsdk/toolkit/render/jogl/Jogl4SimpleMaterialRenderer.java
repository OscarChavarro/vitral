package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL2;

import vsdk.toolkit.environment.material.SimpleMaterial;

public class Jogl4SimpleMaterialRenderer extends Jogl4Renderer
{
    public static void activate(GL2 gl, SimpleMaterial material)
    {
        Jogl2SimpleMaterialRenderer.activate(gl, material);
    }
}
