package vsdk.toolkit.io.gui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import vsdk.toolkit.gui.widget.Widget;
import vsdk.toolkit.gui.widget.WidgetDialog;

/** Reads the shared, ordered dialog definitions without toolkit dependencies. */
public final class DialogPersistence {
    private static final ObjectMapper JSON = new ObjectMapper();
    private DialogPersistence() { }

    public static void importDialogs(InputStream source, Widget widget) throws Exception {
        JsonNode root = JSON.readTree(source);
        if (root == null || root.path("schemaVersion").asInt(-1) != 1)
            throw new IllegalArgumentException("dialogs.json: unsupported schemaVersion");
        JsonNode dialogs = root.path("dialogs");
        if (!dialogs.isArray()) throw new IllegalArgumentException("dialogs.json: dialogs must be an array");
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < dialogs.size(); i++) {
            String path = "dialogs[" + i + "]";
            JsonNode node = dialogs.get(i);
            String id = required(node, "id", path);
            if (!ids.add(id) || widget.getDialogById(id) != null)
                throw new IllegalArgumentException(path + ": duplicate dialog id " + id);
            WidgetDialog dialog = new WidgetDialog();
            dialog.setId(id);
            dialog.setTitleKey(required(node, "titleKey", path));
            dialog.setModal(node.path("modal").asBoolean(false));
            JsonNode content = node.path("content");
            if (!content.isArray()) throw new IllegalArgumentException(path + ".content: expected array");
            Set<String> elementIds = new HashSet<String>();
            for (int j = 0; j < content.size(); j++)
                dialog.addElement(element(content.get(j), path + ".content[" + j + "]", elementIds));
            widget.addDialog(dialog);
        }
    }

    private static WidgetDialog.Element element(JsonNode node, String path, Set<String> ids) {
        String kind = required(node, "kind", path);
        if (!Set.of("column", "row", "textInput", "button", "tree", "action",
                    "fileChooser", "fileFilter").contains(kind))
            throw new IllegalArgumentException(path + ": unsupported kind " + kind);
        String id = optional(node, "id");
        if (id != null && !ids.add(id))
            throw new IllegalArgumentException(path + ": duplicate element id " + id);
        String binding = optional(node, "binding");
        String action = optional(node, "action");
        if (Set.of("textInput", "tree", "fileFilter").contains(kind) && binding == null)
            throw new IllegalArgumentException(path + ": missing binding");
        if (Set.of("button", "action").contains(kind) && action == null)
            throw new IllegalArgumentException(path + ": missing action");
        List<WidgetDialog.Element> children = new ArrayList<WidgetDialog.Element>();
        JsonNode childNodes = node.path("children");
        if (childNodes.isArray()) for (int i = 0; i < childNodes.size(); i++)
            children.add(element(childNodes.get(i), path + ".children[" + i + "]", ids));
        return new WidgetDialog.Element(kind, id, optional(node, "labelKey"), binding,
                                        action, children);
    }

    private static String required(JsonNode node, String field, String path) {
        String value = optional(node, field);
        if (value == null) throw new IllegalArgumentException(path + ": missing " + field);
        return value;
    }
    private static String optional(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) return null;
        return value.asText();
    }
}
