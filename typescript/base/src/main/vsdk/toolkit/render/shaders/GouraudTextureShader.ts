import { LightingShader } from "./LightingShader.js";

// GLSL analogue: gouraudTexturePixelShader.glsl
export class GouraudTextureShader extends LightingShader {
    public constructor(textureEnabled: boolean) {
        super(true, textureEnabled, false);
    }
}
