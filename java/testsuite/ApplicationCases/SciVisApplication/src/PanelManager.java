// Java basic classes
import java.util.ArrayList;

// Java GUI classes
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;

// Internal classes
import vsdk.transition.gui.GuiCache;
import vsdk.transition.render.swing.SwingGuiCacheRenderer;

public abstract class PanelManager extends JPanel implements ActionListener
{
    protected GuiCache parentGui;
    protected ArrayList<ViewerPanel> panels;

    protected int selectedViewerPanel;
    protected int pointedViewerPanel;

    public PanelManager(GuiCache parentGui)
    {
        this.parentGui = parentGui;
        setLayout(new BorderLayout());

        panels = new ArrayList<ViewerPanel>();
        selectedViewerPanel = -1;
        pointedViewerPanel = -1;
        reorganizePanels();
    }

    public void setPopup(String name) {
        JPopupMenu popup = null;
        if ( name != null ) {
            JMenu menu = SwingGuiCacheRenderer.buildPopupMenu(parentGui, name, this);
            popup = menu.getPopupMenu();
            popup.setLightWeightPopupEnabled(false);
        }
        setComponentPopupMenu(popup);
    }

    private void configureSplitDivider(JSplitPane splitPane) {
        splitPane.setUI(new BasicSplitPaneUI());
        splitPane.setBorder(null);

        SplitPaneUI splitUI = splitPane.getUI();
        if ( splitUI instanceof BasicSplitPaneUI ) {
            // obviously this will not work if the ui doen't extend Basic...
            int divSize = splitPane.getDividerSize();
            BasicSplitPaneDivider div = ((BasicSplitPaneUI) splitUI).getDivider();
            assert div != null;
            div.setBorder(null);
            div.setBackground(Color.BLACK);
        }
    }

    private Component layoutOneRow(ArrayList<ViewerPanel> partsList, int start, int offset, Dimension parentDimension, int level)
    {
        //- Trivial case --------------------------------------------------
        if ( offset == 1 ) {
            return partsList.get(start);
        }

        //- Recursive case ------------------------------------------------
        int dividerSize = 2;
        int n;
        n = offset;

        JSplitPane splitPane;
        Component left, right;
        int elementWidth = parentDimension.width / n;

        Dimension d;

        //-----------------------------------------------------------------
        d = new Dimension();
        d.height = parentDimension.height;
        if ( n == 4 && level == 1 ) {
            d.width = parentDimension.width/2;
        }
        else {
            d.width = 2*elementWidth - dividerSize/2;
        }
        left = layoutOneRow(partsList, start, (n+1)/2, d, level+1);
        //left.setPreferredSize(d);

        //-----------------------------------------------------------------
        d = new Dimension();
        d.height = parentDimension.height;
        if ( n == 4 && level == 1 ) {
            d.width = parentDimension.width/2;
        }
        else {
            d.width = parentDimension.width - elementWidth - dividerSize/2;
        }
        right = layoutOneRow(partsList, start+(n+1)/2, n/2, d, level+1);
        //right.setPreferredSize(d);

        //-----------------------------------------------------------------
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);

        if ( n == 4 && level == 1 ) {
            splitPane.setDividerLocation(parentDimension.width/2);
        }
        else {
            splitPane.setDividerLocation(((n+1)/2)*elementWidth);
        }

        splitPane.setDividerSize(dividerSize);
        configureSplitDivider(splitPane);
        return splitPane;
    }

    private void layoutInTwoRows(ArrayList<ViewerPanel> partsList)
    {
        int dividerSize = 2;
        Dimension d;
        d = getSize();
        d.height = (d.height-(dividerSize+2))/2;

        int n = partsList.size();

        //partsList;
        JSplitPane splitPane;
        Component top, bottom;

        if ( n < 2 || n > 8 ) {
            return;
        }

        top = layoutOneRow(partsList, 0, (n+1)/2, d, 1);
        bottom = layoutOneRow(partsList, (n+1)/2, n/2, d, 1);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        splitPane.setDividerSize(dividerSize);
        configureSplitDivider(splitPane);

        top.setPreferredSize(d);
        bottom.setPreferredSize(d);
        add(splitPane);
    }

    public void empty()
    {
        removeAll();
        while ( panels.size() > 0 ) {
            panels.set(0, null);
            panels.remove(0);
        }
    }

    protected void reorganizePanels()
    {
        removeAll();

        if ( panels.size() <= 0 ) {
            add(new JLabel("Empty working area"));
        }
        else if ( panels.size() == 1 ) {
            // New panel is the only child to parent panel
            add(panels.get(0), BorderLayout.CENTER);
        }
        else if ( panels.size() >= 1 && panels.size() < 8 ) {
            // New panel will lie in a 2 rows topology
            layoutInTwoRows(panels);
        }
        if ( panels.size() > 0 ) {
            setSelectedViewerPanel(panels.get(0));
        }
        //parent.mainWindowWidget.pack();

        //print(this, 0);
    }

    public void setSelectedViewerPanel(ViewerPanel target)
    {
        int i;
        ViewerPanel v;

        selectedViewerPanel = -1;
        for ( i = 0; i < panels.size(); i++ ) {
            v = panels.get(i);
            if ( v == target ) { 
                v.setSelected(true);
                selectedViewerPanel = i;
            }
            else {
                v.setSelected(false);
            }
        }
    }

    public void setPointedViewerPanel(ViewerPanel target)
    {
        int i;
        ViewerPanel v;

        pointedViewerPanel = -1;
        for ( i = 0; i < panels.size(); i++ ) {
            v = panels.get(i);
            if ( v == target ) { 
                pointedViewerPanel = i;
            }
        }
    }

    private void print(JComponent parent, int level)
    {
        int i, l;

        if ( level == 0 ) {
            System.out.println("===========================================================================");
        }

        Component[] arr;
        arr = parent.getComponents();

        for ( l = 0; l < level; l++ ) {
            System.out.print("  ");
        }
        System.out.println("Window with " + arr.length + " children");
        for ( i = 0; i < arr.length; i++ ) {
            for ( l = 0; l < level; l++ ) {
                System.out.print("  ");
            }
            System.out.println("  - " + arr[i]);
            if ( arr[i] instanceof JSplitPane ) {
                print((JComponent)arr[i], level+1);
            }
        }

        if ( level == 0 ) {
            System.out.println("---------------------------------------------------------------------------");
        }

    }
}
