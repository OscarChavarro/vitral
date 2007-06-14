//===========================================================================
//===========================================================================

package vsdk.transition.render.swing;

// Basic JAVA JDK classes
import java.util.ArrayList;
import java.util.Iterator;

// GUI JDK classes (Awt + Swing)
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.border.Border; 

// VSDK classes
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.render.awt.AwtRGBAImageRenderer;

// Application specific classes
import vsdk.transition.gui.GuiCache;
import vsdk.transition.gui.GuiMenuCache;
import vsdk.transition.gui.GuiMenuItemCache;
import vsdk.transition.gui.GuiElementCache;
import vsdk.transition.gui.GuiButtonGroupCache;
import vsdk.transition.gui.GuiCommandCache;

public class SwingGuiCacheRenderer
{
    private static int convertMnemonic2Swing(char in)
    {
        char uc = Character.toUpperCase(in);
        int output = 0;

        switch ( uc ) {
          case '0': output = KeyEvent.VK_0; break;
          case '1': output = KeyEvent.VK_1; break;
          case '2': output = KeyEvent.VK_2; break;
          case '3': output = KeyEvent.VK_3; break;
          case '4': output = KeyEvent.VK_4; break;
          case '5': output = KeyEvent.VK_5; break;
          case '6': output = KeyEvent.VK_6; break;
          case '7': output = KeyEvent.VK_7; break;
          case '8': output = KeyEvent.VK_8; break;
          case '9': output = KeyEvent.VK_9; break;
          case 'A': output = KeyEvent.VK_A; break;
          case 'B': output = KeyEvent.VK_B; break;
          case 'C': output = KeyEvent.VK_C; break;
          case 'D': output = KeyEvent.VK_D; break;
          case 'E': output = KeyEvent.VK_E; break;
          case 'F': output = KeyEvent.VK_F; break;
          case 'G': output = KeyEvent.VK_G; break;
          case 'H': output = KeyEvent.VK_H; break;
          case 'I': output = KeyEvent.VK_I; break;
          case 'J': output = KeyEvent.VK_J; break;
          case 'K': output = KeyEvent.VK_K; break;
          case 'L': output = KeyEvent.VK_L; break;
          case 'M': output = KeyEvent.VK_M; break;
          case 'N': output = KeyEvent.VK_N; break;
          case 'O': output = KeyEvent.VK_O; break;
          case 'P': output = KeyEvent.VK_P; break;
          case 'Q': output = KeyEvent.VK_Q; break;
          case 'R': output = KeyEvent.VK_R; break;
          case 'S': output = KeyEvent.VK_S; break;
          case 'T': output = KeyEvent.VK_T; break;
          case 'U': output = KeyEvent.VK_U; break;
          case 'V': output = KeyEvent.VK_V; break;
          case 'W': output = KeyEvent.VK_W; break;
          case 'X': output = KeyEvent.VK_X; break;
          case 'Y': output = KeyEvent.VK_Y; break;
          case 'Z': output = KeyEvent.VK_Z; break;
          default: break;
        }
        return output;
    }

