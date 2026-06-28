package vsdk.toolkit.io.gui;

// Java basic classes
import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

// Vitral classes
import vsdk.toolkit.gui.widget.ExceptionWidgetBadName;
import vsdk.toolkit.gui.widget.ExceptionWidgetParseError;
import vsdk.toolkit.gui.widget.WidgetDialog;
import vsdk.toolkit.gui.widget.Widget;
import vsdk.toolkit.gui.widget.WidgetMenu;
import vsdk.toolkit.gui.widget.WidgetMenuItem;
import vsdk.toolkit.gui.widget.WidgetButtonGroup;
import vsdk.toolkit.gui.widget.WidgetCommand;
import vsdk.toolkit.gui.widget.variable.WidgetVariable;
import vsdk.toolkit.gui.widget.variable.WidgetDoubleVariable;
import vsdk.toolkit.gui.widget.variable.WidgetIntegerVariable;
import vsdk.toolkit.gui.widget.variable.WidgetColorRgbVariable;
import vsdk.toolkit.gui.widget.variable.WidgetVector3DVariable;
import vsdk.toolkit.gui.widget.variable.WidgetStringVariable;
import vsdk.toolkit.gui.widget.variable.WidgetBooleanVariable;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.io.PersistenceElement;

public class GuiPersistence extends PersistenceElement {
    private static void importAquynzaGuiMessages(
            StreamTokenizer parser,
            Widget context) throws Exception {
        int tokenType;
        int level = 0;

        String lastId = null;

        do {
            try {
                tokenType = parser.nextToken();
            } catch (IOException e) {
                System.out.println("Exit case 0");
                throw e;
            }

            switch (tokenType) {
                case StreamTokenizer.TT_EOL:
                    break;
                case StreamTokenizer.TT_EOF:
                    break;
                case StreamTokenizer.TT_NUMBER:
                    break;
                case StreamTokenizer.TT_WORD:
                    if (level == 1) {
                        lastId = parser.sval;
                    }
                    break;
                default:
                    if (parser.ttype == '\"') {
                        context.addMessage(lastId, parser.sval);
                    } else {
                        // Only supposed to contain '{' or '}'
                        char content = parser.toString().charAt(7);
                        if (content == '{') {
                            level++;
                            if (level > 2) {
                                throw new ExceptionWidgetParseError();
                            }
                        } else if (content == '}') {
                            level--;
                            if (level <= 0) {
                                return;
                            }
                        } else {
                            //throw new ExceptionWidgetParseError();
                        }
                    }
                    break;
            }
        } while (tokenType != StreamTokenizer.TT_EOF);
    }

