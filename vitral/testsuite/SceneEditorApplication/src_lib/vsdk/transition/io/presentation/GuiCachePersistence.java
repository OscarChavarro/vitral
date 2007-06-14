//===========================================================================

package vsdk.transition.io.presentation;

import java.io.File;
import java.io.Reader;
import java.io.StreamTokenizer;

import vsdk.transition.gui.GuiButtonGroupCache;
import vsdk.transition.gui.GuiCache;
import vsdk.transition.gui.GuiCommandCache;
import vsdk.transition.gui.GuiMenuCache;
import vsdk.transition.gui.GuiMenuItemCache;
import vsdk.transition.gui.ExceptionGuiCacheBadName;
import vsdk.transition.gui.ExceptionGuiCacheParseError;

import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.io.image.ImagePersistence;

public class GuiCachePersistence {
    private static void importAquynzaGuiMessages(
            StreamTokenizer parser,
            GuiCache context) throws Exception {
        int tokenType;
        int level = 0;

        String lastId = null;

        do {
            try {
                tokenType = parser.nextToken();
              }
              catch (Exception e) {
             System.out.println("Salida 0");
                throw e;
            }

            switch ( tokenType ) {
              case StreamTokenizer.TT_EOL: break;
              case StreamTokenizer.TT_EOF: break;
              case StreamTokenizer.TT_NUMBER:
                break;
              case StreamTokenizer.TT_WORD:
                if ( level == 1 ) {
            lastId = new String(parser.sval);
        }
                break;
              default:
                if ( parser.ttype == '\"' ) {
            context.addMessage(lastId, parser.sval);
                  }
                  else {
                    // Only supposed to contain '{' or '}'
                    char content = parser.toString().charAt(7);
                    if ( content == '{' ) {
             level++;
             if ( level > 2 ) {
                             throw new ExceptionGuiCacheParseError();
             }
                      }
                      else if ( content == '}' ) {
             level--;
             if ( level <= 0 ) {
                             return;
             }
                      }
                      else {
                        //throw new ExceptionGuiCacheParseError();
                    }
                }
                break;
            }
        } while ( tokenType != StreamTokenizer.TT_EOF );
    }

