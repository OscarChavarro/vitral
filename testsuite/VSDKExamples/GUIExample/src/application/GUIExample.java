/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

// Internal classes
import com.sun.java.swing.plaf.gtk.GTKConstants;
import java.awt.*;
import java.io.FileInputStream;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import vsdk.toolkit.gui.Gui;
import vsdk.toolkit.gui.GuiDialog;
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
            //System.out.println(gui);
        } catch (Exception e) {
            System.err.println("Fatal error: can not open GUI file");
            e.printStackTrace();
            System.exit(0);
        }
        JMenuBar menubar;
        menubar = SwingGuiRenderer.buildMenubar(gui, null, executor);

        Dimension d = new Dimension(800, 800);
        JPanel workspace = new JPanel();
        //workspace.setPreferredSize(new Dimension(400, mainWindowWidget.getHeight()));
        
        
        
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(gui.getDialogList().size(), 1));
        //p.setPreferredSize(new Dimension(300, 800));
        //p.setBackground(Color.WHITE);

        TitledBorder tb = new TitledBorder("Applications");
        TitledBorder tb2 = new TitledBorder("Workspace");
        p.setBorder(tb);
        //p.setBackground(Color.WHITE);
        workspace.setBorder(tb2);
        
        /*
         * Dialog english.gui file construction
         */
        for (int i = 0; i < gui.getDialogList().size(); i++) {
            p.add(buildDialogGui(gui.getDialogList().get(i)));
        }
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().add(p);
        mainWindowWidget.add(workspace,BorderLayout.CENTER);
        mainWindowWidget.add(scrollPane,BorderLayout.EAST);
        mainWindowWidget.setJMenuBar(menubar);
        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
    }

    public JPanel buildDialogGui(GuiDialog d) {
        JPanel pan = SwingGuiRenderer.buildDialog(d, gui, executor);
        if (d.getChildren().isEmpty() || d.getChildren() == null) {
            return pan;
        } else {
            for (int j = 0; j < d.getChildren().size(); j++) {
                JPanel panel = null;
                GuiDialog dial = (GuiDialog) d.getChildren().get(j);
                panel = buildDialogGui(dial);
                pan.add(panel);
            }
        }
        return pan;
    }
}
