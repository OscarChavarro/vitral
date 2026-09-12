import { LightingShader } from "./LightingShader.js";

// GLSL analogue: phongPixelShader.glsl
export class PhongShader extends LightingShader {
    public constructor() {
        super(true, false, false);
    }
}