    private static WidgetButtonGroup importAquynzaGuiButtonGroup(
            StreamTokenizer parser,
            Widget context) throws Exception {
        WidgetButtonGroup item;

        item = new WidgetButtonGroup(context);
        int tokenType;
        int level = 0;

        String name = null;

        int parameter = 0;

        do {
            try {
                tokenType = parser.nextToken();
            } catch (IOException e) {
                System.out.println("Salida 0");
                throw e;
            }

            switch (tokenType) {
                case StreamTokenizer.TT_EOL:
                    break;
                case StreamTokenizer.TT_EOF:
                    break;
                case StreamTokenizer.TT_NUMBER:
                    break;
                case StreamTokenizer.TT_WORD:
                    if (level == 2) {
                        item.addCommandByName(parser.sval);
                    }
                    if (parameter == 0 && parser.sval.equals("direction")) {
                        parameter = 1;
                    } else if (parameter == 1) {
                        if (parser.sval.equals("horizontal")) {
                            item.setDirection(WidgetButtonGroup.HORIZONTAL);
                        } else {
                            item.setDirection(WidgetButtonGroup.VERTICAL);
                        }
                        parameter = 0;
                    } else if (parameter == 0 && parser.sval.equals("showIcons")) {
                        parameter = 2;
                    } else if (parameter == 2) {
                        if (parser.sval.equals("on")) {
                            item.setShowIcons(true);
                        } else {
                            item.setShowIcons(false);
                        }
                        parameter = 0;
                    } else if (parameter == 0 && parser.sval.equals("showText")) {
                        parameter = 3;
                    } else if (parameter == 3) {
                        if (parser.sval.equals("on")) {
                            item.setShowText(true);
                        } else {
                            item.setShowText(false);
                        }
                        parameter = 0;
                    } else if (parameter == 0 && parser.sval.equals("showTitle")) {
                        parameter = 4;
                    } else if (parameter == 4) {
                        if (parser.sval.equals("on")) {
                            //item.setShowTitle(true);
                        } else {
                            //item.setShowTitle(false);
                        }
                        parameter = 0;
                    }
                    break;
                default:
                    if (parser.ttype == '\"') {
                        if (name == null) {
                            name = parser.sval;
                            item.setName(name);
                        }
                    } else {
                        // Only supposed to contain '{' or '}'
                        char content = parser.toString().charAt(7);
                        if (content == '{') {
                            level++;
                            if (level > 2) {
                                throw new ExceptionWidgetParseError();
                            }
                        } else if (content == '}') {
                            level--;
                            if (level <= 0) {
                                return item;
                            }
                        } else {
                            //throw new ExceptionWidgetParseError();
                        }
                    }
                    break;
            }
        } while (tokenType != StreamTokenizer.TT_EOF);

        if (name == null) {
            System.out.println("Salida 2");
            throw new ExceptionWidgetBadName();
        }

        return item;
    }

    private static WidgetCommand importAquynzaGuiCommand(
        StreamTokenizer parser, String globalDataPath) throws Exception
    {
        WidgetCommand item;
        RGBAImageUncompressed img;
        RGBImageUncompressed mask;

        item = new WidgetCommand();

        int tokenType;

        String idString = null;

        int stringMode = 0;
        int iconId = 1;

        do {
            try {
                tokenType = parser.nextToken();
            }
            catch (IOException e) {
                throw e;
            }

            switch (tokenType) {
                case StreamTokenizer.TT_EOL:
                    break;
                case StreamTokenizer.TT_EOF:
                    break;
                case StreamTokenizer.TT_NUMBER:
                    break;
                case StreamTokenizer.TT_WORD:
                    if (idString == null) {
                        idString = parser.sval;
                        item.setId(parser.sval);
                    }
                    else if (parser.sval.equals("name")) {
                        stringMode = 1;
                    }
                    else if (parser.sval.equals("icon")) {
                        stringMode = 2;
                        iconId = 1;
                    }
                    else if (parser.sval.equals("secondaryIcon")) {
                        stringMode = 2;
                        iconId = 2;
                    }
                    else if (parser.sval.equals("brief")) {
                        stringMode = 3;
                    }
                    else if (parser.sval.equals("help")) {
                        stringMode = 4;
                    }
                    else if (parser.sval.equals("iconTransparency")) {
                        stringMode = 5;
                        iconId = 1;
                    }
                    else if (parser.sval.equals("secondaryIconTransparency")) {
                        stringMode = 5;
                        iconId = 2;
                    }
                    break;
                default:
                    if (parser.ttype == '\"') {
                        switch (stringMode) {
                            case 1: // name
                                item.setName(parser.sval);
                                break;
                            case 2: // icon
                                try {
                                    img = ImagePersistence.importRGBA(
                                        new File(
                                            globalDataPath + "/" + parser.sval));
                                    if ( iconId == 1 ) {
                                        item.setIcon(img);
                                    }
                                    else {
                                        item.setSecondaryIcon(img);
                                    }
                                } catch (Exception e) {
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
                                    String filename;
                                    filename = globalDataPath + "/" +
                                            parser.sval;
                                    mask = ImagePersistence.importRGB(
                                        new File(filename));
                                    if ( iconId == 1 ) {
                                        item.setIconTransparency(mask);
                                    }
                                    else {
                                        item.setSecondaryIconTransparency(mask);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Warning: could not read the image file \"" + parser.sval + "\".");
                                    System.err.println(e);
                                }
                                break;
                            default:
                                break;
                        }
                    } else {
                        // Only supposed to contain '{' or '}'
                        char content = parser.toString().charAt(7);
                        if (content == '{') {
                            if (idString == null) {
                                throw new ExceptionWidgetParseError();
                            }
                        } else if (content == '}') {
                            item.applyTransparency();
                            item.applySecondTransparency();
                            return item;
                        } else {
                            //throw new ExceptionWidgetParseError();
                        }
                    }
                    break;
            }
        } while (tokenType != StreamTokenizer.TT_EOF);

        if (idString == null) {
            throw new ExceptionWidgetBadName();
        }

        item.applyTransparency();
        item.applySecondTransparency();

        return item;
    }

