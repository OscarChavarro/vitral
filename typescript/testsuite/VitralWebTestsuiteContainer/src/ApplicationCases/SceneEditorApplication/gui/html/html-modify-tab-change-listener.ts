import type { HtmlApplicationHost } from './html-application-host';

/**
 * Port of `gui.awt.AwtModifyTabChangeListener`: when the second tab (the modify
 * panel) is selected, the panel starts editing the selected body.
 */
export class HtmlModifyTabChangeListener {
  parent: HtmlApplicationHost;

  constructor(parent: HtmlApplicationHost) {
    this.parent = parent;
  }

  /**
   * @param selectedIndex index of the selected tab
   */
  stateChanged(selectedIndex: number): void {
    if (selectedIndex === 1) {
      this.parent.getApplicationModel().getGuiState().setModifyPanelSelected(true);
      this.parent.reportTargetToModifyPanel();
    } else {
      this.parent.getApplicationModel().getGuiState().setModifyPanelSelected(false);
    }
  }
}
