//===========================================================================

package application.gui;

// Java AWT/Swing classes
import javax.swing.SingleSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// Application classes
import application.SceneEditorApplication;

public class MyChangeListener implements ChangeListener
{
    public SceneEditorApplication parent;
    public MyChangeListener(SceneEditorApplication parent)
    {
        this.parent = parent;
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        SingleSelectionModel sm = (SingleSelectionModel)e.getSource();
        if ( sm.getSelectedIndex() == 1 ) {
            parent.modifyPanelSelected = true;
            parent.drawingArea.reportTargetToModifyPanel();
        }
        else {
            parent.modifyPanelSelected = false;
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
