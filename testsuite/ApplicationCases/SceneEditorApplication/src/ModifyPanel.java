//===========================================================================

// Java GUI classes
import javax.swing.JLabel;
import javax.swing.JPanel;

// JOGL classes
import javax.media.opengl.GL;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer;

public class ModifyPanel extends JPanel
{
    private SceneEditorApplication parent;
    private SimpleBody target;

    public ModifyPanel(SceneEditorApplication parent)
    {
        this.parent = parent;
        notifyTargetEndEdit();
    }

    public SimpleBody getTarget()
    {
        return target;
    }

    public void notifyTargetBeginEdit(SimpleBody target)
    {
        this.target = target;
        removeAll();
        JLabel label = new JLabel("No editor for " + target.getGeometry().getClass().getName());
        add(label);
    }

    public void notifyTargetEndEdit()
    {
        removeAll();
        JLabel label = new JLabel("No selected object for modifying.");
        add(label);
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
