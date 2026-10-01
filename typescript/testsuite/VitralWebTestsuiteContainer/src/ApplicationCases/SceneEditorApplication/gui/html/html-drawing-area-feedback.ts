import { Logger, VSDK, type RGBImageUncompressed, type SimpleBody } from '@vitral/base';
import type { DrawingArea } from '../../model/drawing-area';
import { SceneSelectionEditor } from '../../model/selection/scene-selection-editor';
import type { BodyEditFeedbackProvider } from '../../render/body-edit-feedback-provider';
import type { DrawingAreaHost } from '../../render/drawing-area-host';
import type { DrawingAreaInteractionListener } from '../drawing-area-interaction-listener';
import { PointerCursor } from '../pointer-cursor';
import type { HtmlApplicationHost } from './html-application-host';
import type { HtmlApplicationModel } from './html-application-model';
import { HtmlCursorWarper } from './html-cursor-warper';
import { HtmlImageControlWindow } from './html-image-control-window';
import { HtmlSelectorDialog } from './html-selector-dialog';
import { HtmlViewportElementScaler } from './html-viewport-element-scaler';

/**
 * Port of `gui.awt.AwtDrawingAreaFeedback`.
 *
 * Presents in the HTML GUI what the drawing area asks for: pointer shapes,
 * status messages, image and selector windows, the modify panel, and the
 * application level commands (raytracing, close, full screen). It also
 * supplies the rendering classes with the services they need from the GUI
 * technology hosting the drawing surface.
 *
 * Pointer images are CSS cursors (`url(...) x y, fallback`), with the hot spot
 * at their center, as Java creates them; their size is known once loaded.
 */
export class HtmlDrawingAreaFeedback implements DrawingAreaInteractionListener, DrawingAreaHost {
  private readonly application: HtmlApplicationHost;
  private readonly drawingArea: DrawingArea;
  private readonly canvas: HTMLElement;
  private readonly cursorWarper: HtmlCursorWarper = new HtmlCursorWarper();
  private readonly elementScaler: HtmlViewportElementScaler;
  private readonly selectionEditor: SceneSelectionEditor;

  private readonly cursors: Map<PointerCursor, string>;

  /**
   * @param application the application whose GUI is used
   * @param canvas the element presenting the drawing surface
   */
  constructor(application: HtmlApplicationHost, canvas: HTMLElement) {
    this.application = application;
    this.drawingArea = application.getApplicationModel().getDrawingArea();
    this.canvas = canvas;
    this.selectionEditor = new SceneSelectionEditor(application.getApplicationModel().getScene());

    this.cursors = new Map<PointerCursor, string>();
    for (const cursor of PointerCursor.values()) {
      this.cursors.set(cursor, 'default');
      this.createCursor(cursor);
    }

    this.elementScaler = new HtmlViewportElementScaler(this.drawingArea.getViewportSet().getElementScaler());
    this.elementScaler.updateFromDefaultScreen();
  }

  /**
   * Loads the image of a pointer (with transparency, hot spot at its center),
   * leaving the default cursor until it arrives or if it cannot be read.
   * @param cursor the pointer to present
   */
  private createCursor(cursor: PointerCursor): void {
    const filename: string | null = cursor.getImagePath();

    if (filename === null) {
      return;
    }
    const image: HTMLImageElement = new Image();
    image.onload = (): void => {
      const x: number = Math.trunc(image.naturalWidth / 2);
      const y: number = Math.trunc(image.naturalHeight / 2);
      this.cursors.set(cursor, 'url("' + filename + '") ' + x + ' ' + y + ', default');
    };
    image.onerror = (): void => {
      Logger.reportMessage(this, VSDK.WARNING, 'createCursor', 'Cannot load cursor image ' + filename);
    };
    image.src = filename;
  }

  /**
   * @return true if the pointer can be placed by the application (needed for
   * the infinite drag of gizmos)
   */
  isCursorWarpAvailable(): boolean {
    return this.cursorWarper.isAvailable();
  }

