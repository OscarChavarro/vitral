// Java basic classes
import com.jogamp.opengl.GL;
import java.util.ArrayList;
import java.util.List;
import models.DebuggerModel;
import java.awt.EventQueue;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.HiddenLineRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglPolyhedralBoundedSolidRenderer;

public class JoglDebuggerRenderer implements GLEventListener
{
    private static final double HUD_INSET_DEPTH = 2.8;

    private final DebuggerModel model;
    private final JoglDebuggerHudRenderer hudRenderer;
    private final Material csgOperandMaterialA;
    private final Material csgOperandMaterialB;

    public JoglDebuggerRenderer(DebuggerModel model)
    {
        this.model = model;
        this.hudRenderer = new JoglDebuggerHudRenderer(model);
        this.csgOperandMaterialA = createInsetMaterial(1.0, 0.502, 0.502);
        this.csgOperandMaterialB = createInsetMaterial(0.502, 1.0, 0.502);
    }

    private static Material createInsetMaterial(double r, double g, double b)
    {
        Material m = new Material();
        m.setAmbient(new ColorRgb(0.2 * r, 0.2 * g, 0.2 * b));
        m.setDiffuse(new ColorRgb(r, g, b));
        m.setSpecular(new ColorRgb(1.0, 1.0, 1.0));
        m.setDoubleSided(false);
        m.setPhongExponent(100);
        return m;
    }

