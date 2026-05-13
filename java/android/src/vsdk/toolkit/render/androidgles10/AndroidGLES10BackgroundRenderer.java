package vsdk.toolkit.render.androidgles10;

// VSDK classes
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.CubemapBackground;

public class AndroidGLES10BackgroundRenderer extends AndroidGLES10Renderer {
    public static void draw(final Background inBackground)
    {
        if ( inBackground == null ) {
            return;
        }
        if ( inBackground instanceof CubemapBackground ) {
            AndroidGLES10CubemapBackgroundRenderer.draw(
                (CubemapBackground)inBackground);
        }
    }
}
