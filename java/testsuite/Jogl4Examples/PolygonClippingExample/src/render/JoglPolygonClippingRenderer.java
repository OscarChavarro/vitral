package render;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import model.PolygonClippingDebuggerModel;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._DoubleLinkedListNode;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._Polygon2DContourWA;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._Polygon2DWA;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._VertexNode2D;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl4MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl4Polygon2DRenderer;
import vsdk.toolkit.render.jogl.Jogl4ShaderProgramUtil;

public class JoglPolygonClippingRenderer implements GLEventListener
{
    private final PolygonClippingDebuggerModel model;
    private final JoglPolygonClippingHudRenderer hudRenderer;

    private int lineProgramId;
    private int constantProgramId;
    private int vaoId;
    private int positionVboId;
    private int colorVboId;

    public JoglPolygonClippingRenderer(PolygonClippingDebuggerModel model)
    {
        this.model = model;
        this.hudRenderer = new JoglPolygonClippingHudRenderer(model);
    }

    @Override
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

        hudRenderer.init(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();
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

        Jogl4CameraRenderer.dispose(gl);
        hudRenderer.dispose(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDisable(GL4.GL_BLEND);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

        Matrix4x4d projection = Jogl4CameraRenderer.activate(gl, model.getCamera());
        drawObjects(gl, projection);

        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
        boolean depthEnabled = gl.glIsEnabled(GL4.GL_DEPTH_TEST);
        boolean blendEnabled = gl.glIsEnabled(GL4.GL_BLEND);
        boolean cullEnabled = gl.glIsEnabled(GL4.GL_CULL_FACE);
        hudRenderer.draw(gl);
        gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        setCapability(gl, GL4.GL_DEPTH_TEST, depthEnabled);
        setCapability(gl, GL4.GL_BLEND, blendEnabled);
        setCapability(gl, GL4.GL_CULL_FACE, cullEnabled);

        if ( model.isTakeSnapshot() ) {
            model.setTakeSnapshot(false);
            RGBImageUncompressed snapshot = captureRgbImage(gl,
                drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
            File output = new File("frame"
                + VSDK.formatNumberWithinZeroes(model.getSnapshotNumber(), 4)
                + ".png");
            ImagePersistence.exportPNG(output, snapshot);
            model.setSnapshotNumber(model.getSnapshotNumber() + 1);
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width,
        int height)
    {
        drawable.getGL().getGL4().glViewport(0, 0, width, height);
        model.getCamera().updateViewportResize(width, height);
        hudRenderer.updateViewportSize(width, height);
    }

    public void refreshCanvasAfterWindowModeChange(GLCanvas canvas)
    {
        if ( canvas != null ) {
            canvas.repaint();
        }
    }

    private void drawObjects(GL4 gl, Matrix4x4d projection)
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
            drawPolygonWA(gl, projection, model.getClipPolygonWA(), 0.20f, 0.75f, 0.25f, 0.70f, 0.20f, 0.0f, 0.0f);
        }
        if ( model.isShowSubjectPolygon() ) {
            drawPolygonWA(gl, projection, model.getSubjectPolygonWA(), 0.80f, 0.74f, 0.20f, 0.82f, 0.56f, 0.0f, 0.0f);
        }

        Matrix4x4d innerTransform = new Matrix4x4d().translation(0.0, 0.0, -panelDepth * 1.25);
        if ( model.isShowInnerPolygon() ) {
            Jogl4Polygon2DRenderer.draw(gl,
                projection.multiply(innerTransform),
                model.getInnerPolygon(),
                polygonQuality,
                0.65f, 0.65f, 0.70f,
                0.82f, 0.58f, 0.36f,
                lineProgramId, constantProgramId, vaoId, positionVboId, colorVboId);
        }

        Matrix4x4d outerTransform = new Matrix4x4d().translation(panelWidth * 1.25, 0.0, 0.0);
        if ( model.isShowOuterPolygon() ) {
            Jogl4Polygon2DRenderer.draw(gl,
                projection.multiply(outerTransform),
                model.getOuterPolygon(),
                polygonQuality,
                0.68f, 0.78f, 0.68f,
                0.18f, 0.72f, 0.24f,
                lineProgramId, constantProgramId, vaoId, positionVboId, colorVboId);
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

    private void drawPolygonWA(GL4 gl, Matrix4x4d mvp, _Polygon2DWA polygon,
        float lineR, float lineG, float lineB, float pointR, float pointG,
        float tx, float tz)
    {
        if ( polygon == null || polygon.loops == null ) {
            return;
        }

        for ( int i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContourWA contour = polygon.loops.get(i);
            if ( contour.vertices == null || contour.vertices.getHead() == null ) {
                continue;
            }

            List<Float> linePositions = new ArrayList<>();
            List<Float> lineColors = new ArrayList<>();
            List<Float> pointPositions = new ArrayList<>();
            List<Float> pointColors = new ArrayList<>();

            _DoubleLinkedListNode<_VertexNode2D> head = contour.vertices.getHead();
            _DoubleLinkedListNode<_VertexNode2D> cursor = head;
            do {
                _DoubleLinkedListNode<_VertexNode2D> next = cursor.next;
                addSegment(linePositions, lineColors,
                    cursor.data.x + tx, 0.0, cursor.data.y + tz,
                    next.data.x + tx, 0.0, next.data.y + tz,
                    lineR, lineG, lineB);

                float r = (cursor.data.pairNode == null) ? pointR : 0.15f;
                float g = (cursor.data.pairNode == null) ? pointG : 0.85f;
                float b = (cursor.data.pairNode == null) ? 0.45f : 0.25f;
                addPoint(pointPositions, pointColors,
                    cursor.data.x + tx, 0.0, cursor.data.y + tz, r, g, b);
                cursor = cursor.next;
            } while ( cursor != head );

            drawLines(gl, mvp, linePositions, lineColors, GL4.GL_LINES, 2.0f);
            if ( model.isShowIntersections() ) {
                drawPoints(gl, mvp, pointPositions, pointColors, 8.0f);
            }
        }
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

    private void drawPoints(GL4 gl, Matrix4x4d mvp, List<Float> positions,
        List<Float> colors, float pointSize)
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
        gl.glPointSize(pointSize);
        gl.glDrawArrays(GL4.GL_POINTS, 0, positions.size() / 3);
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

    private static void addSegment(List<Float> positions, List<Float> colors,
        double x1, double y1, double z1, double x2, double y2, double z2,
        float r, float g, float b)
    {
        addPoint(positions, colors, x1, y1, z1, r, g, b);
        addPoint(positions, colors, x2, y2, z2, r, g, b);
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

    private static void setCapability(GL4 gl, int capability, boolean enabled)
    {
        if ( enabled ) {
            gl.glEnable(capability);
        }
        else {
            gl.glDisable(capability);
        }
    }

    private static RGBImageUncompressed captureRgbImage(GL gl, int width,
        int height)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(3 * width * height);
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, 0, width, height, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, bb);

        RGBImageUncompressed image = new RGBImageUncompressed();
        image.init(width, height);

        int pos = 0;
        for ( int y = image.getYSize() - 1; y >= 0; y-- ) {
            for ( int x = 0; x < image.getXSize(); x++ ) {
                image.putPixel(x, y, bb.get(pos), bb.get(pos + 1), bb.get(pos + 2));
                pos += 3;
            }
        }
        return image;
    }

    private Bounds2D calculateBounds()
    {
        Bounds2D bounds = new Bounds2D();

        includePolygonBounds(bounds, model.getClipPolygonWA());
        includePolygonBounds(bounds, model.getSubjectPolygonWA());
        includePolygonBounds(bounds, model.getInnerPolygon());
        includePolygonBounds(bounds, model.getOuterPolygon());

        if ( !bounds.initialized() ) {
            bounds.include(0.0, 0.0);
            bounds.include(4.0, 4.0);
        }

        return bounds;
    }

    private void includePolygonBounds(Bounds2D bounds, _Polygon2DWA polygon)
    {
        if ( polygon == null || polygon.loops == null ) {
            return;
        }

        for ( int i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContourWA contour = polygon.loops.get(i);
            if ( contour.vertices == null || contour.vertices.getHead() == null ) {
                continue;
            }
            _DoubleLinkedListNode<_VertexNode2D> head = contour.vertices.getHead();
            _DoubleLinkedListNode<_VertexNode2D> cursor = head;
            do {
                bounds.include(cursor.data.x, cursor.data.y);
                cursor = cursor.next;
            } while ( cursor != head );
        }
    }

    private void includePolygonBounds(Bounds2D bounds, Polygon2D polygon)
    {
        if ( polygon == null || polygon.loops == null ) {
            return;
        }

        for ( int i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContour contour = polygon.loops.get(i);
            for ( int j = 0; j < contour.vertices.size(); j++ ) {
                bounds.include(contour.vertices.get(j).x, contour.vertices.get(j).y);
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