  private setStatusText(message: string): void {
    const statusMessage: HTMLElement | null = this.application.getHtmlModel().getStatusMessage();

    if (statusMessage !== null) {
      statusMessage.textContent = message;
    }
  }

  /**
   * Notifies the modify panel of the target it must edit: the first selected
   * body, or none.
   */
  reportTargetToModifyPanel(): void {
    const htmlModel: HtmlApplicationModel = this.application.getHtmlModel();
    let target: SimpleBody | null = null;

    if (this.application.getApplicationModel().getGuiState().isModifyPanelSelected()) {
      target = this.selectionEditor.getFirstSelectedBody();
    }
    const modifyPanel = htmlModel.getModifyPanel();
    if (modifyPanel === null) {
      return;
    }
    if (target !== null) {
      modifyPanel.notifyTargetBeginEdit(target);
    } else {
      modifyPanel.notifyTargetEndEdit();
    }
  }

  //= DrawingAreaInteractionListener ====================================

  cursorRequested(cursor: PointerCursor): void {
    const wanted: string = this.cursors.get(cursor) ?? 'default';

    if (this.canvas.style.cursor !== wanted) {
      this.canvas.style.cursor = wanted;
    }
  }

  cursorWarpRequested(surfaceX: number, surfaceY: number): void {
    this.cursorWarper.warp(this.canvas, this.drawingArea.scaleXToCanvas(surfaceX),
      this.drawingArea.scaleYToCanvas(surfaceY));
  }

  repaintRequested(): void {
    this.application.repaintDrawingArea();
  }

  statusMessageRequested(message: string): void {
    this.setStatusText(message);
  }

  selectionChanged(): void {
    this.reportTargetToModifyPanel();
  }

  raytracingRequested(): void {
    this.setStatusText(this.application.getApplicationModel().getI18nContext()!.getMessage('IDM_COMPUTING_RAYTRACING'));
    void this.application.doRaytracingImage().then((): void => {
      this.showImage(this.application.getApplicationModel().getRaytracedImage());
    });
  }

  selectorDialogRequested(): void {
    const htmlModel: HtmlApplicationModel = this.application.getHtmlModel();

    if (htmlModel.getSelectorDialog() === null) {
      htmlModel.setSelectorDialog(new HtmlSelectorDialog());
    }
    htmlModel.getSelectorDialog()!.setVisible(true);
    htmlModel.getSelectorDialog()!.repaint();
  }

  closeRequested(): void {
    this.application.closeApplication();
  }

  fullScreenGuiToggleRequested(): void {
    this.application.getApplicationModel().getGuiState().toggleFullScreenGuiMode();
    this.application.destroyGUI();
    void this.application.createGUI();
  }

  //= DrawingAreaHost ===================================================

  beforeFrame(): void {
    // Text size follows the resolution of the screen showing the canvas
    this.elementScaler.updateFromComponent(this.canvas);
    this.drawingArea.updateCanvasSize(this.canvas.clientWidth, this.canvas.clientHeight);
  }

  isFullScreenGuiMode(): boolean {
    return this.application.getApplicationModel().getGuiState().isFullScreenGuiMode();
  }

  getBodyEditFeedbackProvider(): BodyEditFeedbackProvider | null {
    return this.application.getHtmlModel().getModifyPanel();
  }

  async raytraceImage(): Promise<void> {
    await this.application.doViewportRaytracingImage();
  }

  showImage(image: RGBImageUncompressed | null): void {
    const htmlModel: HtmlApplicationModel = this.application.getHtmlModel();
    let window: HtmlImageControlWindow | null = htmlModel.getImageControlWindow();

    if (window === null) {
      window = new HtmlImageControlWindow(
        image,
        this.application.getApplicationModel().getI18nContext(),
        htmlModel.getExecutorPanel()!,
      );
      htmlModel.setImageControlWindow(window);
    } else {
      window.setImage(image);
    }
    window.redrawImage();
  }

  showStatusMessage(message: string): void {
    this.setStatusText(message);
  }
}
