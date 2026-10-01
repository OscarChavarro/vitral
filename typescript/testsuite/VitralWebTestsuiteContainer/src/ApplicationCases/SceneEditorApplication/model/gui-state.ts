/**
 * Port of `model.GuiState`.
 *
 * State of the GUI of the editor that does not depend on the GUI technology in
 * use: the language file, which panels are shown, the full screen mode and the
 * folders proposed by file dialogs. The I18N texts themselves are the
 * `ApplicationModel.getI18nContext()`.
 *
 * A browser can not list a folder, so the languages Java finds by listing
 * `etc/gui` come from the manifest `etc/gui/languages.json` the web container
 * generates (see `scripts/sync-etc.sh`): the application reads it at start up
 * and gives it to `setAvailableLanguages`. Java's read folder (an absolute
 * path of the machine) is the server folder of the geometries.
 */
export class GuiState {
  /** Folder with the I18N files (one JSON file per language) used to build the GUI. */
  static readonly GUI_LANGUAGE_FOLDER = './etc/gui/';
  static readonly JSON_EXTENSION = '.json';

  private static availableLanguages: string[] = [];

  private languageGuiFile: string;
  private modifyPanelSelected: boolean;
  private fullScreenGuiMode: boolean;
  private readFolder: string;
  private writeFolder: string;

  constructor() {
    this.languageGuiFile = GuiState.GUI_LANGUAGE_FOLDER + 'english' + GuiState.JSON_EXTENSION;
    this.modifyPanelSelected = false;
    this.fullScreenGuiMode = false;
    this.readFolder = './etc/geometry';
    this.writeFolder = '.';
  }

  /**
   * @return folder proposed when opening files: the one of the last file read
   */
  getReadFolder(): string {
    return this.readFolder;
  }

  /**
   * @param readFolder folder to propose when opening files
   */
  setReadFolder(readFolder: string): void {
    this.readFolder = readFolder;
  }

  /**
   * @return folder proposed when saving files: the one of the last file written
   */
  getWriteFolder(): string {
    return this.writeFolder;
  }

  /**
   * @param writeFolder folder to propose when saving files
   */
  setWriteFolder(writeFolder: string): void {
    this.writeFolder = writeFolder;
  }

  /**
   * @return I18N file the GUI is built from
   */
  getLanguageGuiFile(): string {
    return this.languageGuiFile;
  }

  /**
   * @param languageGuiFile I18N file to build the GUI from
   */
  setLanguageGuiFile(languageGuiFile: string): void {
    this.languageGuiFile = languageGuiFile;
  }

  /**
   * @return true if the panel that edits the selected body is shown
   */
  isModifyPanelSelected(): boolean {
    return this.modifyPanelSelected;
  }

  /**
   * @param modifyPanelSelected true if the panel that edits the selected body
   * is shown
   */
  setModifyPanelSelected(modifyPanelSelected: boolean): void {
    this.modifyPanelSelected = modifyPanelSelected;
  }

  /**
   * @return true if only the drawing area is shown, without the rest of the GUI
   */
  isFullScreenGuiMode(): boolean {
    return this.fullScreenGuiMode;
  }

  /**
   * @param fullScreenGuiMode true to show only the drawing area
   */
  setFullScreenGuiMode(fullScreenGuiMode: boolean): void {
    this.fullScreenGuiMode = fullScreenGuiMode;
  }

  /**
   * Switches between showing only the drawing area and showing all the GUI.
   */
  toggleFullScreenGuiMode(): void {
    this.fullScreenGuiMode = !this.fullScreenGuiMode;
  }

  /**
   * @param languages identifiers of the languages of the manifest
   */
  static setAvailableLanguages(languages: readonly string[]): void {
    GuiState.availableLanguages = [...languages];
  }

  /**
   * @return the identifiers of the languages available for the GUI, sorted:
   * the names (without extension) of the JSON files in the GUI language folder
   */
  static listLanguages(): string[] {
    const languages: string[] = [...GuiState.availableLanguages];
    // Java's Collections.sort of Strings: UTF-16 code unit order
    languages.sort((a: string, b: string): number => (a < b ? -1 : a > b ? 1 : 0));
    return languages;
  }

  /**
   * @param language identifier of a language (see `listLanguages`)
   * @return the I18N file of that language
   */
  static languageFile(language: string): string {
    return GuiState.GUI_LANGUAGE_FOLDER + language + GuiState.JSON_EXTENSION;
  }

  /**
   * @return the identifier of the language currently used by the GUI
   */
  getCurrentLanguage(): string {
    let name: string = this.languageGuiFile.substring(this.languageGuiFile.lastIndexOf('/') + 1);

    if (name.endsWith(GuiState.JSON_EXTENSION)) {
      name = name.substring(0, name.length - GuiState.JSON_EXTENSION.length);
    }
    return name;
  }
}
