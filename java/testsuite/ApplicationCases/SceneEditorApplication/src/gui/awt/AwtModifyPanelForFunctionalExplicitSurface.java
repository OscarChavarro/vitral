package gui.awt;

// Java basic classes
import java.util.EnumMap;
import java.util.Map;

// Java GUI classes
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

// VSDK classes
import vsdk.toolkit.environment.scene.SimpleBody;

// Application classes
import model.editor.FunctionalExplicitSurfaceEditor;
import model.editor.FunctionalExplicitSurfaceEditor.Parameter;

/**
Swing presentation of a `FunctionalExplicitSurfaceEditor`: a list of
predefined configurations and one text field per parameter. What the user
types is passed to the editor, which changes the body.
*/
public class AwtModifyPanelForFunctionalExplicitSurface extends AwtModifyPanel implements ActionListener
{
    private final Map<Parameter, JTextField> fields;
    private FunctionalExplicitSurfaceEditor editor;
    private boolean firstTimer;

    public AwtModifyPanelForFunctionalExplicitSurface(AwtApplicationHost parent)
    {
        super(parent);
        fields = new EnumMap<>(Parameter.class);
        editor = null;
        firstTimer = true;
    }

    public void notifyTargetBeginEdit(SimpleBody target, JPanel parentPanel)
    {
        //-----------------------------------------------------------------
        JPanel container1 = new JPanel();
        JPanel container2 = new JPanel();
        JPanel container3;

        JLabel label;

        Border empty = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        container1.setBorder(empty);
        container2.setBorder(empty);


        container2.setLayout(new GridLayout(21, 1));

        container1.add(container2);

        parentPanel.add(container1);

        this.target = target;
        editor = new FunctionalExplicitSurfaceEditor(target);
        fields.clear();
        removeAll();

        //-----------------------------------------------------------------
        label = new JLabel(FunctionalExplicitSurfaceEditor.TITLE, SwingConstants.CENTER);
        container2.add(label);

        //-----------------------------------------------------------------
        label = new JLabel(FunctionalExplicitSurfaceEditor.PRESETS_LABEL, SwingConstants.LEFT);
        container2.add(label);

        JComboBox<String> jcb = new JComboBox<String>();

        // This is strange, but makes combobox heavyweight and integrable with JOGL!
        jcb.getEditor().getEditorComponent().setBackground(Color.WHITE);

        jcb.setLightWeightPopupEnabled(false);
        jcb.addActionListener(this);
        for ( String preset : FunctionalExplicitSurfaceEditor.getPresets() ) {
            jcb.addItem(preset);
        }
        container2.add(jcb);

        //-----------------------------------------------------------------
        for ( Parameter parameter : Parameter.values() ) {
            JTextField field = new JTextField(editor.getValue(parameter));
            field.addActionListener(this);
            fields.put(parameter, field);

            if ( parameter == Parameter.FUNCTION ) {
                // The function is long: its label goes above it
                container2.add(new JLabel(parameter.getLabel(), SwingConstants.LEFT));
                container2.add(field);
                continue;
            }
            container3 = new JPanel();
            container3.setLayout(new BoxLayout(container3, BoxLayout.X_AXIS));
            container3.add(new JLabel(parameter.getLabel(), SwingConstants.RIGHT));
            container3.add(field);
            container2.add(container3);
        }
    }

    private void refreshFields()
    {
        for ( Map.Entry<Parameter, JTextField> entry : fields.entrySet() ) {
            entry.getValue().setText(editor.getValue(entry.getKey()));
        }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        boolean updated = false;

        if ( editor == null ) {
            return;
        }
        if ( ev.getSource() instanceof JComboBox ) {
            JComboBox<?> origin = (JComboBox<?>)ev.getSource();
            if ( firstTimer ) {
                // Adding the first item selects it: it is not a user choice
                firstTimer = false;
            }
            else if ( editor.applyPreset((String)origin.getSelectedItem()) ) {
                refreshFields();
                updated = true;
            }
        }
        else if ( ev.getSource() instanceof JTextField ) {
            for ( Map.Entry<Parameter, JTextField> entry : fields.entrySet() ) {
                if ( entry.getValue() == ev.getSource() ) {
                    editor.setValue(entry.getKey(), ev.getActionCommand());
                    updated = true;
                    break;
                }
            }
        }
        //-----------------------------------------------------------------
        if ( updated ) {
            parent.repaintDrawingArea();
        }
    }

}
