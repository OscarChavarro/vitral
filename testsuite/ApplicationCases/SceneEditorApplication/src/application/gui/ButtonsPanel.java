//===========================================================================

package application.gui;

// Java basic classes
import java.io.File;

// Java GUI classes
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

// VSDK Classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.render.swing.SwingGuiRenderer;

// Application classes
import application.SceneEditorApplication;
import javax.swing.*;

public class ButtonsPanel extends JPanel implements ActionListener
{
    private SceneEditorApplication parent;
    private String currentFilePathForReading;
    private String currentFilePathForWriting;
    
    private GUIEventExecutor guiEventExecutor;

    public ButtonsPanel(SceneEditorApplication parent, int group, GUIEventExecutor guiEventExecutor)
    {
        this.guiEventExecutor = guiEventExecutor;
        //-------------------------------------------------------------------
        this.parent = parent;
        if ( group < 100 ) {
            // This is a button group inside right tab panels
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            Border empty = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            this.setBorder(empty);
        }
        else {
            // This is a button group part of an icon bar
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            Border empty = BorderFactory.createEmptyBorder(0, 0, 0, 0);
            this.setBorder(empty);
        }

        currentFilePathForReading = (new File("")).getAbsolutePath() + "/../../../etc/geometry";
        currentFilePathForWriting = ".";

        //-------------------------------------------------------------------
        JPanel internal = null;

        switch ( group ) {
          case 1:
            internal = 
            SwingGuiRenderer.buildButtonGroup(parent.gui, "CREATION", this);
            break;
          case 2:
            internal = 
            SwingGuiRenderer.buildButtonGroup(parent.gui, "GUI", this);
            break;
          case 3:
            internal = 
            SwingGuiRenderer.buildButtonGroup(parent.gui, "OTHER", this);
            break;
          case 4:
            internal = 
            SwingGuiRenderer.buildButtonGroup(parent.gui, "RENDER", this);
            break;
          case 101:
            internal = 
            SwingGuiRenderer.buildButtonGroup(parent.gui, "GLOBAL", this);
            break;
        }

        if ( internal != null ) {
            this.add(internal, BorderLayout.WEST);
        }

        //-------------------------------------------------------------------
    }

    

    private static PolyhedralBoundedSolid createCircle(
        double cx, double cy, double rad, double h, int n)
    {
        PolyhedralBoundedSolid solid;

        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(cx + rad, cy, h), 1, 1);
        addArc(solid, 1, 1, cx, cy, rad, h, 0, 
            ((double)(n-1))*360.0/((double)n), n-1);
        solid.smef(1, n, 1, 2);
        solid.validateModel();
        return solid;
    }

    private static void addArc(PolyhedralBoundedSolid solid,
        int faceId, int vertexId,
        double cx, double cy, double rad, double h, double phi1, double phi2,
        int n)
    {
        double x, y, angle, inc;
        int prev, i, nextVertexId;

        angle = Math.toRadians(phi1);
        inc = Math.toRadians(((phi2 - phi1) / ((double)n)));
        prev = vertexId;
        for ( i = 0; i < n; i++ ) {
            angle += inc;
            x = cx + rad * Math.cos(angle);
            y = cy + rad * Math.sin(angle);
            nextVertexId = solid.getMaxVertexId() + 1;
            solid.smev(faceId, prev, nextVertexId, new Vector3D(x, y, h));
            prev = nextVertexId;
        }
        solid.validateModel();
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();

        // This makes event compatible with ButtonGroup scheme of event
        // handling
        if ( ev.getSource() instanceof JButton ) {
            JButton origin = (JButton)ev.getSource();
            label = origin.getName();
        }
        guiEventExecutor.executeCommand(label, currentFilePathForReading, 
                currentFilePathForWriting, parent.mainWindowWidget);
    }

    

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
