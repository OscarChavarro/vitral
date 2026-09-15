import { Matrix4x4d, Polygon2D, RendererConfiguration } from "@vitral/base";
import { _Polygon2DOddWindingTessellator } from "./_Polygon2DOddWindingTessellator.js";

/**
The five OpenGL object names `Jogl4Polygon2DRenderer.draw` receives from its
caller. Java passes five `int` ids, because a desktop GL name is an integer;
WebGL answers objects instead, so the same five travel together in one record.
The caller still owns them and still creates them in its own `init`, exactly as
`JoglPolygonClippingRenderer` does.
*/
export interface WebGLPolygon2DRendererResources {
    lineProgram: WebGLProgram;
    constantProgram: WebGLProgram;
    vertexArray: WebGLVertexArrayObject;
    positionBuffer: WebGLBuffer;
    colorBuffer: WebGLBuffer;
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4Polygon2DRenderer`.

The three passes are the Java ones and run in the Java order: the filled
surface first, then one wire loop per contour, then every contour vertex as a
point, each guarded by the matching `RendererConfiguration` flag. The uniform
names, the point size, the line width and the two attribute layouts -- a `vec4`
position alone for the constant program, a `vec3` position beside a `vec3`
colour for the line program -- are unchanged.

Four runtime boundaries are crossed:

  - `GLU.gluNewTess()` has no browser counterpart. The surface is produced by
    {@link _Polygon2DOddWindingTessellator} instead, which computes the same
    odd-winding region GLU's default rule covers; see that class for why the
    region, and not the particular triangles, is what has to agree.
  - `glGetUniformLocation` answers a negative int for an absent uniform and
    WebGL answers `null`, so each `loc >= 0` guard of the original is a
    `!== null` guard here. Java writes the uniform only when it is present and
    so does this.
  - Desktop OpenGL sizes a point with `glPointSize`, which WebGL does not
    have; the value travels to the vertex shader as the `pointSizeLocal`
    uniform `WebGLShaderPreprocessor` injects, set where Java calls
    `glPointSize`. `gl.lineWidth` is issued where Java issues it, though every
    browser engine clamps it to one.
  - Java keeps no state at all here, and neither does this class: the programs
    and the buffers arrive from the caller.
*/
export class WebGLPolygon2DRenderer {
    private constructor() {}

    public static draw(
        gl: WebGL2RenderingContext,
        mvp: Matrix4x4d,
        polygon: Polygon2D | null,
        quality: RendererConfiguration | null,
        fillR: number,
        fillG: number,
        fillB: number,
        lineR: number,
        lineG: number,
        lineB: number,
        resources: WebGLPolygon2DRendererResources,
    ): void {
        if (polygon === null || polygon.loops.length === 0 || quality === null) {
            return;
        }

        if (quality.isSurfacesSet()) {
            const fillPositions: number[] = _Polygon2DOddWindingTessellator.tessellatePolygonToTriangles(polygon);
            WebGLPolygon2DRenderer.drawTriangles(gl, mvp, fillPositions, fillR, fillG, fillB, resources);
        }

        const pointPositions: number[] | null = quality.isPointsSet() ? [] : null;
        const pointColors: number[] | null = quality.isPointsSet() ? [] : null;

        for (const contour of polygon.loops) {
            if (contour.vertices.length === 0) {
                continue;
            }

            if (quality.isWiresSet() && contour.vertices.length > 1) {
                const linePositions: number[] = [];
                const lineColors: number[] = [];
                for (let j = 0; j < contour.vertices.length; j++) {
                    const next: number = (j + 1) % contour.vertices.length;
                    WebGLPolygon2DRenderer.addSegment(
                        linePositions,
                        lineColors,
                        contour.vertices[j]!.x,
                        0.0,
                        contour.vertices[j]!.y,
                        contour.vertices[next]!.x,
                        0.0,
                        contour.vertices[next]!.y,
                        lineR,
                        lineG,
                        lineB,
                    );
                }
                WebGLPolygon2DRenderer.drawLines(gl, mvp, linePositions, lineColors, resources);
            }

            if (quality.isPointsSet()) {
                for (const vertex of contour.vertices) {
                    WebGLPolygon2DRenderer.addPoint(
                        pointPositions!,
                        pointColors!,
                        vertex.x,
                        0.0,
                        vertex.y,
                        lineR,
                        lineG,
                        lineB,
                    );
                }
            }
        }

        if (quality.isPointsSet()) {
            WebGLPolygon2DRenderer.drawPoints(gl, mvp, pointPositions!, pointColors!, resources);
        }
    }

    private static drawLines(
        gl: WebGL2RenderingContext,
        mvp: Matrix4x4d,
        positions: number[],
        colors: number[],
        resources: WebGLPolygon2DRendererResources,
    ): void {
        if (positions.length === 0) {
            return;
        }

        gl.useProgram(resources.lineProgram);
        WebGLPolygon2DRenderer.setMvpUniform(gl, resources.lineProgram, mvp);
        WebGLPolygon2DRenderer.setFloat(gl, resources.lineProgram, "depthBiasNdc", 0.0);

        WebGLPolygon2DRenderer.bindLineAttributes(gl, positions, colors, resources);
        gl.lineWidth(2.0);
        gl.drawArrays(gl.LINES, 0, positions.length / 3);
        WebGLPolygon2DRenderer.unbind(gl);
    }

    private static drawTriangles(
        gl: WebGL2RenderingContext,
        mvp: Matrix4x4d,
        positions: number[],
        r: number,
        g: number,
        b: number,
        resources: WebGLPolygon2DRendererResources,
    ): void {
        if (positions.length === 0) {
            return;
        }

        gl.useProgram(resources.constantProgram);
        WebGLPolygon2DRenderer.setMvpUniform(gl, resources.constantProgram, mvp);
        WebGLPolygon2DRenderer.setInteger(gl, resources.constantProgram, "withTexture", 0);
        WebGLPolygon2DRenderer.setInteger(gl, resources.constantProgram, "withVertexColors", 0);
        const diffuseLocation = gl.getUniformLocation(resources.constantProgram, "diffuseColor");
        if (diffuseLocation !== null) {
            gl.uniform3f(diffuseLocation, r, g, b);
        }

        WebGLPolygon2DRenderer.bindConstantAttributes(gl, positions, resources);
        gl.drawArrays(gl.TRIANGLES, 0, positions.length / 3);
        WebGLPolygon2DRenderer.unbind(gl);
    }

    private static drawPoints(
        gl: WebGL2RenderingContext,
        mvp: Matrix4x4d,
        positions: number[],
        colors: number[],
        resources: WebGLPolygon2DRendererResources,
    ): void {
        if (positions.length === 0) {
            return;
        }

        gl.useProgram(resources.lineProgram);
        WebGLPolygon2DRenderer.setMvpUniform(gl, resources.lineProgram, mvp);
        WebGLPolygon2DRenderer.setFloat(gl, resources.lineProgram, "depthBiasNdc", 0.0);

        WebGLPolygon2DRenderer.bindLineAttributes(gl, positions, colors, resources);
        WebGLPolygon2DRenderer.setFloat(gl, resources.lineProgram, "pointSizeLocal", 8.0);
        gl.drawArrays(gl.POINTS, 0, positions.length / 3);
        WebGLPolygon2DRenderer.unbind(gl);
    }

    private static bindLineAttributes(
        gl: WebGL2RenderingContext,
        positions: number[],
        colors: number[],
        resources: WebGLPolygon2DRendererResources,
    ): void {
        gl.bindVertexArray(resources.vertexArray);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.positionBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(positions), gl.STREAM_DRAW);
        gl.enableVertexAttribArray(0);
        gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.colorBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(colors), gl.STREAM_DRAW);
        gl.enableVertexAttribArray(1);
        gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 0, 0);
    }

    private static bindConstantAttributes(
        gl: WebGL2RenderingContext,
        positions: number[],
        resources: WebGLPolygon2DRendererResources,
    ): void {
        gl.bindVertexArray(resources.vertexArray);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.positionBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, WebGLPolygon2DRenderer.toVec4Array(positions), gl.STREAM_DRAW);
        gl.enableVertexAttribArray(0);
        gl.vertexAttribPointer(0, 4, gl.FLOAT, false, 0, 0);

        gl.disableVertexAttribArray(1);
        gl.vertexAttrib3f(1, 0.0, 0.0, 0.0);
        gl.disableVertexAttribArray(2);
        gl.vertexAttrib2f(2, 0.0, 0.0);
        gl.bindBuffer(gl.ARRAY_BUFFER, null);
    }

    private static unbind(gl: WebGL2RenderingContext): void {
        gl.disableVertexAttribArray(0);
        gl.disableVertexAttribArray(1);
        gl.disableVertexAttribArray(2);
        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);
        gl.useProgram(null);
    }

    private static setMvpUniform(gl: WebGL2RenderingContext, program: WebGLProgram, mvp: Matrix4x4d): void {
        const location = gl.getUniformLocation(program, "modelViewProjectionLocal");
        if (location !== null) {
            gl.uniformMatrix4fv(location, false, mvp.exportToFloatArrayColumnOrder());
        }
    }

    private static setFloat(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, value: number): void {
        const location = gl.getUniformLocation(program, name);
        if (location !== null) {
            gl.uniform1f(location, value);
        }
    }

    private static setInteger(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, value: number): void {
        const location = gl.getUniformLocation(program, name);
        if (location !== null) {
            gl.uniform1i(location, value);
        }
    }

    private static addSegment(
        positions: number[],
        colors: number[],
        x1: number,
        y1: number,
        z1: number,
        x2: number,
        y2: number,
        z2: number,
        r: number,
        g: number,
        b: number,
    ): void {
        WebGLPolygon2DRenderer.addPoint(positions, colors, x1, y1, z1, r, g, b);
        WebGLPolygon2DRenderer.addPoint(positions, colors, x2, y2, z2, r, g, b);
    }

    private static addPoint(
        positions: number[],
        colors: number[],
        x: number,
        y: number,
        z: number,
        r: number,
        g: number,
        b: number,
    ): void {
        positions.push(x, y, z);
        colors.push(r, g, b);
    }

    private static toVec4Array(xyz: number[]): Float32Array {
        const out = new Float32Array((xyz.length / 3) * 4);
        let j = 0;
        for (let i = 0; i < xyz.length; i += 3) {
            out[j++] = xyz[i]!;
            out[j++] = xyz[i + 1]!;
            out[j++] = xyz[i + 2]!;
            out[j++] = 1.0;
        }
        return out;
    }
}
