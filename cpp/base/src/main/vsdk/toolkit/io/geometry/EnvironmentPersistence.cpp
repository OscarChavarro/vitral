#include "vsdk/toolkit/io/geometry/EnvironmentPersistence.h"
#include "java/lang/String.h"

#include "vsdk/toolkit/io/geometry/ReaderObj.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "java/lang/String.h"
#include <cctype>
#include <cstdlib>
#include "java/lang/String.h"

void EnvironmentPersistence::importEnvironment(const java::File& sceneFile, SimpleScene* scene)
{
    char* extC = extractExtensionFromFile(sceneFile);
    java::String ext = (extC != 0) ? java::String(extC) : java::String();
    if ( extC != 0 ) free(extC);
    for (size_t i = 0; i < ext.size(); i++) ext[i] = (char)std::tolower((unsigned char)ext[i]);

    if (ext == "obj") {
        ReaderObj::importEnvironment(sceneFile, scene);
        return;
    }

    Logger::reportMessage("EnvironmentPersistence", 1,
                          "importEnvironment",
                          "Unsupported scene extension in C++ port");
}
