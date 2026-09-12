export type WebGLShaderKind = "vertex" | "fragment";

export class WebGLShaderPreprocessor {
    public static preprocess(source: string, shaderKind: WebGLShaderKind): string {
        const commentlessSource = WebGLShaderPreprocessor.stripComments(source);
        const body = WebGLShaderPreprocessor.convertOpenGL4BodyToWebGL2(commentlessSource);
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
