package render;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;

import model.PolygonClippingDebuggerModel;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.MonotoneDecompositionTriangulator;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.render.jogl.Jogl4MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl4ShaderProgramUtil;

public class JoglTriangularRenderer
{
    private final PolygonClippingDebuggerModel model;

    private int lineProgramId;
    private int constantProgramId;
    private int vaoId;
    private int positionVboId;
    private int colorVboId;

    public JoglTriangularRenderer(PolygonClippingDebuggerModel model)
    {
        this.model = model;
        this.lineProgramId = 0;
        this.constantProgramId = 0;
        this.vaoId = 0;
        this.positionVboId = 0;
        this.colorVboId = 0;
    }

    public void init(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();
        lineProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
            gl,
            "lineVertexShader.glsl",
            "linePixelShader.glsl");
        constantProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
            gl,
            "constantVertexShader.glsl",
            "constantPixelShader.glsl");

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

        if ( positionVboId != 0 ) {
            tmp[0] = positionVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            positionVboId = 0;
        }
        if ( colorVboId != 0 ) {
            tmp[0] = colorVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            colorVboId = 0;
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

    public void draw(GL4 gl, Matrix4x4d projection)
    {
        if ( model.isShowReferenceFrame() ) {
            drawReferenceFrame(gl, projection);
        }

        Bounds2D bounds = calculateBounds();
        double panelWidth = Math.max(6.0, bounds.width());
        double panelDepth = Math.max(6.0, bounds.height());
        RendererConfiguration polygonQuality = new RendererConfiguration();
        polygonQuality.clone(model.getQuality());
        polygonQuality.setSurfaces(
            polygonQuality.isSurfacesSet() && model.isShowFilledPolygons());

        if ( model.isShowClipPolygon() ) {
            drawTriangulatedPolygon(gl, projection, model.getClipPolygon(),
                polygonQuality, 0.20f, 0.75f, 0.25f, 0.0, 0.0);
        }
        if ( model.isShowSubjectPolygon() ) {
            drawTriangulatedPolygon(gl, projection, model.getSubjectPolygon(),
                polygonQuality, 0.80f, 0.74f, 0.20f, 0.0, 0.0);
        }

        Matrix4x4d innerTransform = new Matrix4x4d().translation(0.0, 0.0, -panelDepth * 1.25);
        if ( model.isShowInnerPolygon() ) {
            drawTriangulatedPolygon(gl, projection.multiply(innerTransform),
                model.getInnerPolygon(), polygonQuality,
                0.65f, 0.65f, 0.70f, 0.0, 0.0);
        }

        Matrix4x4d outerTransform = new Matrix4x4d().translation(panelWidth * 1.25, 0.0, 0.0);
        if ( model.isShowOuterPolygon() ) {
            drawTriangulatedPolygon(gl, projection.multiply(outerTransform),
                model.getOuterPolygon(), polygonQuality,
                0.68f, 0.78f, 0.68f, 0.0, 0.0);
        }
    }

    private void drawReferenceFrame(GL4 gl, Matrix4x4d mvp)
    {
        List<Float> positions = new ArrayList<>();
        List<Float> colors = new ArrayList<>();
        addSegment(positions, colors, 0, 0, 0, 2, 0, 0, 1f, 0f, 0f);
        addSegment(positions, colors, 0, 0, 0, 0, 2, 0, 0f, 1f, 0f);
        addSegment(positions, colors, 0, 0, 0, 0, 0, 2, 0f, 0f, 1f);
        drawLines(gl, mvp, positions, colors, GL4.GL_LINES, 3.0f);
    }

    private void drawTriangulatedPolygon(GL4 gl, Matrix4x4d mvp,
        Polygon2D polygon, RendererConfiguration quality, float fillR,
        float fillG, float fillB, double tx, double tz)
    {
        if ( polygon == null || polygon.loops == null || polygon.loops.isEmpty()
             || quality == null || !quality.isSurfacesSet() ) {
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
            List<Float> positions = new ArrayList<>(triangleCount * 9);
            for ( int i = 0; i < triangleCount; i++ ) {
                MonotoneDecompositionTriangulator.Triangle triangle =
                    triangles.get(i);
                addTriangle(positions, vertices.get(triangle.a), vertices.get(triangle.b),
                    vertices.get(triangle.c), tx, tz);
            }

            drawTriangles(gl, mvp, positions, fillR, fillG, fillB);
        }
        catch ( RuntimeException e ) {
            // Invalid or degenerate polygons are skipped by the visualizer.
        }
    }

    private void drawTriangles(GL4 gl, Matrix4x4d mvp, List<Float> positions,
        float r, float g, float b)
    {
        if ( positions.isEmpty() ) {
            return;
        }

        gl.glUseProgram(constantProgramId);
        setMvpUniform(gl, constantProgramId, mvp);
        int withTextureLoc = gl.glGetUniformLocation(constantProgramId, "withTexture");
        int withVertexColorsLoc = gl.glGetUniformLocation(constantProgramId, "withVertexColors");
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
    }

    private void drawLines(GL4 gl, Matrix4x4d mvp, List<Float> positions,
        List<Float> colors, int primitive, float lineWidth)
    {
        if ( positions.isEmpty() ) {
            return;
        }

        gl.glUseProgram(lineProgramId);
        setMvpUniform(gl, lineProgramId, mvp);
        int depthBiasLoc = gl.glGetUniformLocation(lineProgramId, "depthBiasNdc");
        if ( depthBiasLoc >= 0 ) {
            gl.glUniform1f(depthBiasLoc, 0.0f);
        }

        bindLineAttributes(gl, toArray(positions), toArray(colors));
        gl.glLineWidth(lineWidth);
        gl.glDrawArrays(primitive, 0, positions.size() / 3);
        unbind(gl);
    }

    private void bindLineAttributes(GL4 gl, float[] positions, float[] colors)
    {
        gl.glBindVertexArray(vaoId);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, positionVboId);
        FloatBuffer posBuffer = Buffers.newDirectFloatBuffer(positions);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)positions.length * Float.BYTES, posBuffer, GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorVboId);
        FloatBuffer colorBuffer = Buffers.newDirectFloatBuffer(colors);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)colors.length * Float.BYTES, colorBuffer, GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0L);
    }

    private void bindConstantAttributes(GL4 gl, float[] positions)
    {
        gl.glBindVertexArray(vaoId);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, positionVboId);
        FloatBuffer posBuffer = Buffers.newDirectFloatBuffer(toVec4Array(positions));
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)(positions.length / 3) * 4 * Float.BYTES, posBuffer, GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 4, GL4.GL_FLOAT, false, 0, 0L);

        gl.glDisableVertexAttribArray(1);
        gl.glVertexAttrib3f(1, 0.0f, 0.0f, 0.0f);
        gl.glDisableVertexAttribArray(2);
        gl.glVertexAttrib2f(2, 0.0f, 0.0f);
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

    private static void addTriangle(List<Float> positions, double[] a,
        double[] b, double[] c, double tx, double tz)
    {
        addPoint(positions, a[0] + tx, 0.0, a[1] + tz);
        addPoint(positions, b[0] + tx, 0.0, b[1] + tz);
        addPoint(positions, c[0] + tx, 0.0, c[1] + tz);
    }

    private static void addSegment(List<Float> positions, List<Float> colors,
        double x1, double y1, double z1, double x2, double y2, double z2,
        float r, float g, float b)
    {
        addPoint(positions, colors, x1, y1, z1, r, g, b);
        addPoint(positions, colors, x2, y2, z2, r, g, b);
    }

    private static void addPoint(List<Float> positions, double x, double y, double z)
    {
        positions.add((float)x);
        positions.add((float)y);
        positions.add((float)z);
    }

    private static void addPoint(List<Float> positions, List<Float> colors,
        double x, double y, double z, float r, float g, float b)
    {
        positions.add((float)x);
        positions.add((float)y);
        positions.add((float)z);

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
            out[j] = positions[i];
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

    private Bounds2D calculateBounds()
    {
        Bounds2D bounds = new Bounds2D();

        includePolygonBounds(bounds, model.getClipPolygon());
        includePolygonBounds(bounds, model.getSubjectPolygon());
        includePolygonBounds(bounds, model.getInnerPolygon());
        includePolygonBounds(bounds, model.getOuterPolygon());

        if ( !bounds.initialized() ) {
            bounds.include(0.0, 0.0);
            bounds.include(4.0, 4.0);
        }

        return bounds;
    }

    private void includePolygonBounds(Bounds2D bounds, Polygon2D polygon)
    {
        if ( polygon == null || polygon.loops == null ) {
            return;
        }

        for ( int i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContour contour = polygon.loops.get(i);
            if ( contour.vertices == null ) {
                continue;
            }
            for ( Vertex2D vertex : contour.vertices ) {
                bounds.include(vertex.x, vertex.y);
            }
        }
    }

    private static final class Bounds2D
    {
        private double minX;
        private double minY;
        private double maxX;
        private double maxY;
        private boolean initialized;

        Bounds2D()
        {
            initialized = false;
        }

        void include(double x, double y)
        {
            if ( !initialized ) {
                minX = x;
                maxX = x;
                minY = y;
                maxY = y;
                initialized = true;
                return;
            }

            if ( x < minX ) {
                minX = x;
            }
            if ( x > maxX ) {
                maxX = x;
            }
            if ( y < minY ) {
                minY = y;
            }
            if ( y > maxY ) {
                maxY = y;
            }
        }

        boolean initialized()
        {
            return initialized;
        }

        double width()
        {
            return initialized ? maxX - minX : 0.0;
        }

        double height()
        {
            return initialized ? maxY - minY : 0.0;
        }
    }
}
