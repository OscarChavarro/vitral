/*
 * ControlableScene.java
 *
 * Created on 13 de septiembre de 2005, 12:30 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package vitral.toolkits.visual.jogl;

import javax.swing.*;
import java.util.*;

public interface SceneController 
{
    public JPanel getControlPaneNorth();
    public JPanel getControlPaneSouth();
    public JPanel getControlPaneWest();
    public JPanel getControlPaneEast();
    public ArrayList<JMenuItem> getFileMenus();
    public ArrayList<JMenuItem> getHelpMenus();
    public ArrayList<JMenu> getMenus();
}
    
