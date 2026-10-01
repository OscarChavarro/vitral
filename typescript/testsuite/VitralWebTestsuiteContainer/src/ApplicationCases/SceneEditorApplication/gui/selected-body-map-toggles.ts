import { Logger, NormalMap, Vector3Dd, VSDK, type Image, type IndexedColorImageUncompressed, type SimpleBody } from '@vitral/base';
import { ImageFiles } from '../io/image-files';

/**
 * Port of `gui.SelectedBodyMapToggles`.
 *
 * Debugging toggles of the maps of a body: a sample texture and a sample bump
 * map (normal map), loaded from the etc folder of the project. Java reads the
 * files synchronously; here they are fetched (see `ImageFiles`), so the
 * toggles are asynchronous.
 */
export class SelectedBodyMapToggles {
  private static readonly TEXTURE_FILENAME = './etc/textures/miniearth.png';
  private static readonly BUMP_MAP_FILENAME = './etc/bumpmaps/earth.bw';

  /**
   * Sets the sample texture to a body without texture, or removes its texture.
   * @param body the body to change
   */
  async toggleTexture(body: SimpleBody): Promise<void> {
    let texture: Image | null = body.getTexture();

    if (texture === null) {
      try {
        texture = await ImageFiles.get().importRGB(SelectedBodyMapToggles.TEXTURE_FILENAME);
      } catch {
        // As in Java, a texture that can not be read leaves the body untextured
      }
      body.setTexture(texture);
    } else {
      body.setTexture(null);
    }
  }

  /**
   * Sets the sample bump map to a body without normal map, or removes its
   * normal map.
   * @param body the body to change
   */
  async toggleNormalMap(body: SimpleBody): Promise<void> {
    let normalMap: NormalMap | null = body.getNormalMap();

    if (normalMap === null) {
      try {
        normalMap = new NormalMap();
        const source: IndexedColorImageUncompressed = await ImageFiles.get().importIndexedColor(
          SelectedBodyMapToggles.BUMP_MAP_FILENAME,
        );
        normalMap.importBumpMap(source, new Vector3Dd(1, 1, 0.2));
      } catch (e) {
        Logger.reportMessage(this, VSDK.WARNING, 'toggleNormalMap', '' + e);
      }
      body.setNormalMap(normalMap);
    } else {
      body.setNormalMap(null);
    }
  }
}
