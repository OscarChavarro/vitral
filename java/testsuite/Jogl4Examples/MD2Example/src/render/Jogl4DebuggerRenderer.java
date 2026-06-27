package render;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import model.DebuggerModel;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.render.jogl.Jogl4LightRenderer;
import vsdk.toolkit.gui.gizmo.LightGizmoStyle;
import vsdk.toolkit.render.jogl.Jogl4Md2MeshRenderer;

public class Jogl4DebuggerRenderer {
    private final DebuggerModel model;
    private final Jogl4DebuggerHudRenderer hudRenderer;

    public Jogl4DebuggerRenderer(DebuggerModel model)
    {
        this.model = model;
        this.hudRenderer = new Jogl4DebuggerHudRenderer(model);
    }

    public void init(GLAutoDrawable drawable)
    {
        Jogl4Md2MeshRenderer.initGL(drawable.getGL().getGL4(), model.md2Mesh);
        hudRenderer.init(drawable);
    }

    public void display(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();

        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glClearColor(0.5f, 0.5f, 0.9f, 1.0f);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL4.GL_DEPTH_BUFFER_BIT);
        if ( model.lights != null && !model.lights.isEmpty() ) {
            Jogl4Md2MeshRenderer.draw(
                gl,
                model.md2Mesh,
                model.camera,
                model.lights.get(0),
                model.qualitySelection,
                model.x);

            for ( Light light : model.lights ) {
                if ( light != null ) {
                    Jogl4LightRenderer.draw(gl, light, model.camera, LightGizmoStyle.OMNI_BILLBOARD);
                }
            }
        }
        hudRenderer.draw(drawable);
    }

    public void reshape(GLAutoDrawable drawable, int width, int height)
    {
        GL4 gl = drawable.getGL().getGL4();
        gl.glViewport(0, 0, width, height);
        model.camera.updateViewportResize(width, height);
        hudRenderer.updateViewportSize(width, height);
    }

    public void dispose(GLAutoDrawable drawable)
    {
        Jogl4Md2MeshRenderer.dispose(drawable.getGL().getGL4());
        hudRenderer.dispose(drawable);
    }
}
