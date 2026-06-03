package render;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.MonotoneDecompositionTriangulator;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.render.jogl.Jogl4MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl4ShaderProgramUtil;

/**
Fills polygon surfaces by decomposing them with the
{@link MonotoneDecompositionTriangulator}, as an alternative to the GLU-based
tessellation used by {@code Jogl4Polygon2DRenderer}.

Multi-pass rendering with depth bias guarantees correct visibility ordering:
surfaces are drawn with {@code GL_POLYGON_OFFSET_FILL} so they are pushed
slightly behind co-planar wires and points; wires (all triangle edges, which
expose the full triangulation structure) are drawn with a small negative NDC
depth bias so they always appear in front of the fill; and points are drawn
with an even larger negative NDC depth bias so they remain visible over both.
*/
public class JoglTriangularRenderer
{
    private static final float WIRE_DEPTH_BIAS_NDC  = -0.001f;
    private static final float POINT_DEPTH_BIAS_NDC = -0.002f;

    private int constantProgramId;
    private int lineProgramId;
    private int vaoId;
    private int positionVboId;
    private int colorVboId;

    public JoglTriangularRenderer()
    {
        this.constantProgramId = 0;
        this.lineProgramId = 0;
        this.vaoId = 0;
        this.positionVboId = 0;
        this.colorVboId = 0;
    }

