//===========================================================================

package vsdk.toolkit.render.swing;

// GUI JDK classes (Awt + Swing)
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SwingEventListener implements ActionListener
{
    private String commandName;
    private ActionListener executor;

    SwingEventListener (String c, ActionListener e)
    {
        commandName = c;
        executor = e;
    }

    public void actionPerformed(ActionEvent e)
    {
        ActionEvent e2;

        e2 = new ActionEvent(this, 1, commandName);
        executor.actionPerformed(e2);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
