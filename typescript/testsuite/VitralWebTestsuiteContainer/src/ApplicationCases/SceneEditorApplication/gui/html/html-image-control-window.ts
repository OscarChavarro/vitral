import type { RGBImageUncompressed, Widget } from '@vitral/base';
import {
  HtmlGuiRenderer,
  HtmlImageRenderer,
  HtmlWindow,
  type HtmlActionListener,
  type HtmlMenuBar,
} from '@vitral/webgl';

/**
 * Panel that paints an image with its upper left corner at (10, 10) (Java's
 * package class `ImageDisplayPanel` of `AwtImageControlWindow`).
 */
class ImageDisplayPanel {
  readonly element: HTMLDivElement;
  private readonly canvas: HTMLCanvasElement;
  private imageToPaint: RGBImageUncompressed | null;

  constructor(img: RGBImageUncompressed | null) {
    this.element = document.createElement('div');
    this.element.className = 'scene-editor-image-display';
    this.element.style.minWidth = 320 + 20 + 'px';
    this.element.style.minHeight = 240 + 30 + 'px';
    this.canvas = document.createElement('canvas');
    this.element.appendChild(this.canvas);
    this.imageToPaint = img;
  }

  setImage(i: RGBImageUncompressed | null): void {
    this.imageToPaint = i;
  }

  repaint(): void {
    if (this.imageToPaint !== null) {
      HtmlImageRenderer.exportToCanvas(this.imageToPaint, this.canvas);
    }
  }
}

/**
 * Port of `gui.awt.AwtImageControlWindow`.
 *
 * A window that shows an image obtained by the application (a raytraced image,
 * or a capture of the frame buffers), with the menu bar of the GUI definition
 * and a status bar.
 */
export class HtmlImageControlWindow {
  private readonly windowWidget: HtmlWindow;
  statusMessage: HTMLElement | null = null;
  private controlledImage: RGBImageUncompressed | null;
  private readonly workArea: ImageDisplayPanel;
  private readonly menubar: HtmlMenuBar;

  constructor(image: RGBImageUncompressed | null, gui: Widget | null, executor: HtmlActionListener) {
    this.controlledImage = image;

    this.windowWidget = new HtmlWindow('Image control tool');

    this.menubar = HtmlGuiRenderer.buildMenubar(gui, null, executor);

    const statusBar: HTMLElement = this.createStatusBar();
    this.workArea = new ImageDisplayPanel(this.controlledImage);

    this.windowWidget.content.classList.add('scene-editor-image-window-content');
    this.windowWidget.content.appendChild(this.workArea.element);
    this.windowWidget.element.appendChild(statusBar);
    this.windowWidget.setMenuBar(this.menubar.element);
    this.windowWidget.addCloseListener((): void => this.menubar.dispose());

    this.windowWidget.setSize(640, 480);
    this.windowWidget.setVisible(true);
  }

  setImage(i: RGBImageUncompressed | null): void {
    this.controlledImage = i;
    this.workArea.setImage(i);
  }

  redrawImage(): void {
    this.windowWidget.setVisible(true);
    this.workArea.repaint();
  }

  /**
   * Closes the window.
   */
  dispose(): void {
    this.windowWidget.dispose();
  }

  private createStatusBar(): HTMLElement {
    this.statusMessage = document.createElement('div');
    this.statusMessage.className = 'vitral-status-message';
    this.statusMessage.textContent = 'Image control window ready';

    const panel: HTMLDivElement = document.createElement('div');
    panel.className = 'vitral-status-bar';
    panel.appendChild(this.statusMessage);

    return panel;
  }
}
