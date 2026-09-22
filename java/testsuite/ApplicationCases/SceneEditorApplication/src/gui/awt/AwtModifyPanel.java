package gui.awt;

// Java basic classes
import java.util.ArrayList;
import java.util.List;

// Java GUI classes
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BoxLayout;

// VSDK classes
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.scene.SimpleBody;

// Application classes
import render.BodyEditFeedbackProvider;
import render.RenderPrimitive;

/**
Panel of the GUI that edits the selected body with an editor specific to its
geometry (subclasses of this panel). It does not draw: renderers ask it,
through `BodyEditFeedbackProvider`, for the feedback geometry to present over
the body under edition, so editors do not depend on any rendering technology.
*/
public class AwtModifyPanel extends JPanel implements BodyEditFeedbackProvider
{
    protected AwtApplicationHost parent;
    protected SimpleBody target;

    // Implementations
    private AwtModifyPanelForFunctionalExplicitSurface functionalExplicitSurfaceEditor;
    /// Editor for the current target, or null if there is none
    private AwtModifyPanel activeEditor;

    public AwtModifyPanel(AwtApplicationHost parent)
    {
        this.parent = parent;
        notifyTargetEndEdit();
        functionalExplicitSurfaceEditor = null;
        activeEditor = null;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    @Override
    public SimpleBody getTarget()
    {
        return target;
    }

    public void notifyTargetBeginEdit(SimpleBody target)
    {
        this.target = target;
        removeAll();
        activeEditor = null;

        if ( target.getGeometry() instanceof FunctionalExplicitSurface ) {
            if ( functionalExplicitSurfaceEditor == null ) {
                functionalExplicitSurfaceEditor = new AwtModifyPanelForFunctionalExplicitSurface(parent);
            }
            functionalExplicitSurfaceEditor.notifyTargetBeginEdit(target, this);
            activeEditor = functionalExplicitSurfaceEditor;
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
        activeEditor = null;
    }

    /**
    Editors for specific geometries override this method to present their
    feedback (handles, bounds...) over the target. This one delegates to the
    editor of the current target.
    @return the feedback geometry to present over the target, in world
    coordinates (empty if there is none)
    */
    @Override
    public List<RenderPrimitive> buildEditFeedback()
    {
        if ( activeEditor != null ) {
            return activeEditor.buildEditFeedback();
        }
        return new ArrayList<>();
    }

}
