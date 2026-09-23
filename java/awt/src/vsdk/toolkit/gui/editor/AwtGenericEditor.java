package vsdk.toolkit.gui.editor;

// Java basic classes
import java.util.HashMap;
import java.util.Map;

// Java GUI classes
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
Awt/Swing presentation of a `GenericEditor`: a title, one labeled text field
per control specification and a message line. All the logic (reading,
validating and writing values) lives in `GenericEditor`; this class only
creates widgets and forwards what the user types.
*/
public class AwtGenericEditor extends GenericEditor implements ActionListener
{
    private static final Color INVALID_BACKGROUND = new Color(255, 200, 200);

    private final JPanel container;
    private final Map<JTextField, ControlSpecification> fields;
    private JPanel rows;
    private JLabel messageLabel;

    /**
    @param container Swing panel that will hold the editor; its previous
    contents are removed on each `build`
    */
    public AwtGenericEditor(JPanel container)
    {
        this.container = container;
        fields = new HashMap<JTextField, ControlSpecification>();
        rows = null;
        messageLabel = null;
    }

    @Override
    protected void beginBuild(String title)
    {
        container.removeAll();
        fields.clear();

        JPanel wrapper = new JPanel();
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        rows = new JPanel();
        rows.setLayout(new GridLayout(0, 1));
        wrapper.add(rows);
        container.add(wrapper);

        rows.add(new JLabel(title.toUpperCase() + " EDITOR",
            SwingConstants.CENTER));
        messageLabel = new JLabel(" ", SwingConstants.LEFT);
        messageLabel.setForeground(Color.RED);
    }

    @Override
    protected void addControl(ControlSpecification specification, String value)
    {
        String label = specification.getLabel();
        if ( !specification.getIntervalText().isEmpty() ) {
            label += " " + specification.getIntervalText();
        }

        JTextField field = new JTextField(value, 10);
        field.addActionListener(this);
        fields.put(field, specification);

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(new JLabel(label + ": ", SwingConstants.RIGHT));
        row.add(field);
        rows.add(row);
    }

    @Override
    protected void endBuild()
    {
        rows.add(messageLabel);
        container.revalidate();
        container.repaint();
    }

    @Override
    protected void showValidationMessage(String message)
    {
        if ( messageLabel != null ) {
            messageLabel.setText(message == null ? " " : message);
        }
    }

    @Override
    protected void setControlValue(ControlSpecification specification,
        String value)
    {
        for ( Map.Entry<JTextField, ControlSpecification> entry :
                  fields.entrySet() ) {
            if ( entry.getValue() == specification ) {
                entry.getKey().setText(value);
                entry.getKey().setBackground(Color.WHITE);
            }
        }
    }

    @Override
    protected void clearControls(String message)
    {
        container.removeAll();
        fields.clear();
        rows = null;
        messageLabel = null;
        container.add(new JLabel(message, SwingConstants.CENTER));
        container.revalidate();
        container.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        if ( !(ev.getSource() instanceof JTextField) ) {
            return;
        }
        JTextField field = (JTextField)ev.getSource();
        ControlSpecification specification = fields.get(field);
        if ( specification == null ) {
            return;
        }

        // On success the entity emits UPDATED and `setControlValue`
        // refreshes the field
        if ( !updateValue(specification, field.getText()) ) {
            field.setBackground(INVALID_BACKGROUND);
        }
    }

}
