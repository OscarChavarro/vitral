import { type Widget } from '@vitral/base';
import { HtmlActionEvent, HtmlGuiRenderer, type HtmlActionListener } from '@vitral/webgl';
import type { HtmlApplicationHost } from './html-application-host';
import type { HtmlCommandExecutor } from './html-command-executor';

/**
 * Port of `gui.awt.AwtButtonsPanel`.
 *
 * A panel with one of the button groups of the GUI definition, whose buttons
 * (and menus, as it is also the executor of the menu bars) send their commands
 * to an `HtmlCommandExecutor`. Groups below 100 are the columns of the tabs of
 * the right panel, group 101 is the icon bar.
 */
export class HtmlButtonsPanel implements HtmlActionListener {
  readonly element: HTMLDivElement;
  private readonly parent: HtmlApplicationHost;
  private readonly guiEventExecutor: HtmlCommandExecutor;

  constructor(parent: HtmlApplicationHost, group: number, guiEventExecutor: HtmlCommandExecutor) {
    this.guiEventExecutor = guiEventExecutor;
    //-------------------------------------------------------------------
    this.parent = parent;
    this.element = document.createElement('div');
    if (group < 100) {
      // This is a button group inside right tab panels
      this.element.className = 'scene-editor-buttons-panel scene-editor-buttons-panel-tab';
    } else {
      // This is a button group part of an icon bar
      this.element.className = 'scene-editor-buttons-panel scene-editor-buttons-panel-bar';
    }

    //-------------------------------------------------------------------
    let internal: HTMLElement | null = null;
    const context: Widget = parent.getApplicationModel().getI18nContext()!;

    switch (group) {
      case 1:
        internal = HtmlGuiRenderer.buildButtonGroup(context, 'CREATION', this);
        break;
      case 2:
        internal = HtmlGuiRenderer.buildButtonGroup(context, 'GUI', this);
        break;
      case 3:
        internal = HtmlGuiRenderer.buildButtonGroup(context, 'OTHER', this);
        break;
      case 4:
        internal = HtmlGuiRenderer.buildButtonGroup(context, 'RENDER', this);
        break;
      case 101:
        internal = HtmlGuiRenderer.buildButtonGroup(context, 'GLOBAL', this);
        break;
    }

    if (internal !== null) {
      this.element.appendChild(internal);
    }

    //-------------------------------------------------------------------
  }

  actionPerformed(ev: HtmlActionEvent): void {
    let label: string = ev.getActionCommand();

    // This makes event compatible with ButtonGroup scheme of event
    // handling
    const source: object = ev.getSource();
    if (source instanceof HTMLButtonElement) {
      label = source.name;
    }
    void this.guiEventExecutor.executeCommand(label, this.parent.getHtmlModel().getMainWindowWidget());
  }
}
