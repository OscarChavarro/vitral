package gui.awt;

// Java AWT/Swing classes
import javax.swing.SingleSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// Application classes

public class AwtModifyTabChangeListener implements ChangeListener
{
    public AwtApplicationHost parent;
    public AwtModifyTabChangeListener(AwtApplicationHost parent)
    {
        this.parent = parent;
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        SingleSelectionModel sm = (SingleSelectionModel)e.getSource();
        if ( sm.getSelectedIndex() == 1 ) {
            parent.getApplicationModel().getGuiState().setModifyPanelSelected(true);
            parent.reportTargetToModifyPanel();
        }
        else {
            parent.getApplicationModel().getGuiState().setModifyPanelSelected(false);
        }
    }
}
