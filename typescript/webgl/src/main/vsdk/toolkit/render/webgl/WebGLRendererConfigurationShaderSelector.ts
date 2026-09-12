import { Matrix4x4d, RendererConfiguration } from "@vitral/base";
import { WebGLShaderProgramUtil } from "./WebGLShaderProgramUtil.js";

/**
The nine shader programs `Jogl4RendererConfigurationShaderSelector` keeps in
static fields, named by the same two GLSL files each one is built from.
*/
type ShaderProgramName =
    "constant" | "textured" | "flat" | "flatTextured" | "gouraud" | "phong" | "phongBump" | "cook" | "cookBump";

const SHADER_PROGRAM_FILES: Readonly<Record<ShaderProgramName, readonly [string, string]>> = {
    constant: ["constantVertexShader.glsl", "constantPixelShader.glsl"],
    textured: ["constantTextureVertexShader.glsl", "constantTexturePixelShader.glsl"],
    gouraud: ["gouraudTextureVertexShader.glsl", "gouraudTexturePixelShader.glsl"],
    flat: ["flatVertexShader.glsl", "flatPixelShader.glsl"],
    flatTextured: ["flatTexturedVertexShader.glsl", "flatTexturedPixelShader.glsl"],
    phong: ["phongTextureVertexShader.glsl", "phongTexturePixelShader.glsl"],
    phongBump: ["phongTextureBumpVertexShader.glsl", "phongTextureBumpPixelShader.glsl"],
    cook: ["phongTextureVertexShader.glsl", "cookTexturePixelShader.glsl"],
    cookBump: ["phongTextureBumpVertexShader.glsl", "cookTextureBumpPixelShader.glsl"],
};

/**
Port of `vsdk.toolkit.render.jogl.Jogl4RendererConfigurationShaderSelector`.

The selection rules, the program set, the GLSL file pairs, and the uniform
activation sequence are the Java ones. Three runtime boundaries are crossed:

  - Java keeps one program id per shader in a static field, because a JOGL
    application owns a single `GL4` context for its whole life. A browser page
    can hold several independent `WebGL2RenderingContext` objects, and a
    program belongs to exactly one of them, so the nine ids are held per
    context in a `WeakMap`, as the rest of `@vitral/webgl` already does.
  - Java's `ensurePrograms` compiles all nine programs the first time any of
    them is asked for, which it can do because reading a shader file is
    synchronous. Here a shader source arrives over `fetch`, so a program is
    compiled when the selection rules first return it. The set of programs, the
    files behind each one, and the selection outcome are unchanged; only the
    moment of compilation differs, and it is per program instead of all at
    once.
  - `glGetUniformLocation` answers a negative int for an absent uniform, while
    WebGL answers `null`; every `loc >= 0` guard of the Java original is a
    `!== null` guard here.
*/
export class WebGLRendererConfigurationShaderSelector {
    private static readonly programs = new WeakMap<WebGL2RenderingContext, Map<ShaderProgramName, WebGLProgram>>();

    public static async selectShaderProgram(
        gl: WebGL2RenderingContext,
        quality: RendererConfiguration | null,
    ): Promise<WebGLProgram> {
        if (quality !== null && quality.isTextureSet()) {
            return WebGLRendererConfigurationShaderSelector.ensureProgram(gl, "textured");
        }
        return WebGLRendererConfigurationShaderSelector.ensureProgram(gl, "constant");
    }

