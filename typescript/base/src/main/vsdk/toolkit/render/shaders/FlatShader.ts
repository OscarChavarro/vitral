import { LightingShader } from "./LightingShader.js";

// GLSL analogue: flatPixelShader.glsl
export class FlatShader extends LightingShader {
    public constructor() {
        super(true, false, false);
    }
}
