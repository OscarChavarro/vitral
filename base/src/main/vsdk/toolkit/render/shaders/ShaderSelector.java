package vsdk.toolkit.render.shaders;

import vsdk.toolkit.common.RendererConfiguration;

public final class ShaderSelector {
    private ShaderSelector()
    {
    }

    public static Shader select(RendererConfiguration qualitySelection)
    {
        int shadingType = qualitySelection.getShadingType();
        boolean textureEnabled = qualitySelection.isTextureSet();
        boolean bumpMapEnabled = qualitySelection.isBumpMapSet();

        if ( shadingType == RendererConfiguration.SHADING_TYPE_NOLIGHT ) {
            if ( textureEnabled ) {
                return new ConstantTextureShader();
            }
            return new ConstantShader();
        }

        if ( shadingType == RendererConfiguration.SHADING_TYPE_FLAT ) {
            if ( textureEnabled ) {
                return new FlatTexturedShader();
            }
            return new FlatShader();
        }

        if ( shadingType == RendererConfiguration.SHADING_TYPE_GOURAUD ) {
            return new GouraudTextureShader(textureEnabled);
        }

        if ( shadingType == RendererConfiguration.SHADING_TYPE_PHONG ) {
            if ( bumpMapEnabled ) {
                if ( textureEnabled ) {
                    return new PhongTextureBumpShader();
                }
                return new PhongBumpShader();
            }
            if ( textureEnabled ) {
                return new PhongTextureShader();
            }
            return new PhongShader();
        }

        return new GouraudTextureShader(textureEnabled);
    }
}
