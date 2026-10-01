import type { RGBImageUncompressed } from '@vitral/base';
import type { BodyEditFeedbackProvider } from './body-edit-feedback-provider';

/**
 * Port of `render.DrawingAreaHost`.
 *
 * Services that the drawing area renderers need from the GUI technology
 * hosting the drawing surface, so rendering classes do not depend on it.
 * `raytraceImage` is asynchronous: the raytracing runs in Web Workers.
 */
export interface DrawingAreaHost {
  /**
   * Called at the beginning of each frame, so the host can update what depends
   * on the component presenting the surface (size, screen resolution).
   */
  beforeFrame(): void;

  /**
   * @return true if the GUI is in full screen mode
   */
  isFullScreenGuiMode(): boolean;

  /**
   * @return the editor of the selected body (which presents feedback over
   * it), or null if there is none
   */
  getBodyEditFeedbackProvider(): BodyEditFeedbackProvider | null;

  /**
   * Computes the raytraced image of the scene for a viewport in CPU render
   * mode, leaving it in the application model.
   */
  raytraceImage(): Promise<void>;

  /**
   * @param image an image obtained from the renderer, to be presented to the
   * user
   */
  showImage(image: RGBImageUncompressed | null): void;

  /**
   * @param message text for the status message of the application
   */
  showStatusMessage(message: string): void;
}
