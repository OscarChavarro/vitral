package render;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import model.DebuggerModel;
import vsdk.toolkit.render.jogl.Jogl2CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl2LightRenderer;
import vsdk.toolkit.render.jogl.Jogl2Md2MeshRenderer;

public class Jogl2DebuggerRenderer {
    private final DebuggerModel model;
    private final Jogl2DebuggerHudRenderer hudRenderer;

    public Jogl2DebuggerRenderer(DebuggerModel model)
    {
        this.model = model;
        this.hudRenderer = new Jogl2DebuggerHudRenderer(model);
    }

    public void init(GLAutoDrawable drawable)
    {
        Jogl2Md2MeshRenderer.initGL(drawable.getGL().getGL2(), model.md2Mesh);
        hudRenderer.init(drawable);
    }

    public void display(GLAutoDrawable drawable)
    {
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glClearColor(0.5f, 0.5f, 0.9f, 1.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        Jogl2CameraRenderer.activate(gl, model.camera);
        for (int i = 0; i < model.lights.size(); i++) {
            Jogl2LightRenderer.activate(gl, model.lights.get(i));
            Jogl2LightRenderer.draw(gl, model.lights.get(i));
        }

        drawObjectsGL(gl);
        hudRenderer.draw(drawable);
    }

    public void reshape(GLAutoDrawable drawable, int width, int height)
    {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        model.camera.updateViewportResize(width, height);
        hudRenderer.updateViewportSize(width, height);
    }

    public void dispose(GLAutoDrawable drawable)
    {
        hudRenderer.dispose(drawable);
    }

    private void drawObjectsGL(GL2 gl)
    {
        gl.glLoadIdentity();
        gl.glTranslated(model.x, 0, 0);

        gl.glLineWidth((float)3.0);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
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

        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_LIGHTING);

        Jogl2Md2MeshRenderer.draw(gl, model.md2Mesh, model.qualitySelection);
    }
}
