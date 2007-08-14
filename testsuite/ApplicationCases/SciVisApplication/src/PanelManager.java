//===========================================================================

// Java basic classes
import java.util.ArrayList;

// Java GUI classes
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

// Internal classes
import vsdk.transition.render.swing.SwingGuiCacheRenderer;

public class PanelManager extends JPanel implements ActionListener
{
    private SciVisApplication parent;
    private ArrayList<ViewerPanel> panels;
    private JSplitPane splitPane;

    public PanelManager(SciVisApplication parent)
    {
        this.parent = parent;
        setBackground(Color.RED);
        setLayout(new BorderLayout());

        JMenu menu = SwingGuiCacheRenderer.buildPopupMenu(parent.gui, "POPUP_PANEL_MANAGER_BASE", this);
        JPopupMenu popup = menu.getPopupMenu();
        setComponentPopupMenu(popup);

	splitPane = null;
        panels = new ArrayList<ViewerPanel>();
    }

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();
        if ( label.equals("IDC_PANEL_MANAGER_NEW_AREA_3D") ) {
            ViewerPanel p = new ViewerPanel(parent);
	    if ( panels.size() == 0 ) {
                // New panel is the only child to parent panel
		removeAll();
	        add(p, BorderLayout.CENTER);
	        panels.add(p);
	        parent.mainWindowWidget.pack();
	    }
	    else if ( panels.size() == 1 ) {
                // New panel is the only child to parent panel

	    }
	}
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
