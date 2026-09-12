import { LightingShader } from "./LightingShader.js";

// GLSL analogue: phongTexturePixelShader.glsl
export class PhongTextureShader extends LightingShader {
    public constructor() {
        super(true, true, false);
    }
}
