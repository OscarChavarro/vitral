package vsdk.toolkit.render.shaders;

// GLSL analogue: gouraudTexturePixelShader.glsl
public final class GouraudTextureShader extends LightingShader {
    public GouraudTextureShader(boolean textureEnabled)
    {
        super(true, textureEnabled, false);
    }
}
