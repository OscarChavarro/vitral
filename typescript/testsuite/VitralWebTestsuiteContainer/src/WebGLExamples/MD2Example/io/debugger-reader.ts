import type { Md2Mesh } from '@vitral/base';
import { WebMd2Persistence } from '@vitral/webgl';

/**
 * Port of `java/testsuite/Jogl4Examples/MD2Example/src/io/DebuggerReader.java`.
 *
 * Java names two files under `../../../../etc/md2/`, reads them with
 * `Md2Persistence.read(md2Path, texturePath, md2Mesh, 2.0)` and selects the
 * first animation. A browser names the same two resources by URL and reads
 * them with `WebMd2Persistence`, which fetches both and hands the bytes to the
 * same parser; the gamma exponent and the `setCurrentAnimationInd(0)` that
 * follows are Java's.
 */
export class DebuggerReader {
  private static readonly TEXTURE_GAMMA_CORRECTION = 2.0;

  async readMd2WithTexture(md2Url: string, textureUrl: string, md2Mesh: Md2Mesh): Promise<void> {
    await WebMd2Persistence.read(
      md2Url,
      textureUrl,
      md2Mesh,
      DebuggerReader.TEXTURE_GAMMA_CORRECTION,
    );
    md2Mesh.setCurrentAnimationInd(0);
  }
}
