package gui.awt;

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
import vsdk.toolkit.render.swing.SwingGuiRenderer;

// Application classes

public class AwtButtonsPanel extends JPanel implements ActionListener
{
    private AwtApplicationHost parent;
    
    private AwtCommandExecutor guiEventExecutor;

    public AwtButtonsPanel(AwtApplicationHost parent, int group, AwtCommandExecutor guiEventExecutor)
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

        //-------------------------------------------------------------------
        JPanel internal = null;

        switch ( group ) {
          case 1:
            internal = 
            SwingGuiRenderer.buildButtonGroup(parent.getApplicationModel().getI18nContext(), "CREATION", this);
            break;
          case 2:
            internal = 
            SwingGuiRenderer.buildButtonGroup(parent.getApplicationModel().getI18nContext(), "GUI", this);
            break;
          case 3:
            internal = 
            SwingGuiRenderer.buildButtonGroup(parent.getApplicationModel().getI18nContext(), "OTHER", this);
            break;
          case 4:
            internal = 
            SwingGuiRenderer.buildButtonGroup(parent.getApplicationModel().getI18nContext(), "RENDER", this);
            break;
          case 101:
            internal = 
            SwingGuiRenderer.buildButtonGroup(parent.getApplicationModel().getI18nContext(), "GLOBAL", this);
            break;
        }

        if ( internal != null ) {
            this.add(internal, BorderLayout.WEST);
        }

        //-------------------------------------------------------------------
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
        guiEventExecutor.executeCommand(label, parent.getAwtModel().getMainWindowWidget());
    }

    

}