    public static JMenu buildPopupMenu(GuiCache context, String name,
                                       ActionListener executor)
    {
        JMenu widgetPopup;
        JMenuItem widgetOption;
        SwingEventListener eventListener;
        int mnemonic;

        widgetPopup = new JMenu(name);
        widgetPopup.getPopupMenu().setLightWeightPopupEnabled(false);

        GuiMenuCache menu = context.getPopup(name);

        if ( menu == null ) {
            widgetOption = 
                widgetPopup.add(new JMenuItem("Popup menu not found on GUI"));
          }
          else {
            ArrayList<GuiElementCache> children;
            children = menu.getChildren();

            Iterator i;
            GuiElementCache element;
            String className;

            for ( i = children.iterator(); i.hasNext(); ) {
                element = (GuiElementCache)i.next();
                className = element.getClass().getName();
                if ( className.equals("vsdk.transition.gui.GuiMenuCache") ) {
                    GuiMenuCache submenu = (GuiMenuCache)element;
                    JMenu widgetSubmenu = buildPopupMenu(context, 
                                                         submenu.getName(), 
                                                         executor);
                    widgetPopup.add(widgetSubmenu);
                }
                else if ( className.equals("vsdk.transition.gui.GuiMenuItemCache") ) {
                    GuiMenuItemCache option = (GuiMenuItemCache)element;
                    if ( option.isSeparator() ) {
                        widgetPopup.addSeparator();
                      }
                      else {
                        widgetOption = new JMenuItem(option.getName());
                        mnemonic = convertMnemonic2Swing(option.getMnemonic());
                        if ( mnemonic != 0 ) {
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

    public static JPanel
    buildButtonGroup(GuiCache context, String name, ActionListener executor)
    {
        JPanel frame;
        JLabel l;
        JButton b;

        frame = new JPanel();
        GuiButtonGroupCache group;
        group = context.getButtonGroup(name);

        Border empty = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        frame.setBorder(empty);
        if ( group.getDirection() == group.HORIZONTAL ) {
            //frame.setLayout(new BoxLayout(frame, BoxLayout.X_AXIS));
            frame.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        }
        else {
            frame.setLayout(new BoxLayout(frame, BoxLayout.Y_AXIS));
        }

        if ( group == null ) {
            l = new JLabel("No ButtonGroup \"" + name + "\" found in GUI");
            frame.add(l);
            return frame;
        }

        ArrayList<GuiCommandCache> list = group.getCommands();
        Iterator i;
        GuiCommandCache element;
        RGBAImage img;

        for ( i = list.iterator(); i.hasNext(); ) {
            element = (GuiCommandCache)i.next();

            // Button goes with images ... if any inside command
            img = element.getIcon();

            if ( img == null || !group.isShowIconsSet() ) {
                b = new JButton(element.getName());
              }
              else {
                b = new JButton(new ImageIcon(
                      AwtRGBAImageRenderer.exportToAwtBufferedImage(img)));
                b.setMargin(new Insets(0, 0, 0, 0));
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
            if ( brief != null ) {
                b.setToolTipText(brief);
            }
            b.addActionListener(executor);
            frame.add(b);
        }

        return frame;
    }

    /**
    This method construct the swing menu structure for the menu contained
    in data context which has the specified name. If null is given as name,
    the context's menubar is used. In this way, different frame windows
    could have different menubars.

    The builded menu is supposed to be used as a menubar inside a swing
    JFrame.

    @todo: permit the selection of a diferent name menu
    */
    public static JMenuBar 
    buildMenubar(GuiCache context, String name, ActionListener executor)
    {
        JMenu widgetPopup;
        JMenuItem widgetOption;
        JMenuBar widgetMenubar;
        GuiMenuCache menubar = null;
        String errorMenu = null;
        int mnemonic;

        if ( context != null ) {
            menubar = context.getMenubar();
            if ( menubar == null ) {
                errorMenu = "No menubar in GUI!";
            }
          }
          else {
            errorMenu = "No GuiCache specified!";
        }

        widgetMenubar = new JMenuBar();

        if ( errorMenu != null ) {
            widgetPopup = new JMenu(errorMenu);
            widgetMenubar.add(widgetPopup);
            widgetOption = widgetPopup.add(new JMenuItem("Exit"));
            widgetOption.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }});

            widgetPopup.getPopupMenu().setLightWeightPopupEnabled(false);
          }
          else {
            ArrayList<GuiElementCache> children;
            children = menubar.getChildren();

            Iterator i;
            GuiElementCache element;
            GuiMenuCache menu;
            String className;

            for ( i = children.iterator(); i.hasNext(); ) {
                element = (GuiElementCache)i.next();
                className = element.getClass().getName();
                if ( className.equals("vsdk.transition.gui.GuiMenuCache") ) {
                    menu = (GuiMenuCache)element;
                    widgetPopup = buildPopupMenu(context, menu.getName(),
                                                 executor);
                    mnemonic = convertMnemonic2Swing(menu.getMnemonic());
                    if ( mnemonic != 0 ) {
                        widgetPopup.setMnemonic(mnemonic);
                    }
                    widgetMenubar.add(widgetPopup);
                }
            }
        }
        return widgetMenubar;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
