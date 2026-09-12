export type WebGLShaderKind = "vertex" | "fragment";

export class WebGLShaderPreprocessor {
    public static preprocess(source: string, shaderKind: WebGLShaderKind): string {
        const commentlessSource = WebGLShaderPreprocessor.stripComments(source);
        let body = WebGLShaderPreprocessor.convertOpenGL4BodyToWebGL2(commentlessSource);
        if (shaderKind === "vertex") {
            body = WebGLShaderPreprocessor.injectPointSize(body);
            body = WebGLShaderPreprocessor.convertClipDistanceToVarying(body);
        } else {
            body = WebGLShaderPreprocessor.injectClipDistanceDiscard(body);
        }
        const header = ["#version 300 es"];

        if (shaderKind === "fragment") {
            header.push("precision highp float;");
        }
        // GLSL ES 3.00 gives `sampler2D` and `samplerCube` a default precision
        // but gives `sampler3D` none, so a shader that declares one fails to
        // compile with "No precision specified" unless a default is stated.
        // OpenGL 4.1 core has no precision qualifiers at all, which is why no
        // VitralSDK shader carries this. It is stated for both stages, since
        // either may sample a solid texture.
        header.push("precision highp sampler3D;");

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

    /**
    Desktop OpenGL clips a primitive against a user plane by writing
    `gl_ClipDistance[0]` in the vertex shader, and the rasterizer then discards
    whatever falls on the negative side. GLSL ES 3.00 has neither the array nor
    the `GL_CLIP_DISTANCE0` enable that goes with it (`EXT_clip_cull_distance`
    is an extension no VitralSDK target may assume), so the distance is carried
    across as an ordinary varying and the fragment shader discards on its sign.

    The translation is unconditional and symmetric on both stages, because a
    shader is preprocessed without knowing what it will be linked against: a
    fragment `in` with no matching vertex `out` is a link error. A vertex
    shader that never wrote a clip distance therefore emits the constant 1.0
    that OpenGL itself uses for a disabled plane, and its fragment partner's
    discard is then unreachable.

    The result is per-fragment rather than per-primitive clipping. For a plane
    cut through a solid that is the same image, since the clip plane only ever
    removes fragments; what it costs is the early-depth rejection a shader
    without a `discard` would get.
    */
    public static readonly CLIP_DISTANCE_VARYING = "clipDistanceLocal";

    private static convertClipDistanceToVarying(source: string): string {
        const declaration = "out float " + WebGLShaderPreprocessor.CLIP_DISTANCE_VARYING + ";\n";
        // A fresh regular expression per call: a `/g` one carries a `lastIndex`
        // from the test into the replace, and would skip the first match.
        if (/\bgl_ClipDistance\s*\[\s*0\s*\]\s*=/.test(source)) {
            return (
                declaration +
                source.replace(
                    /\bgl_ClipDistance\s*\[\s*0\s*\]\s*=/g,
                    WebGLShaderPreprocessor.CLIP_DISTANCE_VARYING + " =",
                )
            );
        }

        return (
            declaration +
            WebGLShaderPreprocessor.insertAtStartOfMain(
                source,
                "\n    " + WebGLShaderPreprocessor.CLIP_DISTANCE_VARYING + " = 1.0;",
            )
        );
    }

    private static injectClipDistanceDiscard(source: string): string {
        return (
            "in float " +
            WebGLShaderPreprocessor.CLIP_DISTANCE_VARYING +
            ";\n" +
            WebGLShaderPreprocessor.insertAtStartOfMain(
                source,
                "\n    if ( " + WebGLShaderPreprocessor.CLIP_DISTANCE_VARYING + " < 0.0 ) { discard; }",
            )
        );
    }

    private static insertAtStartOfMain(source: string, statement: string): string {
        const mainPattern = /\bvoid\s+main\s*\(\s*(?:void)?\s*\)\s*\{/;
        const match = mainPattern.exec(source);
        if (match === null) {
            return source;
        }
        const insertionPoint = match.index + match[0].length;
        return source.substring(0, insertionPoint) + statement + source.substring(insertionPoint);
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
