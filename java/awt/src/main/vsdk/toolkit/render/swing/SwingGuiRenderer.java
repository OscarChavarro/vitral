package vsdk.toolkit.render.swing;

// Basic JAVA JDK classes
import java.util.ArrayList;

// GUI JDK classes (Awt + Swing)
import java.awt.LayoutManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.border.Border;


// VSDK classes
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.render.awt.AwtRGBAImageUncompressedRenderer;
import vsdk.toolkit.gui.PresentationElement;
import vsdk.toolkit.gui.widget.variable.WidgetBooleanVariable;
import vsdk.toolkit.gui.widget.variable.WidgetColorRgbVariable;
import vsdk.toolkit.gui.widget.variable.WidgetDoubleVariable;
import vsdk.toolkit.gui.widget.variable.WidgetIntegerVariable;
import vsdk.toolkit.gui.widget.variable.WidgetStringVariable;
import vsdk.toolkit.gui.widget.variable.WidgetVariable;
import vsdk.toolkit.gui.widget.variable.WidgetVector3DVariable;
import vsdk.toolkit.gui.widget.Widget;
import vsdk.toolkit.gui.widget.WidgetButtonGroup;
import vsdk.toolkit.gui.widget.WidgetCommand;
import vsdk.toolkit.gui.widget.WidgetMenu;
import vsdk.toolkit.gui.widget.WidgetMenuItem;
import vsdk.toolkit.gui.widget.WidgetMenuElement;
import vsdk.toolkit.gui.widget.WidgetDialog;

public class SwingGuiRenderer extends PresentationElement {

    private static int convertMnemonic2Swing(char in) {
        char uc = Character.toUpperCase(in);
        int output = 0;

        switch (uc) {
            case '0':
                output = KeyEvent.VK_0;
                break;
            case '1':
                output = KeyEvent.VK_1;
                break;
            case '2':
                output = KeyEvent.VK_2;
                break;
            case '3':
                output = KeyEvent.VK_3;
                break;
            case '4':
                output = KeyEvent.VK_4;
                break;
            case '5':
                output = KeyEvent.VK_5;
                break;
            case '6':
                output = KeyEvent.VK_6;
                break;
            case '7':
                output = KeyEvent.VK_7;
                break;
            case '8':
                output = KeyEvent.VK_8;
                break;
            case '9':
                output = KeyEvent.VK_9;
                break;
            case 'A':
                output = KeyEvent.VK_A;
                break;
            case 'B':
                output = KeyEvent.VK_B;
                break;
            case 'C':
                output = KeyEvent.VK_C;
                break;
            case 'D':
                output = KeyEvent.VK_D;
                break;
            case 'E':
                output = KeyEvent.VK_E;
                break;
            case 'F':
                output = KeyEvent.VK_F;
                break;
            case 'G':
                output = KeyEvent.VK_G;
                break;
            case 'H':
                output = KeyEvent.VK_H;
                break;
            case 'I':
                output = KeyEvent.VK_I;
                break;
            case 'J':
                output = KeyEvent.VK_J;
                break;
            case 'K':
                output = KeyEvent.VK_K;
                break;
            case 'L':
                output = KeyEvent.VK_L;
                break;
            case 'M':
                output = KeyEvent.VK_M;
                break;
            case 'N':
                output = KeyEvent.VK_N;
                break;
            case 'O':
                output = KeyEvent.VK_O;
                break;
            case 'P':
                output = KeyEvent.VK_P;
                break;
            case 'Q':
                output = KeyEvent.VK_Q;
                break;
            case 'R':
                output = KeyEvent.VK_R;
                break;
            case 'S':
                output = KeyEvent.VK_S;
                break;
            case 'T':
                output = KeyEvent.VK_T;
                break;
            case 'U':
                output = KeyEvent.VK_U;
                break;
            case 'V':
                output = KeyEvent.VK_V;
                break;
            case 'W':
                output = KeyEvent.VK_W;
                break;
            case 'X':
                output = KeyEvent.VK_X;
                break;
            case 'Y':
                output = KeyEvent.VK_Y;
                break;
            case 'Z':
                output = KeyEvent.VK_Z;
                break;
            default:
                break;
        }
        return output;
    }

