import { LightingShader } from "./LightingShader.js";

// GLSL analogue: flatTexturedPixelShader.glsl
export class FlatTexturedShader extends LightingShader {
    public constructor() {
        super(true, true, false);
    }
}
