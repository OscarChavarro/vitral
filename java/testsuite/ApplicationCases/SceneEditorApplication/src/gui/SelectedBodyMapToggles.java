package gui;

import java.io.File;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImageUncompressed;
import vsdk.toolkit.media.NormalMap;

/**
Debugging toggles of the maps of a body: a sample texture and a sample bump
map (normal map), loaded from the etc folder of the project.
*/
public class SelectedBodyMapToggles
{
    private static final String TEXTURE_FILENAME = "../../../../etc/textures/miniearth.png";
    private static final String BUMP_MAP_FILENAME = "../../../../etc/bumpmaps/earth.bw";

    /**
    Sets the sample texture to a body without texture, or removes its texture.
    @param body the body to change
    */
    public void toggleTexture(SimpleBody body)
    {
        Image texture = body.getTexture();

        if ( texture == null ) {
            try {
                texture = ImagePersistence.importRGB(new File(TEXTURE_FILENAME));
            }
            catch ( Exception e ) {
            }
            body.setTexture(texture);
        }
        else {
            body.setTexture(null);
        }
    }

    /**
    Sets the sample bump map to a body without normal map, or removes its
    normal map.
    @param body the body to change
    */
    public void toggleNormalMap(SimpleBody body)
    {
        NormalMap normalMap = body.getNormalMap();

        if ( normalMap == null ) {
            try {
                normalMap = new NormalMap();
                IndexedColorImageUncompressed source =
                    ImagePersistence.importIndexedColor(new File(BUMP_MAP_FILENAME));
                normalMap.importBumpMap(source, new Vector3Dd(1, 1, 0.2));
            }
            catch ( Exception e ) {
                Logger.reportMessage(this, VSDK.WARNING, "toggleNormalMap", "" + e);
            }
            body.setNormalMap(normalMap);
        }
        else {
            body.setNormalMap(null);
        }
    }
}