    public static JMenu buildPopupMenu(Widget context, String name,
            ActionListener executor) {
        JMenu widgetPopup;
        JMenuItem widgetOption;
        SwingEventListener eventListener;
        int mnemonic;

        widgetPopup = new JMenu(name);
        widgetPopup.getPopupMenu().setLightWeightPopupEnabled(false);

        WidgetMenu menu = context.getPopup(name);

        if (menu == null) {
            //widgetOption =
            widgetPopup.add(new JMenuItem("Popup menu not found on GUI"));
        } else {
            ArrayList<WidgetMenuElement> children;
            children = menu.getChildren();

            int i;
            WidgetMenuElement element;
            String className;

            for (i = 0; i < children.size(); i++) {
                element = children.get(i);
                className = element.getClass().getName();
                if (className.equals("vsdk.toolkit.gui.widget.WidgetMenu")) {
                    WidgetMenu submenu = (WidgetMenu) element;
                    JMenu widgetSubmenu = buildPopupMenu(context,
                            submenu.getName(),
                            executor);
                    widgetPopup.add(widgetSubmenu);
                } else if (className.equals("vsdk.toolkit.gui.widget.WidgetMenuItem")) {
                    WidgetMenuItem option = (WidgetMenuItem) element;
                    if (option.isSeparator()) {
                        widgetPopup.addSeparator();
                    } else {
                        widgetOption = new JMenuItem(option.getName());
                        mnemonic = convertMnemonic2Swing(option.getMnemonic());
                        if (mnemonic != 0) {
                            widgetOption.setMnemonic(mnemonic);
                        }
                        widgetPopup.add(widgetOption);
                        eventListener = new SwingEventListener(
                                option.getCommandName(), executor);
                        widgetOption.addActionListener(eventListener);
                    }
                }
            }
        }
        return widgetPopup;
    }

    public static JPanel buildButtonGroup(
        Widget context, String name, ActionListener executor) {
        JPanel frame;
        JLabel l;
        JButton b;

        frame = new JPanel();
        WidgetButtonGroup group;
        group = context.getButtonGroup(name);

        if (group == null) {
            frame.setBackground(new Color(1.0f, 0.0f, 0.0f));
            l = new JLabel("No ButtonGroup \"" + name + "\" found in GUI");
            frame.add(l);
            return frame;
        }

        Border empty = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        frame.setBorder(empty);
        if (group.getDirection() == WidgetButtonGroup.HORIZONTAL) {
            //frame.setLayout(new BoxLayout(frame, BoxLayout.X_AXIS));
            frame.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        } else {
            frame.setLayout(new BoxLayout(frame, BoxLayout.Y_AXIS));
        }

        ArrayList<WidgetCommand> list = group.getCommands();
        int i;
        WidgetCommand element;
        RGBAImageUncompressed img;

        for ( i = 0; i < list.size(); i++ ) {
            element = list.get(i);

            // Button goes with images ... if any inside command
            img = element.getIcon();


            if ( img == null || !group.isShowIconsSet() ) {
                b = new JButton(element.getName());
            }
            else {
                final ImageIcon primaryIcon;
                primaryIcon = new ImageIcon(
                        AwtRGBAImageUncompressedRenderer.exportToAwtBufferedImage(img));
                b = new JButton(primaryIcon);
                b.setMargin(new Insets(0, 0, 0, 0));
                final JButton bb = b;
                img = element.getSecondaryIcon();
                if ( img != null ) {
                    final ImageIcon secondaryIcon;
                    secondaryIcon = new ImageIcon(
                            AwtRGBAImageUncompressedRenderer.exportToAwtBufferedImage(img));
                    b.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            ImageIcon i = (ImageIcon)bb.getIcon();
                            if ( i == primaryIcon ) {
                                bb.setIcon(secondaryIcon);
                            }
                            else {
                                bb.setIcon(primaryIcon);
                            }
                        }
                    });

                    //b.setPressedIcon(new ImageIcon(
                    //    AwtRGBAImageUncompressedRenderer.exportToAwtBufferedImage(img)));
                }
            }

            b.setName(element.getId());

            if ( group.isShowTextSet() ) {
                b.setText(element.getName());
            }

            // Configure button
            Dimension d = b.getMaximumSize();

            d.width = Short.MAX_VALUE;
            b.setAlignmentX(0.5f);
            b.setMaximumSize(d);
            String brief = element.getBriefDescription();
            // Warning: This is not working!
            if (brief != null) {
                b.setToolTipText(brief);
            }
            b.addActionListener(executor);
            frame.add(b);
        }

        return frame;
    }

    /**
    This method construct the swing menu structure for the menu contained in
    data context which has the specified name. If null is given as name, the
    context's menubar is used. In this way, different frame windows could
    have different menubars.

    The builded menu is supposed to be used as a menubar inside a swing
    JFrame.

    \todo : permit the selection of a different name menu
    @param context
    @param name
    @param executor
    @return
    */
    public static JMenuBar buildMenubar(
        Widget context, String name, ActionListener executor)
    {
        JMenu widgetPopup;
        JMenuItem widgetOption;
        JMenuBar widgetMenubar;
        WidgetMenu menubar = null;
        String errorMenu = null;
        int mnemonic;

        if ( context != null ) {
            menubar = context.getMenubar();
        }
        else {
            errorMenu = "No Widget specified!";
        }
        if ( menubar == null ) {
            errorMenu = "No menubar in GUI!";
        }

        widgetMenubar = new JMenuBar();

        if ( menubar == null || errorMenu != null) {
            widgetPopup = new JMenu(errorMenu);
            widgetMenubar.add(widgetPopup);
            widgetOption = widgetPopup.add(new JMenuItem("Exit"));
            widgetOption.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });

            widgetPopup.getPopupMenu().setLightWeightPopupEnabled(false);
        } else {
            ArrayList<WidgetMenuElement> children;
            children = menubar.getChildren();

            int i;
            WidgetMenuElement element;
            WidgetMenu menu;
            String className;

            for (i = 0; i < children.size(); i++) {
                element = children.get(i);
                className = element.getClass().getName();
                if (className.equals("vsdk.toolkit.gui.widget.WidgetMenu")) {
                    menu = (WidgetMenu) element;
                    widgetPopup = buildPopupMenu(context, menu.getName(),
                            executor);
                    mnemonic = convertMnemonic2Swing(menu.getMnemonic());
                    if (mnemonic != 0) {
                        widgetPopup.setMnemonic(mnemonic);
                    }
                    widgetMenubar.add(widgetPopup);
                }
            }
        }
        return widgetMenubar;
    }
