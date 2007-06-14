//===========================================================================
//===========================================================================

package render.swing;

// Basic JAVA JDK classes
import java.util.ArrayList;
import java.util.Iterator;

// GUI JDK classes (Awt + Swing)
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

// Application specific classes
import gui.GuiCache;
import gui.GuiMenuCache;
import gui.GuiMenuItemCache;
import gui.GuiElementCache;

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
                if ( className.equals("gui.GuiMenuCache") ) {
                    GuiMenuCache submenu = (GuiMenuCache)element;
                    JMenu widgetSubmenu = buildPopupMenu(context, 
                             submenu.getName(), 
                                                         executor);
                    widgetPopup.add(widgetSubmenu);
                }
                else if ( className.equals("gui.GuiMenuItemCache") ) {
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
                if ( className.equals("gui.GuiMenuCache") ) {
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
