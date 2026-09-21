package application.gui;

// Java GUI classes
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BoxLayout;

// JOGL classes
import com.jogamp.opengl.GL4;

// VSDK classes
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.light.Light;
import application.render.jogl.Jogl4SceneRenderer;

// Application classes
import application.SceneEditorApplication;

public class ModifyPanel extends JPanel
{
    protected SceneEditorApplication parent;
    protected SimpleBody target;

    // Implementations
    private ModifyPanelForFunctionalExplicitSurface functionalExplicitSurfaceEditor;

    public ModifyPanel(SceneEditorApplication parent)
    {
        this.parent = parent;
        notifyTargetEndEdit();
        functionalExplicitSurfaceEditor = null;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public SimpleBody getTarget()
    {
        return target;
    }

    public void notifyTargetBeginEdit(SimpleBody target)
    {
        this.target = target;
        removeAll();

        if ( target.getGeometry() instanceof FunctionalExplicitSurface ) {
            if ( functionalExplicitSurfaceEditor == null ) {
                functionalExplicitSurfaceEditor = new ModifyPanelForFunctionalExplicitSurface(parent);
            }
            functionalExplicitSurfaceEditor.notifyTargetBeginEdit(target, this);
        }
        else {
            JLabel label = new JLabel("No editor for " + target.getGeometry().getClass().getName());
            add(label);
        }
    }

    public final void notifyTargetEndEdit()
    {
        removeAll();
        JLabel label = new JLabel("No selected object for modifying.");
        add(label);
        repaint();
        target = null;
    }

    public void draw(GL4 gl, Camera camera, Light light, RendererConfiguration quality)
    {
        Jogl4SceneRenderer.drawBody(gl, target, camera, light, quality);
    }

}
