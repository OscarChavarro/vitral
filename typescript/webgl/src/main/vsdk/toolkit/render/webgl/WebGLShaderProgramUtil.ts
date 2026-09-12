import { WebGLShaderLoader } from "./WebGLShaderLoader.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4ShaderProgramUtil`.

Two runtime boundaries are crossed here:

  - Shader sources are fetched, not read from disk, so
    `createProgramFromFiles` is asynchronous. See {@link WebGLShaderLoader}.
  - Java calls `glBindFragDataLocation(program, 0, "fragColor")` before
    linking. WebGL2 has no such entry point; GLSL ES 3.00 requires the
    fragment output location to be declared in the shader text instead, which
    is what `WebGLShaderPreprocessor` leaves in place when it rewrites the
    OpenGL 4 `layout(location = 0) out` declaration. The resulting binding is
    the same single output at location 0.

Java's `createProgramFromPaths` has no counterpart: a path is a file-system
concept, and the file flavor is `createProgramFromFiles` above.
*/
export class WebGLShaderProgramUtil {
    public static async createProgramFromFiles(
        gl: WebGL2RenderingContext,
        vertexShaderFile: string,
        fragmentShaderFile: string,
    ): Promise<WebGLProgram> {
        const vertexSource = await WebGLShaderLoader.readShaderSource(vertexShaderFile, "vertex");
        const fragmentSource = await WebGLShaderLoader.readShaderSource(fragmentShaderFile, "fragment");

        return WebGLShaderProgramUtil.createProgramFromSources(gl, vertexSource, fragmentSource);
    }

    public static createProgramFromSources(
        gl: WebGL2RenderingContext,
        vertexSource: string,
        fragmentSource: string,
    ): WebGLProgram {
        const vertexShader = WebGLShaderProgramUtil.compileShader(gl, gl.VERTEX_SHADER, vertexSource);
        const fragmentShader = WebGLShaderProgramUtil.compileShader(gl, gl.FRAGMENT_SHADER, fragmentSource);

        const program = gl.createProgram();
        if (program === null) {
            gl.deleteShader(vertexShader);
            gl.deleteShader(fragmentShader);
            throw new Error("Failed to create shader program");
        }
        gl.attachShader(program, vertexShader);
        gl.attachShader(program, fragmentShader);
        gl.linkProgram(program);

        const linkStatus = gl.getProgramParameter(program, gl.LINK_STATUS) as boolean;
        if (!linkStatus) {
            const log = WebGLShaderProgramUtil.getProgramInfoLog(gl, program);
            gl.deleteShader(vertexShader);
            gl.deleteShader(fragmentShader);
            gl.deleteProgram(program);
            throw new Error("Program link error: " + log);
        }

        gl.detachShader(program, vertexShader);
        gl.detachShader(program, fragmentShader);
        gl.deleteShader(vertexShader);
        gl.deleteShader(fragmentShader);

        return program;
    }

    public static compileShader(gl: WebGL2RenderingContext, shaderType: number, source: string): WebGLShader {
        const shader = gl.createShader(shaderType);
        if (shader === null) {
            throw new Error("Failed to create shader");
        }
        gl.shaderSource(shader, source);
        gl.compileShader(shader);

        const compileStatus = gl.getShaderParameter(shader, gl.COMPILE_STATUS) as boolean;
        if (!compileStatus) {
            const log = WebGLShaderProgramUtil.getShaderInfoLog(gl, shader);
            gl.deleteShader(shader);
            throw new Error("Shader compile error: " + log);
        }

        return shader;
    }

    public static getShaderInfoLog(gl: WebGL2RenderingContext, shader: WebGLShader): string {
        const log = gl.getShaderInfoLog(shader);
        if (log === null || log.length <= 1) {
            return "(no log)";
        }
        return log;
    }

    public static getProgramInfoLog(gl: WebGL2RenderingContext, program: WebGLProgram): string {
        const log = gl.getProgramInfoLog(program);
        if (log === null || log.length <= 1) {
            return "(no log)";
        }
        return log;
    }
}
