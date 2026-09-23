package vsdk.toolkit.gui.editor;

// VSDK classes
import vsdk.toolkit.common.Entity;

/**
Receives notifications from a `GenericEditor` when the user changes a value
of the entity under edition, so the application can repaint or react.
*/
public interface GenericEditorListener
{
    /**
    Called after a new value has been validated and written to the entity.
    @param entity the entity that changed
    */
    void notifyEntityChanged(Entity entity);
}
