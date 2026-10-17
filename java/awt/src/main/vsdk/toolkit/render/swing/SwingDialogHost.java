package vsdk.toolkit.render.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import vsdk.toolkit.gui.widget.Widget;
import vsdk.toolkit.gui.widget.WidgetDialog;
import vsdk.toolkit.gui.dialog.runtime.DialogSession;
import vsdk.toolkit.gui.dialog.components.DialogFileFilter;

/** Swing presentation of the reusable dialog elements and native file service. */
public final class SwingDialogHost {
    private SwingDialogHost() { }

    public static final class Form {
        private final JDialog dialog;
        private final Map<String, JTextField> fields = new HashMap<String, JTextField>();
        private final Map<String, JButton> actions = new HashMap<String, JButton>();
        private final DialogSession session;
        private Form(JDialog dialog, DialogSession session) {
            this.dialog = dialog; this.session = session;
        }
        public JDialog dialog() { return dialog; }
        public JTextField field(String binding) { return fields.get(binding); }
        public JButton action(String id) { return actions.get(id); }
        public DialogSession session() { return session; }
    }

    public static Form form(JFrame owner, Widget messages, WidgetDialog definition,
                            Consumer<String> onAction) {
        DialogSession session = new DialogSession(definition);
        JDialog dialog = new JDialog(owner, messages.getMessage(definition.getTitleKey()),
                                     definition.isModal());
        dialog.setName(definition.getId());
        Form form = new Form(dialog, session);
        dialog.setLayout(new BorderLayout(8, 8));
        JPanel content = new JPanel(new BorderLayout(8, 8));
        JPanel buttons = new JPanel(new FlowLayout());
        for (WidgetDialog.Element element : definition.getElements())
            addElement(element, content, buttons, messages, form, onAction);
        dialog.add(content, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent event) { session.close(); }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        return form;
    }

    private static void addElement(WidgetDialog.Element element, JPanel content,
                                   JPanel buttons, Widget messages, Form form,
                                   Consumer<String> onAction) {
        switch (element.getKind()) {
            case "textInput" -> {
                JPanel fieldPanel = new JPanel(new BorderLayout(8, 8));
                fieldPanel.add(new JLabel(messages.getMessage(element.getLabelKey())), BorderLayout.NORTH);
                JTextField field = new JTextField(36);
                field.setName(element.getBinding());
                form.fields.put(element.getBinding(), field);
                fieldPanel.add(field, BorderLayout.CENTER);
                content.add(fieldPanel, BorderLayout.CENTER);
            }
            case "button" -> {
                JButton button = new JButton(messages.getMessage(element.getLabelKey()));
                button.setName(element.getAction());
                form.actions.put(element.getAction(), button);
                button.addActionListener(event -> onAction.accept(element.getAction()));
                buttons.add(button);
            }
            case "row", "column" -> {
                for (WidgetDialog.Element child : element.getChildren())
                    addElement(child, content, buttons, messages, form, onAction);
            }
            default -> throw new IllegalArgumentException("Unsupported form element: " + element.getKind());
        }
    }

    public static JFileChooser fileChooser(WidgetDialog definition, String folder) {
        JFileChooser chooser = new JFileChooser(folder);
        chooser.setName(definition.getId());
        chooser.removeChoosableFileFilter(chooser.getFileFilter());
        for (WidgetDialog.Element element : definition.getElements()) {
            if (!"fileChooser".equals(element.getKind())) continue;
            for (WidgetDialog.Element filter : element.getChildren()) {
                if (!"fileFilter".equals(filter.getKind())) continue;
                DialogFileFilter policy = new DialogFileFilter(filter.getBinding(), filter.getLabelKey());
                chooser.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
                    @Override public boolean accept(java.io.File file) {
                        return policy.accepts(file.getPath(), file.isDirectory());
                    }
                    @Override public String getDescription() { return policy.description(); }
                });
            }
        }
        return chooser;
    }
}
