package vsdk.toolkit.gui.editor;

// Java basic classes
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

// VSDK classes
import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.EntityEvent;
import vsdk.toolkit.common.EntityListener;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;

/**
Builds an editor for any `Entity` from its control specifications (see
`Entity.getControlSpecifications()` and `ControlSpecification`), without
code specific to the entity class.

This class holds all the logic independent of GUI technology: it parses the
specifications, reads and writes values through the entity accessors
(reflection in Java), validates the intervals and orchestrates the
construction. Each GUI technology (Awt, Qt, Web...) provides a subclass that
only implements the presentation hooks: `beginBuild`, `addControl`,
`endBuild`, `showValidationMessage`, `setControlValue` and `clearControls`.

The editor subscribes to the entity under edition: when the entity emits
`UPDATED` the controls are refreshed, and when it emits `DELETED` the
controls are removed and an "Entity deleted" message is shown.
*/
public abstract class GenericEditor implements EntityListener
{
    protected Entity entity;
    protected final List<ControlSpecification> specifications;
    private GenericEditorListener listener;
    private boolean entityDeleted;

    protected GenericEditor()
    {
        entity = null;
        specifications = new ArrayList<ControlSpecification>();
        listener = null;
        entityDeleted = false;
    }

    /**
    @param listener object notified after each accepted change, may be null
    */
    public void setListener(GenericEditorListener listener)
    {
        this.listener = listener;
    }

    /**
    Builds the editor for the given entity: one control per supported control
    specification, or a message when there is nothing to edit.
    @param entity the entity to edit
    */
    public final void build(Entity entity)
    {
        detach();
        this.entity = entity;
        specifications.clear();

        if ( entity == null ) {
            Logger.reportMessage(this, VSDK.WARNING, "GenericEditor.build",
                "Null entity received, building an empty editor.");
            beginBuild("");
            showValidationMessage("No object to edit.");
            endBuild();
            return;
        }

        entity.addEntityListener(this);
        beginBuild(entity.getClass().getSimpleName());

        for ( String text : entity.getControlSpecifications() ) {
            ControlSpecification specification;
            specification = ControlSpecification.parse(text);
            if ( specification == null ) {
                continue;
            }
            if ( !typeIsSupported(specification.getType()) ) {
                Logger.reportMessage(this, VSDK.WARNING, "GenericEditor.build",
                    "Unsupported type \"" + specification.getType() +
                    "\" in control specification \"" + text + "\" of class " +
                    entity.getClass().getName() + ", control skipped.");
                continue;
            }
            String value = readValue(specification);
            if ( value == null ) {
                continue;
            }
            specifications.add(specification);
            addControl(specification, value);
        }

        if ( specifications.isEmpty() ) {
            showValidationMessage("No editable attributes for " +
                entity.getClass().getName());
        }
        else {
            showValidationMessage(null);
        }
        endBuild();
    }

    /**
    Stops listening to the entity under edition, if any. Call it when the
    editor is hidden or reused for another entity.
    */
    public void detach()
    {
        if ( entity != null ) {
            entity.removeEntityListener(this);
        }
        entity = null;
        entityDeleted = false;
    }

    /**
    @return true if the last entity under edition emitted `DELETED` and the
    editor is showing the "Entity deleted" message
    */
    public boolean isEntityDeleted()
    {
        return entityDeleted;
    }

    /**
    Reacts to the events of the entity under edition.
    @param event the event emitted by the entity
    */
    @Override
    public void notifyEntityEvent(EntityEvent event)
    {
        if ( event == null || event.getSource() != entity ) {
            return;
        }
        if ( event.getType() == EntityEvent.Type.DELETED ) {
            String className = entity.getClass().getSimpleName();
            entity.removeEntityListener(this);
            entity = null;
            specifications.clear();
            entityDeleted = true;
            clearControls("Entity deleted (" + className + ").");
        }
        else if ( event.getType() == EntityEvent.Type.UPDATED ) {
            for ( ControlSpecification specification : specifications ) {
                String value = readValue(specification);
                if ( value != null ) {
                    setControlValue(specification, value);
                }
            }
        }
    }

    /**
    @return the entity under edition, or null
    */
    public Entity getEntity()
    {
        return entity;
    }

    /**
    Reads the current value of an attribute through its getter.
    @param specification the attribute to read
    @return the value as text, or null if the entity has no usable getter
    */
    public String readValue(ControlSpecification specification)
    {
        Method getter = findGetter(specification);
        if ( getter == null ) {
            return null;
        }
        try {
            return String.valueOf(getter.invoke(entity));
        }
        catch ( IllegalAccessException | InvocationTargetException e ) {
            Logger.reportMessage(this, VSDK.WARNING, "GenericEditor.readValue",
                "Cannot call " + getter + ": " + e);
            return null;
        }
    }

