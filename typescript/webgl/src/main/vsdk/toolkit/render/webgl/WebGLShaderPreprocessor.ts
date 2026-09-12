export type WebGLShaderKind = "vertex" | "fragment";

export class WebGLShaderPreprocessor {
    public static preprocess(source: string, shaderKind: WebGLShaderKind): string {
        const commentlessSource = WebGLShaderPreprocessor.stripComments(source);
        let body = WebGLShaderPreprocessor.convertOpenGL4BodyToWebGL2(commentlessSource);
        if (shaderKind === "vertex") {
            body = WebGLShaderPreprocessor.injectPointSize(body);
        }
        const header = ["#version 300 es"];

        if (shaderKind === "fragment") {
            header.push("precision highp float;");
        }

        return [...header, "", body].join("\n").trimEnd() + "\n";
    }

    public static stripComments(source: string): string {
        let result = "";
        let index = 0;

        while (index < source.length) {
            const current = source[index];
            const next = source[index + 1];

            if (current === "/" && next === "/") {
                index += 2;
                while (index < source.length && source[index] !== "\n") {
                    index++;
                }
                continue;
            }

            if (current === "/" && next === "*") {
                index += 2;
                while (index < source.length) {
                    if (source[index] === "\n") {
                        result += "\n";
                        index++;
                        continue;
                    }
                    if (source[index] === "*" && source[index + 1] === "/") {
                        index += 2;
                        break;
                    }
                    index++;
                }
                continue;
            }

            result += current;
            index++;
        }

        return result;
    }

    /**
    Desktop OpenGL sizes a rasterized point with `glPointSize`, a client-side
    entry point that WebGL does not have: in GLSL ES the vertex shader must
    write `gl_PointSize` itself, and a shader that leaves it unwritten
    rasterizes points at an implementation-defined size.

    None of the VitralSDK shaders writes it, because none of them has to under
    OpenGL 4. So the declaration and the assignment are added here, driven by a
    `pointSizeLocal` uniform that a renderer sets exactly where the Java
    renderer calls `glPointSize`. An unset GLSL uniform reads as zero, so the
    assignment falls back to 1.0, which is OpenGL's own default point size;
    a shader used for anything but a point draw is therefore unaffected. A
    shader that already writes `gl_PointSize` is left alone.
    */
    private static injectPointSize(source: string): string {
        if (/\bgl_PointSize\b/.test(source)) {
            return source;
        }

        const mainPattern = /\bvoid\s+main\s*\(\s*(?:void)?\s*\)\s*\{/;
        const match = mainPattern.exec(source);
        if (match === null) {
            return source;
        }

        const insertionPoint = match.index + match[0].length;
        return (
            "uniform float pointSizeLocal;\n" +
            source.substring(0, insertionPoint) +
            "\n    gl_PointSize = pointSizeLocal > 0.0 ? pointSizeLocal : 1.0;" +
            source.substring(insertionPoint)
        );
    }

    private static convertOpenGL4BodyToWebGL2(source: string): string {
        return source
            .replace(/^\uFEFF/, "")
            .split(/\r?\n/)
            .map((line) => line.trim())
            .filter((line) => line.length > 0)
            .filter((line) => !/^#version\b/.test(line))
            .join("\n")
            .replace(/\blayout\s*\(\s*location\s*=\s*\d+\s*\)\s*out\b/g, "out");
    }
}
