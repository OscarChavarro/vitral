#include "ShaderSelector.h"
#include "ConstantShader.h"
#include "ConstantTextureShader.h"
#include "FlatShader.h"
#include "FlatTexturedShader.h"
#include "GouraudTextureShader.h"
#include "PhongShader.h"
#include "PhongBumpShader.h"
#include "PhongTextureShader.h"
#include "PhongTextureBumpShader.h"
#include "CookTorranceShader.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"

Shader* ShaderSelector::select(const RendererConfiguration* qualitySelection)
{
    int shadingType = qualitySelection->getShadingType();
    bool textureEnabled = qualitySelection->isTextureSet();
    bool bumpMapEnabled = qualitySelection->isBumpMapSet();

    if ( shadingType == RendererConfiguration::SHADING_TYPE_NOLIGHT ) {
        if ( textureEnabled ) return new ConstantTextureShader();
        return new ConstantShader();
    }
    if ( shadingType == RendererConfiguration::SHADING_TYPE_FLAT ) {
        if ( textureEnabled ) return new FlatTexturedShader();
        return new FlatShader();
    }
    if ( shadingType == RendererConfiguration::SHADING_TYPE_GOURAUD ) {
        return new GouraudTextureShader(textureEnabled);
    }
    if ( shadingType == RendererConfiguration::SHADING_TYPE_PHONG ) {
        if ( bumpMapEnabled ) {
            if ( textureEnabled ) return new PhongTextureBumpShader();
            return new PhongBumpShader();
        }
        if ( textureEnabled ) return new PhongTextureShader();
        return new PhongShader();
    }
    if ( shadingType == RendererConfiguration::SHADING_TYPE_COOK_TERRANCE ) {
        return new CookTorranceShader(textureEnabled, bumpMapEnabled);
    }
    return new GouraudTextureShader(textureEnabled);
}
