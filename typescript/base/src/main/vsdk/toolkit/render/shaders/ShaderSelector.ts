import { RendererConfiguration } from "../../environment/material/RendererConfiguration.js";
import { ConstantShader } from "./ConstantShader.js";
import { ConstantTextureShader } from "./ConstantTextureShader.js";
import { CookTorranceShader } from "./CookTorranceShader.js";
import { FlatShader } from "./FlatShader.js";
import { FlatTexturedShader } from "./FlatTexturedShader.js";
import { GouraudTextureShader } from "./GouraudTextureShader.js";
import { PhongBumpShader } from "./PhongBumpShader.js";
import { PhongShader } from "./PhongShader.js";
import { PhongTextureBumpShader } from "./PhongTextureBumpShader.js";
import { PhongTextureShader } from "./PhongTextureShader.js";
import type { Shader } from "./Shader.js";

export class ShaderSelector {
    private constructor() {}

    public static select(qualitySelection: RendererConfiguration): Shader {
        const shadingType: number = qualitySelection.getShadingType();
        const textureEnabled: boolean = qualitySelection.isTextureSet();
        const bumpMapEnabled: boolean = qualitySelection.isBumpMapSet();

        if (shadingType === RendererConfiguration.SHADING_TYPE_NOLIGHT) {
            if (textureEnabled) {
                return new ConstantTextureShader();
            }
            return new ConstantShader();
        }

        if (shadingType === RendererConfiguration.SHADING_TYPE_FLAT) {
            if (textureEnabled) {
                return new FlatTexturedShader();
            }
            return new FlatShader();
        }

        if (shadingType === RendererConfiguration.SHADING_TYPE_GOURAUD) {
            return new GouraudTextureShader(textureEnabled);
        }

        if (shadingType === RendererConfiguration.SHADING_TYPE_PHONG) {
            if (bumpMapEnabled) {
                if (textureEnabled) {
                    return new PhongTextureBumpShader();
                }
                return new PhongBumpShader();
            }
            if (textureEnabled) {
                return new PhongTextureShader();
            }
            return new PhongShader();
        }

        if (shadingType === RendererConfiguration.SHADING_TYPE_COOK_TERRANCE) {
            return new CookTorranceShader(textureEnabled, bumpMapEnabled);
        }

        return new GouraudTextureShader(textureEnabled);
    }
}
