package vsdk.toolkit.render.jogl;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallbackAdapter;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;
import vsdk.toolkit.environment.material.RendererConfiguration;

public class Jogl4Polygon2DRenderer extends Jogl4Renderer
{
    public static void draw(
        GL4 gl,
        Matrix4x4d mvp,
        Polygon2D polygon,
        RendererConfiguration quality,
        float fillR,
        float fillG,
        float fillB,
        float lineR,
        float lineG,
        float lineB,
        int lineProgramId,
        int constantProgramId,
        int vaoId,
        int positionVboId,
        int colorVboId)
    {
        if ( polygon == null || polygon.loops == null || polygon.loops.isEmpty()
             || quality == null ) {
            return;
        }

        if ( quality.isSurfacesSet() ) {
            List<Float> fillPositions = tessellatePolygonToTriangles(polygon);
            drawTriangles(gl, mvp, fillPositions, fillR, fillG, fillB,
                constantProgramId, vaoId, positionVboId);
        }

        List<Float> pointPositions = quality.isPointsSet() ? new ArrayList<>() : null;
        List<Float> pointColors = quality.isPointsSet() ? new ArrayList<>() : null;

        for ( int i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContour contour = polygon.loops.get(i);
            if ( contour.vertices == null || contour.vertices.isEmpty() ) {
                continue;
            }

            if ( quality.isWiresSet() && contour.vertices.size() > 1 ) {
                List<Float> linePositions = new ArrayList<>();
                List<Float> lineColors = new ArrayList<>();
                for ( int j = 0; j < contour.vertices.size(); j++ ) {
                    int next = (j + 1) % contour.vertices.size();
                    addSegment(linePositions, lineColors,
                        contour.vertices.get(j).x, 0.0, contour.vertices.get(j).y,
                        contour.vertices.get(next).x, 0.0, contour.vertices.get(next).y,
                        lineR, lineG, lineB);
                }
                drawLines(gl, mvp, linePositions, lineColors, lineProgramId, vaoId,
                    positionVboId, colorVboId);
            }

            if ( quality.isPointsSet() ) {
                for ( int j = 0; j < contour.vertices.size(); j++ ) {
                    addPoint(pointPositions, pointColors,
                        contour.vertices.get(j).x, 0.0, contour.vertices.get(j).y,
                        lineR, lineG, lineB);
                }
            }
        }

        if ( quality.isPointsSet() ) {
            drawPoints(gl, mvp, pointPositions, pointColors, lineProgramId, vaoId,
                positionVboId, colorVboId);
        }
    }

    private static void drawLines(
        GL4 gl,
        Matrix4x4d mvp,
        List<Float> positions,
        List<Float> colors,
        int lineProgramId,
        int vaoId,
        int positionVboId,
        int colorVboId)
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

