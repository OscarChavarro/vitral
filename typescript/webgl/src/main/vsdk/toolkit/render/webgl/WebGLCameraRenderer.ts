import { Camera, Matrix4x4d } from "@vitral/base";

interface LineRendererResources {
    program: WebGLProgram;
    vertexArray: WebGLVertexArrayObject;
    positionBuffer: WebGLBuffer;
    colorBuffer: WebGLBuffer;
    mvpLocation: WebGLUniformLocation;
}

export class WebGLCameraRenderer {
    private static readonly CAMERA_ORIGIN_COLOR = [1.0, 0.5, 0.5] as const;
    private static readonly FRUSTUM_COLOR = [0.0, 1.0, 1.0] as const;
    private static readonly resources = new WeakMap<WebGL2RenderingContext, LineRendererResources>();

    public static activate(_gl: WebGL2RenderingContext, camera: Camera): Matrix4x4d {
        return camera.calculateProjectionMatrix();
    }

    public static activateCenter(_gl: WebGL2RenderingContext, camera: Camera): Matrix4x4d {
        const centeredCamera = new Camera(camera);
        centeredCamera.setPosition(centeredCamera.getPosition().multiply(0));
        centeredCamera.setNearPlaneDistance(0.1);
        centeredCamera.setFarPlaneDistance(10.0);
        return centeredCamera.calculateProjectionMatrix();
    }

    public static draw(gl: WebGL2RenderingContext, camera: Camera, projection?: Matrix4x4d): void {
        const resolvedProjection = projection ?? camera.calculateProjectionMatrix();
        const model = camera.getRotation().withTranslation(camera.getPosition());
        const modelViewProjection = resolvedProjection.multiply(model);
        WebGLCameraRenderer.drawVolume(gl, camera, modelViewProjection);
    }

