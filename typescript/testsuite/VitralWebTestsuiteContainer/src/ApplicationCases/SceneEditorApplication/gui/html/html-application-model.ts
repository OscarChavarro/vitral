import type { HtmlButtonsPanel } from './html-buttons-panel';
import type { HtmlCommandExecutor } from './html-command-executor';
import type { HtmlImageControlWindow } from './html-image-control-window';
import type { HtmlModifyPanel } from './html-modify-panel';
import type { HtmlSelectorDialog } from './html-selector-dialog';

/**
 * Port of `gui.awt.AwtApplicationModel`.
 *
 * DOM elements and services of the GUI. Technology independent GUI state is
 * kept in `model/gui-state`. Java's look and feel class name is the CSS class
 * of the theme (see `html-look-and-feel-tuner.ts`).
 */
export class HtmlApplicationModel {
  private statusMessage: HTMLElement | null = null;
  private statusBarPanel: HTMLElement | null = null;
  private imageControlWindow: HtmlImageControlWindow | null = null;
  private selectorDialog: HtmlSelectorDialog | null = null;
  private executor: HtmlCommandExecutor | null = null;
  private executorPanel: HtmlButtonsPanel | null = null;
  private mainWindowWidget: HTMLElement | null = null;
  private lookAndFeel = '';

  private modifyPanel: HtmlModifyPanel | null = null;

  getStatusMessage(): HTMLElement | null {
    return this.statusMessage;
  }

  setStatusMessage(statusMessage: HTMLElement | null): void {
    this.statusMessage = statusMessage;
  }

  getStatusBarPanel(): HTMLElement | null {
    return this.statusBarPanel;
  }

  setStatusBarPanel(statusBarPanel: HTMLElement | null): void {
    this.statusBarPanel = statusBarPanel;
  }

  getImageControlWindow(): HtmlImageControlWindow | null {
    return this.imageControlWindow;
  }

  setImageControlWindow(imageControlWindow: HtmlImageControlWindow | null): void {
    this.imageControlWindow = imageControlWindow;
  }

  getSelectorDialog(): HtmlSelectorDialog | null {
    return this.selectorDialog;
  }

  setSelectorDialog(selectorDialog: HtmlSelectorDialog | null): void {
    this.selectorDialog = selectorDialog;
  }

  getExecutor(): HtmlCommandExecutor | null {
    return this.executor;
  }

  setExecutor(executor: HtmlCommandExecutor | null): void {
    this.executor = executor;
  }

  getExecutorPanel(): HtmlButtonsPanel | null {
    return this.executorPanel;
  }

  setExecutorPanel(executorPanel: HtmlButtonsPanel | null): void {
    this.executorPanel = executorPanel;
  }

  getMainWindowWidget(): HTMLElement | null {
    return this.mainWindowWidget;
  }

  setMainWindowWidget(mainWindowWidget: HTMLElement | null): void {
    this.mainWindowWidget = mainWindowWidget;
  }

  getLookAndFeel(): string {
    return this.lookAndFeel;
  }

  setLookAndFeel(lookAndFeel: string): void {
    this.lookAndFeel = lookAndFeel;
  }

  getModifyPanel(): HtmlModifyPanel | null {
    return this.modifyPanel;
  }

  setModifyPanel(modifyPanel: HtmlModifyPanel | null): void {
    this.modifyPanel = modifyPanel;
  }
}
