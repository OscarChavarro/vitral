import { WebGLShaderPreprocessor, type WebGLShaderKind } from "./WebGLShaderPreprocessor.js";

/**
Browser counterpart of `vsdk.toolkit.render.jogl.Jogl4ShaderLoader`.

Java searches the local file system for `etc/glslShaders/<name>`, walking up
from the working directory. A browser has no file system and no working
directory, so the same shader tree is served as a static asset and reached over
`fetch`. The GLSL sources themselves are the very same files: the Vitral
testsuite container publishes `etc/` verbatim, and
{@link WebGLShaderPreprocessor} adapts the OpenGL 4.1 core text to WebGL2
(GLSL ES 3.00), which is the only translation this runtime boundary needs.

Because `fetch` is asynchronous where `Files.readString` is not, every operation
that reaches a shader source is asynchronous in this port.
*/
export class WebGLShaderLoader {
    public static readonly SHADER_BASE_URL = "/etc/glslShaders";

    private static readonly sources = new Map<string, Promise<string>>();

    public static async readShaderSource(shaderFileName: string, shaderKind: WebGLShaderKind): Promise<string> {
        const cacheKey = shaderKind + ":" + shaderFileName;
        const cached = WebGLShaderLoader.sources.get(cacheKey);
        if (cached !== undefined) {
            return cached;
        }

        const pending = WebGLShaderLoader.fetchShaderSource(shaderFileName, shaderKind);
        WebGLShaderLoader.sources.set(cacheKey, pending);
        try {
            return await pending;
        } catch (error) {
            WebGLShaderLoader.sources.delete(cacheKey);
            throw error;
        }
    }

    private static async fetchShaderSource(shaderFileName: string, shaderKind: WebGLShaderKind): Promise<string> {
        const response = await fetch(`${WebGLShaderLoader.SHADER_BASE_URL}/${shaderFileName}`);
        if (!response.ok) {
            throw new Error(
                "Shader not found: " + shaderFileName + " (searched " + WebGLShaderLoader.SHADER_BASE_URL + ")",
            );
        }
        return WebGLShaderPreprocessor.preprocess(await response.text(), shaderKind);
    }
}
