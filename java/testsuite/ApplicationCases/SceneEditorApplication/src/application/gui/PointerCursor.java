package application.gui;

/**
Kinds of pointer shapes the drawing area asks the GUI technology to present.
*/
public enum PointerCursor
{
    /** Normal pointer for selection */
    SELECT,
    /** Normal pointer over the title of a viewport (feedback that it can be clicked) */
    VIEWPORT_TITLE,
    /** Camera rotation gesture */
    CAMERA_ROTATE,
    /** Camera translation gesture */
    CAMERA_TRANSLATE,
    /** Camera advance gesture */
    CAMERA_ADVANCE,
    /** Selected things can be translated (translation mode with a selection) */
    TRANSLATE,
    /** Selected things can be rotated (rotation mode with a selection) */
    ROTATE,
    /** Selected things can be scaled (scale mode with a selection) */
    SCALE
}
