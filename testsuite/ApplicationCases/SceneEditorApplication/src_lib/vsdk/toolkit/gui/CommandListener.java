/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vsdk.toolkit.gui;

/**
 *This class represents the concept of event language as explained in section
 * [FOLE1992.10.6] and figure [FOLE1992.10.24].
 * 
 * @author TaakeSlottet
 */
public interface CommandListener {
    public boolean executeCommand(String commandId);
}
