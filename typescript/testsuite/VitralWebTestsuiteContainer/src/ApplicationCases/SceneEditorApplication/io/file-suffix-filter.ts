/**
 * Port of `io.FileSuffixFilter`.
 *
 * Accepts the files with a given suffix (extension), and the folders so the
 * user can browse them, as file choosers do. It does not depend on any GUI
 * technology: each one adapts it to its file chooser. A browser file is known
 * by its name (folders are browsed by the browser's own chooser).
 */
export class FileSuffixFilter {
  private readonly suffix: string | null;
  private readonly description: string;

  /**
   * @param suffix accepted suffix, without the dot and in lower case
   * @param description text describing the kind of files
   */
  constructor(suffix: string | null, description: string) {
    this.suffix = suffix;
    this.description = description;
  }

  /**
   * @param path path of a file
   * @return the suffix of the file name (after the last dot) in lower case,
   * or null if it has none
   */
  static getSuffix(path: string): string | null {
    let suffix: string | null = null;
    const i: number = path.lastIndexOf('.');

    if (i > 0 && i < path.length - 1) {
      suffix = path.substring(i + 1).toLowerCase();
    }
    return suffix;
  }

  /**
   * @param fileName name (or path) of a file
   * @param isDirectory true if it is a folder
   * @return true for folders and for files with the suffix of this filter
   */
  accept(fileName: string, isDirectory: boolean = false): boolean {
    if (isDirectory) {
      return true;
    }
    return this.suffix !== null && this.suffix === FileSuffixFilter.getSuffix(fileName);
  }

  /**
   * @return the accepted suffix, or null
   */
  getAcceptedSuffix(): string | null {
    return this.suffix;
  }

  /**
   * @return the description of the kind of files, with the suffix
   */
  getDescription(): string {
    return this.description + ' (*.' + this.suffix + ')';
  }
}
