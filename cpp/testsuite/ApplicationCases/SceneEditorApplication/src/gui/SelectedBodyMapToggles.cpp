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

namespace {
const char* const TEXTURE_FILENAME = "../../../../etc/textures/miniearth.png";
const char* const BUMP_MAP_FILENAME = "../../../../etc/bumpmaps/earth.bw";
}

void SelectedBodyMapToggles::toggleTexture(SimpleBody* body)
{
    Image* texture = body->getTexture();

    if ( texture == nullptr ) {
        texture = ImagePersistence::importRGB(java::File(TEXTURE_FILENAME));
        body->setTexture(texture);
    }
    else {
        body->setTexture(nullptr);
    }
}

void SelectedBodyMapToggles::toggleNormalMap(SimpleBody* body)
{
    NormalMap* normalMap = body->getNormalMap();

    if ( normalMap == nullptr ) {
        normalMap = new NormalMap();
        IndexedColorImageUncompressed* source =
            ImagePersistence::importIndexedColor(java::File(BUMP_MAP_FILENAME));
        if ( source != nullptr ) {
            normalMap->importBumpMap(source, Vector3Dd(1, 1, 0.2));
            delete source;
        }
        else {
            Logger::reportMessage("SelectedBodyMapToggles", Logger::WARNING,
                "toggleNormalMap",
                java::String("Can not read ") + BUMP_MAP_FILENAME);
        }
        body->setNormalMap(normalMap);
    }
    else {
        body->setNormalMap(nullptr);
    }
}