    private static WidgetMenuItem importAquynzaGuiMenuItem(
            StreamTokenizer parser,
            Widget context) throws Exception {
        WidgetMenuItem item;

        item = new WidgetMenuItem(context);

        int tokenType;

        String name = null;

        do {
            try {
                tokenType = parser.nextToken();
            } catch (IOException e) {
                throw e;
            }
            switch (tokenType) {
                case StreamTokenizer.TT_EOL:
                    break;
                case StreamTokenizer.TT_EOF:
                    break;
                case StreamTokenizer.TT_NUMBER:
                    break;
                case StreamTokenizer.TT_WORD:
                    if (parser.sval.equals("MENUITEM")
                            || parser.sval.equals("POPUP")) {
                        parser.pushBack();
                        item.setName(name);
                        return item;
                    }
                    item.addModifier(parser.sval);
                    break;
                default:
                    if (parser.ttype == '\"') {
                        name = parser.sval;
                        item.setName(name);
                    } else {
                        // Only supposed to contain '{' or '}'
                        char content = parser.toString().charAt(7);
                        if (content == '{') {
                            throw new ExceptionWidgetParseError();
                        } else if (content == '}') {
                            parser.pushBack();
                            item.setName(name);
                            return item;
                        } else {
                            //throw new ExceptionWidgetParseError();
                        }
                    }
                    break;
            }
        } while (tokenType != StreamTokenizer.TT_EOF);

        if (name == null) {
            throw new ExceptionWidgetBadName();
        }
        item.setName(name);
        return item;
    }

    private static WidgetMenu importAquynzaGuiMenu(StreamTokenizer parser,
            Widget context) throws Exception {
        WidgetMenu menu;

        menu = new WidgetMenu(context);

        int level = 0;

        int tokenType;

        String name = null;

        do {
            try {
                tokenType = parser.nextToken();
            } catch (IOException e) {
                throw e;
            }
            switch (tokenType) {
                case StreamTokenizer.TT_EOL:
                    break;
                case StreamTokenizer.TT_EOF:
                    break;
                case StreamTokenizer.TT_NUMBER:
                    break;
                case StreamTokenizer.TT_WORD:
                    if (parser.sval.equals("POPUP")) {
                        WidgetMenu popup = importAquynzaGuiMenu(parser, context);
                        context.addPopupMenu(popup);
                        menu.addChild(popup);
                    } else if (parser.sval.equals("MENUITEM")) {
                        WidgetMenuItem item;
                        item = importAquynzaGuiMenuItem(parser, context);
                        menu.addChild(item);
                    }

                    break;
                default:
                    if (parser.ttype == '\"') {
                        name = parser.sval;
                    } else {
                        // Only supposed to contain '{' or '}'
                        char content = parser.toString().charAt(7);
                        if (content == '{') {
                            //System.out.println("{ MARK");
                            level++;
                        } else if (content == '}') {
                            //System.out.println("} MARK");
                            level--;
                            if (level == 0) {
                                tokenType = StreamTokenizer.TT_EOF;
                            }
                        } else {
                            //throw new ExceptionWidgetParseError();
                        }
                    }
                    break;
            }
        } while (tokenType != StreamTokenizer.TT_EOF);

        if (name == null) {
            throw new ExceptionWidgetBadName();
        }
        menu.setName(name);

        return menu;
    }

