package render;

// Java basic classes
import com.jogamp.opengl.GL;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import models.DebuggerModel;
import java.awt.EventQueue;

// JOGL classes
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.hiddenLine.HiddenLineRenderer;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;
import vsdk.toolkit.render.jogl.Jogl4LineRenderer;
import vsdk.toolkit.render.jogl.Jogl4SimpleMaterialRenderer;
import vsdk.toolkit.render.jogl.Jogl4LightRenderer;
import vsdk.toolkit.render.jogl.Jogl4RendererConfigurationShaderSelector;
import vsdk.toolkit.gui.LightGizmoStyle;
import vsdk.toolkit.render.jogl.polyhedralBoundedSolid.Jogl4PolyhedralBoundedSolidRenderer;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.Calligraphic2DBuffer;
import vsdk.toolkit.media.RGBImageUncompressed;

public class Jogl4DebuggerRenderer implements GLEventListener
{
    private static final double HUD_INSET_DEPTH = 2.8;

    private final DebuggerModel model;
    private final Jogl4DebuggerHudRenderer hudRenderer;
    private final SimpleMaterial csgOperandMaterialA;
    private final SimpleMaterial csgOperandMaterialB;
    private File pendingScreenshotFile;

    public Jogl4DebuggerRenderer(DebuggerModel model)
    {
        this.model = model;
        this.hudRenderer = new Jogl4DebuggerHudRenderer(model);
        this.csgOperandMaterialA = createInsetMaterial(1.0, 0.502, 0.502);
        this.csgOperandMaterialB = createInsetMaterial(0.502, 1.0, 0.502);
        this.pendingScreenshotFile = null;
    }

    public void requestScreenshot(File outputFile)
    {
        pendingScreenshotFile = outputFile;
    }

