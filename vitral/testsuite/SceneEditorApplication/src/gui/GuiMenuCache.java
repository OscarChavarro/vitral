package gui;

import java.io.StreamTokenizer;
import java.util.Vector;
import java.util.Enumeration;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class GuiMenuCache extends GuiElementCache
{
    private GuiCache parent;
    private Vector <GuiElementCache> children;
    private String name;

    public GuiMenuCache(GuiCache parent)
    {
        this.parent = parent;
        children = new Vector<GuiElementCache>();
        name = null;
    }

    public void read(StreamTokenizer parser) throws Exception
    {
        int tokenType;
        int level = 0;

        System.out.println("- MENU ----------------------------------");

        do {
            try {
                tokenType = parser.nextToken();
              }
              catch (Exception e) {
                throw e;
            }
            switch ( tokenType ) {
              case StreamTokenizer.TT_EOL: break;
              case StreamTokenizer.TT_EOF: break;
              case StreamTokenizer.TT_NUMBER:
                //System.out.println("NUMBER " + parser.sval);
                break;
              case StreamTokenizer.TT_WORD:
                if ( parser.sval.equals("POPUP") ) {
                    GuiMenuCache popup = new GuiMenuCache(parent);
                    try {
                      popup.read(parser);
                    }
                    catch (Exception e) {
                        throw e;
                    }
                    children.add(popup);
                }

                else if ( parser.sval.equals("MENUITEM") ) {
                    GuiMenuItemCache item = new GuiMenuItemCache(parent);
                    try {
                      item.read(parser);
                    }
                    catch (Exception e) {
                        throw e;
                    }
                    children.add(item);
                }

                break;
              default:
                if ( parser.ttype == '\"' ) {
                    name = new String(parser.sval);
                  }
                  else {
                    // Only supposed to contain '{' or '}'
                    char content = parser.toString().charAt(7);
                    if ( content == '{' ) {
                        //System.out.println("{ MARK");
                        level++;
                      }
                      else if ( content == '}' ) {
                        //System.out.println("} MARK");
                        level--;
                        if ( level == 0 ) {
                            tokenType = StreamTokenizer.TT_EOF;
                        }
                      }
                      else {
                        throw new ExceptionGuiCacheParseError();
                    }
                }
                break;
            }
        } while ( tokenType != StreamTokenizer.TT_EOF );

        if ( name == null ) {
            throw new ExceptionGuiCacheBadName();
        }
    }

    public String toString()
    {
        return "Menu " + name;
    }

    public JMenuBar exportSwingMenubar()
    {
        JMenu popup;
        JMenuItem option;
        JMenuBar menubar;

        menubar = new JMenuBar();

        System.out.println("---------------------------------------------------------------------------");
        Enumeration i;

        for ( i = children.elements(); i.hasMoreElements(); ) {
            System.out.println((GuiElementCache)i.nextElement());
        }

        popup = new JMenu("WOW");
        menubar.add(popup);
        option = popup.add(new JMenuItem("COOL"));
        option.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }});

        popup.getPopupMenu().setLightWeightPopupEnabled(false);

        System.out.println("---------------------------------------------------------------------------");

        return menubar;
    }

}
