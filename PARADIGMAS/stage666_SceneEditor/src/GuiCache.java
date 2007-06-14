
import java.io.Reader;
import java.io.StreamTokenizer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class GuiCache
{
    GuiMenuCache menubar;

    public GuiCache(Reader source)
    {
        menubar = null;

        StreamTokenizer parser = new StreamTokenizer(source);

        parser.resetSyntax();
        parser.eolIsSignificant(false);
        parser.quoteChar('\"');
        parser.slashSlashComments(true);
        parser.slashStarComments(true);
    parser.whitespaceChars(' ', ' ');
    parser.whitespaceChars(',', ',');
    parser.whitespaceChars('\t', '\t');
        parser.wordChars('A', 'Z');
        parser.wordChars('a', 'z');
        parser.wordChars('0', '9');
        parser.wordChars('_', '_');

        int tokenType;

        do {
            try {
                tokenType = parser.nextToken();
          }
          catch (Exception e) {
        break;
        }
            switch ( tokenType ) {
            case StreamTokenizer.TT_EOL: break;
            case StreamTokenizer.TT_EOF: break;
          case StreamTokenizer.TT_NUMBER:
                //System.out.println("NUMBER " + parser.sval);
            break;
          case StreamTokenizer.TT_WORD:
                //System.out.println("WORD " + parser.sval);
        if ( parser.sval.equals("MENU") ) {
                    menubar = new GuiMenuCache(this);
                    try {
              menubar.read(parser);
            }
            catch (Exception e) {
            System.err.println(e);
            }
        }
            break;
          default:
        if ( parser.ttype == '\"' ) {
                    //System.out.println("STRING " + parser.sval);
          }
          else {
                    // Only supposed to contain '{' or '}'
            char content = parser.toString().charAt(7);
            if ( content == '{' ) {
                        //System.out.println("{ MARK");
              }
              else if ( content == '}' ) {
                        //System.out.println("} MARK");
              }
              else {
                        System.err.println("UNKNOWN " + parser);
            return;
            }
        }
            break;
        }
    } while ( tokenType != StreamTokenizer.TT_EOF );
    }

    public JMenuBar exportSwingMenubar()
    {
        JMenu popup;
        JMenuItem option;
        JMenuBar menubar;

        if ( this.menubar == null ) {
            menubar = new JMenuBar();
            popup = new JMenu("File (NOMENUFOUND)");
            menubar.add(popup);

            option = popup.add(new JMenuItem("Exit"));
            option.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }});

            popup.getPopupMenu().setLightWeightPopupEnabled(false);
    }
    else {
            menubar = this.menubar.exportSwingMenubar();
    }

        return menubar;
    }
}
