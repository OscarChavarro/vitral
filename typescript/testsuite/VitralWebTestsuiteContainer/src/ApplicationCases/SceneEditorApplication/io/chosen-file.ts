/**
 * A file the user chose to read (Java's `java.io.File` given by a file
 * chooser): a file of the server, with its path, or one of the user's
 * computer. `HtmlChosenFile` of `@vitral/webgl` is one.
 */
export interface ChosenFile {
  /** Name of the file (Java's `File.getName()`) */
  readonly name: string;
  /** Path of a server file, or null for a file of the user's computer */
  readonly path: string | null;

  /**
   * @return the folder of a server file (Java's `getParentFile()`), or null
   */
  getParentPath(): string | null;

  /**
   * @return the contents of the file
   */
  readBytes(): Promise<Uint8Array>;

  /**
   * @return the contents of the file, as text
   */
  readText(): Promise<string>;
}