    public static async selectSurfaceShaderProgram(
        gl: WebGL2RenderingContext,
        quality: RendererConfiguration | null,
        hasTexture: boolean,
        hasNormalMap: boolean,
    ): Promise<WebGLProgram> {
        const ensure = (name: ShaderProgramName): Promise<WebGLProgram> =>
            WebGLRendererConfigurationShaderSelector.ensureProgram(gl, name);

        if (quality === null) {
            return ensure(hasTexture ? "textured" : "constant");
        }

        const shadingType = quality.getShadingType();

        if (shadingType === RendererConfiguration.SHADING_TYPE_NOLIGHT) {
            return ensure(quality.isTextureSet() && hasTexture ? "textured" : "constant");
        }

        if (shadingType === RendererConfiguration.SHADING_TYPE_FLAT) {
            return ensure(quality.isTextureSet() && hasTexture ? "flatTextured" : "flat");
        }

        if (shadingType === RendererConfiguration.SHADING_TYPE_PHONG) {
            if (quality.isBumpMapSet() && hasNormalMap) {
                return ensure("phongBump");
            }
            return ensure("phong");
        }

        if (shadingType === RendererConfiguration.SHADING_TYPE_COOK_TERRANCE) {
            if (quality.isBumpMapSet() && hasNormalMap) {
                return ensure("cookBump");
            }
            return ensure("cook");
        }

        return ensure("gouraud");
    }

    public static activateShader(
        gl: WebGL2RenderingContext,
        program: WebGLProgram,
        modelViewProjection: Matrix4x4d,
        quality: RendererConfiguration | null,
        diffuseR: number,
        diffuseG: number,
        diffuseB: number,
    ): void {
        gl.useProgram(program);

        const modelViewProjectionLoc = gl.getUniformLocation(program, "modelViewProjectionLocal");
        if (modelViewProjectionLoc !== null) {
            gl.uniformMatrix4fv(modelViewProjectionLoc, false, modelViewProjection.exportToFloatArrayColumnOrder());
        }

        const diffuseColorLoc = gl.getUniformLocation(program, "diffuseColor");
        if (diffuseColorLoc !== null) {
            gl.uniform3f(diffuseColorLoc, diffuseR, diffuseG, diffuseB);
        }

        const withTextureLoc = gl.getUniformLocation(program, "withTexture");
        if (withTextureLoc !== null) {
            const useTexture = quality !== null && quality.isTextureSet() ? 1 : 0;
            gl.uniform1i(withTextureLoc, useTexture);
        }

        const withVertexColorsLoc = gl.getUniformLocation(program, "withVertexColors");
        if (withVertexColorsLoc !== null) {
            const useVertexColors = quality !== null && quality.getUseVertexColors() ? 1 : 0;
            gl.uniform1i(withVertexColorsLoc, useVertexColors);
        }

        const textureSamplerLoc = gl.getUniformLocation(program, "sTexture");
        if (textureSamplerLoc !== null) {
            gl.uniform1i(textureSamplerLoc, 0);
        }

        const normalSamplerLoc = gl.getUniformLocation(program, "sNormalMap");
        if (normalSamplerLoc !== null) {
            gl.uniform1i(normalSamplerLoc, 1);
        }
    }

    public static deactivateShader(gl: WebGL2RenderingContext): void {
        gl.useProgram(null);
    }

    public static dispose(gl: WebGL2RenderingContext): void {
        const programs = WebGLRendererConfigurationShaderSelector.programs.get(gl);
        if (programs === undefined) {
            return;
        }
        for (const program of programs.values()) {
            gl.deleteProgram(program);
        }
        programs.clear();
        WebGLRendererConfigurationShaderSelector.programs.delete(gl);
    }

    private static async ensureProgram(gl: WebGL2RenderingContext, name: ShaderProgramName): Promise<WebGLProgram> {
        let programs = WebGLRendererConfigurationShaderSelector.programs.get(gl);
        if (programs === undefined) {
            programs = new Map<ShaderProgramName, WebGLProgram>();
            WebGLRendererConfigurationShaderSelector.programs.set(gl, programs);
        }

        const existing = programs.get(name);
        if (existing !== undefined) {
            return existing;
        }

        const files = SHADER_PROGRAM_FILES[name];
        const program = await WebGLShaderProgramUtil.createProgramFromFiles(gl, files[0], files[1]);

        // A concurrent caller may have finished the same program while this
        // one was awaiting its shader sources; keep the first one installed.
        const installed = programs.get(name);
        if (installed !== undefined) {
            gl.deleteProgram(program);
            return installed;
        }

        programs.set(name, program);
        return program;
    }
}
