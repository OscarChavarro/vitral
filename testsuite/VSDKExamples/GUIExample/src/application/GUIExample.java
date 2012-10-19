/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

// Internal classes
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.FileInputStream;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
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
        mainWindowWidget.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       
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

        JPanel p = new JPanel();
        p.setBackground(Color.red);
        mainWindowWidget.getContentPane().add(p,BorderLayout.CENTER);
        
        JPanel q = SwingGuiRenderer.buildVariable(gui.getVariableByName("color"));
        p.add(q);
        
        mainWindowWidget.setJMenuBar(menubar);
        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
    }
}
