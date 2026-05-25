#include "vsdk/toolkit/io/geometry/EnvironmentPersistence.h"

#include "vsdk/toolkit/io/geometry/ReaderObj.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include <cctype>

void EnvironmentPersistence::importEnvironment(const java::File& sceneFile, SimpleScene* scene)
{
    char* extC = extractExtensionFromFile(sceneFile);
    std::string ext = (extC != 0) ? std::string(extC) : std::string();
    if ( extC != 0 ) delete [] extC;
    for (size_t i = 0; i < ext.size(); i++) ext[i] = (char)std::tolower((unsigned char)ext[i]);

    if (ext == "obj") {
        ReaderObj::importEnvironment(sceneFile, scene);
        return;
    }

    Logger::reportMessage("EnvironmentPersistence", 1,
                          "importEnvironment",
                          "Unsupported scene extension in C++ port");
}
