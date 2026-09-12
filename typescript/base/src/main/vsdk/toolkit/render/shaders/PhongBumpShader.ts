import { LightingShader } from "./LightingShader.js";

// GLSL analogue: phongTextureBumpPixelShader.glsl with texture disabled
export class PhongBumpShader extends LightingShader {
    public constructor() {
        super(true, false, true);
    }
}
