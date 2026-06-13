package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL;

import vsdk.toolkit.environment.material.SimpleMaterial;

public class Jogl4SimpleMaterialRenderer extends Jogl4Renderer
{
    private static final SimpleMaterial DEFAULT_MATERIAL = new SimpleMaterial();
    private static SimpleMaterial activeMaterial = DEFAULT_MATERIAL;

    public static void activate(GL gl, SimpleMaterial material)
    {
        if ( material == null ) {
            activeMaterial = DEFAULT_MATERIAL;
            return;
        }
        activeMaterial = new SimpleMaterial(material);
    }

    public static SimpleMaterial getActiveMaterial()
    {
        return new SimpleMaterial(activeMaterial);
    }
}
