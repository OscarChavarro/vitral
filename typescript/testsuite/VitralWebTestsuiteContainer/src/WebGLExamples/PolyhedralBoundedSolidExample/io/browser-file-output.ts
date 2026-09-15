/**
 * Where `java.io.FileOutputStream` writes in this module.
 *
 * Java writes `screenshot.png` and `output.stl` beside the program. A page has
 * no such directory; the bytes the Java stream would receive are handed to the
 * browser as a download under the same name, which is the one place a page
 * can put a file.
 */
export function saveBytesAs(name: string, bytes: Uint8Array, mimeType: string): void {
  const blob = new Blob([bytes.slice()], { type: mimeType });
  const url: string = URL.createObjectURL(blob);
  const anchor: HTMLAnchorElement = document.createElement('a');
  anchor.href = url;
  anchor.download = name;
  anchor.click();
  setTimeout(() => URL.revokeObjectURL(url));
}
