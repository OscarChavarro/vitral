/**
 * Port of `gui.PointerCursor`.
 *
 * Kinds of pointer shapes the drawing area asks the GUI technology to present.
 * Each one names the image that presents it (in `IMAGE_FOLDER`), shared by all
 * the GUI technologies, or none for the default pointer of the system. Java's
 * enum constants are the instances below, in declaration order.
 */
export class PointerCursor {
  /** Folder with the images of the pointers */
  static readonly IMAGE_FOLDER = './etc/cursors/';

  /** Normal pointer for selection */
  static readonly SELECT = new PointerCursor('SELECT', null, 'Select');
  /** Normal pointer over the title of a viewport (feedback that it can be clicked) */
  static readonly VIEWPORT_TITLE = new PointerCursor('VIEWPORT_TITLE', null, 'ViewportTitle');
  /** Camera rotation gesture */
  static readonly CAMERA_ROTATE = new PointerCursor('CAMERA_ROTATE', 'cursor_camrotate.gif', 'CameraRotation');
  /** Camera translation gesture */
  static readonly CAMERA_TRANSLATE = new PointerCursor('CAMERA_TRANSLATE', 'cursor_camtranslate.gif', 'CameraTranslation');
  /** Camera advance gesture */
  static readonly CAMERA_ADVANCE = new PointerCursor('CAMERA_ADVANCE', 'cursor_camadvance.gif', 'CameraAdvance');
  /** Selected things can be translated (translation mode with a selection) */
  static readonly TRANSLATE = new PointerCursor('TRANSLATE', 'cursor_translate.png', 'Translation');
  /** Selected things can be rotated (rotation mode with a selection) */
  static readonly ROTATE = new PointerCursor('ROTATE', 'cursor_rotate.png', 'Rotation');
  /** Selected things can be scaled (scale mode with a selection) */
  static readonly SCALE = new PointerCursor('SCALE', 'cursor_scale.png', 'Scale');

  /** Java's `PointerCursor.values()` */
  static values(): PointerCursor[] {
    return [
      PointerCursor.SELECT,
      PointerCursor.VIEWPORT_TITLE,
      PointerCursor.CAMERA_ROTATE,
      PointerCursor.CAMERA_TRANSLATE,
      PointerCursor.CAMERA_ADVANCE,
      PointerCursor.TRANSLATE,
      PointerCursor.ROTATE,
      PointerCursor.SCALE,
    ];
  }

  private constructor(
    private readonly constantName: string,
    private readonly imageFileName: string | null,
    private readonly displayName: string,
  ) {}

  /**
   * @return Java's `Enum.name()`
   */
  name(): string {
    return this.constantName;
  }

  /**
   * @return path of the image of this pointer (hot spot at its center), or
   * null if the default pointer of the system is used
   */
  getImagePath(): string | null {
    return this.imageFileName === null ? null : PointerCursor.IMAGE_FOLDER + this.imageFileName;
  }

  /**
   * @return name of the pointer, for GUI technologies that name their cursors
   */
  getDisplayName(): string {
    return this.displayName;
  }
}
