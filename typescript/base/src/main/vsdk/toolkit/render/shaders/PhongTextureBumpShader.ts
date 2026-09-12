import { LightingShader } from "./LightingShader.js";

// GLSL analogue: phongTextureBumpPixelShader.glsl
export class PhongTextureBumpShader extends LightingShader {
    public constructor() {
        super(true, true, true);
    }
}
