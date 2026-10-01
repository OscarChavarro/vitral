import type { ApplicationModel } from '../../model/application-model';
import type { HtmlApplicationModel } from './html-application-model';

/**
 * Port of `gui.awt.AwtApplicationHost`.
 *
 * Services that the HTML GUI classes need from the application hosting
 * them. It keeps those classes independent of the rendering technology used
 * by the application (see `application/html-webgl-scene-editor-application.ts`).
 * Raytracing runs in Web Workers, so its operations are asynchronous.
 */
export interface HtmlApplicationHost {
  /**
   * @return technology independent model of the application
   */
  getApplicationModel(): ApplicationModel;

  /**
   * @return model of the HTML GUI
   */
  getHtmlModel(): HtmlApplicationModel;

  /**
   * Requests a new frame in the drawing area.
   */
  repaintDrawingArea(): void;

  /**
   * Tells the modify panel which is the body currently selected.
   */
  reportTargetToModifyPanel(): void;

  /**
   * Ray traces the scene into the raytraced image of the application model,
   * reporting it in the console and exporting it to a file.
   */
  doRaytracingImage(): Promise<void>;

  /**
   * Ray traces the scene into the raytraced image of the application model,
   * silently: used each frame by viewports in CPU render mode.
   */
  doViewportRaytracingImage(): Promise<void>;

  /**
   * Ends the application.
   */
  closeApplication(): void;

  /**
   * Destroys the main window of the GUI.
   */
  destroyGUI(): void;

  /**
   * Creates the main window of the GUI.
   */
  createGUI(): Promise<void>;
}
