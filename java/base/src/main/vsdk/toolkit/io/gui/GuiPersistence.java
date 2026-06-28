package vsdk.toolkit.io.gui;

// Java basic classes
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

// External libraries
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// Vitral classes
import vsdk.toolkit.gui.widget.ExceptionWidgetBadName;
import vsdk.toolkit.gui.widget.Widget;
import vsdk.toolkit.gui.widget.WidgetButtonGroup;
import vsdk.toolkit.gui.widget.WidgetCommand;
import vsdk.toolkit.gui.widget.WidgetDialog;
import vsdk.toolkit.gui.widget.WidgetMenu;
import vsdk.toolkit.gui.widget.WidgetMenuElement;
import vsdk.toolkit.gui.widget.WidgetMenuItem;
import vsdk.toolkit.gui.widget.variable.WidgetBooleanVariable;
import vsdk.toolkit.gui.widget.variable.WidgetColorRgbVariable;
import vsdk.toolkit.gui.widget.variable.WidgetDoubleVariable;
import vsdk.toolkit.gui.widget.variable.WidgetIntegerVariable;
import vsdk.toolkit.gui.widget.variable.WidgetStringVariable;
import vsdk.toolkit.gui.widget.variable.WidgetVariable;
import vsdk.toolkit.gui.widget.variable.WidgetVector3DVariable;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBAImageUncompressed;

public class GuiPersistence extends PersistenceElement {
    private static final ObjectMapper JSON = new ObjectMapper();

    /**
    Imports the Vitral widget GUI model from the modern JSON representation.

    The public method name is preserved because older applications already call
    this entry point; only the on-disk format changed from Aquynza .gui text to
    JSON.
    */
    public static Widget importAquynzaGui(
        InputStream source,
        String globalDataPath) throws Exception {
        JsonNode root = JSON.readTree(source);
        Widget context = new Widget();

        importCommands(root.path("commands"), context, globalDataPath);
        importVariables(root.path("variables"), context);
        importMenubar(root.path("menubar"), context);
        importPopups(root.path("popups"), context);
        importButtonGroups(root.path("buttonGroups"), context);
        importDialogs(root.path("dialogs"), context);
        importMessages(root.path("messages"), context);

        return context;
    }

    private static void importCommands(
        JsonNode commandNodes,
        Widget context,
        String globalDataPath) throws Exception {
        if (!commandNodes.isArray()) {
            return;
        }

        for (JsonNode commandNode : commandNodes) {
            WidgetCommand command = new WidgetCommand();
            command.setId(requiredText(commandNode, "id"));
            command.setName(text(commandNode, "name"));
            command.setBrief(text(commandNode, "brief"));

            JsonNode help = commandNode.get("help");
            if (help != null) {
                if (help.isArray()) {
                    for (JsonNode line : help) {
                        command.appendToHelp(line.asText());
                    }
                }
                else {
                    command.setHelp(help.asText());
                }
            }

            loadCommandImage(command, commandNode, "icon", globalDataPath, true, false);
            loadCommandImage(command, commandNode, "secondaryIcon", globalDataPath, false, false);
            loadCommandImage(command, commandNode, "iconTransparency", globalDataPath, true, true);
            loadCommandImage(command, commandNode, "secondaryIconTransparency", globalDataPath, false, true);
            command.applyTransparency();
            command.applySecondTransparency();

            context.addCommand(command);
        }
    }

    private static void loadCommandImage(
        WidgetCommand command,
        JsonNode commandNode,
        String fieldName,
        String globalDataPath,
        boolean primary,
        boolean transparency) {
        String filename = text(commandNode, fieldName);
        if (filename == null) {
            return;
        }

        try {
            File file = new File(globalDataPath + "/" + filename);
            if (transparency) {
                RGBImageUncompressed mask = ImagePersistence.importRGB(file);
                if (primary) {
                    command.setIconTransparency(mask);
                }
                else {
                    command.setSecondaryIconTransparency(mask);
                }
            }
            else {
                RGBAImageUncompressed image = ImagePersistence.importRGBA(file);
                if (primary) {
                    command.setIcon(image);
                }
                else {
                    command.setSecondaryIcon(image);
                }
            }
        }
        catch (Exception e) {
            System.err.println("Warning: could not read the image file \"" + filename + "\".");
            System.err.println(e);
        }
    }

    private static void importVariables(JsonNode variableNodes, Widget context)
        throws ExceptionWidgetBadName {
        if (!variableNodes.isArray()) {
            return;
        }

        for (JsonNode variableNode : variableNodes) {
            WidgetVariable variable = buildVariable(variableNode);
            if (variable != null) {
                context.addVariable(variable);
            }
        }
    }

    private static WidgetVariable buildVariable(JsonNode variableNode)
        throws ExceptionWidgetBadName {
        String id = requiredText(variableNode, "id");
        String type = text(variableNode, "type");
        String range = text(variableNode, "range");
        String initialValue = text(variableNode, "initialValue");

        WidgetVariable variable = null;
        if (type != null && type.equalsIgnoreCase("double")) {
            variable = new WidgetDoubleVariable();
        }
        else if (type != null &&
                 (type.equalsIgnoreCase("Vector3D") ||
                  type.equalsIgnoreCase("Vector3Dd"))) {
            variable = new WidgetVector3DVariable();
        }
        else if (type != null && type.equalsIgnoreCase("ColorRgb")) {
            variable = new WidgetColorRgbVariable();
        }
        else if (type != null && type.equalsIgnoreCase("Integer")) {
            variable = new WidgetIntegerVariable();
        }
        else if (type != null && type.equalsIgnoreCase("boolean")) {
            variable = new WidgetBooleanVariable();
        }
        else if (type != null && type.equalsIgnoreCase("String")) {
            variable = new WidgetStringVariable();
        }

        if (variable == null) {
            return null;
        }

        variable.setName(id);
        if (range != null) {
            variable.setValidRange(range);
        }
        if (initialValue != null) {
            variable.setInitialvalue(initialValue);
        }
        return variable;
    }

