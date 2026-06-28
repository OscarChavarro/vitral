
package application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import vsdk.toolkit.gui.CommandListener;

public class WidgetEventExecutor extends CommandListener implements ActionListener {

    @Override
    public boolean executeCommand(String label) {
        System.out.println("Executing command " + label);
	if (label.equals("IDC_FILE_QUIT")) {
	    System.exit(0);
	}
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        executeCommand(ae.getActionCommand());
    }

}
