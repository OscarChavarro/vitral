//===========================================================================

// Java AWT/Swing classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.border.EtchedBorder;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

// Vitral classes
import vsdk.toolkit.gui.CameraControllerAquynza;

public class ControlPanel extends JPanel implements AdjustmentListener, ActionListener
{
    private CohenSutherlandClipping parent;

    public ControlPanel(CohenSutherlandClipping parent) {
        //-----------------------------------------------------------------
        JScrollBar sb;
        JLabel jl;
        JPanel frame;
        JPanel innerframe;
        String names[] = new String[6];

        this.parent = parent;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        //-----------------------------------------------------------------
        JRadioButton rb;
        ButtonGroup bg = new ButtonGroup();

        frame = new JPanel();
        frame.setLayout(new BoxLayout(frame, BoxLayout.X_AXIS));
        add(frame);
        frame.add(new JLabel("CAMERAS CONTROL -> "));

        innerframe = new JPanel();
        innerframe.setBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        innerframe.setLayout(new BoxLayout(innerframe, BoxLayout.X_AXIS));
        rb = new JRadioButton("Primary camera selected for control");
        rb.setSelected(true);
        rb.setActionCommand("ActivateCamera1");
        rb.addActionListener(this);
        bg.add(rb);
        innerframe.add(rb);
        rb = new JRadioButton("Secondary camera selected for control");
        rb.setSelected(false);
        rb.setActionCommand("ActivateCamera2");
        rb.addActionListener(this);
        bg.add(rb);
        innerframe.add(rb);
        frame.add(innerframe);

        //-----------------------------------------------------------------
        JPanel frame2;

        frame2 = new JPanel();
        JButton b;

        b = new JButton("Top view");
        b.addActionListener(this);
        frame2.add(b);

        b = new JButton("Front view");
        b.addActionListener(this);
        frame2.add(b);

        b = new JButton("Left view");
        b.addActionListener(this);
        frame2.add(b);

        b = new JButton("Perspective view");
        b.addActionListener(this);
        frame2.add(b);

        add(frame2);
        //-----------------------------------------------------------------
        names[0] = "x1";
        names[1] = "y1";
        names[2] = "z1";
        names[3] = "x2";
        names[4] = "y2";
        names[5] = "z2";
        for ( int i = 0; i < 6; i++ ) {
            if ( i == 0 || i == 3 ) {
                frame = new JPanel();
                frame.setLayout(new BoxLayout(frame, BoxLayout.X_AXIS));
                add(frame);
                if ( i == 0 ) frame.add(new JLabel("     FIRST POINT -> "));
                else frame.add(new JLabel(" SECOND POINT -> "));

                innerframe = new JPanel();
                innerframe.setBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
                innerframe.setLayout(new BoxLayout(innerframe, BoxLayout.X_AXIS));
                frame.add(innerframe);
            }
            jl = new JLabel(names[i] + ": ");
            innerframe.add(jl);
            sb = new JScrollBar(JScrollBar.HORIZONTAL);
            sb.setName(names[i]);
            sb.addAdjustmentListener(this);
            sb.setMinimum(0);
            sb.setMaximum(100);
            //sb.setValue(50);
            innerframe.add(sb);
        }
        //-----------------------------------------------------------------
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent ev) {
        double val = (((double)ev.getValue()) - 50.0) / 10.0;

        if ( ((JScrollBar)ev.getAdjustable()).getName().equals("x1") ) {
            parent.point0.x = val;
        }
        else if ( ((JScrollBar)ev.getAdjustable()).getName().equals("y1") ) {
            parent.point0.y = val;
        }
        else if ( ((JScrollBar)ev.getAdjustable()).getName().equals("z1") ) {
            parent.point0.z = val;
        }
        else if ( ((JScrollBar)ev.getAdjustable()).getName().equals("x2") ) {
            parent.point1.x = val;
        }
        else if ( ((JScrollBar)ev.getAdjustable()).getName().equals("y2") ) {
            parent.point1.y = val;
        }
        else if ( ((JScrollBar)ev.getAdjustable()).getName().equals("z2") ) {
            parent.point1.z = val;
        }
        parent.canvas.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if ( ((String)e.getActionCommand()).equals("ActivateCamera1") ) {
            parent.cameraController = 
                new CameraControllerAquynza(parent.camera1);
        }
        else if ( ((String)e.getActionCommand()).equals("Top view") ) {
            parent.setTopView();
        }
        else if ( ((String)e.getActionCommand()).equals("Front view") ) {
            parent.setFrontView();
        }
        else if ( ((String)e.getActionCommand()).equals("Left view") ) {
            parent.setLeftView();
        }
        else if ( ((String)e.getActionCommand()).equals("Perspective view") ) {
            parent.setPerspectiveView();
        }
        else {
            parent.cameraController = 
                new CameraControllerAquynza(parent.camera2);
        }

        parent.canvas.repaint();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
