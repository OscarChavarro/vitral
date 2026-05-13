package vsdk.toolkit.render.androidgles20;

// VSDK classes
import vsdk.toolkit.environment.Material;

public class AndroidGLES20MaterialRenderer extends AndroidGLES20Renderer
{
    public static void activate(Material m)
    {
        currentMaterial = new Material(m);
        activateShaders();
    }
}
