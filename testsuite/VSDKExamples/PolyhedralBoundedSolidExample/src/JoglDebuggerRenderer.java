// Java basic classes
import java.util.ArrayList;
import models.DebuggerModel;
import java.awt.EventQueue;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.HiddenLineRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglPolyhedralBoundedSolidRenderer;

public class JoglDebuggerRenderer implements GLEventListener
{
    private final DebuggerModel model;
    private final JoglDebuggerHudRenderer hudRenderer;

    public JoglDebuggerRenderer(DebuggerModel model)
    {
        this.model = model;
        this.hudRenderer = new JoglDebuggerHudRenderer(model);
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
    renderLinesResult(GL2 gl, ArrayList <Vector3D> contourLines,
                      ArrayList <Vector3D> visibleLines,
                      ArrayList <Vector3D> hiddenLines)
    {
        int i;
        Vector3D p;

        gl.glPushAttrib(GL2.GL_DEPTH_TEST);
        gl.glDisable(GL2.GL_DEPTH_TEST);

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

    private void drawObjectsGL(GL2 gl)
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
        if ( model.isDebugVertices() ) {
            JoglPolyhedralBoundedSolidRenderer.drawDebugVertices(gl, model.getSolid(), model.getCamera());
        }

        //-----------------------------------------------------------------
        ArrayList <Vector3D> contourLines;
        ArrayList <Vector3D> visibleLines;
        ArrayList <Vector3D> hiddenLines;
        ArrayList <SimpleBody> bodyArray;
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

        drawObjectsGL(gl);
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