        bindLineAttributes(gl, toArray(positions), toArray(colors), vaoId,
            positionVboId, colorVboId);
        gl.glLineWidth(2.0f);
        gl.glDrawArrays(GL4.GL_LINES, 0, positions.size() / 3);
        unbind(gl);
    }

    private static void drawTriangles(
        GL4 gl,
        Matrix4x4d mvp,
        List<Float> positions,
        float r,
        float g,
        float b,
        int constantProgramId,
        int vaoId,
        int positionVboId)
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
        bindConstantAttributes(gl, posArray, vaoId, positionVboId);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, posArray.length / 3);
        unbind(gl);
    }

    private static void drawPoints(
        GL4 gl,
        Matrix4x4d mvp,
        List<Float> positions,
        List<Float> colors,
        int lineProgramId,
        int vaoId,
        int positionVboId,
        int colorVboId)
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

        bindLineAttributes(gl, toArray(positions), toArray(colors), vaoId,
            positionVboId, colorVboId);
        gl.glPointSize(8.0f);
        gl.glDrawArrays(GL4.GL_POINTS, 0, positions.size() / 3);
        unbind(gl);
    }

    private static List<Float> tessellatePolygonToTriangles(Polygon2D polygon)
    {
        List<Float> out = new ArrayList<>();
        if ( polygon == null || polygon.loops == null || polygon.loops.isEmpty() ) {
            return out;
        }

        GLUtessellator tess = GLU.gluNewTess();
        TesselationCollector collector = new TesselationCollector(out);

        GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, collector);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, collector);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_END, collector);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, collector);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, collector);

        GLU.gluTessBeginPolygon(tess, null);
        for ( int i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContour contour = polygon.loops.get(i);
            if ( contour.vertices == null || contour.vertices.size() < 3 ) {
                continue;
            }
            GLU.gluTessBeginContour(tess);
            for ( int j = 0; j < contour.vertices.size(); j++ ) {
                double[] vertex = new double[] {
                    contour.vertices.get(j).x,
                    0.0,
                    contour.vertices.get(j).y
                };
                collector.keepReference(vertex);
                GLU.gluTessVertex(tess, vertex, 0, vertex);
            }
            GLU.gluTessEndContour(tess);
        }
        GLU.gluTessEndPolygon(tess);
        GLU.gluDeleteTess(tess);
        return out;
    }

    private static void bindLineAttributes(
        GL4 gl,
        float[] positions,
        float[] colors,
        int vaoId,
        int positionVboId,
        int colorVboId)
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

    private static void bindConstantAttributes(
        GL4 gl,
        float[] positions,
        int vaoId,
        int positionVboId)
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

    private static void unbind(GL4 gl)
    {
        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glDisableVertexAttribArray(2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    private static void setMvpUniform(GL4 gl, int programId, Matrix4x4d mvp)
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

    private static void addSegment(
        List<Float> positions,
        List<Float> colors,
        double x1,
        double y1,
        double z1,
        double x2,
        double y2,
        double z2,
        float r,
        float g,
        float b)
    {
        addPoint(positions, colors, x1, y1, z1, r, g, b);
        addPoint(positions, colors, x2, y2, z2, r, g, b);
    }

    private static void addPoint(
        List<Float> positions,
        List<Float> colors,
        double x,
        double y,
        double z,
        float r,
        float g,
        float b)
    {
        positions.add((float)x);
        positions.add((float)y);
        positions.add((float)z);

        colors.add(r);
        colors.add(g);
        colors.add(b);
    }

    private static void addVertex3(List<Float> out, double x, double y,
        double z)
    {
        out.add((float)x);
        out.add((float)y);
        out.add((float)z);
    }

    private static float[] toArray(List<Float> input)
    {
        float[] out = new float[input.size()];
        for ( int i = 0; i < input.size(); i++ ) {
            out[i] = input.get(i);
        }
        return out;
    }

    private static float[] toVec4Array(float[] xyz)
    {
        float[] out = new float[(xyz.length / 3) * 4];
        int j = 0;
        for ( int i = 0; i < xyz.length; i += 3 ) {
            out[j++] = xyz[i];
            out[j++] = xyz[i + 1];
            out[j++] = xyz[i + 2];
            out[j++] = 1.0f;
        }
        return out;
    }

    private static final class TesselationCollector
        extends GLUtessellatorCallbackAdapter
    {
        private final List<Float> out;
        private final List<double[]> current;
        private final List<Object> keepAlive;
        private int mode;

        TesselationCollector(List<Float> out)
        {
            this.out = out;
            this.current = new ArrayList<>();
            this.keepAlive = new ArrayList<>();
            this.mode = -1;
        }

        void keepReference(Object ref)
        {
            keepAlive.add(ref);
        }

        @Override
        public void begin(int type)
        {
            mode = type;
            current.clear();
        }

        @Override
        public void vertex(Object vertexData)
        {
            if ( !(vertexData instanceof double[]) ) {
                return;
            }
            double[] v = (double[])vertexData;
            current.add(v);

            if ( mode == GL.GL_TRIANGLES && current.size() >= 3 ) {
                int n = current.size();
                emitTriangle(current.get(n - 3), current.get(n - 2), current.get(n - 1));
            }
            else if ( mode == GL.GL_TRIANGLE_FAN && current.size() >= 3 ) {
                int n = current.size();
                emitTriangle(current.get(0), current.get(n - 2), current.get(n - 1));
            }
            else if ( mode == GL.GL_TRIANGLE_STRIP && current.size() >= 3 ) {
                int n = current.size();
                if ( (n & 1) == 0 ) {
                    emitTriangle(current.get(n - 2), current.get(n - 3), current.get(n - 1));
                }
                else {
                    emitTriangle(current.get(n - 3), current.get(n - 2), current.get(n - 1));
                }
            }
        }

        @Override
        public void combine(double[] coords, Object[] data, float[] weight,
            Object[] outData)
        {
            double[] created = new double[] { coords[0], coords[1], coords[2] };
            keepReference(created);
            outData[0] = created;
        }

        @Override
        public void error(int errnum)
        {
            // Keep rendering even if tessellation reports non-fatal issues.
        }

        private void emitTriangle(double[] a, double[] b, double[] c)
        {
            addVertex3(out, a[0], a[1], a[2]);
            addVertex3(out, b[0], b[1], b[2]);
            addVertex3(out, c[0], c[1], c[2]);
        }
    }
}