    /**
    WARNING, pending to check character format

    @param source
    @param globalDataPath
    @return
    @throws Exception
    */
    public static Widget importAquynzaGui(InputStream source,
        String globalDataPath) throws Exception {
        Widget context;

        context = new Widget();

        StreamTokenizer parser = new StreamTokenizer(new InputStreamReader(source));

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

        // First pass process
        do {
            try {
                tokenType = parser.nextToken();
            } catch (IOException e) {
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
                    if (parser.sval.equals("MENU")) {
                        WidgetMenu menubar =
                                importAquynzaGuiMenu(parser, context);
                        context.setMenubar(menubar);
                    } else if (parser.sval.equals("POPUP")) {
                        WidgetMenu popup =
                                importAquynzaGuiMenu(parser, context);
                        context.addPopupMenu(popup);
                    } else if (parser.sval.equals("COMMAND")) {
                        WidgetCommand command =
                                importAquynzaGuiCommand(parser,
                                    globalDataPath);
                        context.addCommand(command);
                    } else if (parser.sval.equals("DIALOG")) {
                        WidgetDialog dialog =
                                importAquynzaGuiDialog(parser, context);
                        context.addDialog(dialog);
                    } else if (parser.sval.equals("VARIABLE")) {
                        WidgetVariable variable =
                                importAquynzaGuiVariable(parser, context);
                        context.addVariable(variable);
                    } else if (parser.sval.equals("BUTTON_GROUP")) {
                        WidgetButtonGroup bg =
                                importAquynzaGuiButtonGroup(parser, context);
                        context.addButtonGroup(bg);
                    } else if (parser.sval.equals("MESSAGES")) {
                        importAquynzaGuiMessages(parser, context);
                    }
                    else if(parser.sval.equals("IMAGE"))
                    {

                    }
                    else {
                        System.out.println("NotProcessedIdentifier " + parser.sval);
                    }
                    break;
                default:
                    if (parser.ttype == '\"') {
                        //System.out.println("STRING " + parser.sval);
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


        // Second pass process
        int i, j;
        ArrayList<WidgetDialog> dl;

        dl = context.getDialogList();
        for (i = 0; i < dl.size(); i++) {
            ArrayList<String> pv = dl.get(i).getPendingVariableNames();
            for (j = 0; j < pv.size(); j++) {
                //dl.get(i).associateVariable(pv.get(j));
            }

            ArrayList<String> pc = dl.get(i).getPendingCommandNames();
            for (j = 0; j < pc.size(); j++) {
                //dl.get(i).associateCommand(pc.get(j));
            }
            ArrayList<String> pd = dl.get(i).getPendingDialogNames();
            for (j = 0; j < pd.size(); j++) {
                //dl.get(i).associateDialog(pd.get(j));
            }
        }

        return context;
    }

    private static WidgetDialog importAquynzaGuiDialog(StreamTokenizer parser,
            Widget context) throws ExceptionWidgetBadName, ExceptionWidgetParseError {
        WidgetDialog dialog;

        dialog = new WidgetDialog();
        int tokenType = 0;
        String idString = null;
        int stringMode = 0;

        do {
            try {
                tokenType = parser.nextToken();
            } catch (IOException ex) {
                try {
                    throw ex;
                } catch (IOException ex1) {
                    ex1.getStackTrace();
                }
            }

            switch (tokenType) {
                case StreamTokenizer.TT_EOL:
                    break;
                case StreamTokenizer.TT_EOF:
                    break;
                case StreamTokenizer.TT_NUMBER:
                    break;
                case StreamTokenizer.TT_WORD:
                    if (idString == null) {
                        idString = parser.sval;
                        dialog.setId(parser.sval);
                    } else if (parser.sval.equals("name")) {
                        stringMode = 1;
                    } else if (parser.sval.equals("variable")) {
                        stringMode = 2;
                    } else if (parser.sval.equals("command")) {
                        stringMode = 3;
                    } else if (parser.sval.equals("DIALOG")) {
                        WidgetDialog dialogS =
                                importAquynzaGuiDialog(parser, context);
                        dialog.getChildren().add(dialogS);
                    } else if (parser.sval.equals("dialogref")) {
                        stringMode = 5;
                    } else if (parser.sval.equals("iconTransparency")) {
                        stringMode = 6;
                    } else if (parser.sval.equals("orientation")) {
                        stringMode = 7;
                    } else if (parser.sval.equals("collapsable")) {
                        stringMode = 8;
                    } else {
                        switch (stringMode) {
                            case 1:
                                dialog.getPendingDialogNames().add(parser.sval);
                                break;
                            case 2: // variables
                                dialog.getPendingVariableNames().add(parser.sval);
                                break;
                            case 3:

                                dialog.getPendingCommandNames().add(parser.sval);
                                break;
                            case 4: // dialog
                                break;
                            case 5:  // dialogref
                                dialog.getPendingDialogRefNames().add(parser.sval);
                                break;
                            case 6:
                                break;
                            case 7: // orientation
                                if (parser.sval.equals("vertical")) {
                                    dialog.setOrientation(WidgetDialog.ORIENTATION_VERTICAL);
                                } else {
                                    dialog.setOrientation(WidgetDialog.ORIENTATION_HORIZONTAL);
                                }

                                break;
                            case 8: // collapsable
                                if (parser.sval.equals("true")) {
                                    dialog.setCollapsable(true);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    break;
                default:
                    if (parser.ttype == '\"') {
                        switch (stringMode) {
                            case 1: // name
                                dialog.setName(parser.sval);
                                break;
                            case 2: // variable
                                dialog.getPendingVariableNames().add(parser.sval);
                                break;
                            case 3: // command
                                dialog.getPendingCommandNames().add(parser.sval);
                                break;
                            case 4: // dialog
                                dialog.getPendingDialogNames().add(parser.sval);
                                break;
                            case 5:  // dialogref
                                dialog.getPendingDialogRefNames().add(parser.sval);
                                break;
                            case 6:
                                break;
                            case 7: // orientation
                                if (parser.sval.equals("vertical")) {
                                    dialog.setOrientation(WidgetDialog.ORIENTATION_VERTICAL);
                                } else {
                                    dialog.setOrientation(WidgetDialog.ORIENTATION_HORIZONTAL);
                                }

                                break;
                            case 8: // collapsable
                                if (parser.sval.equals("true")) {
                                    dialog.setCollapsable(true);
                                }
                                break;
                            default:
                                break;
                        }
                    } else {
                        // Only supposed to contain '{' or '}'
                        char content = parser.toString().charAt(7);
                        if (content == '{') {
                            if (idString == null) {
                                throw new ExceptionWidgetParseError();
                            }
                        } else if (content == '}') {
                            return dialog;
                        } else {
                            //throw new ExceptionWidgetParseError();
                        }
                    }
                    break;
            }
        } while (tokenType != StreamTokenizer.TT_EOF);

        if (idString == null) {
            throw new ExceptionWidgetBadName();
        }

        return dialog;
    }

    private static WidgetVariable importAquynzaGuiVariable(StreamTokenizer parser,
            Widget context) throws ExceptionWidgetBadName, ExceptionWidgetParseError {
        WidgetVariable variable;
        variable = null;
        int tokenType = 0;
        String idString = null;
        int stringMode = 0;

        String typeName = null;
        String rangeName = null;
        String initialvalueName = null;

        do {
            try {
                tokenType = parser.nextToken();
            } catch (IOException ex) {
                try {
                    throw ex;
                } catch (IOException ex1) {
                    ex1.getStackTrace();
                }
            }

            switch (tokenType) {
                case StreamTokenizer.TT_EOL:
                    break;
                case StreamTokenizer.TT_EOF:
                    break;
                case StreamTokenizer.TT_NUMBER:
                    break;
                case StreamTokenizer.TT_WORD:
                    if (idString == null) {
                        idString = parser.sval;
                    } else if (parser.sval.equals("name")) {
                        stringMode = 1;
                    } else if (parser.sval.equals("VARIABLE")) {
                        stringMode = 2;
                    } else if (parser.sval.equals("type")) {
                        stringMode = 3;
                    } else if (parser.sval.equals("range")) {
                        stringMode = 4;
                    } else if (parser.sval.equals("initialValue")) {
                        stringMode = 5;
                    } else if (parser.sval.equals("iconTransparency")) {
                        stringMode = 6;
                    } else {
                        switch (stringMode) {
                            case 1: // name
                                break;
                            case 2: // VARIABLE
                                break;
                            case 3: // type
                                typeName = parser.sval;
                                break;
                            case 4: // range
                                rangeName = parser.sval;
                                break;
                            case 5:  // initialValue
                                initialvalueName = parser.sval;
                                break;
                            case 6: // iconTransparency
                                break;
                            default:
                                break;
                        }
                    }
                    break;
                default:
                    if (parser.ttype == '\"') {
                        switch (stringMode) {
                            case 1: // name
                                break;
                            case 2: // variable
                                break;
                            case 3: // type
                                typeName = parser.sval;
                                break;
                            case 4: // range
                                rangeName = parser.sval;
                                break;
                            case 5: // InitialValues
                                initialvalueName = parser.sval;
                                break;
                            default:
                                break;
                        }
                    } else {
                        // Only supposed to contain '{' or '}'
                        char content = parser.toString().charAt(7);
                        if (content == '{') {
                            if (idString == null) {
                                throw new ExceptionWidgetParseError();
                            }
                        } else if (content == '}') {
                            if (typeName != null && typeName.equalsIgnoreCase("double")) {
                                variable = new WidgetDoubleVariable();
                                variable.setName(idString);
                                variable.setValidRange(rangeName);
                                variable.setInitialvalue(initialvalueName);
                                return variable;
                            } else if (typeName != null && typeName.equalsIgnoreCase("Vector3Dd")) {
                                variable = new WidgetVector3DVariable();
                                variable.setName(idString);
                                variable.setValidRange(rangeName);
                                variable.setInitialvalue(initialvalueName);
                                return variable;
                            } else if (typeName != null && typeName.equalsIgnoreCase("ColorRgb")) {
                                variable = new WidgetColorRgbVariable();
                                variable.setName(idString);
                                variable.setValidRange(rangeName);
                                variable.setInitialvalue(initialvalueName);
                                return variable;
                            } else if (typeName != null && typeName.equalsIgnoreCase("Integer")) {
                                variable = new WidgetIntegerVariable();
                                variable.setName(idString);
                                variable.setValidRange(rangeName);
                                variable.setInitialvalue(initialvalueName);
                                return variable;
                            } else if (typeName != null && typeName.equalsIgnoreCase("boolean")) {
                                variable = new WidgetBooleanVariable();
                                variable.setName(idString);
                                //variable.setValidRange(rangeName);
                                //variable.setInitialvalue(initialvalueName);
                                return variable;
                            } else if (typeName != null && typeName.equalsIgnoreCase("String")) {
                                variable = new WidgetStringVariable();
                                variable.setName(idString);
                                //variable.setValidRange(rangeName);
                                //variable.setInitialvalue(initialvalueName);
                                return variable;
                            }

                        } else {
                            //throw new ExceptionWidgetParseError();
                        }
                    }
                    break;
            }
        } while (tokenType != StreamTokenizer.TT_EOF);

        if (idString == null) {
            throw new ExceptionWidgetBadName();
        }
        return variable;
    }
}