    private static void importMenubar(JsonNode menubarNode, Widget context)
        throws ExceptionWidgetBadName {
        if (menubarNode == null || menubarNode.isMissingNode() || menubarNode.isNull()) {
            return;
        }
        context.setMenubar(buildMenu(menubarNode, context, true));
    }

    private static void importPopups(JsonNode popupNodes, Widget context)
        throws ExceptionWidgetBadName {
        if (!popupNodes.isArray()) {
            return;
        }

        for (JsonNode popupNode : popupNodes) {
            context.addPopupMenu(buildMenu(popupNode, context, true));
        }
    }

    private static WidgetMenu buildMenu(
        JsonNode menuNode,
        Widget context,
        boolean registerChildPopups) throws ExceptionWidgetBadName {
        WidgetMenu menu = new WidgetMenu(context);
        menu.setName(requiredText(menuNode, "name"));

        JsonNode children = menuNode.path("children");
        if (children.isArray()) {
            for (JsonNode child : children) {
                String type = text(child, "type");
                if ("menu".equals(type)) {
                    WidgetMenu childMenu = buildMenu(child, context, registerChildPopups);
                    if (registerChildPopups) {
                        context.addPopupMenu(childMenu);
                    }
                    menu.addChild(childMenu);
                }
                else if ("item".equals(type)) {
                    menu.addChild(buildMenuItem(child, context));
                }
            }
        }

        return menu;
    }

    private static WidgetMenuElement buildMenuItem(JsonNode itemNode, Widget context) {
        WidgetMenuItem item = new WidgetMenuItem(context);
        String name = text(itemNode, "name");
        if (name != null) {
            item.setName(name);
        }

        JsonNode modifiers = itemNode.path("modifiers");
        if (modifiers.isArray()) {
            for (JsonNode modifier : modifiers) {
                item.addModifier(modifier.asText());
            }
        }

        return item;
    }

    private static void importButtonGroups(JsonNode buttonGroupNodes, Widget context)
        throws ExceptionWidgetBadName {
        if (!buttonGroupNodes.isArray()) {
            return;
        }

        for (JsonNode buttonGroupNode : buttonGroupNodes) {
            WidgetButtonGroup group = new WidgetButtonGroup(context);
            group.setName(requiredText(buttonGroupNode, "name"));
            group.setShowIcons(booleanValue(buttonGroupNode, "showIcons", false));
            group.setShowText(booleanValue(buttonGroupNode, "showText", false));
            group.setTitle(booleanValue(buttonGroupNode, "showTitle", false));

            String direction = text(buttonGroupNode, "direction");
            if ("horizontal".equals(direction)) {
                group.setDirection(WidgetButtonGroup.HORIZONTAL);
            }
            else {
                group.setDirection(WidgetButtonGroup.VERTICAL);
            }

            JsonNode commands = buttonGroupNode.path("commands");
            if (commands.isArray()) {
                for (JsonNode command : commands) {
                    group.addCommandByName(command.asText());
                }
            }

            context.addButtonGroup(group);
        }
    }

    private static void importDialogs(JsonNode dialogNodes, Widget context)
        throws ExceptionWidgetBadName {
        if (!dialogNodes.isArray()) {
            return;
        }

        for (JsonNode dialogNode : dialogNodes) {
            context.addDialog(buildDialog(dialogNode, context));
        }
    }

    private static WidgetDialog buildDialog(JsonNode dialogNode, Widget context)
        throws ExceptionWidgetBadName {
        WidgetDialog dialog = new WidgetDialog();
        dialog.setId(requiredText(dialogNode, "id"));
        dialog.setName(text(dialogNode, "name"));
        dialog.setCollapsable(booleanValue(dialogNode, "collapsable", false));

        String orientation = text(dialogNode, "orientation");
        if ("horizontal".equals(orientation)) {
            dialog.setOrientation(WidgetDialog.ORIENTATION_HORIZONTAL);
        }
        else if ("vertical".equals(orientation)) {
            dialog.setOrientation(WidgetDialog.ORIENTATION_VERTICAL);
        }

        addTextArray(dialogNode.path("variables"), dialog.getPendingVariableNames());
        addTextArray(dialogNode.path("commands"), dialog.getPendingCommandNames());
        addTextArray(dialogNode.path("dialogRefs"), dialog.getPendingDialogRefNames());

        JsonNode children = dialogNode.path("dialogs");
        if (children.isArray()) {
            for (JsonNode child : children) {
                dialog.getChildren().add(buildDialog(child, context));
            }
        }

        return dialog;
    }

    private static void addTextArray(JsonNode source, java.util.ArrayList<String> destination) {
        if (!source.isArray()) {
            return;
        }
        for (JsonNode value : source) {
            destination.add(value.asText());
        }
    }

    private static void importMessages(JsonNode messagesNode, Widget context) {
        if (!messagesNode.isObject()) {
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = messagesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            context.addMessage(field.getKey(), field.getValue().asText());
        }
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static String requiredText(JsonNode node, String fieldName)
        throws ExceptionWidgetBadName {
        String value = text(node, fieldName);
        if (value == null || value.length() == 0) {
            throw new ExceptionWidgetBadName();
        }
        return value;
    }

    private static boolean booleanValue(JsonNode node, String fieldName, boolean defaultValue) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String text = value.asText();
        return "true".equalsIgnoreCase(text) || "on".equalsIgnoreCase(text);
    }
}
