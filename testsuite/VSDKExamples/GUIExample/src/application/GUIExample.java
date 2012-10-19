/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

// Internal classes
import java.awt.Dimension;
import java.io.FileInputStream;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import vsdk.toolkit.gui.Gui;
import vsdk.toolkit.io.gui.GuiPersistence;
import vsdk.toolkit.render.swing.SwingGuiRenderer;

public class GUIExample {

    // Application GUI
    public Gui gui;
    private String languageGuiFile;
    private JFrame mainWindowWidget;
    private GUIEventExecutor executor;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        GUIExample instance = new GUIExample();
        instance.createGUI();
    }

    public GUIExample() {
        executor = new GUIEventExecutor();
    }    
    
    public void createGUI(){
        languageGuiFile = "./etc/english.gui";
        
        mainWindowWidget = new JFrame("VITRAL GUI Example");
       
        try {
            gui = GuiPersistence.importAquynzaGui(
                                new FileInputStream(languageGuiFile));
            System.out.println(gui);
        }
        catch ( Exception e ) {
            System.err.println("Fatal error: can not open GUI file");
            System.exit(0);
        }
        JMenuBar menubar;
        menubar = SwingGuiRenderer.buildMenubar(gui, null, executor);
        
        Dimension d = new Dimension(800, 600);

        mainWindowWidget.setJMenuBar(menubar);
        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
    }
}
