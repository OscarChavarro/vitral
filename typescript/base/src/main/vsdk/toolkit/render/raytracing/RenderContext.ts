import type { Shader } from "../shaders/Shader.js";

/**
Package-private in Java; TypeScript has no package visibility, so the class is
exported but is not re-exported from the package index.
*/
export class RenderContext {
    public readonly localLightingEnabled: boolean;
    public readonly textureEnabled: boolean;
    public readonly bumpMappingEnabled: boolean;
    public readonly localShader: Shader;

    public constructor(
        localLightingEnabled: boolean,
        textureEnabled: boolean,
        bumpMappingEnabled: boolean,
        localShader: Shader,
    ) {
        this.localLightingEnabled = localLightingEnabled;
        this.textureEnabled = textureEnabled;
        this.bumpMappingEnabled = bumpMappingEnabled;
        this.localShader = localShader;
    }
}
