package vsdk.toolkit.gui.dialog.components;

import java.util.Locale;

/** File suffix policy shared by dialog hosts, independent of file APIs. */
public final class DialogFileFilter {
    private final String suffix;
    private final String description;
    public DialogFileFilter(String suffix, String description) {
        this.suffix = suffix.toLowerCase(Locale.ROOT);
        this.description = description;
    }
    public boolean accepts(String path, boolean directory) {
        if (directory) return true;
        int dot = path.lastIndexOf('.');
        return dot > 0 && dot < path.length() - 1 &&
            suffix.equals(path.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
    public String description() { return description + " (*." + suffix + ")"; }
}
