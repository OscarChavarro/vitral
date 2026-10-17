package vsdk.toolkit.gui.dialog.runtime;

import java.util.HashMap;
import java.util.Map;
import vsdk.toolkit.gui.widget.WidgetDialog;

/** Per-opening values and lifetime for a dialog definition. */
public final class DialogSession {
    private final WidgetDialog definition;
    private final Map<String, String> drafts = new HashMap<String, String>();
    private boolean open = true;
    private boolean busy;
    private long generation;

    public DialogSession(WidgetDialog definition) {
        if (definition == null) throw new IllegalArgumentException("Missing dialog definition");
        this.definition = definition;
    }
    public WidgetDialog getDefinition() { return definition; }
    public void setDraft(String binding, String value) { drafts.put(binding, value); }
    public String getDraft(String binding) { return drafts.get(binding); }
    public boolean isOpen() { return open; }
    public boolean isBusy() { return busy; }
    public long beginAction() {
        if (!open || busy) return -1;
        busy = true;
        return ++generation;
    }
    public boolean completeAction(long request) {
        if (!open || !busy || request != generation) return false;
        busy = false;
        return true;
    }
    public void close() { open = false; busy = false; ++generation; }
}
