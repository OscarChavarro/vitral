package vsdk.toolkit.render;

final class RenderContext {
    final boolean localLightingEnabled;
    final boolean textureEnabled;
    final boolean bumpMappingEnabled;

    RenderContext(
        boolean localLightingEnabled,
        boolean textureEnabled,
        boolean bumpMappingEnabled)
    {
        this.localLightingEnabled = localLightingEnabled;
        this.textureEnabled = textureEnabled;
        this.bumpMappingEnabled = bumpMappingEnabled;
    }
}
