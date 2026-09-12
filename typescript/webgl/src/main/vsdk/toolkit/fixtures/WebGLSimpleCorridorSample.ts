import { Matrix4x4d } from "@vitral/base";
import { WebGLShaderPreprocessor } from "../render/webgl/WebGLShaderPreprocessor.js";

const SHADER_BASE_URL = "/etc/glslShaders";

export class WebGLSimpleCorridorSample {
    private readonly a = 6;
    private readonly na = 6;
    private readonly b = 20;
    private readonly nb = 20;
    private readonly c = 4;
    private readonly nc = 4;
    private readonly interSpace = 0.05;

    private initialized = false;
    private shaderProgram: WebGLProgram | null = null;
    private vertexArray: WebGLVertexArrayObject | null = null;
    private positionBuffer: WebGLBuffer | null = null;
    private colorBuffer: WebGLBuffer | null = null;
    private modelViewProjectionLocalLoc: WebGLUniformLocation | null = null;
    private withTextureLoc: WebGLUniformLocation | null = null;
    private withVertexColorsLoc: WebGLUniformLocation | null = null;
    private diffuseColorLoc: WebGLUniformLocation | null = null;
    private vertexCount = 0;

    /**
    Builds this fixture's geometry and shader program without drawing.

    Java has no counterpart: `Jogl4SimpleCorridorSample.drawGL` compiles on
    first use, and reading a GLSL file there is synchronous. Here a shader
    source arrives over `fetch`, so a first frame that compiled on demand would
    span a browser task boundary, and a WebGL drawing buffer is presented and
    cleared at such a boundary unless `preserveDrawingBuffer` is set: whatever
    was drawn before the boundary would be lost. Callers therefore prepare
    their resources once, which is what a JOGL program does in
    `init(GLAutoDrawable)`, and only then draw frames that never await a
    network read.
    */
    public async prepare(gl: WebGL2RenderingContext): Promise<void> {
        if (!this.initialized) {
            await this.initialize(gl);
        }
    }

    public async drawGL(gl: WebGL2RenderingContext, modelViewProjection: Matrix4x4d): Promise<void> {
        if (!this.initialized) {
            await this.initialize(gl);
        }

        gl.enable(gl.CULL_FACE);
        gl.cullFace(gl.BACK);

        gl.useProgram(this.shaderProgram);
        gl.uniformMatrix4fv(
            this.modelViewProjectionLocalLoc,
            false,
            modelViewProjection.exportToFloatArrayColumnOrder(),
        );

        if (this.withTextureLoc) {
            gl.uniform1i(this.withTextureLoc, 0);
        }
        if (this.withVertexColorsLoc) {
            gl.uniform1i(this.withVertexColorsLoc, 1);
        }
        if (this.diffuseColorLoc) {
            gl.uniform3f(this.diffuseColorLoc, 1.0, 1.0, 1.0);
        }

        gl.bindVertexArray(this.vertexArray);
        gl.drawArrays(gl.TRIANGLES, 0, this.vertexCount);
        gl.bindVertexArray(null);
        gl.useProgram(null);
    }

    public dispose(gl: WebGL2RenderingContext): void {
        if (this.positionBuffer) {
            gl.deleteBuffer(this.positionBuffer);
            this.positionBuffer = null;
        }
        if (this.colorBuffer) {
            gl.deleteBuffer(this.colorBuffer);
            this.colorBuffer = null;
        }
        if (this.vertexArray) {
            gl.deleteVertexArray(this.vertexArray);
            this.vertexArray = null;
        }
        if (this.shaderProgram) {
            gl.deleteProgram(this.shaderProgram);
            this.shaderProgram = null;
        }

        this.initialized = false;
        this.vertexCount = 0;
        this.modelViewProjectionLocalLoc = null;
        this.withTextureLoc = null;
        this.withVertexColorsLoc = null;
        this.diffuseColorLoc = null;
    }

