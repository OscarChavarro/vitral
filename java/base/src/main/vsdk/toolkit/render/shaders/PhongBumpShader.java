package vsdk.toolkit.render.shaders;

// GLSL analogue: phongTextureBumpPixelShader.glsl with texture disabled
public final class PhongBumpShader extends LightingShader {
    public PhongBumpShader()
    {
        super(true, false, true);
    }
}
