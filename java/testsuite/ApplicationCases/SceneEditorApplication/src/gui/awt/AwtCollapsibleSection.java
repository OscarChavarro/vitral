package gui.awt;

// Java GUI classes
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
Section of a side panel with a title that shows or hides its content when
clicked. The expanded and collapsed marks are the ones of the trees of the
current look and feel.
*/
public class AwtCollapsibleSection extends JPanel
{
    private final JLabel header;
    private final JComponent content;
    private boolean expanded;

    /**
    @param title text of the header of the section
    @param content component shown or hidden by the section
    @param expanded true if the section starts showing its content
    */
    public AwtCollapsibleSection(String title, JComponent content,
                                 boolean expanded)
    {
        super(new BorderLayout());
        this.content = content;

        header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                setExpanded(!isExpanded());
            }
        });

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setExpanded(expanded);
    }

    /**
    A section grows only in width, so the sections stacked in a panel keep
    together at its top when they are collapsed.
    @return maximum size of the section
    */
    @Override
    public Dimension getMaximumSize()
    {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /**
    @return true if the section shows its content
    */
    public boolean isExpanded()
    {
        return expanded;
    }

    /**
    @param expanded true to show the content of the section, false to hide it
    */
    public void setExpanded(boolean expanded)
    {
        Icon mark;

        this.expanded = expanded;
        mark = UIManager.getIcon(expanded ? "Tree.expandedIcon" : "Tree.collapsedIcon");
        header.setIcon(mark);
        content.setVisible(expanded);
        revalidate();
        repaint();
    }
}
