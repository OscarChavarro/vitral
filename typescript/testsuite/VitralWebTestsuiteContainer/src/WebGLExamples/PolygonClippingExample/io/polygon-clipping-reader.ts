import { POLYGON_CLIPPING_CASES } from '../model/polygon-clipping-fixtures';

/**
 * The browser half of `PolygonClippingModelingTools.buildPolygon`.
 *
 * Java reads a contour file with `Files.readAllLines` at the moment the scene
 * is rebuilt, which happens on every `[1]`, `[2]` and `[3]` keystroke and once
 * at startup. A page cannot read a file, and a frame must not wait on a
 * network read, so the fetch is lifted out of `rebuildScene` and made once:
 * every file the forty-three fixtures of `PolygonClippingFixtures` name is
 * fetched when the module opens, and `rebuildScene` then finds the text
 * already in hand and stays synchronous, as Java's is. The parse itself did
 * not move: it is in `PolygonClippingModelingTools`, where Java has it.
 *
 * The Java constant `POLYGONS_PATH` is `"../../../../etc/polygons/"`, relative
 * to the program's working directory. Here the directory is named by URL,
 * chosen in the dialog, and defaults to the same tree the container serves.
 */
export class PolygonClippingReader {
  /**
   * The distinct file names of the fixture table, in first-appearance order.
   * Several cases name the same contour file, and it is fetched once.
   */
  static referencedFileNames(): readonly string[] {
    const names: string[] = [];
    for (const testCase of POLYGON_CLIPPING_CASES) {
      if (!names.includes(testCase.clipFile)) {
        names.push(testCase.clipFile);
      }
      if (!names.includes(testCase.subjectFile)) {
        names.push(testCase.subjectFile);
      }
    }
    return names;
  }

  async read(polygonsBaseUrl: string): Promise<Map<string, string>> {
    const base: string = polygonsBaseUrl.endsWith('/') ? polygonsBaseUrl : polygonsBaseUrl + '/';
    const names: readonly string[] = PolygonClippingReader.referencedFileNames();
    const texts = new Map<string, string>();

    const contents: string[] = await Promise.all(
      names.map((name) => PolygonClippingReader.readText(base + name)),
    );
    for (let i = 0; i < names.length; i++) {
      texts.set(names[i]!, contents[i]!);
    }
    return texts;
  }

  private static async readText(url: string): Promise<string> {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error('Polygon file not found: ' + url);
    }
    return response.text();
  }
}