    private static SimpleMaterial createInsetMaterial(double r, double g, double b)
    {
        SimpleMaterial m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(0.2 * r, 0.2 * g, 0.2 * b));
        m = m.withDiffuse(new ColorRgb(r, g, b));
        m = m.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
        m = m.withDoubleSided(false);
        m = m.withPhongExponent(100);
        return m;
    }

    private static Vector3Dd solidCenter(PolyhedralBoundedSolid solid)
    {
        double[] minMax;

        if ( solid == null ) {
            return new Vector3Dd(0, 0, 0);
        }
        minMax = solid.getMinMax();
        if ( minMax == null || minMax.length < 6 ) {
            return new Vector3Dd(0, 0, 0);
        }
        return new Vector3Dd(
            (minMax[0] + minMax[3]) / 2.0,
            (minMax[1] + minMax[4]) / 2.0,
            (minMax[2] + minMax[5]) / 2.0);
    }

    private static double solidMaxExtent(PolyhedralBoundedSolid solid)
    {
        double[] minMax;
        double ex;
        double ey;
        double ez;

        if ( solid == null ) {
            return 1.0;
        }
        minMax = solid.getMinMax();
        if ( minMax == null || minMax.length < 6 ) {
            return 1.0;
        }
        ex = Math.abs(minMax[0] - minMax[3]);
        ey = Math.abs(minMax[1] - minMax[4]);
        ez = Math.abs(minMax[2] - minMax[5]);
        return Math.max(ex, Math.max(ey, ez));
    }

    private static Vector3Dd cameraRelativeAnchor(Camera camera,
        double ndcX,
        double ndcY,
        double depth)
    {
        Vector3Dd eye = camera.getPosition();
        Vector3Dd front = camera.getFront().normalized();
        Vector3Dd up = camera.getUp().normalized();
        Vector3Dd right = camera.getLeft().multiply(-1).normalized();
        double viewportY = Math.max(camera.getViewportYSize(), 1e-9);
        double aspect = camera.getViewportXSize() / viewportY;
        double offsetX;
        double offsetY;
        double safeDepth = Math.max(depth, 1e-9);

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            double zoom = Math.max(camera.getOrthogonalZoom(), 1e-9);
            offsetX = ndcX * (aspect / zoom);
            offsetY = ndcY * (1.0 / zoom);
        }
        else {
            double halfHeight = safeDepth *
                Math.tan(Math.toRadians(camera.getFov() / 2.0));
            double halfWidth = halfHeight * aspect;
            offsetX = ndcX * halfWidth;
            offsetY = ndcY * halfHeight;
        }

        return eye.add(front.multiply(safeDepth))
                  .add(right.multiply(offsetX))
                  .add(up.multiply(offsetY));
    }

    private static Matrix4x4d buildInsetModelMatrix(
        Vector3Dd anchorPoint,
        double scale,
        Vector3Dd center)
    {
        return new Matrix4x4d()
            .translation(anchorPoint)
            .multiply(new Matrix4x4d().scale(scale, scale, scale)
                .multiply(new Matrix4x4d().translation(-center.x(), -center.y(),
                    -center.z())));
    }

    private void drawInsetSolid(GL4 gl,
        PolyhedralBoundedSolid solid,
        SimpleMaterial material,
        Vector3Dd anchorPoint,
        double mainSolidExtent)
    {
        Vector3Dd center;
        double extent;
        double scale;

        if ( solid == null ) {
            return;
        }
        center = solidCenter(solid);
        extent = solidMaxExtent(solid);
        if ( extent < 1e-12 ) {
            extent = 1.0;
        }
        if ( mainSolidExtent < 1e-12 ) {
            mainSolidExtent = 1.0;
        }
        scale = 0.75 * (mainSolidExtent / extent);

        Matrix4x4d modelMatrix = buildInsetModelMatrix(anchorPoint, scale, center);
        Jogl4SimpleMaterialRenderer.activate(gl, material);
        Jogl4PolyhedralBoundedSolidRenderer.draw(gl, solid, model.getCamera(),
            model.getQuality(), modelMatrix);
    }

    private void drawCsgOperandInsets(GL4 gl, int viewportWidth, int viewportHeight)
    {
        PolyhedralBoundedSolid operandA = model.getCsgPreviewOperandA();
        PolyhedralBoundedSolid operandB = model.getCsgPreviewOperandB();
        PolyhedralBoundedSolid mainSolid = model.getSolid();
        Camera camera = model.getCamera();
        double mainExtent;
        Vector3Dd leftAnchor;
        Vector3Dd rightAnchor;

        if ( operandA == null || operandB == null || mainSolid == null ) {
            return;
        }
        if ( viewportWidth <= 0 || viewportHeight <= 0 ) {
            return;
        }
        mainExtent = solidMaxExtent(mainSolid);

        leftAnchor = cameraRelativeAnchor(camera, -0.76, -0.76,
            HUD_INSET_DEPTH);
        rightAnchor = cameraRelativeAnchor(camera, 0.76, -0.76,
            HUD_INSET_DEPTH);

        drawInsetSolid(gl, operandA, csgOperandMaterialA, leftAnchor, mainExtent);
        drawInsetSolid(gl, operandB, csgOperandMaterialB, rightAnchor, mainExtent);
        Jogl4SimpleMaterialRenderer.activate(gl, model.getMaterial());
    }

    public void refreshCanvasAfterWindowModeChange()
    {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                if ( model.getCanvas() == null ) {
                    return;
                }
                if ( model.getMainFrame() != null ) {
                    model.getMainFrame().validate();
                    model.getMainFrame().repaint();
                }
                model.getCanvas().revalidate();
                model.getCanvas().repaint();
                model.getCanvas().display();
                model.getCanvas().requestFocusInWindow();
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run()
                    {
                        if ( model.getCanvas() != null ) {
                            model.getCanvas().display();
                        }
                    }
                });
            }
        });
    }

    private static void appendSegmentedLines(
        Calligraphic2DBuffer source,
        float r,
        float g,
        float b,
        ArrayList<Float> positions,
        ArrayList<Float> colors)
    {
        for ( int i = 0; i < source.getNumLines(); i++ ) {
            Vector3Dd[] segment = source.get2DLine(i);
            Vector3Dd p0 = segment[0];
            Vector3Dd p1 = segment[1];
            positions.add((float)p0.x());
            positions.add((float)p0.y());
            positions.add((float)p0.z());
            positions.add((float)p1.x());
            positions.add((float)p1.y());
            positions.add((float)p1.z());
            for ( int v = 0; v < 2; v++ ) {
                colors.add(r);
                colors.add(g);
                colors.add(b);
            }
        }
    }

    private static float[] toFloatArray(List<Float> values)
    {
        float[] out = new float[values.size()];
        for ( int i = 0; i < values.size(); i++ ) {
            out[i] = values.get(i);
        }
        return out;
    }

    private static void drawBufferedLines(GL4 gl,
                                          Calligraphic2DBuffer lines,
                                          float r,
                                          float g,
                                          float b,
                                          float lineWidth)
    {
        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Float> colors = new ArrayList<Float>();

        appendSegmentedLines(lines, r, g, b, positions, colors);
        if ( positions.isEmpty() ) {
            return;
        }
        Jogl4LineRenderer.drawLines(gl, Matrix4x4d.identityMatrix(),
            toFloatArray(positions), toFloatArray(colors), lineWidth, -4.0e-4f);
    }

    private void
    renderLinesResult(GL4 gl,
                      Calligraphic2DBuffer contourLines,
                      Calligraphic2DBuffer visibleLines,
                      Calligraphic2DBuffer hiddenLines)
    {
        drawBufferedLines(gl, hiddenLines, 0.7f, 0.7f, 0.7f, 1.0f);
        drawBufferedLines(gl, visibleLines, 0.0f, 0.0f, 0.0f, 4.0f);
        drawBufferedLines(gl, contourLines, 0.0f, 0.0f, 0.0f, 8.0f);
    }

    private void drawReferenceFrame(GL4 gl, Matrix4x4d mvp)
    {
        if ( model.getEdgeIndex() <= -3 || !model.isShowCoordinateSystem() ) {
            return;
        }
        float[] positions = new float[] {
            0, 0, 0, 1, 0, 0,
            0, 0, 0, 0, 1, 0,
            0, 0, 0, 0, 0, 1
        };
        float[] colors = new float[] {
            1, 0, 0, 1, 0, 0,
            0, 1, 0, 0, 1, 0,
            0, 0, 1, 0, 0, 1
        };
        Jogl4LineRenderer.drawLines(gl, mvp, positions, colors, 3.0f, -3.0e-4f);
    }

    private void drawObjectsGL(GL4 gl, int viewportWidth, int viewportHeight)
    {
        if ( model.getSolid() == null ) {
            return;
        }
        Matrix4x4d modelMatrix = model.getSolidModelMatrix();
        Matrix4x4d mvp = model.getCamera().calculateProjectionMatrix()
            .multiply(modelMatrix);

        Jogl4SimpleMaterialRenderer.activate(gl, model.getMaterial());
        Jogl4LightRenderer.activate(gl, model.getLight1());
        Jogl4LightRenderer.draw(gl, model.getLight1(), model.getCamera(),
            LightGizmoStyle.OMNI_BILLBOARD);
        Jogl4LightRenderer.activate(gl, model.getLight2());
        Jogl4LightRenderer.draw(gl, model.getLight2(), model.getCamera(),
            LightGizmoStyle.OMNI_BILLBOARD);
        Jogl4PolyhedralBoundedSolidRenderer.draw(gl, model.getSolid(),
            model.getCamera(), model.getQuality(), modelMatrix);

        drawReferenceFrame(gl, mvp);
        Jogl4PolyhedralBoundedSolidRenderer.drawDebugFaceBoundary(gl,
            model.getSolid(), model.getFaceIndex(), mvp);
        Jogl4PolyhedralBoundedSolidRenderer.drawDebugFace(gl, model.getSolid(),
            model.getFaceIndex(), modelMatrix, mvp, model.getCamera());

        Calligraphic2DBuffer contourLines;
        Calligraphic2DBuffer visibleLines;
        Calligraphic2DBuffer hiddenLines;
        List <SimpleBody> bodyArray;
        SimpleBody body;

        if ( model.isDebugEdges() && model.getEdgeIndex() > -3 ) {
            Jogl4PolyhedralBoundedSolidRenderer.drawDebugEdges(gl,
                model.getSolid(), model.getCamera(), model.getEdgeIndex(), mvp);
        }
        else if ( model.getEdgeIndex() == -3 ) {
            contourLines = new Calligraphic2DBuffer();
            visibleLines = new Calligraphic2DBuffer();
            hiddenLines = new Calligraphic2DBuffer();
            bodyArray = new ArrayList <SimpleBody>();

            body = new SimpleBody();
            body.setGeometry(model.getSolid());
            body.setPosition(new Vector3Dd());
            body.setRotation(modelMatrix);
            body.setRotationInverse(modelMatrix.inverse());
            bodyArray.add(body);
            HiddenLineRenderer.executeAppelAlgorithm(bodyArray, model.getCamera(),
                contourLines, visibleLines, hiddenLines);
            renderLinesResult(gl, contourLines, visibleLines, hiddenLines);
        }

        /*
        contourLines = null;
        visibleLines = null;
        hiddenLines = null;
        bodyArray = null;
        body = null;
        */
    }

    /** Called by drawable to initiate drawing
    @param drawable 
    */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();

        gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL4.GL_DEPTH_TEST);

        drawObjectsGL(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        gl.glClear(GL4.GL_DEPTH_BUFFER_BIT);
        drawCsgOperandInsets(gl, drawable.getSurfaceWidth(),
            drawable.getSurfaceHeight());
        if ( model.isHudEnabled() ) {
            hudRenderer.draw(drawable);
        }
        exportPendingScreenshot(gl, drawable.getSurfaceWidth(),
            drawable.getSurfaceHeight());
    }
   
    /** Not used method, but needed to instanciate GLEventListener
    @param drawable 
    */
    @Override
    public void init(GLAutoDrawable drawable) {
        hudRenderer.init(drawable);
    }

    /** Not used method, but needed to instanciate GLEventListener
    @param drawable 
    */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        if ( drawable != null ) {
            GL4 gl = drawable.getGL().getGL4();
            Jogl4LineRenderer.release(gl);
            Jogl4PolyhedralBoundedSolidRenderer.release(gl);
            Jogl4ImageRenderer.dispose(gl);
            Jogl4RendererConfigurationShaderSelector.dispose(gl);
        }
        hudRenderer.dispose(drawable);
    }
    
    /** Called to indicate the drawing surface has been moved and/or resized
    @param drawable
    @param x
    @param y
    @param width
    @param height
    */
    @Override
    public void reshape (GLAutoDrawable drawable,
                         int x,
                         int y,
                         int width,
                         int height) {
        GL4 gl = drawable.getGL().getGL4();
        gl.glViewport(0, 0, width, height); 

        model.getCamera().updateViewportResize(width, height);
        hudRenderer.updateViewportSize(width, height);
    }

    private void exportPendingScreenshot(GL4 gl, int width, int height)
    {
        File outputFile = pendingScreenshotFile;
        if ( outputFile == null || width <= 0 || height <= 0 ) {
            return;
        }

        pendingScreenshotFile = null;
        gl.glFinish();
        RGBImageUncompressed image = captureRgbImage(gl, width, height);
        ensureParentFolder(outputFile);
        ImagePersistence.exportPNG(outputFile, image);
        System.out.println("[PolyhedralBoundedSolidExample] Exported " +
            outputFile.getPath());
    }

    private static RGBImageUncompressed captureRgbImage(GL4 gl, int width, int height)
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

    private static void ensureParentFolder(File outputFile)
    {
        File parent = outputFile.getParentFile();
        if ( parent != null && !parent.exists() ) {
            parent.mkdirs();
        }
    }
}
