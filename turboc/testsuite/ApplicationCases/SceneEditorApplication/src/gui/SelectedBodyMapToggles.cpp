#include "java/io/File.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/IndexedColorImageUncompressed.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "gui/SelectedBodyMapToggles.h"

static const char* const TEXTURE_FILENAME = "../../../../etc/textures/miniearth.png";
static const char* const BUMP_MAP_FILENAME = "../../../../etc/bumpmaps/earth.bw";

void SelectedBodyMapToggles::toggleTexture(SimpleBody* body)
{
    Image* texture = body->getTexture();

    if ( texture == NULL ) {
        texture = ImagePersistence::importRGB(File(TEXTURE_FILENAME));
        body->setTexture(texture);
    }
    else {
        body->setTexture(NULL);
    }
}

void SelectedBodyMapToggles::toggleNormalMap(SimpleBody* body)
{
    NormalMap* normalMap = body->getNormalMap();

    if ( normalMap == NULL ) {
        normalMap = new NormalMap();
        IndexedColorImageUncompressed* source =
            ImagePersistence::importIndexedColor(File(BUMP_MAP_FILENAME));
        if ( source != NULL ) {
            normalMap->importBumpMap(source, Vector3Dd(1, 1, 0.2));
            delete source;
        }
        else {
            Logger::reportMessage("SelectedBodyMapToggles", Logger::WARNING,
                "toggleNormalMap",
                String("Can not read ") + BUMP_MAP_FILENAME);
        }
        body->setNormalMap(normalMap);
    }
    else {
        body->setNormalMap(NULL);
    }
}