    public static drawVolume(gl: WebGL2RenderingContext, camera: Camera, mvp: Matrix4x4d): void {
        const nearPlaneDistance = camera.getNearPlaneDistance();
        const farPlaneDistance = camera.getFarPlaneDistance();
        let nearWidth: number;
        let nearHeight: number;
        let farWidth: number;
        let farHeight: number;

        if (camera.getProjectionMode() !== Camera.PROJECTION_MODE_ORTHOGONAL) {
            nearHeight = 2 * nearPlaneDistance * Math.tan((camera.getFov() * Math.PI) / 360);
            nearWidth = nearHeight * (camera.getViewportXSize() / camera.getViewportYSize());
        } else if (camera.getViewportXSize() > camera.getViewportYSize()) {
            nearWidth = 1;
            nearHeight = camera.getViewportYSize() / camera.getViewportXSize();
        } else {
            nearWidth = camera.getViewportXSize() / camera.getViewportYSize();
            nearHeight = 1;
        }

        farWidth = nearWidth;
        farHeight = nearHeight;
        if (camera.getProjectionMode() !== Camera.PROJECTION_MODE_ORTHOGONAL) {
            farHeight = 2 * farPlaneDistance * Math.tan((camera.getFov() * Math.PI) / 360);
            farWidth = farHeight * (camera.getViewportXSize() / camera.getViewportYSize());
        }

        const originPositions: number[] = [];
        const originColors: number[] = [];
        const delta = 0.1;
        WebGLCameraRenderer.addLine(
            originPositions,
            originColors,
            0,
            0,
            0,
            delta,
            0,
            0,
            WebGLCameraRenderer.CAMERA_ORIGIN_COLOR,
        );
        WebGLCameraRenderer.addLine(
            originPositions,
            originColors,
            0,
            -delta,
            0,
            0,
            delta,
            0,
            WebGLCameraRenderer.CAMERA_ORIGIN_COLOR,
        );
        WebGLCameraRenderer.addLine(
            originPositions,
            originColors,
            0,
            0,
            -delta,
            0,
            0,
            delta,
            WebGLCameraRenderer.CAMERA_ORIGIN_COLOR,
        );

        const frustumPositions: number[] = [];
        const frustumColors: number[] = [];
        WebGLCameraRenderer.addLoopRectangle(
            frustumPositions,
            frustumColors,
            nearPlaneDistance,
            nearWidth,
            nearHeight,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );
        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            nearPlaneDistance,
            -nearWidth / 10,
            -nearHeight / 10,
            nearPlaneDistance,
            nearWidth / 10,
            nearHeight / 10,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );
        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            nearPlaneDistance,
            nearWidth / 10,
            -nearHeight / 10,
            nearPlaneDistance,
            -nearWidth / 10,
            nearHeight / 10,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );
        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            nearPlaneDistance,
            -nearWidth / 10,
            nearHeight / 2,
            nearPlaneDistance,
            0,
            nearHeight / 2 + nearHeight / 10,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );
        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            nearPlaneDistance,
            0,
            nearHeight / 2 + nearHeight / 10,
            nearPlaneDistance,
            nearWidth / 10,
            nearHeight / 2,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );

        WebGLCameraRenderer.addLoopRectangle(
            frustumPositions,
            frustumColors,
            farPlaneDistance,
            farWidth,
            farHeight,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );
        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            farPlaneDistance,
            -farWidth / 10,
            -farHeight / 10,
            farPlaneDistance,
            farWidth / 10,
            farHeight / 10,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );
        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            farPlaneDistance,
            farWidth / 10,
            -farHeight / 10,
            farPlaneDistance,
            -farWidth / 10,
            farHeight / 10,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );
        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            farPlaneDistance,
            -farWidth / 10,
            farHeight / 2,
            farPlaneDistance,
            0,
            farHeight / 2 + farHeight / 10,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );
        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            farPlaneDistance,
            0,
            farHeight / 2 + farHeight / 10,
            farPlaneDistance,
            farWidth / 10,
            farHeight / 2,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );

        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            nearPlaneDistance,
            -nearWidth / 2,
            -nearHeight / 2,
            farPlaneDistance,
            -farWidth / 2,
            -farHeight / 2,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );
        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            nearPlaneDistance,
            nearWidth / 2,
            -nearHeight / 2,
            farPlaneDistance,
            farWidth / 2,
            -farHeight / 2,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );
        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            nearPlaneDistance,
            nearWidth / 2,
            nearHeight / 2,
            farPlaneDistance,
            farWidth / 2,
            farHeight / 2,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );
        WebGLCameraRenderer.addLine(
            frustumPositions,
            frustumColors,
            nearPlaneDistance,
            -nearWidth / 2,
            nearHeight / 2,
            farPlaneDistance,
            -farWidth / 2,
            farHeight / 2,
            WebGLCameraRenderer.FRUSTUM_COLOR,
        );

        WebGLCameraRenderer.drawLines(gl, mvp, new Float32Array(originPositions), new Float32Array(originColors), 1.0);
        WebGLCameraRenderer.drawLines(
            gl,
            mvp,
            new Float32Array(frustumPositions),
            new Float32Array(frustumColors),
            2.0,
        );
    }

    public static drawLines(
        gl: WebGL2RenderingContext,
        mvp: Matrix4x4d,
        positions: Float32Array,
        colors: Float32Array,
        lineWidth = 1.0,
    ): void {
        const resources = WebGLCameraRenderer.getResources(gl);
        gl.useProgram(resources.program);
        gl.bindVertexArray(resources.vertexArray);
        gl.uniformMatrix4fv(resources.mvpLocation, false, mvp.exportToFloatArrayColumnOrder());
        gl.bindBuffer(gl.ARRAY_BUFFER, resources.positionBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, positions, gl.STREAM_DRAW);
        gl.bindBuffer(gl.ARRAY_BUFFER, resources.colorBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, colors, gl.STREAM_DRAW);
        gl.lineWidth(lineWidth);
        gl.drawArrays(gl.LINES, 0, positions.length / 3);
        gl.bindVertexArray(null);
        gl.useProgram(null);
    }

    public static dispose(gl: WebGL2RenderingContext): void {
        const resources = WebGLCameraRenderer.resources.get(gl);
        if (!resources) return;
        gl.deleteBuffer(resources.positionBuffer);
        gl.deleteBuffer(resources.colorBuffer);
        gl.deleteVertexArray(resources.vertexArray);
        gl.deleteProgram(resources.program);
        WebGLCameraRenderer.resources.delete(gl);
    }

    private static getResources(gl: WebGL2RenderingContext): LineRendererResources {
        const existing = WebGLCameraRenderer.resources.get(gl);
        if (existing) return existing;

        const program = WebGLCameraRenderer.createProgram(gl);
        const vertexArray = gl.createVertexArray();
        const positionBuffer = gl.createBuffer();
        const colorBuffer = gl.createBuffer();
        const mvpLocation = gl.getUniformLocation(program, "modelViewProjectionLocal");
        if (!vertexArray || !positionBuffer || !colorBuffer || !mvpLocation) {
            throw new Error("Failed to create WebGL camera renderer resources.");
        }

        gl.bindVertexArray(vertexArray);
        gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
        gl.enableVertexAttribArray(0);
        gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);
        gl.bindBuffer(gl.ARRAY_BUFFER, colorBuffer);
        gl.enableVertexAttribArray(1);
        gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 0, 0);
        gl.bindVertexArray(null);

        const resources = { program, vertexArray, positionBuffer, colorBuffer, mvpLocation };
        WebGLCameraRenderer.resources.set(gl, resources);
        return resources;
    }

    private static createProgram(gl: WebGL2RenderingContext): WebGLProgram {
        const vertexShader = WebGLCameraRenderer.compileShader(
            gl,
            gl.VERTEX_SHADER,
            `#version 300 es
uniform mat4 modelViewProjectionLocal;
layout(location = 0) in vec3 PObject;
layout(location = 1) in vec3 emissionColor;
out vec3 vertexColor;
void main() {
    vertexColor = emissionColor;
    gl_Position = modelViewProjectionLocal * vec4(PObject, 1.0);
}`,
        );
        const fragmentShader = WebGLCameraRenderer.compileShader(
            gl,
            gl.FRAGMENT_SHADER,
            `#version 300 es
precision highp float;
in vec3 vertexColor;
out vec4 fragColor;
void main() {
    fragColor = vec4(vertexColor, 1.0);
}`,
        );
        const program = gl.createProgram();
        if (!program) throw new Error("Failed to create WebGL camera renderer shader program.");
        gl.attachShader(program, vertexShader);
        gl.attachShader(program, fragmentShader);
        gl.linkProgram(program);
        gl.deleteShader(vertexShader);
        gl.deleteShader(fragmentShader);
        if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
            const log = gl.getProgramInfoLog(program) ?? "(no log)";
            gl.deleteProgram(program);
            throw new Error(`WebGL camera renderer program link error: ${log}`);
        }
        return program;
    }

    private static compileShader(gl: WebGL2RenderingContext, shaderType: number, source: string): WebGLShader {
        const shader = gl.createShader(shaderType);
        if (!shader) throw new Error("Failed to create WebGL camera renderer shader.");
        gl.shaderSource(shader, source);
        gl.compileShader(shader);
        if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
            const log = gl.getShaderInfoLog(shader) ?? "(no log)";
            gl.deleteShader(shader);
            throw new Error(`WebGL camera renderer shader compile error: ${log}`);
        }
        return shader;
    }

    private static addLoopRectangle(
        positions: number[],
        colors: number[],
        x: number,
        width: number,
        height: number,
        rgb: readonly number[],
    ): void {
        WebGLCameraRenderer.addLine(positions, colors, x, -width / 2, -height / 2, x, width / 2, -height / 2, rgb);
        WebGLCameraRenderer.addLine(positions, colors, x, width / 2, -height / 2, x, width / 2, height / 2, rgb);
        WebGLCameraRenderer.addLine(positions, colors, x, width / 2, height / 2, x, -width / 2, height / 2, rgb);
        WebGLCameraRenderer.addLine(positions, colors, x, -width / 2, height / 2, x, -width / 2, -height / 2, rgb);
    }

    private static addLine(
        positions: number[],
        colors: number[],
        x1: number,
        y1: number,
        z1: number,
        x2: number,
        y2: number,
        z2: number,
        rgb: readonly number[],
    ): void {
        WebGLCameraRenderer.addVertex(positions, colors, x1, y1, z1, rgb);
        WebGLCameraRenderer.addVertex(positions, colors, x2, y2, z2, rgb);
    }

    private static addVertex(
        positions: number[],
        colors: number[],
        x: number,
        y: number,
        z: number,
        rgb: readonly number[],
    ): void {
        positions.push(x, y, z);
        colors.push(rgb[0] ?? 0, rgb[1] ?? 0, rgb[2] ?? 0);
    }
}
