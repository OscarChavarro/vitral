#include <ctype.h>
#include <stdlib.h>

#include "java/lang/String.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/io/geometry/EnvironmentPersistence.h"
#include "vsdk/toolkit/io/geometry/ReaderObj.h"
#include "vsdk/toolkit/io/geometry/ReaderPly.h"
#include "vsdk/toolkit/io/geometry/WriterGts.h"
#include "vsdk/toolkit/io/geometry/WriterObj.h"
#include "vsdk/toolkit/io/geometry/WriterVtk.h"

void
EnvironmentPersistence::importEnvironment(const File& sceneFile, SimpleScene* scene)
{
    char* extC = extractExtensionFromFile(sceneFile);
    String ext = (extC != 0) ? String(extC) : String();
    if ( extC != 0 ) free(extC);
    for (size_t i = 0; i < (size_t)ext.size(); i++) ext[i] = (char)tolower((unsigned char)ext[i]);

    if (ext == "obj") {
        ReaderObj::importEnvironment(sceneFile, scene);
        return;
    }
    if (ext == "ply") {
        ReaderPly::importEnvironment(sceneFile, scene);
        return;
    }

    Logger::reportMessage("EnvironmentPersistence", 1,
                          "importEnvironment",
                          "Unsupported scene extension in C++ port");
}

void
EnvironmentPersistence::exportEnvironmentObj(OutputStream& outputStream, SimpleScene* scene)
{
    WriterObj::exportEnvironment(outputStream, scene);
}

void
EnvironmentPersistence::exportEnvironmentGts(OutputStream& outputStream, SimpleScene* scene)
{
    WriterGts::exportEnvironment(outputStream, scene);
}

void
EnvironmentPersistence::exportEnvironmentVtk(OutputStream& outputStream, SimpleScene* scene)
{
    WriterVtk::exportEnvironment(outputStream, scene);
}
