import { FileSuffixFilter } from '../../io/file-suffix-filter';

/**
 * Port of `gui.awt.AwtSuffixFileFilter`.
 *
 * Adapter of a `FileSuffixFilter` for the browser's file chooser: the
 * `accept` attribute of an `<input type="file">` and the name of the filter.
 */
export class HtmlSuffixFileFilter {
  private readonly filter: FileSuffixFilter;

  /**
   * @param suffix accepted suffix, without the dot and in lower case
   * @param description text describing the kind of files
   */
  constructor(suffix: string, description: string) {
    this.filter = new FileSuffixFilter(suffix, description);
  }

  /**
   * @param fileName name of a file
   * @return true if the file has the suffix of the filter
   */
  accept(fileName: string): boolean {
    return this.filter.accept(fileName);
  }

  /**
   * @return the description of the kind of files, with the suffix
   */
  getDescription(): string {
    return this.filter.getDescription();
  }

  /**
   * @return the value for the `accept` attribute of a file input
   */
  getAcceptAttribute(): string {
    return '.' + this.filter.getAcceptedSuffix();
  }
}
