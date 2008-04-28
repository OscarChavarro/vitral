//===========================================================================

// Java GUI classes
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BoxLayout;

// JOGL classes
import javax.media.opengl.GL;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.FunctionalExplicitSurface;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer;

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

    public void notifyTargetEndEdit()
    {
        removeAll();
        JLabel label = new JLabel("No selected object for modifying.");
        add(label);
        repaint();
        target = null;
    }

    public void draw(GL gl, Camera camera, RendererConfiguration quality)
    {
        JoglSimpleBodyRenderer.draw(gl, target, camera, quality);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
