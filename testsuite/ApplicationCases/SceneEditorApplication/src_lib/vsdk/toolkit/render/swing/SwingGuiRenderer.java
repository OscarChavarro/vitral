//===========================================================================
package vsdk.toolkit.render.swing;

// Basic JAVA JDK classes
import java.awt.*;
import java.util.ArrayList;

// GUI JDK classes (Awt + Swing)
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

// VSDK classes
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.render.awt.AwtRGBAImageRenderer;

// Application specific classes
import vsdk.toolkit.gui.Gui;
import vsdk.toolkit.gui.GuiMenu;
import vsdk.toolkit.gui.GuiMenuItem;
import vsdk.toolkit.gui.GuiMenuElement;
import vsdk.toolkit.gui.GuiButtonGroup;
import vsdk.toolkit.gui.GuiCommand;
import vsdk.toolkit.gui.GuiDialog;
import vsdk.toolkit.gui.variable.*;

public class SwingGuiRenderer {

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

    public static JMenu buildPopupMenu(Gui context, String name,
            ActionListener executor) {
        JMenu widgetPopup;
        JMenuItem widgetOption;
        SwingEventListener eventListener;
        int mnemonic;

        widgetPopup = new JMenu(name);
        widgetPopup.getPopupMenu().setLightWeightPopupEnabled(false);

        GuiMenu menu = context.getPopup(name);

        if (menu == null) {
            widgetOption =
                    widgetPopup.add(new JMenuItem("Popup menu not found on GUI"));
        } else {
            ArrayList<GuiMenuElement> children;
            children = menu.getChildren();

            int i;
            GuiMenuElement element;
            String className;

            for (i = 0; i < children.size(); i++) {
                element = children.get(i);
                className = element.getClass().getName();
                if (className.equals("vsdk.toolkit.gui.GuiMenu")) {
                    GuiMenu submenu = (GuiMenu) element;
                    JMenu widgetSubmenu = buildPopupMenu(context,
                            submenu.getName(),
                            executor);
                    widgetPopup.add(widgetSubmenu);
                } else if (className.equals("vsdk.toolkit.gui.GuiMenuItem")) {
                    GuiMenuItem option = (GuiMenuItem) element;
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

    public static JPanel buildButtonGroup(Gui context, String name, ActionListener executor) {
        JPanel frame;
        JLabel l;
        JButton b;

        frame = new JPanel();
        GuiButtonGroup group;
        group = context.getButtonGroup(name);

        if (group == null) {
            frame.setBackground(new Color(1.0f, 0.0f, 0.0f));
            return frame;
        }

        Border empty = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        frame.setBorder(empty);
        if (group.getDirection() == GuiButtonGroup.HORIZONTAL) {
            //frame.setLayout(new BoxLayout(frame, BoxLayout.X_AXIS));
            frame.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        } else {
            frame.setLayout(new BoxLayout(frame, BoxLayout.Y_AXIS));
        }

        if (group == null) {
            l = new JLabel("No ButtonGroup \"" + name + "\" found in GUI");
            frame.add(l);
            return frame;
        }

        ArrayList<GuiCommand> list = group.getCommands();
        int i;
        GuiCommand element;
        RGBAImage img;

        for (i = 0; i < list.size(); i++) {
            element = list.get(i);

            // Button goes with images ... if any inside command
            img = element.getIcon();

            if (img == null || !group.isShowIconsSet()) {
                b = new JButton(element.getName());
            } else {
                b = new JButton(new ImageIcon(
                        AwtRGBAImageRenderer.exportToAwtBufferedImage(img)));
                b.setMargin(new Insets(0, 0, 0, 0));
            }
            b.setName(element.getId());

            if (group.isShowTextSet()) {
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
     * This method construct the swing menu structure for the menu contained in
     * data context which has the specified name. If null is given as name, the
     * context's menubar is used. In this way, different frame windows could
     * have different menubars.
     *
     * The builded menu is supposed to be used as a menubar inside a swing
     * JFrame.
     *
     * @todo: permit the selection of a diferent name menu
     */
    public static JMenuBar buildMenubar(Gui context, String name, ActionListener executor) {
        JMenu widgetPopup;
        JMenuItem widgetOption;
        JMenuBar widgetMenubar;
        GuiMenu menubar = null;
        String errorMenu = null;
        int mnemonic;

        if (context != null) {
            menubar = context.getMenubar();
            if (menubar == null) {
                errorMenu = "No menubar in GUI!";
            }
        } else {
            errorMenu = "No Gui specified!";
        }

        widgetMenubar = new JMenuBar();

        if (errorMenu != null) {
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
            ArrayList<GuiMenuElement> children;
            children = menubar.getChildren();

            int i;
            GuiMenuElement element;
            GuiMenu menu;
            String className;

            for (i = 0; i < children.size(); i++) {
                element = children.get(i);
                className = element.getClass().getName();
                if (className.equals("vsdk.toolkit.gui.GuiMenu")) {
                    menu = (GuiMenu) element;
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

    public static JPanel buildBooleanVariable(GuiBooleanVariable v) {
        JPanel p = new JPanel();
        //JCheckBox cb = new JCheckBox(v.getName());
        p.add(buildNumberWidget(v.getName()));
        //p.add(cb);
        return p;
    }

    private static JPanel buildNumberWidget(String subname) {
        JPanel p;
        JLabel l;
        JTextField t;
        p = new JPanel();
        l = new JLabel(subname);
        t = new JTextField("0.0");
        p.add(l);
        p.add(t);
        return p;
    }

    public static JPanel buildVector3DVariable(GuiVector3DVariable v) {
        JPanel p = new JPanel();
        JLabel lv = new JLabel(v.getName());
        p.add(lv);
        p.add(buildNumberWidget("X:"));
        p.add(buildNumberWidget("Y:"));
        p.add(buildNumberWidget("Z:"));
        return p;
    }

    public static JPanel buildColorRgbVariable(GuiColorRgbVariable v) {
        JPanel p = new JPanel();
        JLabel lv = new JLabel(v.getName());
        p.add(lv);
        p.add(buildNumberWidget("R:"));
        p.add(buildNumberWidget("G:"));
        p.add(buildNumberWidget("B:"));
        return p;
    }

    public static JPanel buildDoubleVariable(GuiDoubleVariable v) {
        JPanel p = new JPanel();
        p.add(buildNumberWidget(v.getName()));
        return p;
    }

    public static JPanel buildIntegerVariable(GuiIntegerVariable v) {
        JPanel p = new JPanel();
        //JScrollBar sb = new JScrollBar();
        //JSlider js = new JSlider();
        p.add(buildNumberWidget(v.getName()));
        //p.add(sb);
        //p.add(js);
        return p;
    }

    public static JPanel buildStringVariable(GuiStringVariable v) {
        JPanel p = new JPanel();
        //JLabel l = new JLabel(v.getName());
        //p.add(l);
        p.add(buildNumberWidget(v.getName()));
        return p;
    }
    
    public static JButton buildCommandButton(GuiCommand c){
        JButton b = new JButton(c.getName());
        return b;
    }

    public static JPanel buildVariable(GuiVariable v) {
        JPanel containingPanelWidget = new JPanel();
        if (v == null) {
            JLabel l = new JLabel("NULL Variable ");
            containingPanelWidget.add(l);
            return containingPanelWidget;
        }

        if (v instanceof vsdk.toolkit.gui.variable.GuiBooleanVariable) {
            containingPanelWidget = buildBooleanVariable((GuiBooleanVariable) v);
        } else if (v instanceof vsdk.toolkit.gui.variable.GuiVector3DVariable) {
            containingPanelWidget = buildVector3DVariable((GuiVector3DVariable) v);
        } else if (v instanceof vsdk.toolkit.gui.variable.GuiColorRgbVariable) {
            containingPanelWidget = buildColorRgbVariable((GuiColorRgbVariable) v);
        } else if (v instanceof vsdk.toolkit.gui.variable.GuiDoubleVariable) {
            containingPanelWidget = buildDoubleVariable((GuiDoubleVariable) v);
        } else if (v instanceof vsdk.toolkit.gui.variable.GuiIntegerVariable) {
            containingPanelWidget = buildIntegerVariable((GuiIntegerVariable) v);
        } else if (v instanceof vsdk.toolkit.gui.variable.GuiStringVariable) {
            containingPanelWidget = buildStringVariable((GuiStringVariable) v);
        } else {
            JLabel l = new JLabel("Variable of type " + v.getClass().getName() + " not supported yet");
            containingPanelWidget.add(l);
        }
        return containingPanelWidget;
    }

    public static JPanel buildDialog(GuiDialog d, Gui gui) {
        JPanel panel = new JPanel();
        TitledBorder tb;
        String aux = d.getId();
        if (d == null) {
            JLabel l = new JLabel("NULL Dialog ");
            panel.add(l);
            return panel;
        }
        
        //panel.setPreferredSize(new Dimension(aux.length(), 50));
        tb = new TitledBorder(aux);
        panel.setBorder(tb);

        for (int k = 0; k < d.getPendingVariableNames().size(); k++) {
            JPanel q = SwingGuiRenderer.buildVariable(gui.getVariableByName(d.getPendingVariableNames().get(k)));
            panel.add(q);
        }
        
        for(int i = 0; i < d.getPendingCommandNames().size(); i++){
            GuiCommand c = new GuiCommand();
            c.setName(d.getPendingCommandNames().get(i));
            panel.add(buildCommandButton(c));
        }
        
        for(int i = 0; i < d.getPendingDialogRefNames().size(); i++){
            GuiDialog dial = new GuiDialog();
            dial.setName(d.getPendingDialogRefNames().get(i));
            panel.add(buildDialog(dial, gui));
        }
        
        
        return panel;
    }
}
//===========================================================================
//= EOF                                                                     =
//===========================================================================
