package application;

// Java basic classes
import java.io.File;
import java.awt.BorderLayout;
import java.awt.Color;

// Java AWT/Swing classes
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.gui.CommandListener;

// Application classes
import gui.awt.AwtCommandExecutor;
import gui.awt.AwtImageControlWindow;
import gui.awt.AwtSuffixFileFilter;
import model.GuiState;
import application.commands.CommandResult;
import application.commands.ExportFormat;
import application.commands.GuiEventExecutor;

/**
Executes the commands of the GUI of the editor that need Swing (file
dialogs, the image window, look and feel and language changes, that rebuild
the Swing GUI) and repaints the drawing area after each command. The rest of
the commands are executed by the technology independent `GuiEventExecutor`.
*/
public class AwtJogl4GuiEventExecutor extends CommandListener
    implements AwtCommandExecutor {

    private AwtJogl4SceneEditorApplication parent;
    private final GuiEventExecutor commands;

    public AwtJogl4GuiEventExecutor(AwtJogl4SceneEditorApplication parent) {
        this.parent = parent;
        this.commands = new GuiEventExecutor(parent.getApplicationModel(),
            this::showStatusMessage);
    }

    private GuiState guiState()
    {
        return parent.getApplicationModel().getGuiState();
    }

    private void showStatusMessage(String message)
    {
        parent.getAwtModel().getStatusMessage().setText(message);
    }

    @Override
    public boolean executeCommand(String label, JFrame mainWindowWidget) {

        CommandResult result = commands.execute(label);

        if ( result == CommandResult.FAILED ) {
            return false;
        }
        if ( result == CommandResult.NOT_HANDLED &&
             !executeSwingCommand(label, mainWindowWidget) ) {
            return false;
        }

        //-----------------------------------------------------------------
        parent.getJogl4Controller().repaint();
        return true;
    }

    /**
    Executes the commands that need Swing.
    @return false if the command failed
    */
    private boolean executeSwingCommand(String label, JFrame mainWindowWidget)
    {
        //- FILE ----------------------------------------------------------
        if ( label.equals("IDC_TOOLS_TANGIBLE_INTERFACE_CLIENT") ) {
            showTangibleInterfaceDialog(mainWindowWidget);
        }
        else if ( label.equals("IDC_FILE_QUIT") ) {
            System.exit(0);
        }
        else if ( label.equals("IDC_IMPORT_OBJECTS_FROM_FILE") ) {
            JFileChooser jfc;
            jfc = new JFileChooser(guiState().getReadFolder());
            jfc.removeChoosableFileFilter(jfc.getFileFilter());
            jfc.addChoosableFileFilter(new AwtSuffixFileFilter("3ds", "3ds Kinetix/Discreet 3DStudio/3DStudioMax binary scene file"));
            jfc.addChoosableFileFilter(new AwtSuffixFileFilter("vtk", "vtk Kitware vtk legacy binary file (mesh only)"));
            jfc.addChoosableFileFilter(new AwtSuffixFileFilter("gts", "gts Gts mesh ASCII file"));
            jfc.addChoosableFileFilter(new AwtSuffixFileFilter("obj", "obj Alias/Wavefront text mesh"));
            jfc.addChoosableFileFilter(new AwtSuffixFileFilter("ply", "ply Ply mesh"));
            jfc.addChoosableFileFilter(new AwtSuffixFileFilter("ray", "ray MIT raytracing scene"));

            // The chooser must belong to the editor window.  An invisible
            // JPanel parent can make the native macOS dialog appear behind
            // the heavyweight OpenGL window, which makes File > Import look
            // as if it did nothing.
            int opc = jfc.showOpenDialog(mainWindowWidget);
            if (opc == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = jfc.getSelectedFile();

                    commands.importObjects(file);

                    guiState().setReadFolder(file.getParentFile().getAbsolutePath());

                    parent.getAwtModel().getMainWindowWidget().repaint();
                }
                catch ( Exception ex ) {
                    Logger.reportMessage(this, VSDK.WARNING, "executeCommand", "Failed to read file...\n" + ex);
                    return false;
                }
            }

        }
        else if ( label.equals("IDC_EXPORT_OBJECTS_TO_OBJ") ) {
            JFileChooser jfc;
            jfc = new JFileChooser(guiState().getWriteFolder());
            jfc.removeChoosableFileFilter(jfc.getFileFilter());

            int opc = jfc.showOpenDialog(new JPanel());
            if ( opc == JFileChooser.APPROVE_OPTION ) {
                try {
                    File file = jfc.getSelectedFile();
                    commands.exportObjects(file, ExportFormat.OBJ);

                    guiState().setWriteFolder(file.getParentFile().getAbsolutePath());

                    mainWindowWidget.repaint();
                }
                catch (Exception ex) {
                    System.out.println("Failed to write file...\n" + ex);
                    return false;
                }
            }

        }
        else if ( label.equals("IDC_EXPORT_OBJECTS_TO_GTS") ) {
            JFileChooser jfc;
            jfc = new JFileChooser(guiState().getWriteFolder());
            jfc.removeChoosableFileFilter(jfc.getFileFilter());

            int opc = jfc.showOpenDialog(new JPanel());
            if ( opc == JFileChooser.APPROVE_OPTION ) {
                try {
                    File file = jfc.getSelectedFile();
                    commands.exportObjects(file, ExportFormat.GTS);

                    guiState().setWriteFolder(file.getParentFile().getAbsolutePath());

                    parent.getAwtModel().getMainWindowWidget().repaint();
                }
                catch (Exception ex) {
                    Logger.reportMessage(this, VSDK.WARNING, "execute", "Failed to write file...\n" + ex);
                    return false;
                }
            }
        }
        else if ( label.equals("IDC_EXPORT_OBJECTS_TO_VTK") ) {
            JFileChooser jfc;
            jfc = new JFileChooser(guiState().getWriteFolder());
            jfc.removeChoosableFileFilter(jfc.getFileFilter());

            int opc = jfc.showOpenDialog(new JPanel());
            if ( opc == JFileChooser.APPROVE_OPTION ) {
                try {
                    File file = jfc.getSelectedFile();
                    commands.exportObjects(file, ExportFormat.VTK);

                    guiState().setWriteFolder(file.getParentFile().getAbsolutePath());

                    parent.getAwtModel().getMainWindowWidget().repaint();
                }
                catch (Exception ex) {
                    Logger.reportMessage(this, VSDK.WARNING, "execute", "Failed to write file...\n" + ex);
                    return false;
                }
            }
        }
        //- RENDERING -----------------------------------------------------
        else if ( label.equals("Select palette for depthmap display") ||
                  label.equals("IDC_RENDERING_SELECTPALETTEDEPTH") ) {
            JFileChooser jfc;
            jfc = new JFileChooser( (new File("")).getAbsolutePath() + "/../../../../etc/palettes");
            jfc.removeChoosableFileFilter(jfc.getFileFilter());
            jfc.addChoosableFileFilter(new AwtSuffixFileFilter("gpl", "gpl Gimp Palettes"));
            int opc = jfc.showOpenDialog(new JPanel());
            if (opc == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = jfc.getSelectedFile();
                    commands.loadPalette(file);
                    parent.getAwtModel().getMainWindowWidget().repaint();
                }
                catch (Exception ex) {
                    System.out.println("Failed to read file");
                    return false;
                }
            }
        }
        else if ( label.equals("IDC_RENDERING_RAYTRACING") ) {
            showStatusMessage(
                parent.getApplicationModel().getI18nContext().getMessage("IDM_COMPUTING_RAYTRACING"));
            parent.doRaytracingImage();
            if ( parent.getAwtModel().getImageControlWindow() == null ) {
                parent.getAwtModel().setImageControlWindow(new AwtImageControlWindow(
                    parent.getApplicationModel().getRaytracedImage(),
                    parent.getApplicationModel().getI18nContext(),
                    parent.getAwtModel().getExecutorPanel()));
            }
            else {
                parent.getAwtModel().getImageControlWindow().setImage(
                    parent.getApplicationModel().getRaytracedImage());
            }
            parent.getAwtModel().getImageControlWindow().redrawImage();
        }
        //- CUSTOMIZE -----------------------------------------------------
        else if ( label.equals("IDC_CUSTOMIZE_LAF_MOTIF") ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LAF_JAVA") ) {
            parent.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LAF_GTK") ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LAF_WINDOWS") ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LANGUAGE_ENGLISH") ) {
            parent.setGuiLanguage(GuiState.languageFile("english"));
        }
        else if ( label.equals("IDC_CUSTOMIZE_LANGUAGE_SPANISH") ) {
            parent.setGuiLanguage(GuiState.languageFile("spanish"));
        }
        return true;
    }

    private void showTangibleInterfaceDialog(JFrame mainWindowWidget)
    {
        JTextField serverField = new JTextField(parent.getTangibleServiceUrl(), 36);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel(parent.getApplicationModel().getI18nContext().getMessage(
            "IDM_TANGIBLE_INTERFACE_SERVER_PROMPT")), BorderLayout.NORTH);
        panel.add(serverField, BorderLayout.CENTER);

        String connect = parent.getApplicationModel().getI18nContext().getMessage(
            "IDM_TANGIBLE_INTERFACE_CONNECT");
        String cancel = parent.getApplicationModel().getI18nContext().getMessage(
            "IDM_TANGIBLE_INTERFACE_CANCEL");
        JButton connectButton = new JButton(connect);
        JButton cancelButton = new JButton(cancel);
        JPanel buttons = new JPanel();
        buttons.add(connectButton);
        buttons.add(cancelButton);

        JDialog dialog = new JDialog(mainWindowWidget,
            parent.getApplicationModel().getI18nContext().getMessage(
                "IDM_TANGIBLE_INTERFACE_TITLE"), true);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(mainWindowWidget);

        cancelButton.addActionListener(event -> dialog.dispose());
        connectButton.addActionListener(event -> {
            String serviceUrl = serverField.getText().trim();
            if ( serviceUrl.isEmpty() ) {
                return;
            }
            connectButton.setBackground(null);
            connectButton.setText(connect);
            parent.connectTangibleInterface(serviceUrl, connected ->
                SwingUtilities.invokeLater(() -> {
                    if ( connected ) {
                        dialog.dispose();
                    }
                    else {
                        connectButton.setBackground(Color.RED);
                        connectButton.setText(parent.getApplicationModel().getI18nContext()
                            .getMessage("IDM_TANGIBLE_INTERFACE_COULD_NOT_CONNECT_RETRY"));
                    }
                }));
        });
        dialog.setVisible(true);
    }

    @Override
    public boolean executeCommand(String commandId) {
        return commands.executeCommand(commandId);
    }
}
