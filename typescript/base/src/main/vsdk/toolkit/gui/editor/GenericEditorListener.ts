import type { Entity } from "../../common/Entity.js";

/**
Receives notifications from a `GenericEditor` when the user changes a value
of the entity under edition, so the application can repaint or react.
*/
export interface GenericEditorListener {
    /**
    Called after a new value has been validated and written to the entity.
    @param entity the entity that changed
    */
    notifyEntityChanged(entity: Entity): void;
}
