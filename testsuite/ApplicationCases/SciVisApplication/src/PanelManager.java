//===========================================================================

// Java GUI classes
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

// Internal classes
import vsdk.transition.render.swing.SwingGuiCacheRenderer;

public class PanelManager extends JPanel implements ActionListener
{
    private SciVisApplication parent;

    public PanelManager(SciVisApplication parent)
    {
        this.parent = parent;
	setBackground(Color.RED);

        JMenu menu = SwingGuiCacheRenderer.buildPopupMenu(parent.gui, "POPUP_PANEL_MANAGER", this);
	JPopupMenu popup = menu.getPopupMenu();
        setComponentPopupMenu(popup);
    }

    public void actionPerformed(ActionEvent ev) {
	System.out.println("Guepa!");
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
