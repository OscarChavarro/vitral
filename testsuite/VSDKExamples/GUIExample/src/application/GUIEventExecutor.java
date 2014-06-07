
package application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import vsdk.toolkit.gui.CommandListener;

public class GUIEventExecutor extends CommandListener implements ActionListener {

    @Override
    public boolean executeCommand(String label) {
        System.out.println("Ejecuto el comando "+label);
        return true;
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
        executeCommand(ae.getActionCommand());
    }
    
}
