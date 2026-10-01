/**
 * Port of `gui.awt.AwtCommandExecutor`.
 *
 * Executes the commands issued by the buttons and menus of the HTML GUI.
 * Commands that open dialogs or run the raytracer finish later, so the result
 * is a promise.
 */
export interface HtmlCommandExecutor {
  /**
   * Folders proposed by file dialogs are kept in `model/gui-state`.
   * @param label identifier of the command
   * @param mainWindowWidget main window, parent of dialogs
   * @return true if the command was recognized
   */
  executeCommand(label: string, mainWindowWidget: HTMLElement | null): Promise<boolean>;
}
