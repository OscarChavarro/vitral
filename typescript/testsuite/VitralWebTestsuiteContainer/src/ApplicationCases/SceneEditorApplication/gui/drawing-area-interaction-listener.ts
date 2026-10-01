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
}