    private static Vector3D solidCenter(PolyhedralBoundedSolid solid)
    {
        double[] minMax;

        if ( solid == null ) {
            return new Vector3D(0, 0, 0);
        }
        minMax = solid.getMinMax();
        if ( minMax == null || minMax.length < 6 ) {
            return new Vector3D(0, 0, 0);
        }
        return new Vector3D(
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

    private static Vector3D cameraRelativeAnchor(Camera camera,
        double ndcX,
        double ndcY,
        double depth)
    {
        Vector3D eye = camera.getPosition();
        Vector3D front = camera.getFront().normalized();
        Vector3D up = camera.getUp().normalized();
        Vector3D right = camera.getLeft().multiply(-1).normalized();
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

    private void drawInsetSolid(GL2 gl,
        PolyhedralBoundedSolid solid,
        Material material,
        Vector3D anchorPoint,
        double mainSolidExtent)
    {
        Vector3D center;
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

        gl.glPushMatrix();
        gl.glTranslated(anchorPoint.x(), anchorPoint.y(), anchorPoint.z());
        gl.glScaled(scale, scale, scale);
        gl.glTranslated(-center.x(), -center.y(), -center.z());
        JoglMaterialRenderer.activate(gl, material);
        JoglPolyhedralBoundedSolidRenderer.draw(gl, solid, model.getCamera(),
            model.getQuality());
        gl.glPopMatrix();
    }

    private void drawCsgOperandInsets(GL2 gl, int viewportWidth, int viewportHeight)
    {
        PolyhedralBoundedSolid operandA = model.getCsgPreviewOperandA();
        PolyhedralBoundedSolid operandB = model.getCsgPreviewOperandB();
        PolyhedralBoundedSolid mainSolid = model.getSolid();
        Camera camera = model.getCamera();
        double mainExtent;
        Vector3D leftAnchor;
        Vector3D rightAnchor;

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
        JoglMaterialRenderer.activate(gl, model.getMaterial());
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

    private void
    renderLinesResult(GL2 gl, List <Vector3D> contourLines,
                      List <Vector3D> visibleLines,
                      List <Vector3D> hiddenLines)
    {
        int i;
        Vector3D p;

        gl.glPushAttrib(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_DEPTH_TEST);

        //-----------------------------------------------------------------
        gl.glLineWidth(4.0f);
        gl.glColor3d(0, 0, 0);
        gl.glBegin(GL2.GL_LINES);
        for ( i = 0; i < contourLines.size(); i++ ) {
            p = contourLines.get(i);
            gl.glVertex3d(p.x(), p.y(), p.z());
        }
        gl.glEnd();

        gl.glLineWidth(4.0f);
        gl.glColor3d(0, 0, 0);
        gl.glBegin(GL2.GL_LINES);
        for ( i = 0; i < visibleLines.size(); i++ ) {
            p = visibleLines.get(i);
            gl.glVertex3d(p.x(), p.y(), p.z());
        }
        gl.glEnd();

/*
        gl.glLineWidth(1.0f);
        gl.glColor3d(0, 0, 0);
        gl.glBegin(gl.GL_LINES);
        for ( i = 0; i < hiddenLines.size(); i++ ) {
            p = hiddenLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        gl.glEnd();
*/
        //-----------------------------------------------------------------
/*
        gl.glPointSize(4.0f);
        gl.glColor3d(0.5, 0.5, 0.9);
        gl.glBegin(gl.GL_POINTS);
        for ( i = 0; i < contourLines.size(); i++ ) {
            p = contourLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        for ( i = 0; i < visibleLines.size(); i++ ) {
            p = visibleLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        gl.glEnd();
*/
        //-----------------------------------------------------------------
        gl.glPopAttrib();
    }

    private void drawObjectsGL(GL2 gl, int viewportWidth, int viewportHeight)
    {
        gl.glLoadIdentity();

        //-----------------------------------------------------------------
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth((float)3.0);

        if ( model.getEdgeIndex() > -3 && model.isShowCoordinateSystem() ) {
            gl.glBegin(GL2.GL_LINES);
                gl.glColor3d(1, 0, 0);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(1, 0, 0);
    
                gl.glColor3d(0, 1, 0);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(0, 1, 0);
    
                gl.glColor3d(0, 0, 1);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(0, 0, 1);
            gl.glEnd();
        }

        if ( model.getSolid() == null ) {
            return;
        }

        //-----------------------------------------------------------------
        JoglMaterialRenderer.activate(gl, model.getMaterial());
        JoglLightRenderer.activate(gl, model.getLight1());
        JoglLightRenderer.draw(gl, model.getLight1());
        JoglLightRenderer.activate(gl, model.getLight2());
        JoglLightRenderer.draw(gl, model.getLight2());
        gl.glEnable(GL2.GL_LIGHTING);
        JoglPolyhedralBoundedSolidRenderer.draw(gl, model.getSolid(), model.getCamera(), model.getQuality());
        JoglPolyhedralBoundedSolidRenderer.drawDebugFaceBoundary(gl, model.getSolid(), model.getFaceIndex());
        JoglPolyhedralBoundedSolidRenderer.drawDebugFace(gl, model.getSolid(), model.getFaceIndex());

        //-----------------------------------------------------------------
        List<Vector3D> contourLines;
        List <Vector3D> visibleLines;
        List <Vector3D> hiddenLines;
        List <SimpleBody> bodyArray;
        SimpleBody body;

        if ( model.isDebugEdges() && model.getEdgeIndex() > -3 ) {
            JoglPolyhedralBoundedSolidRenderer.drawDebugEdges(gl, model.getSolid(), model.getCamera(), model.getEdgeIndex());
        }
        else if ( model.getEdgeIndex() == -3 ) {
            contourLines = new ArrayList <Vector3D>();
            visibleLines = new ArrayList <Vector3D>();
            hiddenLines = new ArrayList <Vector3D>();
            bodyArray = new ArrayList <SimpleBody>();

            body = new SimpleBody();
            body.setGeometry(model.getSolid());
            body.setPosition(new Vector3D());
            body.setRotation(new Matrix4x4());
            body.setRotationInverse(new Matrix4x4());
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
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);
        gl.glEnable(GL2.GL_DEPTH_TEST);

        JoglCameraRenderer.activate(gl, model.getCamera());

        drawObjectsGL(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
        drawCsgOperandInsets(gl, drawable.getSurfaceWidth(),
            drawable.getSurfaceHeight());
        hudRenderer.draw(drawable);
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
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height); 

        model.getCamera().updateViewportResize(width, height);
        hudRenderer.updateViewportSize(width, height);
    }
}
