/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

// Internal classes
import com.sun.java.swing.plaf.gtk.GTKConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.TextArea;
import java.io.FileInputStream;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.border.TitledBorder;
import vsdk.toolkit.gui.Gui;
import vsdk.toolkit.gui.GuiDialog;
import vsdk.toolkit.gui.variable.GuiVariable;
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

    public void createGUI() {
        languageGuiFile = "./etc/english.gui";

        mainWindowWidget = new JFrame("VITRAL GUI Example");
        mainWindowWidget.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            gui = GuiPersistence.importAquynzaGui(
                    new FileInputStream(languageGuiFile));
            System.out.println(gui);
        } catch (Exception e) {
            System.err.println("Fatal error: can not open GUI file");
            e.printStackTrace();
            System.exit(0);
        }
        JMenuBar menubar;
        menubar = SwingGuiRenderer.buildMenubar(gui, null, executor);

        Dimension d = new Dimension(1200, 600);

        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);


        /*
         * Dialog test
         */
        for (int i = 0; i < gui.getDialogList().size(); i++) {
            GuiDialog dial = gui.getDialogList().get(i);
            JPanel panel = SwingGuiRenderer.buildDialog(gui.getDialogList().get(i));

            for (int j = 0; j < dial.getChildren().size(); j++) {
                GuiDialog dialChild = (GuiDialog) dial.getChildren().get(j);
                JPanel panelChild = SwingGuiRenderer.buildDialog(dialChild);
  
                for (int k = 0; k < dialChild.getPendingVariableNames().size(); k++) {
                    JPanel q = SwingGuiRenderer.buildVariable(gui.getVariableByName(dialChild.getPendingVariableNames().get(k)));
                    panelChild.add(q);
                }
                panel.add(panelChild);
            }
            
            for (int k = 0; k < dial.getPendingVariableNames().size(); k++) {
                JPanel q = SwingGuiRenderer.buildVariable(gui.getVariableByName(dial.getPendingVariableNames().get(k)));
                panel.add(q);
            }
            p.add(panel);
        }

        TitledBorder tb = new TitledBorder("Test Sphere Modifier");
        p.setBorder(tb);
        p.setBackground(Color.DARK_GRAY);
        mainWindowWidget.getContentPane().add(p);

        //mainWindowWidget.add(panel,BorderLayout.SOUTH);

        mainWindowWidget.setJMenuBar(menubar);
        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
    }
}
