package gui;

/**
Kinds of pointer shapes the drawing area asks the GUI technology to present.
Each one names the image that presents it (in `IMAGE_FOLDER`), shared by all
the GUI technologies, or none for the default pointer of the system.
*/
public enum PointerCursor
{
    /** Normal pointer for selection */
    SELECT(null, "Select"),
    /** Normal pointer over the title of a viewport (feedback that it can be clicked) */
    VIEWPORT_TITLE(null, "ViewportTitle"),
    /** Camera rotation gesture */
    CAMERA_ROTATE("cursor_camrotate.gif", "CameraRotation"),
    /** Camera translation gesture */
    CAMERA_TRANSLATE("cursor_camtranslate.gif", "CameraTranslation"),
    /** Camera advance gesture */
    CAMERA_ADVANCE("cursor_camadvance.gif", "CameraAdvance"),
    /** Selected things can be translated (translation mode with a selection) */
    TRANSLATE("cursor_translate.png", "Translation"),
    /** Selected things can be rotated (rotation mode with a selection) */
    ROTATE("cursor_rotate.png", "Rotation"),
    /** Selected things can be scaled (scale mode with a selection) */
    SCALE("cursor_scale.png", "Scale");

    /** Folder with the images of the pointers */
    public static final String IMAGE_FOLDER = "./etc/cursors/";

    private final String imageFileName;
    private final String displayName;

    PointerCursor(String imageFileName, String displayName)
    {
        this.imageFileName = imageFileName;
        this.displayName = displayName;
    }

    /**
    @return path of the image of this pointer (hot spot at its center), or
    null if the default pointer of the system is used
    */
    public String getImagePath()
    {
        return imageFileName == null ? null : IMAGE_FOLDER + imageFileName;
    }

    /**
    @return name of the pointer, for GUI technologies that name their cursors
    */
    public String getDisplayName()
    {
        return displayName;
    }
}
