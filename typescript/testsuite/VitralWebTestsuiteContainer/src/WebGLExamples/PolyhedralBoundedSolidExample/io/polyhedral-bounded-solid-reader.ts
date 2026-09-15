import { WebFontReader } from '@vitral/webgl';
import {
  GeneralModelsBuilder,
  type GeneralModelsResources,
} from '../models/general-models-builder';

/**
 * The browser half of the two file reads `GeneralModelsBuilder` makes.
 *
 * Java opens `../../../../etc/solids/kurlanderBowl.step` with `StepReader`
 * and `../../../../etc/fonts/cyrvetic.ttf` with `AwtFontReader` at the moment
 * the solid is built, which happens at startup and on every keystroke that
 * rebuilds. A page cannot read a file, and a build must not wait on a network
 * read, so both are fetched once, when the module opens, from the URLs chosen
 * in the dialog, and registered under the names Java uses, where the builder
 * finds them in hand and stays synchronous, as Java's is. A STEP file that could not be
 * fetched is simply absent, and the builder falls back to the holed box the
 * way Java falls back when the file is missing; a font that could not be
 * fetched makes `WebFontReader` answer null, which Java's `createFontBlock`
 * turns into the build error the HUD prints.
 */
export class PolyhedralBoundedSolidReader {
  async read(stepFileUrl: string, fontFileUrl: string): Promise<GeneralModelsResources> {
    const stepFiles = new Map<string, Uint8Array>();
    const fontReader = new WebFontReader();

    const [stepBytes] = await Promise.all([
      PolyhedralBoundedSolidReader.readBytes(stepFileUrl),
      PolyhedralBoundedSolidReader.loadFont(fontReader, fontFileUrl),
    ]);
    if (stepBytes !== null) {
      stepFiles.set(GeneralModelsBuilder.KURLANDER_BOWL_STEP_FILE, stepBytes);
    }
    return { stepFiles, fontReader };
  }

  private static async readBytes(url: string): Promise<Uint8Array | null> {
    try {
      const response = await fetch(url);
      if (!response.ok) {
        console.error('[PolyhedralBoundedSolidExample] STEP file not found: ' + url);
        return null;
      }
      return new Uint8Array(await response.arrayBuffer());
    } catch (error) {
      console.error(
        '[PolyhedralBoundedSolidExample] ' +
          (error instanceof Error ? error.message : String(error)),
      );
      return null;
    }
  }

  private static async loadFont(fontReader: WebFontReader, url: string): Promise<void> {
    try {
      await fontReader.load(url, GeneralModelsBuilder.FONT_BLOCK_FONT_FILE);
    } catch (error) {
      console.error(
        '[PolyhedralBoundedSolidExample] ' +
          (error instanceof Error ? error.message : String(error)),
      );
    }
  }
}
