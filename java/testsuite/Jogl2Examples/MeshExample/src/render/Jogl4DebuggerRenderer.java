package render;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import model.MeshModel;
import vsdk.toolkit.render.jogl.Jogl2CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl2LightRenderer;
import vsdk.toolkit.render.jogl.Jogl2SimpleBodyRenderer;

public class Jogl4DebuggerRenderer implements GLEventListener {
    private final MeshModel model;

    public Jogl4DebuggerRenderer(MeshModel model) {
        this.model = model;
    }

    private void drawObjectsGL(GL2 gl) {
        gl.glLoadIdentity();
        gl.glDisable(GL2.GL_CULL_FACE);

        Jogl2SimpleBodyRenderer.setAutomaticDisplayListManagement(true);

        for ( int i = 0; i < model.getScene().getSimpleBodies().size(); i++ ) {
            Jogl2SimpleBodyRenderer.drawWithVertexArrays(
                gl,
                model.getScene().getSimpleBodies().get(i),
                model.getCamera(),
                model.getQualitySelection());
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glClearColor(0.5f, 0.5f, 0.9f, 1.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        Jogl2CameraRenderer.activate(gl, model.getCamera());
        Jogl2LightRenderer.activate(gl, model.getLight());

        drawObjectsGL(gl);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        model.getCamera().updateViewportResize(width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }
}
