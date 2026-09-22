package model;

/**
Interaction modes of the drawing area: what the mouse and keyboard do over
the scene.
*/
public enum InteractionMode
{
    /** Mouse drags change the camera of the viewport */
    CAMERA,
    /** Mouse clicks select objects (as in 3ds Max, no gizmo is shown) */
    SELECT,
    /** Selected things are moved with the translation gizmo */
    TRANSLATE,
    /** Selected body is rotated with the rotation gizmo */
    ROTATE,
    /** Selected body is scaled with the scale gizmo */
    SCALE
}
