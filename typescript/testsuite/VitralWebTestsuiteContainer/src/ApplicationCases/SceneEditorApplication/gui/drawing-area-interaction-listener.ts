import type { PointerCursor } from './pointer-cursor';

/**
 * Port of `gui.DrawingAreaInteractionListener`.
 *
 * Receives the requests that `DrawingAreaInteractionTechniques` derives from
 * user interaction, so the GUI technology in use can present them (pointer
 * shape, messages, dialogs...).
 */
export interface DrawingAreaInteractionListener {
  /**
   * @param cursor the pointer shape to present over the drawing area
   */
  cursorRequested(cursor: PointerCursor): void;

  /**
   * The pointer must be placed at a position (infinite drag of a gizmo).
   * @param surfaceX horizontal position, in pixels of the viewport set area
   * @param surfaceY vertical position, in pixels of the viewport set area
   */
  cursorWarpRequested(surfaceX: number, surfaceY: number): void;

  /**
   * The drawing area content changed and must be drawn again.
   */
  repaintRequested(): void;

  /**
   * The drawing area must be drawn again, but only cameras or overlays
   * (gizmos, highlights, selected viewport) changed: the content of the scene
   * did not, so images computed from it for the same cameras (i.e. raytraced
   * ones) can be presented again.
   */
  viewRepaintRequested(): void;

  /**
   * A continuous gesture of the pointer over the drawing area started (it was
   * dragged) or ended (it was released). While it is in course, expensive
   * render modes may present approximated frames.
   * @param inCourse true when it starts, false when it ends
   */
  interactiveGestureChanged(inCourse: boolean): void;

  /**
   * @param message text for the status message of the application
   */
  statusMessageRequested(message: string): void;

  /**
   * The selection of things changed: panels showing the selected target should
   * be updated.
   */
  selectionChanged(): void;

  /**
   * A raytraced image of the scene was requested.
   */
  raytracingRequested(): void;

  /**
   * The object selector dialog was requested.
   */
  selectorDialogRequested(): void;

  /**
   * The user requested to close the application.
   */
  closeRequested(): void;

  /**
   * The user requested to toggle the full screen GUI mode.
   */
  fullScreenGuiToggleRequested(): void;

  /**
   * An animation started (i.e. the camera turning to a view chosen with the
   * camera control gizmo): `frame` must be called periodically (some tens of
   * times per second), drawing the drawing area again after each call, while
   * it returns true. A new request replaces the one in course.
   * @param frame advances the animation to the current time; returns false
   * when it ended (Java's `BooleanSupplier`)
   */
  animationRequested(frame: () => boolean): void;
}
