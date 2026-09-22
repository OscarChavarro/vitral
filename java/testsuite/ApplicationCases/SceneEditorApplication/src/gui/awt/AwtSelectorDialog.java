package gui.awt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

public class AwtSelectorDialog extends JDialog
{
    public AwtSelectorDialog()
    {
        super();

        //-----------------------------------------------------------------
        JPanel mainFrameWidget = new JPanel();
        Dimension size = new Dimension(526, 530);
        setMinimumSize(size);
        setSize(size);

        //-----------------------------------------------------------------
        JPanel bottomAreaWidget = new JPanel();
        JPanel centralAreaWidget = new JPanel();
        //JPanel rightAreaWidget = new JPanel();

        mainFrameWidget.add(bottomAreaWidget, BorderLayout.SOUTH);
        mainFrameWidget.add(centralAreaWidget, BorderLayout.NORTH);
        //mainFrameWidget.add(rightAreaWidget, BorderLayout.EAST);

        //-----------------------------------------------------------------
        JButton b = new JButton("Test bottom");
        bottomAreaWidget.add(b);

        b = new JButton("Test central");
        centralAreaWidget.add(b);

        //b = new JButton("Test right");
        //rightAreaWidget.add(b);

        add(mainFrameWidget);
        //-----------------------------------------------------------------
    }
}
