// Java GUI classes

import java.awt.event.ActionEvent;

// VSDK classes
import vsdk.toolkit.media.Image;

// Internal classes
import vsdk.transition.gui.GuiCache;

public class PanelManagerSciVisApplication extends PanelManager
{
    private SciVisApplication parent;

    public PanelManagerSciVisApplication(SciVisApplication parent, GuiCache parentGui)
    {
        super(parentGui);
        this.parent = parent;
    }

    public void propagateImage(int currentTimeTake, int currentSlice)
    {
        Image img = null;

        System.out.println("Show take " + currentTimeTake + ", slice " + currentSlice);

        if ( parent.study == null ) {
            return;
        }

        img = parent.study.getSliceImageAt(currentTimeTake, currentSlice);

        int i;

        ViewerPanel p;
        ViewerPanel2DSwing pe;
        for ( i = 0; i < panels.size(); i++ ) {
            p = panels.get(i);
            if ( p instanceof ViewerPanel2DSwing ) {
                pe = (ViewerPanel2DSwing)p;
                pe.setImage(img);
                pe.repaint();
            }
        }
    }

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();
        int i;

        if ( label.equals("IDC_PANEL_MANAGER_NEW_AREA_2D") ) {
            ViewerPanel p = new ViewerPanel2DSwing(parent, this);
            panels.add(p);
            reorganizePanels();
            parent.mainWindowWidget.pack();
        }
        else if ( label.equals("IDC_PANEL_MANAGER_NEW_AREA_3D") ) {
            ViewerPanel p = new ViewerPanel3DJogl(parent, this);
            p.configureTestColor();
            panels.add(p);
            reorganizePanels();
            parent.mainWindowWidget.pack();
        }
        else if ( label.equals("IDC_PANEL_MANAGER_DELETE_AREA") ) {
            for ( i = 0; i < panels.size(); i++ ) {
                if ( i == pointedViewerPanel ) {
                    panels.remove(i);
                    break;
                }
            }
            reorganizePanels();
            parent.mainWindowWidget.pack();
        }
        else { 
            parent.executorPanel.actionPerformed(ev);
        }
    }
}