    /**
    Validates a value typed by the user and, if valid, writes it to the
    entity through its setter and notifies the listener. On failure, the
    reason is presented with `showValidationMessage`.
    @param specification the attribute to change
    @param text the new value as typed by the user
    @return true if the value was accepted and written
    */
    public boolean updateValue(ControlSpecification specification, String text)
    {
        if ( entity == null || specification == null || text == null ) {
            return false;
        }

        Object value;
        try {
            value = parseValue(specification.getType(), text.trim());
        }
        catch ( NumberFormatException e ) {
            showValidationMessage(specification.getLabel() + ": \"" + text +
                "\" is not a valid " + specification.getType() + ".");
            return false;
        }

        double numeric = ((Number)value).doubleValue();
        if ( !specification.contains(numeric) ) {
            showValidationMessage(specification.getLabel() +
                " must be in " + specification.getIntervalText() + ".");
            return false;
        }

        Method setter = findSetter(specification);
        if ( setter == null ) {
            showValidationMessage(specification.getLabel() +
                " cannot be changed.");
            return false;
        }
        try {
            setter.invoke(entity, value);
        }
        catch ( IllegalAccessException | InvocationTargetException e ) {
            Logger.reportMessage(this, VSDK.WARNING,
                "GenericEditor.updateValue", "Cannot call " + setter + ": " + e);
            showValidationMessage(specification.getLabel() +
                " could not be changed.");
            return false;
        }

        showValidationMessage(null);
        Entity changed = entity;
        changed.update();
        if ( listener != null ) {
            listener.notifyEntityChanged(changed);
        }
        return true;
    }

    /**
    Starts the presentation of a new editor, discarding any previous one.
    @param title name of the edited entity class
    */
    protected abstract void beginBuild(String title);

    /**
    Adds the control for one attribute. When the user confirms a new value,
    the subclass must call `updateValue(specification, text)`.
    @param specification the attribute presented by the control
    @param value current value as text
    */
    protected abstract void addControl(ControlSpecification specification,
        String value);

    /**
    Finishes the presentation of the editor.
    */
    protected abstract void endBuild();

    /**
    Presents a message to the user about the last edition.
    @param message message to show, or null to clear it
    */
    protected abstract void showValidationMessage(String message);

    /**
    Shows a new value in the control of one attribute, after the entity
    emitted `UPDATED`.
    @param specification the attribute whose control must change
    @param value current value as text
    */
    protected abstract void setControlValue(ControlSpecification specification,
        String value);

    /**
    Removes all the controls and presents only a message, used when the
    entity under edition is deleted.
    @param message message to show in place of the controls
    */
    protected abstract void clearControls(String message);

    private static boolean typeIsSupported(String type)
    {
        return accessorType(type) != null;
    }

    private static Class<?> accessorType(String type)
    {
        switch ( type ) {
            case "double":
                return double.class;
            case "float":
                return float.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            default:
                return null;
        }
    }

    private static Object parseValue(String type, String text)
    {
        switch ( type ) {
            case "double":
                return Double.parseDouble(text);
            case "float":
                return Float.parseFloat(text);
            case "int":
                return Integer.parseInt(text);
            case "long":
                return Long.parseLong(text);
            default:
                throw new NumberFormatException("Unsupported type " + type);
        }
    }

    private Method findGetter(ControlSpecification specification)
    {
        String methodName = "get" + specification.getLabel();
        try {
            Method getter = entity.getClass().getMethod(methodName);
            if ( getter.getReturnType() != accessorType(specification.getType()) ) {
                reportMissingAccessor(specification, methodName + "() returning " +
                    specification.getType());
                return null;
            }
            return getter;
        }
        catch ( NoSuchMethodException e ) {
            reportMissingAccessor(specification, methodName + "()");
            return null;
        }
    }

    private Method findSetter(ControlSpecification specification)
    {
        String methodName = "set" + specification.getLabel();
        try {
            return entity.getClass().getMethod(methodName,
                accessorType(specification.getType()));
        }
        catch ( NoSuchMethodException e ) {
            reportMissingAccessor(specification, methodName + "(" +
                specification.getType() + ")");
            return null;
        }
    }

    private void reportMissingAccessor(ControlSpecification specification,
        String accessor)
    {
        Logger.reportMessage(this, VSDK.WARNING, "GenericEditor",
            "Class " + entity.getClass().getName() + " declares control \"" +
            specification.getName() + "\" but has no public " + accessor +
            "; add it or fix the control specification.");
    }

}
