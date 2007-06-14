import java.io.StreamTokenizer;
import java.util.Vector;

public class GuiMenuItemCache extends GuiElementCache
{
    private GuiCache parent;
    private String name;

    public GuiMenuItemCache(GuiCache parent)
    {
        this.parent = parent;
        name = null;
    }

    public void read(StreamTokenizer parser) throws Exception
    {
        int tokenType;

        System.out.println(" - MENUITEM");

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
                if ( parser.sval.equals("MENUITEM") ||
                     parser.sval.equals("POPUP") ) {
                    parser.pushBack();
                    return;
                }
                System.out.println("    . MODIFIER [" + parser.sval + "]");
                break;
              default:
                if ( parser.ttype == '\"' ) {
                    System.out.println("    . STRING [" + parser.sval + "]");
                    name = new String(parser.sval);
                  }
                  else {
                    // Only supposed to contain '{' or '}'
                    char content = parser.toString().charAt(7);
                    if ( content == '{' ) {
                        throw new ExceptionGuiCacheParseError();
                        //System.out.println("{ MARK");
                      }
                      else if ( content == '}' ) {
                        //System.out.println("} MARK");
                          return;
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
        return "MenuItem " + name;
    }

}
