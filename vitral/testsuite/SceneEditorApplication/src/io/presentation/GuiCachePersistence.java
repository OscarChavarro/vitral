//===========================================================================

package io.presentation;

import java.io.Reader;
import java.io.StreamTokenizer;

import gui.GuiCache;
import gui.GuiMenuCache;
import gui.GuiMenuItemCache;
import gui.ExceptionGuiCacheBadName;
import gui.ExceptionGuiCacheParseError;

public class GuiCachePersistence {
    private static GuiMenuItemCache importAquynzaGuiMenuItem(
            StreamTokenizer parser,
            GuiCache context) throws Exception {
        GuiMenuItemCache item;

        item = new GuiMenuItemCache(context);

        int tokenType;

        String name = null;

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
                break;
              case StreamTokenizer.TT_WORD:
                if ( parser.sval.equals("MENUITEM") ||
                     parser.sval.equals("POPUP") ) {
                    parser.pushBack();
                    item.setName(name);
                    return item;
                }
                item.addModifier(parser.sval);
                break;
              default:
                if ( parser.ttype == '\"' ) {
                    name = new String(parser.sval);
                    item.setName(name);
                  }
                  else {
                    // Only supposed to contain '{' or '}'
                    char content = parser.toString().charAt(7);
                    if ( content == '{' ) {
                        throw new ExceptionGuiCacheParseError();
                      }
                      else if ( content == '}' ) {
                          parser.pushBack();
                          item.setName(name);
                          return item;
                      }
                      else {
                        //throw new ExceptionGuiCacheParseError();
                    }
                }
                break;
            }
        } while ( tokenType != StreamTokenizer.TT_EOF );

        if ( name == null ) {
            throw new ExceptionGuiCacheBadName();
        }
        item.setName(name);
        return item;
    }

    private static GuiMenuCache importAquynzaGuiMenu(StreamTokenizer parser,
            GuiCache context) throws Exception {
        GuiMenuCache menu;

        menu = new GuiMenuCache(context);

        int level = 0;

        int tokenType;

        String name = null;

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
              case StreamTokenizer.TT_NUMBER: break;
              case StreamTokenizer.TT_WORD:
                if ( parser.sval.equals("POPUP") ) {
                    GuiMenuCache popup = importAquynzaGuiMenu(parser, context);
                    context.addPopupMenu(popup);
                    menu.addChild(popup);
                }

                else if ( parser.sval.equals("MENUITEM") ) {
                    GuiMenuItemCache item;
                    item = importAquynzaGuiMenuItem(parser, context);
                    menu.addChild(item);
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
                        //throw new ExceptionGuiCacheParseError();
                    }
                }
                break;
            }
        } while ( tokenType != StreamTokenizer.TT_EOF );

        if ( name == null ) {
            throw new ExceptionGuiCacheBadName();
        }
        menu.setName(name);

        return menu;
    }

    public static GuiCache importAquynzaGui(Reader source) throws Exception {
        GuiCache context;

        context = new GuiCache();

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
            } catch (Exception e) {

                break;
            }
            switch (tokenType) {
            case StreamTokenizer.TT_EOL:
                break;
            case StreamTokenizer.TT_EOF:
                break;
            case StreamTokenizer.TT_NUMBER:
                //System.out.println("NUMBER " + parser.sval);
                break;
            case StreamTokenizer.TT_WORD:
                //System.out.println("WORD " + parser.sval);
                if (parser.sval.equals("MENU")) {
                    GuiMenuCache menubar =
                            importAquynzaGuiMenu(parser, context);
                    context.setMenubar(menubar);
                }
                else if (parser.sval.equals("POPUP")) {
                    GuiMenuCache popup =
                            importAquynzaGuiMenu(parser, context);
                    context.addPopupMenu(popup);
                }
                break;
            default:
                if (parser.ttype == '\"') {
                    //System.out.println("STRING " + parser.sval);
            ;
                } else {
                    // Only supposed to contain '{' or '}'
                    String report;
                    report = parser.toString();
                    if (report.length() >= 8) {
                        char content = report.charAt(7);
                        if (content == '{') {
                            //System.out.println("{ MARK");
                        } else if (content == '}') {
                            //System.out.println("} MARK");
                        } else {
                            // Nothing is done, as this is and unknown token,
                            // posibly corresponding to an empty token (i.e.
                            // a comment line with no real information)
                        }
                    }
                }
                break;
            }
        } while (tokenType != StreamTokenizer.TT_EOF);

        return context;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