    public void init(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();
        constantProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
            gl,
            "constantVertexShader.glsl",
            "constantPixelShader.glsl");
        lineProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
            gl,
            "lineVertexShader.glsl",
            "linePixelShader.glsl");

        int[] tmp = new int[1];
        gl.glGenVertexArrays(1, tmp, 0);
        vaoId = tmp[0];
        gl.glGenBuffers(1, tmp, 0);
        positionVboId = tmp[0];
        gl.glGenBuffers(1, tmp, 0);
        colorVboId = tmp[0];
    }

    public void dispose(GL4 gl)
    {
        int[] tmp = new int[1];

        if ( colorVboId != 0 ) {
            tmp[0] = colorVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            colorVboId = 0;
        }
        if ( positionVboId != 0 ) {
            tmp[0] = positionVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            positionVboId = 0;
        }
        if ( vaoId != 0 ) {
            tmp[0] = vaoId;
            gl.glDeleteVertexArrays(1, tmp, 0);
            vaoId = 0;
        }
        if ( lineProgramId != 0 ) {
            gl.glDeleteProgram(lineProgramId);
            lineProgramId = 0;
        }
        if ( constantProgramId != 0 ) {
            gl.glDeleteProgram(constantProgramId);
            constantProgramId = 0;
        }
    }

    /**
    Triangulates the given polygon with the monotone decomposition triangulator
    and draws the resulting geometry according to {@code config}. The render
    order is: surfaces first (with polygon offset), then wires (triangle edges,
    revealing the triangulation), then points. Each subsequent pass uses a
    larger negative NDC depth bias so it always draws in front of the previous
    one. Degenerate or invalid polygons are silently skipped.

    @param gl     active GL4 context
    @param mvp    model-view-projection transform for this polygon
    @param polygon polygon to render
    @param config  rendering configuration; controls which passes are drawn
    @param fillR, fillG, fillB surface fill color
    @param lineR, lineG, lineB wire and point color
    */
    public void fillPolygonSurface(GL4 gl, Matrix4x4d mvp, Polygon2D polygon,
        RendererConfiguration config,
        float fillR, float fillG, float fillB,
        float lineR, float lineG, float lineB)
    {
        if ( polygon == null || polygon.loops == null || polygon.loops.isEmpty() ) {
            return;
        }
        if ( config == null ) {
            return;
        }
        if ( !config.isSurfacesSet() && !config.isWiresSet() && !config.isPointsSet() ) {
            return;
        }

        try {
            MonotoneDecompositionTriangulator pipeline =
                new MonotoneDecompositionTriangulator();
            List<MonotoneDecompositionTriangulator.Triangle> triangles =
                new ArrayList<>();
            int triangleCount = pipeline.triangulate(polygon, triangles);
            if ( triangleCount <= 0 ) {
                return;
            }

            List<double[]> vertices = flattenVertices(polygon);

            if ( config.isSurfacesSet() ) {
                List<Float> fillPositions =
                    buildFillPositions(triangleCount, triangles, vertices);
                if ( !fillPositions.isEmpty() ) {
                    drawTriangleSurfaces(gl, mvp, fillPositions, fillR, fillG, fillB);
                }
            }

            if ( config.isWiresSet() ) {
                List<Float> wirePositions = new ArrayList<>();
                List<Float> wireColors = new ArrayList<>();
                buildWirePositions(triangleCount, triangles, vertices,
                    wirePositions, wireColors, lineR, lineG, lineB);
                if ( !wirePositions.isEmpty() ) {
                    drawTriangleWires(gl, mvp, wirePositions, wireColors);
                }
            }

            if ( config.isPointsSet() ) {
                List<Float> pointPositions = new ArrayList<>();
                List<Float> pointColors = new ArrayList<>();
                buildPointPositions(triangleCount, triangles, vertices,
                    pointPositions, pointColors, lineR, lineG, lineB);
                if ( !pointPositions.isEmpty() ) {
                    drawTrianglePoints(gl, mvp, pointPositions, pointColors);
                }
            }
        }
        catch ( RuntimeException e ) {
            // Invalid or degenerate polygons are skipped by the visualizer.
        }
    }

    private static List<Float> buildFillPositions(int triangleCount,
        List<MonotoneDecompositionTriangulator.Triangle> triangles,
        List<double[]> vertices)
    {
        List<Float> out = new ArrayList<>(triangleCount * 9);
        for ( int i = 0; i < triangleCount; i++ ) {
            MonotoneDecompositionTriangulator.Triangle t = triangles.get(i);
            addFillTriangle(out, vertices.get(t.a), vertices.get(t.b), vertices.get(t.c));
        }
        return out;
    }

    private static void buildWirePositions(int triangleCount,
        List<MonotoneDecompositionTriangulator.Triangle> triangles,
        List<double[]> vertices,
        List<Float> positions, List<Float> colors,
        float r, float g, float b)
    {
        for ( int i = 0; i < triangleCount; i++ ) {
            MonotoneDecompositionTriangulator.Triangle t = triangles.get(i);
            double[] a = vertices.get(t.a);
            double[] b2 = vertices.get(t.b);
            double[] c = vertices.get(t.c);
            addLineEdge(positions, colors, a, b2, r, g, b);
            addLineEdge(positions, colors, b2, c, r, g, b);
            addLineEdge(positions, colors, c, a, r, g, b);
        }
    }

    private static void buildPointPositions(int triangleCount,
        List<MonotoneDecompositionTriangulator.Triangle> triangles,
        List<double[]> vertices,
        List<Float> positions, List<Float> colors,
        float r, float g, float b)
    {
        for ( int i = 0; i < triangleCount; i++ ) {
            MonotoneDecompositionTriangulator.Triangle t = triangles.get(i);
            addLinePoint(positions, colors, vertices.get(t.a), r, g, b);
            addLinePoint(positions, colors, vertices.get(t.b), r, g, b);
            addLinePoint(positions, colors, vertices.get(t.c), r, g, b);
        }
    }

    private void drawTriangleSurfaces(GL4 gl, Matrix4x4d mvp, List<Float> positions,
        float r, float g, float b)
    {
        if ( positions.isEmpty() ) {
            return;
        }

        gl.glEnable(GL4.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(1.0f, 1.0f);

        gl.glUseProgram(constantProgramId);
        setMvpUniform(gl, constantProgramId, mvp);
        int withTextureLoc = gl.glGetUniformLocation(constantProgramId, "withTexture");
        int withVertexColorsLoc =
            gl.glGetUniformLocation(constantProgramId, "withVertexColors");
        int diffuseLoc = gl.glGetUniformLocation(constantProgramId, "diffuseColor");
        if ( withTextureLoc >= 0 ) {
            gl.glUniform1i(withTextureLoc, 0);
        }
        if ( withVertexColorsLoc >= 0 ) {
            gl.glUniform1i(withVertexColorsLoc, 0);
        }
        if ( diffuseLoc >= 0 ) {
            gl.glUniform3f(diffuseLoc, r, g, b);
        }

        float[] posArray = toArray(positions);
        bindConstantAttributes(gl, posArray);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, posArray.length / 3);
        unbind(gl);

        gl.glPolygonOffset(0.0f, 0.0f);
        gl.glDisable(GL4.GL_POLYGON_OFFSET_FILL);
    }

    private void drawTriangleWires(GL4 gl, Matrix4x4d mvp,
        List<Float> positions, List<Float> colors)
    {
        if ( positions.isEmpty() ) {
            return;
        }

        gl.glUseProgram(lineProgramId);
        setMvpUniform(gl, lineProgramId, mvp);
        int depthBiasLoc = gl.glGetUniformLocation(lineProgramId, "depthBiasNdc");
        if ( depthBiasLoc >= 0 ) {
            gl.glUniform1f(depthBiasLoc, WIRE_DEPTH_BIAS_NDC);
        }

        bindLineAttributes(gl, toArray(positions), toArray(colors));
        gl.glLineWidth(1.0f);
        gl.glDrawArrays(GL4.GL_LINES, 0, positions.size() / 3);
        unbind(gl);
    }

    private void drawTrianglePoints(GL4 gl, Matrix4x4d mvp,
        List<Float> positions, List<Float> colors)
    {
        if ( positions.isEmpty() ) {
            return;
        }

        gl.glUseProgram(lineProgramId);
        setMvpUniform(gl, lineProgramId, mvp);
        int depthBiasLoc = gl.glGetUniformLocation(lineProgramId, "depthBiasNdc");
        if ( depthBiasLoc >= 0 ) {
            gl.glUniform1f(depthBiasLoc, POINT_DEPTH_BIAS_NDC);
        }

        bindLineAttributes(gl, toArray(positions), toArray(colors));
        gl.glPointSize(8.0f);
        gl.glDrawArrays(GL4.GL_POINTS, 0, positions.size() / 3);
        unbind(gl);
    }

    private void bindConstantAttributes(GL4 gl, float[] positions)
    {
        gl.glBindVertexArray(vaoId);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, positionVboId);
        FloatBuffer posBuffer = Buffers.newDirectFloatBuffer(toVec4Array(positions));
        gl.glBufferData(GL4.GL_ARRAY_BUFFER,
            (long)(positions.length / 3) * 4 * Float.BYTES, posBuffer,
            GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 4, GL4.GL_FLOAT, false, 0, 0L);

        gl.glDisableVertexAttribArray(1);
        gl.glVertexAttrib3f(1, 0.0f, 0.0f, 0.0f);
        gl.glDisableVertexAttribArray(2);
        gl.glVertexAttrib2f(2, 0.0f, 0.0f);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
    }

    private void bindLineAttributes(GL4 gl, float[] positions, float[] colors)
    {
        gl.glBindVertexArray(vaoId);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, positionVboId);
        FloatBuffer posBuffer = Buffers.newDirectFloatBuffer(positions);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)positions.length * Float.BYTES,
            posBuffer, GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorVboId);
        FloatBuffer colorBuffer = Buffers.newDirectFloatBuffer(colors);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)colors.length * Float.BYTES,
            colorBuffer, GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
    }

    private void unbind(GL4 gl)
    {
        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glDisableVertexAttribArray(2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    private void setMvpUniform(GL4 gl, int programId, Matrix4x4d mvp)
    {
        int mvpLoc = gl.glGetUniformLocation(programId, "modelViewProjectionLocal");
        if ( mvpLoc >= 0 ) {
            gl.glUniformMatrix4fv(
                mvpLoc,
                1,
                false,
                Jogl4MatrixRenderer.toColumnMajorFloatArray(mvp),
                0);
        }
    }

    private static void addFillTriangle(List<Float> positions,
        double[] a, double[] b, double[] c)
    {
        addFillPoint(positions, a[0], 0.0, a[1]);
        addFillPoint(positions, b[0], 0.0, b[1]);
        addFillPoint(positions, c[0], 0.0, c[1]);
    }

    private static void addFillPoint(List<Float> positions, double x, double y, double z)
    {
        positions.add((float)x);
        positions.add((float)y);
        positions.add((float)z);
    }

    private static void addLineEdge(List<Float> positions, List<Float> colors,
        double[] a, double[] b, float r, float g, float bColor)
    {
        addLinePoint(positions, colors, a, r, g, bColor);
        addLinePoint(positions, colors, b, r, g, bColor);
    }

    private static void addLinePoint(List<Float> positions, List<Float> colors,
        double[] p, float r, float g, float b)
    {
        positions.add((float)p[0]);
        positions.add(0.0f);
        positions.add((float)p[1]);
        colors.add(r);
        colors.add(g);
        colors.add(b);
    }

    private static float[] toArray(List<Float> input)
    {
        float[] out = new float[input.size()];
        for ( int i = 0; i < input.size(); i++ ) {
            out[i] = input.get(i);
        }
        return out;
    }

    private static float[] toVec4Array(float[] positions)
    {
        float[] out = new float[(positions.length / 3) * 4];
        for ( int i = 0, j = 0; i < positions.length; i += 3, j += 4 ) {
            out[j]     = positions[i];
            out[j + 1] = positions[i + 1];
            out[j + 2] = positions[i + 2];
            out[j + 3] = 1.0f;
        }
        return out;
    }

    private List<double[]> flattenVertices(Polygon2D polygon)
    {
        List<double[]> vertices = new ArrayList<>();
        if ( polygon == null || polygon.loops == null ) {
            return vertices;
        }

        for ( int i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContour contour = polygon.loops.get(i);
            if ( contour.vertices == null ) {
                continue;
            }
            for ( Vertex2D vertex : contour.vertices ) {
                vertices.add(new double[] { vertex.x, vertex.y });
            }
        }
        return vertices;
    }
}
