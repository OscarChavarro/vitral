import { HtmlWindow } from '@vitral/webgl';

/**
 * Port of `gui.awt.AwtSelectorDialog`: a dialog of 526x530 pixels, with a test
 * button at its top and another at its bottom.
 */
export class HtmlSelectorDialog {
  private readonly dialog: HtmlWindow;

  constructor() {
    this.dialog = new HtmlWindow('');

    //-----------------------------------------------------------------
    const mainFrameWidget: HTMLDivElement = document.createElement('div');
    mainFrameWidget.className = 'scene-editor-selector-dialog';
    this.dialog.element.style.minWidth = '526px';
    this.dialog.element.style.minHeight = '530px';
    this.dialog.setSize(526, 530);

    //-----------------------------------------------------------------
    const bottomAreaWidget: HTMLDivElement = document.createElement('div');
    const centralAreaWidget: HTMLDivElement = document.createElement('div');

    mainFrameWidget.appendChild(centralAreaWidget);
    mainFrameWidget.appendChild(bottomAreaWidget);

    //-----------------------------------------------------------------
    let b: HTMLButtonElement = document.createElement('button');
    b.type = 'button';
    b.className = 'vitral-button';
    b.textContent = 'Test bottom';
    bottomAreaWidget.appendChild(b);

    b = document.createElement('button');
    b.type = 'button';
    b.className = 'vitral-button';
    b.textContent = 'Test central';
    centralAreaWidget.appendChild(b);

    this.dialog.content.appendChild(mainFrameWidget);
    //-----------------------------------------------------------------
  }

  setVisible(visible: boolean): void {
    this.dialog.setVisible(visible);
  }

  /**
   * Swing's `repaint`: the DOM repaints by itself.
   */
  repaint(): void {
    // The browser presents the dialog as soon as it changes
  }

  dispose(): void {
    this.dialog.dispose();
  }
}