///codeOscar
    // Code Oz
    public static JPanel buildBooleanVariable(WidgetBooleanVariable v, ActionListener executor) {
        JPanel p = new JPanel();
        /*
        v.getImageForFalseState();
        v.getImageForTrueState();
        */

        JButton optionA;
        JButton optionB = new JButton();

            optionA= new JButton("./etc/1.jpg");
                p.add(optionA);


        JCheckBox cb = new JCheckBox(v.getName());
      //  p.add(buildNumberWidget(v.getName()));

        cb.addActionListener(executor);
        p.add(cb);
        p.add(optionA);



        //JCheckBox cb = new JCheckBox(v.getName());
        //p.add(buildNumberWidget(v.getName()));
        //cb.addActionListener(executor);
        //p.add(cb);

      /*
        JCheckBox b = new JCheckBox("NUll);
        p.add(b);
        b = new JCheckBox("False");
        p.add(b);*/

       // RGBAImageUncompressed image = ImagePersistence.importRGBA(new File());

        /*
        getImageForTrueState
        */


        return p;
    }




    private static JPanel buildNumberWidget(String subname, ActionListener executor) {
        JPanel p;
        JLabel l;
        JTextField t;
        p = new JPanel();
        l = new JLabel(subname);
        t = new JTextField("0.0");
        t.addActionListener(executor);
        p.add(l);
        p.add(t);
        return p;
    }

    public static JPanel buildVector3DVariable(WidgetVector3DVariable v, ActionListener executor) {
        JPanel p = new JPanel();
        JLabel lv = new JLabel(v.getName());
        p.add(lv);
        p.add(buildNumberWidget("X:", executor));
        p.add(buildNumberWidget("Y:", executor));
        p.add(buildNumberWidget("Z:", executor));
        return p;
    }

    public static JPanel buildColorRgbVariable(WidgetColorRgbVariable v, ActionListener executor) {
        JPanel p = new JPanel();
        p.add(buildNumberWidget("R:", executor));
        p.add(buildNumberWidget("G:", executor));
        p.add(buildNumberWidget("B:", executor));
        return p;
    }

    public static JPanel buildDoubleVariable(WidgetDoubleVariable v, ActionListener executor) {
        JPanel p = new JPanel();
        p.add(buildNumberWidget(v.getName() + ":", executor));
        return p;
    }

    public static JPanel buildIntegerVariable(WidgetIntegerVariable v, ActionListener executor) {
        JPanel p = new JPanel();
        //JScrollBar sb = new JScrollBar();
        //JSlider js = new JSlider();
        p.add(buildNumberWidget(v.getName(), executor));
        //p.add(sb);
        //p.add(js);
        return p;
    }

    public static JPanel buildStringVariable(WidgetStringVariable v, ActionListener executor) {
        JPanel p = new JPanel();
        //JLabel l = new JLabel(v.getName());
        //p.add(l);
        p.add(buildNumberWidget(v.getName(), executor));
        return p;
    }

    public static JButton buildCommandButton(WidgetCommand c, ActionListener executor) {
        JButton b = new JButton(c.getName());
        b.setToolTipText("Test Message: " + c.getName());
        b.addActionListener(executor);
        return b;
    }

    public static JPanel buildVariable(WidgetVariable v, ActionListener executor) {
        JPanel containingPanelWidget = new JPanel();
        if (v == null) {
            JLabel l = new JLabel("NULL Variable ");
            containingPanelWidget.add(l);
            return containingPanelWidget;
        }

        if (v instanceof vsdk.toolkit.gui.widget.variable.WidgetBooleanVariable) {
            containingPanelWidget = buildBooleanVariable((WidgetBooleanVariable) v, executor);
        } else if (v instanceof vsdk.toolkit.gui.widget.variable.WidgetVector3DVariable) {
            containingPanelWidget = buildVector3DVariable((WidgetVector3DVariable) v, executor);
        } else if (v instanceof vsdk.toolkit.gui.widget.variable.WidgetColorRgbVariable) {
            containingPanelWidget = buildColorRgbVariable((WidgetColorRgbVariable) v, executor);
        } else if (v instanceof vsdk.toolkit.gui.widget.variable.WidgetDoubleVariable) {
            containingPanelWidget = buildDoubleVariable((WidgetDoubleVariable) v, executor);
        } else if (v instanceof vsdk.toolkit.gui.widget.variable.WidgetIntegerVariable) {
            containingPanelWidget = buildIntegerVariable((WidgetIntegerVariable) v, executor);
        } else if (v instanceof vsdk.toolkit.gui.widget.variable.WidgetStringVariable) {
            containingPanelWidget = buildStringVariable((WidgetStringVariable) v, executor);
        } else {
            JLabel l = new JLabel("Variable of type " + v.getClass().getName() + " not supported yet");
            containingPanelWidget.add(l);
        }
        return containingPanelWidget;
    }

    public static void buildGuiCommandConfiguration(JPanel panel, String aux, GridBagConstraints cons, WidgetCommand c, ActionListener executor) {
        if (aux.equals("Camera Rotations")) {
            if (c.getName().equals("UP")) {
                cons.gridx = 1;
                cons.gridy = 0;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                cons.anchor = GridBagConstraints.CENTER;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("DOWN")) {
                cons.gridx = 1;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("LEFT")) {
                cons.gridx = 0;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("RIGHT")) {
                cons.gridx = 2;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            }
        } else if (aux.equals("Camera Position Movements")) {
            if (c.getName().equals("UP")) {
                cons.gridx = 1;
                cons.gridy = 0;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                cons.anchor = GridBagConstraints.CENTER;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("DOWN")) {
                cons.gridx = 1;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("LEFT")) {
                cons.gridx = 0;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("RIGHT")) {
                cons.gridx = 2;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("BACK")) {
                cons.gridx = 0;
                cons.gridy = 0;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("FORWARD")) {
                cons.gridx = 2;
                cons.gridy = 0;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            }
        } else if (aux.equals("Terrain Position")) {
            if (c.getName().equals("UP")) {
                cons.gridx = 1;
                cons.gridy = 0;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                cons.anchor = GridBagConstraints.CENTER;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("DOWN")) {
                cons.gridx = 1;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c,executor), cons);
            } else if (c.getName().equals("LEFT")) {
                cons.gridx = 0;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("RIGHT")) {
                cons.gridx = 2;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("BACK")) {
                cons.gridx = 0;
                cons.gridy = 0;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("FORWARD")) {
                cons.gridx = 2;
                cons.gridy = 0;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            }
        } else if (aux.equals("Terrain Visualization")) {
            if (c.getName().equals("UP")) {
                cons.gridx = 1;
                cons.gridy = 0;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                cons.anchor = GridBagConstraints.CENTER;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("DOWN")) {
                cons.gridx = 1;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("LEFT")) {
                cons.gridx = 0;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("RIGHT")) {
                cons.gridx = 2;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("BACK")) {
                cons.gridx = 0;
                cons.gridy = 0;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("FORWARD")) {
                cons.gridx = 2;
                cons.gridy = 0;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            }
        } else if (aux.equals("Lines Visualization")) {
            if (c.getName().equals("LEFT")) {
                cons.gridx = 0;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c, executor), cons);
            } else if (c.getName().equals("RIGHT")) {
                cons.gridx = 2;
                cons.gridy = 1;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildCommandButton(c,executor), cons);
            }

    }
    }


        public static void buildGuiVariableConfiguration(JPanel panel, String aux, GridBagConstraints cons, Widget gui, ActionListener executor) {
            if (aux.equals("MODE_1")) {
                cons.gridx = 0;
                cons.gridy = 0;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildVariable(gui.getVariableByName(aux), executor), cons);
            } else if (aux.equals("MODE_2")) {
                cons.gridx = 10;
                cons.gridy = 11;
                cons.gridwidth = 1;
                cons.gridheight = 1;
                panel.add(buildVariable(gui.getVariableByName(aux), executor), cons);
        } else if (aux.equals("MODE_3")) {
            cons.gridx = 0;
            cons.gridy = 2;
            cons.gridwidth = 1;
            cons.gridheight = 1;
            panel.add(buildVariable(gui.getVariableByName(aux), executor), cons);
        } else if (aux.equals("color")) {
            cons.gridx = 0;
            cons.gridy = 2;
            cons.gridwidth = 1;
            cons.gridheight = 1;
            panel.add(buildVariable(gui.getVariableByName(aux), executor), cons);
        } else if (aux.equals("TYPE_LINES")) {
            cons.gridx = 0;
            cons.gridy = 0;
            cons.gridwidth = 1;
            cons.gridheight = 1;
            panel.add(buildVariable(gui.getVariableByName(aux), executor), cons);
        } else if (aux.equals("TOGGLE")) {
            cons.gridx = 2;
            cons.gridy = 0;
            cons.gridwidth = 1;
            cons.gridheight = 1;
            panel.add(buildVariable(gui.getVariableByName(aux), executor), cons);
        }
    }

    public static JPanel buildDialog(WidgetDialog d, Widget gui, ActionListener executor) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        LayoutManager cons;

        //TitledBorder tb;
        //String aux = d.getName();

        if (d == null) {
            JLabel l = new JLabel("NULL Dialog ");
            panel.add(l);
            return panel;
        }

        //panel.setPreferredSize(new Dimension(aux.length(), 50));
        //tb = new TitledBorder(aux);
        //panel.setBorder(tb);
        panel.setToolTipText("Test dialog: " + d.getId());

        //- Define geometry management for panel ---------------------------
        int ncols;
        int nrows;
        if (d.getOrientation() == WidgetDialog.ORIENTATION_HORIZONTAL) {
            ncols = d.getPendingVariableNames().size()
                    + d.getPendingCommandNames().size()
                    + d.getPendingDialogRefNames().size();
            nrows = 1;
        } else {
            ncols = 1;
            nrows = d.getPendingVariableNames().size()
                    + d.getPendingCommandNames().size()
                    + d.getPendingDialogRefNames().size();
        }


        cons = new GridLayout(nrows, ncols);
        panel.setLayout(cons);

        //------------------------------------------------------------------

        for (int k = 0; k < d.getPendingVariableNames().size(); k++) {
            String vn = d.getPendingVariableNames().get(k);
            //JPanel q = new JPanel();
            JPanel q = buildVariable(gui.getVariableByName(vn), executor);
            //buildGuiVariableConfiguration(q, vn, cons, gui);
            panel.add(q);
        }

        for (int i = 0; i < d.getPendingCommandNames().size(); i++) {
            WidgetCommand c = new WidgetCommand();
            c.setName(d.getPendingCommandNames().get(i));
            panel.add(buildCommandButton(c, executor));
            //buildGuiCommandConfiguration(panel, aux, cons, c);
        }

        for (int i = 0; i < d.getPendingDialogRefNames().size(); i++) {
            WidgetDialog dial = new WidgetDialog();
            dial.setId(d.getPendingDialogRefNames().get(i));
            panel.add(buildDialog(dial, gui, executor));
        }
        if(d.isCollapsable()){
            CollapsablePanel pan = new CollapsablePanel(d.getName(), panel);
            return pan;
        }
        return panel;
    }

    private static void setPreferredSize(Dimension size) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void setMinimumSize(Dimension size) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void setMaximumSize(Dimension size) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
