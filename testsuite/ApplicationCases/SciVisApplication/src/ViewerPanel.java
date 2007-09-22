//===========================================================================

// Java GUI classes
import java.awt.Color;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.border.MatteBorder;

// Internal classes
import vsdk.transition.render.swing.SwingGuiCacheRenderer;

public class ViewerPanel extends JPanel implements MouseListener
{
    protected SciVisApplication parent;
    protected PanelManager container;
    static int lastId = 0;

    public void configureTestColor()
    {
        //-----------------------------------------------------------------
        setBorder(new MatteBorder(2, 2, 2, 2, Color.PINK));
        Color c = Color.BLACK;

        lastId++;

        switch ( lastId % 13 ) {
          case 1: c = Color.RED; break;
          case 2: c = Color.GREEN; break;
          case 3: c = Color.BLUE; break;
          case 4: c = Color.BLACK; break;
          case 5: c = Color.CYAN; break;
          case 6: c = Color.MAGENTA; break;
          case 7: c = Color.YELLOW; break;
          case 8: c = Color.ORANGE; break;
          case 9: c = Color.PINK; break;
          case 10: c = Color.DARK_GRAY; break;
          case 11: c = Color.GRAY; break;
          case 12: c = Color.LIGHT_GRAY; break;
          case 13: c = Color.WHITE; break;
        }
        setBackground(c);

        //-----------------------------------------------------------------
    }

    public ViewerPanel(SciVisApplication parent, PanelManager container)
    {
        this.parent = parent;
        this.container = container;

        addMouseListener(this);

        //-----------------------------------------------------------------
        JMenu menu = SwingGuiCacheRenderer.buildPopupMenu(parent.gui, "POPUP_PANEL_MANAGER_VIEW", container);
        JPopupMenu popup = menu.getPopupMenu();
        setComponentPopupMenu(popup);
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        container.setPointedViewerPanel(this);
        requestFocusInWindow();
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        container.setSelectedViewerPanel(this);
    }

    public void setSelected(boolean newState)
    {
        if ( newState ) {
            setBorder(new MatteBorder(2, 2, 2, 2, new Color(1.0f, 0.96f, 0.0f)));
        }
        else {
            setBorder(new MatteBorder(2, 2, 2, 2, new Color(0.21f, 0.25f, 0.29f)));
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
