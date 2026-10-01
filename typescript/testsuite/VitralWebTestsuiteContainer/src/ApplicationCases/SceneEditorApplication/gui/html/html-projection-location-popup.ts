import {
  ViewportSetCommands,
  WidgetMenuItem,
  type Viewport,
  type ViewportSet,
  type Widget,
  type WidgetMenu,
} from '@vitral/base';
import { HtmlPopupMenu, type HtmlMenuItem } from '@vitral/webgl';
import type { DrawingAreaInteractionTechniques } from '../drawing-area-interaction-techniques';
import { MouseEventKind, PopupDismissClickFilter } from '../popup-dismiss-click-filter';

/**
 * Port of `gui.awt.AwtProjectionLocationPopup`.
 *
 * DOM presentation of the menu of a viewport: the
 * `VIEWPORT_SET_PROJECTION_LOCATION` popup of the viewport set, to change the
 * projection location of a viewport (Perspective, Top, ...), followed, after a
 * separator, by the `VIEWPORT_SET_RENDER_MODE` popup, to render it with the GPU
 * or the CPU (raytracing). Texts come from the I18N context of the viewport set
 * (so it always shows the language currently selected by the user). The
 * projection and the render mode currently used are marked.
 *
 * The menu is built each time it is requested, and it floats over the canvas
 * (see `HtmlPopupMenu`), with the keyboard focus: the user can move with the
 * up and down keys and select with enter or space (escape closes it). Clicking
 * outside closes the menu without applying anything, and that click is not
 * processed by the canvas (see `consumesMouseEvent` and
 * `PopupDismissClickFilter`). When the menu closes, the keyboard focus goes
 * back to the canvas.
 */
export class HtmlProjectionLocationPopup {
  private readonly viewportSet: ViewportSet;
  private readonly techniques: DrawingAreaInteractionTechniques;
  private readonly canvas: HTMLElement;
  private readonly repaint: () => void;
  private popup: HtmlPopupMenu | null;
  private readonly dismissClickFilter: PopupDismissClickFilter;

  /**
   * @param viewportSet the set whose I18N context gives the texts
   * @param techniques the techniques that execute the chosen command (and
   * record it in the view history of the viewport)
   * @param canvas the element where the viewport set is presented; it
   * receives the keyboard focus back when the menu closes
   * @param repaint requests a new frame of the canvas
   */
  constructor(
    viewportSet: ViewportSet,
    techniques: DrawingAreaInteractionTechniques,
    canvas: HTMLElement,
    repaint: () => void,
  ) {
    this.viewportSet = viewportSet;
    this.techniques = techniques;
    this.canvas = canvas;
    this.repaint = repaint;
    this.popup = null;
    this.dismissClickFilter = new PopupDismissClickFilter();
  }

  /**
   * @return true if the menu is currently shown
   */
  isVisible(): boolean {
    return this.popup !== null && this.popup.isVisible();
  }

  /**
   * Shows the menu to change the projection location of a viewport.
   * @param viewport the viewport to change
   * @param canvasX horizontal position, in coordinates of the canvas
   * @param canvasY vertical position, in coordinates of the canvas
   * @return false if the menu can not be shown because the I18N context does
   * not define it
   */
  show(viewport: Viewport, canvasX: number, canvasY: number): boolean {
    const context: Widget | null = this.viewportSet.getI18nContext();
    let definition: WidgetMenu | null = null;

    if (context !== null) {
      definition = context.getPopup(ViewportSetCommands.POPUP_PROJECTION_LOCATION);
    }
    if (definition === null || context === null) {
      return false;
    }
    if (this.isVisible()) {
      this.popup!.hide();
    }

    const popup: HtmlPopupMenu = new HtmlPopupMenu();
    this.popup = popup;
    const current: HtmlMenuItem | null = this.fillMenu(popup, definition, viewport,
      viewport.getProjectionLocationCommand());
    const renderModeDefinition: WidgetMenu | null = context.getPopup(ViewportSetCommands.POPUP_RENDER_MODE);
    if (renderModeDefinition !== null) {
      popup.addSeparator();
      this.fillMenu(popup, renderModeDefinition, viewport, viewport.getRenderModeCommand());
    }
    popup.addCloseListener((): void => {
      this.dismissClickFilter.popupClosed(Date.now());
      // Give the focus back once the menu is gone
      setTimeout((): void => this.canvas.focus({ preventScroll: true }));
    });

    const box: DOMRect = this.canvas.getBoundingClientRect();
    popup.show(box.left + canvasX, box.top + canvasY);

    // Keyboard navigation starts at the projection in use
    if (current !== null) {
      popup.setActiveItem(current);
    }
    this.dismissClickFilter.popupShown();
    return true;
  }

  /**
   * Adds the items of a popup definition as a group of radio items.
   * @param popup menu to fill
   * @param definition popup of the I18N context
   * @param viewport viewport the commands act over
   * @param currentCommand command of the item to mark as selected
   * @return the item marked as selected, or null if none
   */
  private fillMenu(
    popup: HtmlPopupMenu,
    definition: WidgetMenu,
    viewport: Viewport,
    currentCommand: string,
  ): HtmlMenuItem | null {
    let currentItem: HtmlMenuItem | null = null;

    for (const element of definition.getChildren()) {
      if (!(element instanceof WidgetMenuItem)) {
        continue;
      }
      if (element.isSeparator()) {
        popup.addSeparator();
        continue;
      }

      const command: string = element.getCommandName();
      const mnemonic: string = element.getMnemonic();
      const item: HtmlMenuItem = popup.addRadioItem(element.getName(), mnemonic, command === currentCommand,
        (): void => {
          this.techniques.processViewportCommand(command, viewport);
          this.repaint();
        });
      if (command === currentCommand) {
        currentItem = item;
      }
    }
    return currentItem;
  }

  /**
   * Mouse handlers of the canvas must ask this before processing the mouse
   * presses, releases, clicks and drags: the events of a press that closed
   * the menu are consumed (see `PopupDismissClickFilter`).
   * @param kind kind of the mouse event of the canvas
   * @return true if the event must be ignored
   */
  consumesMouseEvent(kind: MouseEventKind): boolean {
    return this.dismissClickFilter.consumes(kind, Date.now());
  }
}
