package vsdk.toolkit.render;
import vsdk.toolkit.render.shaders.Shader;

final class RenderContext {
    final boolean localLightingEnabled;
    final boolean textureEnabled;
    final boolean bumpMappingEnabled;
    final Shader localShader;

    RenderContext(
        boolean localLightingEnabled,
        boolean textureEnabled,
        boolean bumpMappingEnabled,
        Shader localShader)
    {
        this.localLightingEnabled = localLightingEnabled;
        this.textureEnabled = textureEnabled;
        this.bumpMappingEnabled = bumpMappingEnabled;
        this.localShader = localShader;
    }
}
