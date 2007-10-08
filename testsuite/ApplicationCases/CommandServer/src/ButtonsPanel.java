//===========================================================================

// Java GUI classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

public class ButtonsPanel extends JPanel implements ActionListener, DocumentListener
{
    private CommandServer parent;
    public ButtonsPanel(CommandServer parent)
    {
        this.parent = parent;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Border empty = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        this.setBorder(empty);

        //-----------------------------------------------------------------
        JScrollPane sp;
        JPanel frame = new JPanel();
        sp = new JScrollPane(frame);

        empty = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        frame.setBorder(empty);
        frame.setLayout(new BoxLayout(frame, BoxLayout.Y_AXIS));

        for ( int i = 0; i < parent.commandSet.size(); i++ ) {
            String msg = parent.commandSet.get(i).getName();
            JButton b = new JButton(msg);
            b.addActionListener(this);
            b.setName(msg);

            Dimension d = b.getMaximumSize();

            d.width = Short.MAX_VALUE;
            b.setAlignmentX(0.5f);
            b.setMaximumSize(d);

            frame.add(b);
        }

        //-----------------------------------------------------------------

        this.add(sp, BorderLayout.WEST);
    }

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();

        // This makes event compatible with ButtonGroup scheme of event
        // handling
        if ( ev.getSource() instanceof JButton ) {
            JButton origin = (JButton)ev.getSource();
            label = origin.getName();
            parent.currentCommand = label;
        }
        else if ( ev.getSource() instanceof JTextField ) {
            // Erase current text field's line of text
            parent.notifySpeech();
        }
    }

    private void updateModel(DocumentEvent e)
    {
        parent.updateTime();
/*
        //-----------------------------------------------------------------
        if ( e.getType() == DocumentEvent.EventType.CHANGE ) {
            System.out.print("D ");
        }
        else if ( e.getType() == DocumentEvent.EventType.INSERT ) {
            System.out.print("+ ");
        }
        else if ( e.getType() == DocumentEvent.EventType.REMOVE ) {
            System.out.print("- ");
        }
        else {
            System.out.println("? ");
        }
        System.out.println(" : offset " + e.getOffset() + ", length " + e.getLength());

        //-----------------------------------------------------------------
        Document d = e.getDocument();
        try {
            System.out.println("  -> Change: " + d.getText(0, d.getLength()));        
        }
        catch ( Exception ex ) {
            System.out.println(ex);
        }
*/
    }

    public void changedUpdate(DocumentEvent e)
    {
        updateModel(e);
    }    

    public void removeUpdate(DocumentEvent e)
    {
        updateModel(e);
    }

    public void insertUpdate(DocumentEvent e)
    {
        updateModel(e);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