    private async initialize(gl: WebGL2RenderingContext): Promise<void> {
        const geometry = this.buildGeometry();
        const program = await this.createShaderProgram(gl);

        this.shaderProgram = program;
        this.modelViewProjectionLocalLoc = gl.getUniformLocation(program, "modelViewProjectionLocal");
        if (!this.modelViewProjectionLocalLoc) {
            throw new Error("Missing modelViewProjectionLocal uniform");
        }
        this.withTextureLoc = gl.getUniformLocation(program, "withTexture");
        this.withVertexColorsLoc = gl.getUniformLocation(program, "withVertexColors");
        this.diffuseColorLoc = gl.getUniformLocation(program, "diffuseColor");

        this.vertexArray = gl.createVertexArray();
        this.positionBuffer = gl.createBuffer();
        this.colorBuffer = gl.createBuffer();
        if (!this.vertexArray || !this.positionBuffer || !this.colorBuffer) {
            throw new Error("Failed to create WebGL simple corridor resources.");
        }

        gl.bindVertexArray(this.vertexArray);

        gl.bindBuffer(gl.ARRAY_BUFFER, this.positionBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, geometry.positions, gl.STATIC_DRAW);
        gl.enableVertexAttribArray(0);
        gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);

        gl.bindBuffer(gl.ARRAY_BUFFER, this.colorBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, geometry.colors, gl.STATIC_DRAW);
        gl.enableVertexAttribArray(1);
        gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 0, 0);

        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);

        this.vertexCount = geometry.positions.length / 3;
        this.initialized = true;
    }

    private async createShaderProgram(gl: WebGL2RenderingContext): Promise<WebGLProgram> {
        const [vertexSource, fragmentSource] = await Promise.all([
            this.readShaderSource("constantVertexShader.glsl", "vertex"),
            this.readShaderSource("constantPixelShader.glsl", "fragment"),
        ]);
        const vertexShader = this.compileShader(gl, gl.VERTEX_SHADER, vertexSource);
        const fragmentShader = this.compileShader(gl, gl.FRAGMENT_SHADER, fragmentSource);
        const program = gl.createProgram();
        if (!program) {
            throw new Error("Failed to create WebGL simple corridor shader program.");
        }

        gl.attachShader(program, vertexShader);
        gl.attachShader(program, fragmentShader);
        gl.linkProgram(program);
        gl.deleteShader(vertexShader);
        gl.deleteShader(fragmentShader);

        if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
            const log = gl.getProgramInfoLog(program) ?? "(no log)";
            gl.deleteProgram(program);
            throw new Error(`WebGL simple corridor program link error: ${log}`);
        }

        return program;
    }

    private compileShader(gl: WebGL2RenderingContext, shaderType: number, source: string): WebGLShader {
        const shader = gl.createShader(shaderType);
        if (!shader) {
            throw new Error("Failed to create WebGL simple corridor shader.");
        }
        gl.shaderSource(shader, source);
        gl.compileShader(shader);
        if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
            const log = gl.getShaderInfoLog(shader) ?? "(no log)";
            gl.deleteShader(shader);
            throw new Error(`WebGL simple corridor shader compile error: ${log}`);
        }
        return shader;
    }

    private async readShaderSource(shaderFileName: string, shaderKind: "vertex" | "fragment"): Promise<string> {
        const response = await fetch(`${SHADER_BASE_URL}/${shaderFileName}`);
        if (!response.ok) {
            throw new Error(`Shader not found: ${shaderFileName}`);
        }
        return WebGLShaderPreprocessor.preprocess(await response.text(), shaderKind);
    }

    private buildGeometry(): { positions: Float32Array; colors: Float32Array } {
        const positions: number[] = [];
        const colors: number[] = [];

        this.appendTilesCenter(positions, colors, 0.5, 0.5, 0.9, 0, false, 0);
        for (let i = 0; i < 4; i++) {
            this.appendTilesLong(positions, colors, 0.5, 0.5, 0.9, 90 * i, false, 0);
        }

        this.appendTilesCenter(positions, colors, 0.0, 0.0, 1.0, 0, true, this.c);
        for (let i = 0; i < 4; i++) {
            this.appendTilesLong(positions, colors, 0.0, 0.0, 1.0, 90 * i, true, this.c);
        }

        for (let i = 0; i < 4; i++) {
            switch (i) {
                case 0:
                    this.appendTilesWallA(positions, colors, 0.9, 0.5, 0.5, 90 * i);
                    break;
                case 1:
                    this.appendTilesWallA(positions, colors, 0.5, 0.9, 0.5, 90 * i);
                    break;
                case 2:
                    this.appendTilesWallA(positions, colors, 1.0, 0.0, 0.0, 90 * i);
                    break;
                default:
                    this.appendTilesWallA(positions, colors, 0.0, 1.0, 0.0, 90 * i);
                    break;
            }
        }

        for (let i = 0; i < 4; i++) {
            this.appendTilesWallB(positions, colors, 0.9, 0.5, 0.8, 90 * i);
            this.appendTilesWallC(positions, colors, 0.9, 0.5, 0.8, 90 * i);
        }

        return { positions: new Float32Array(positions), colors: new Float32Array(colors) };
    }

    private appendTilesCenter(
        positions: number[],
        colors: number[],
        r: number,
        g: number,
        bColor: number,
        rotZDeg: number,
        flipYZ: boolean,
        translateZ: number,
    ): void {
        const da = this.a / this.na;
        const epsilon = 0.005;

        for (let i = 0; i < this.na; i++) {
            const x = -this.a / 2 + i * da;
            for (let j = 0; j < this.na; j++) {
                const y = -this.a / 2 + j * da;
                this.addQuad(
                    positions,
                    colors,
                    r,
                    g,
                    bColor,
                    x + this.interSpace / 2,
                    y + this.interSpace / 2,
                    -epsilon,
                    x + da - this.interSpace / 2,
                    y + this.interSpace / 2,
                    -epsilon,
                    x + da - this.interSpace / 2,
                    y + da - this.interSpace / 2,
                    -epsilon,
                    x + this.interSpace / 2,
                    y + da - this.interSpace / 2,
                    -epsilon,
                    rotZDeg,
                    flipYZ,
                    translateZ,
                );
            }
        }
    }

    private appendTilesLong(
        positions: number[],
        colors: number[],
        r: number,
        g: number,
        bColor: number,
        rotZDeg: number,
        flipYZ: boolean,
        translateZ: number,
    ): void {
        const da = this.a / this.na;
        const db = this.b / this.nb;
        const epsilon = 0.001;

        for (let i = 0; i < this.nb; i++) {
            const x = -this.a / 2 - this.b + i * db;
            for (let j = 0; j < this.na; j++) {
                const y = -this.a / 2 + j * da;
                this.addQuad(
                    positions,
                    colors,
                    r,
                    g,
                    bColor,
                    x + this.interSpace / 2,
                    y + this.interSpace / 2,
                    -epsilon,
                    x + da - this.interSpace / 2,
                    y + this.interSpace / 2,
                    -epsilon,
                    x + da - this.interSpace / 2,
                    y + da - this.interSpace / 2,
                    -epsilon,
                    x + this.interSpace / 2,
                    y + da - this.interSpace / 2,
                    -epsilon,
                    rotZDeg,
                    flipYZ,
                    translateZ,
                );
            }
        }
    }

    private appendTilesWallA(
        positions: number[],
        colors: number[],
        r: number,
        g: number,
        bColor: number,
        rotZDeg: number,
    ): void {
        const da = this.a / this.na;
        const dc = this.c / this.nc;

        for (let i = 0; i < this.nc; i++) {
            const z = i * dc;
            for (let j = 0; j < this.na; j++) {
                const y = -this.a / 2 + j * da;
                this.addQuad(
                    positions,
                    colors,
                    r,
                    g,
                    bColor,
                    -this.a / 2 - this.b,
                    y + this.interSpace / 2,
                    z + dc - this.interSpace / 2,
                    -this.a / 2 - this.b,
                    y + this.interSpace / 2,
                    z + this.interSpace / 2,
                    -this.a / 2 - this.b,
                    y + da - this.interSpace / 2,
                    z + this.interSpace / 2,
                    -this.a / 2 - this.b,
                    y + da - this.interSpace / 2,
                    z + dc - this.interSpace / 2,
                    rotZDeg,
                    false,
                    0,
                );
            }
        }
    }

    private appendTilesWallB(
        positions: number[],
        colors: number[],
        r: number,
        g: number,
        bColor: number,
        rotZDeg: number,
    ): void {
        const db = this.b / this.nb;
        const dc = this.c / this.nc;

        for (let i = 0; i < this.nc; i++) {
            const z = i * dc;
            for (let j = 0; j < this.nb; j++) {
                const y = this.a / 2 + j * db;
                this.addQuad(
                    positions,
                    colors,
                    r,
                    g,
                    bColor,
                    -this.a / 2,
                    y + this.interSpace / 2,
                    z + dc - this.interSpace / 2,
                    -this.a / 2,
                    y + this.interSpace / 2,
                    z + this.interSpace / 2,
                    -this.a / 2,
                    y + db - this.interSpace / 2,
                    z + this.interSpace / 2,
                    -this.a / 2,
                    y + db - this.interSpace / 2,
                    z + dc - this.interSpace / 2,
                    rotZDeg,
                    false,
                    0,
                );
            }
        }
    }

    private appendTilesWallC(
        positions: number[],
        colors: number[],
        r: number,
        g: number,
        bColor: number,
        rotZDeg: number,
    ): void {
        const db = this.b / this.nb;
        const dc = this.c / this.nc;

        for (let i = 0; i < this.nb; i++) {
            const x = -this.a / 2 - this.b + i * db;
            for (let j = 0; j < this.nc; j++) {
                const z = j * dc;
                this.addQuad(
                    positions,
                    colors,
                    r,
                    g,
                    bColor,
                    x + this.interSpace / 2,
                    this.a / 2,
                    z + this.interSpace / 2,
                    x + db - this.interSpace / 2,
                    this.a / 2,
                    z + this.interSpace / 2,
                    x + db - this.interSpace / 2,
                    this.a / 2,
                    z + dc - this.interSpace / 2,
                    x + this.interSpace / 2,
                    this.a / 2,
                    z + dc - this.interSpace / 2,
                    rotZDeg,
                    false,
                    0,
                );
            }
        }
    }

    private addQuad(
        positions: number[],
        colors: number[],
        r: number,
        g: number,
        bColor: number,
        x1: number,
        y1: number,
        z1: number,
        x2: number,
        y2: number,
        z2: number,
        x3: number,
        y3: number,
        z3: number,
        x4: number,
        y4: number,
        z4: number,
        rotZDeg: number,
        flipYZ: boolean,
        translateZ: number,
    ): void {
        this.addVertex(positions, colors, x1, y1, z1, r, g, bColor, rotZDeg, flipYZ, translateZ);
        this.addVertex(positions, colors, x2, y2, z2, r, g, bColor, rotZDeg, flipYZ, translateZ);
        this.addVertex(positions, colors, x3, y3, z3, r, g, bColor, rotZDeg, flipYZ, translateZ);
        this.addVertex(positions, colors, x1, y1, z1, r, g, bColor, rotZDeg, flipYZ, translateZ);
        this.addVertex(positions, colors, x3, y3, z3, r, g, bColor, rotZDeg, flipYZ, translateZ);
        this.addVertex(positions, colors, x4, y4, z4, r, g, bColor, rotZDeg, flipYZ, translateZ);
    }

    private addVertex(
        positions: number[],
        colors: number[],
        x: number,
        y: number,
        z: number,
        r: number,
        g: number,
        bColor: number,
        rotZDeg: number,
        flipYZ: boolean,
        translateZ: number,
    ): void {
        let tx = x;
        let ty = y;
        let tz = z;

        if (flipYZ) {
            ty = -ty;
            tz = -tz;
        }

        const angle = (rotZDeg * Math.PI) / 180;
        const cos = Math.cos(angle);
        const sin = Math.sin(angle);
        const rx = tx * cos - ty * sin;
        const ry = tx * sin + ty * cos;
        const rz = tz + translateZ;

        positions.push(rx, ry, rz);
        colors.push(r, g, bColor);
    }
}