    private static GuiButtonGroupCache importAquynzaGuiButtonGroup(
            StreamTokenizer parser,
            GuiCache context) throws Exception {
        GuiButtonGroupCache item;

        item = new GuiButtonGroupCache(context);
        int tokenType;
        int level = 0;

        String name = null;

        int param = 0;

        do {
            try {
                tokenType = parser.nextToken();
              }
              catch (Exception e) {
             System.out.println("Salida 0");
                throw e;
            }

            switch ( tokenType ) {
              case StreamTokenizer.TT_EOL: break;
              case StreamTokenizer.TT_EOF: break;
              case StreamTokenizer.TT_NUMBER:
                break;
              case StreamTokenizer.TT_WORD:
                if ( level == 2 ) {
            item.addCommandByName(parser.sval);
        }
        if ( param == 0 && parser.sval.equals("direction") ) {
                    param = 1;
        }
        else if ( param == 1 ) {
                    if ( parser.sval.equals("horizontal") ) {
                        item.setDirection(item.HORIZONTAL);
            }
            else {
                        item.setDirection(item.VERTICAL);
            }
            param = 0;
        }
        else if ( param == 0 && parser.sval.equals("showIcons") ) {
                    param = 2;
        }
        else if ( param == 2 ) {
                    if ( parser.sval.equals("on") ) {
                        item.setShowIcons(true);
            }
            else {
                        item.setShowIcons(false);
            }
            param = 0;
        }
        else if ( param == 0 && parser.sval.equals("showText") ) {
                    param = 3;
        }
        else if ( param == 3 ) {
                    if ( parser.sval.equals("on") ) {
                        item.setShowText(true);
            }
            else {
                        item.setShowText(false);
            }
            param = 0;
        }
        else if ( param == 0 && parser.sval.equals("showTitle") ) {
                    param = 4;
        }
        else if ( param == 4 ) {
                    if ( parser.sval.equals("on") ) {
                        //item.setShowTitle(true);
            }
            else {
                        //item.setShowTitle(false);
            }
            param = 0;
        }
                break;
              default:
                if ( parser.ttype == '\"' ) {
              if ( name == null ) {
                        name = parser.sval;
                        item.setName(name);
                }
                  }
                  else {
                    // Only supposed to contain '{' or '}'
                    char content = parser.toString().charAt(7);
                    if ( content == '{' ) {
             level++;
             if ( level > 2 ) {
                             throw new ExceptionGuiCacheParseError();
             }
                      }
                      else if ( content == '}' ) {
             level--;
             if ( level <= 0 ) {
                             return item;
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
        System.out.println("Salida 2");
            throw new ExceptionGuiCacheBadName();
        }

        return item;
    }

    private static GuiCommandCache importAquynzaGuiCommand(
            StreamTokenizer parser,
            GuiCache context) throws Exception {
        GuiCommandCache item;
        RGBAImage img;
        RGBImage mask;

        item = new GuiCommandCache();

        int tokenType;

        String idString = null;

        int stringMode = 0;

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
        if ( idString == null ) {
                    idString = parser.sval;
            item.setId(parser.sval);
            }
                else if ( parser.sval.equals("name") ) {
                    stringMode = 1;
                }
                else if ( parser.sval.equals("icon") ) {
                    stringMode = 2;
                }
                else if ( parser.sval.equals("brief") ) {
                    stringMode = 3;
                }
                else if ( parser.sval.equals("help") ) {
                    stringMode = 4;
                }
                else if ( parser.sval.equals("iconTransparency") ) {
                    stringMode = 5;
                }
                break;
              default:
                if ( parser.ttype == '\"' ) {
                    switch ( stringMode ) {
                      case 1: // name
                item.setName(parser.sval);
            break;
                      case 2: // icon
                        try {
                            img = 
                            ImagePersistence.importRGBA(new File(parser.sval));
                  item.setIcon(img);
                        }
                        catch ( Exception e ) {
                            System.err.println("Warning: could not read the image file \"" + parser.sval + "\".");
                            System.err.println(e);
                        }
            break;
                      case 3: // brief
            item.setBrief(parser.sval);
            break;
                      case 4: // help
            item.appendToHelp(parser.sval);
            break;
                      case 5: // icon transparency
                        try {
                            mask = 
                            ImagePersistence.importRGB(new File(parser.sval));
                  item.setIconTransparency(mask);
                        }
                        catch ( Exception e ) {
                            System.err.println("Warning: could not read the image file \"" + parser.sval + "\".");
                            System.err.println(e);
                        }
            break;
                      default:
            break;
            }
                  }
                  else {
                    // Only supposed to contain '{' or '}'
                    char content = parser.toString().charAt(7);
                    if ( content == '{' ) {
                        if ( idString == null ) {
                            throw new ExceptionGuiCacheParseError();
            }
                      }
                      else if ( content == '}' ) {
                          //parser.pushBack();
                          //item.setName(name);
                          item.applyTransparency();
                          return item;
                      }
                      else {
                        //throw new ExceptionGuiCacheParseError();
                    }
                }
                break;
            }
        } while ( tokenType != StreamTokenizer.TT_EOF );

        if ( idString == null ) {
            throw new ExceptionGuiCacheBadName();
        }

        item.applyTransparency();

        return item;
    }

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
                if ( parser.sval.equals("MENU") ) {
                    GuiMenuCache menubar =
                            importAquynzaGuiMenu(parser, context);
                    context.setMenubar(menubar);
                }
                else if ( parser.sval.equals("POPUP") ) {
                    GuiMenuCache popup =
                            importAquynzaGuiMenu(parser, context);
                    context.addPopupMenu(popup);
                }
                else if ( parser.sval.equals("COMMAND") ) {
                    GuiCommandCache command = 
                importAquynzaGuiCommand(parser, context);
                    context.addCommand(command);
        }
                else if ( parser.sval.equals("BUTTON_GROUP") ) {
                    GuiButtonGroupCache bg = 
                importAquynzaGuiButtonGroup(parser, context);
                    context.addButtonGroup(bg);
        }
        else if ( parser.sval.equals("MESSAGES") ) {
                    importAquynzaGuiMessages(parser, context);
        }
        else {
                    System.out.println("NotProcessedIdentifier " + parser.sval);
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
