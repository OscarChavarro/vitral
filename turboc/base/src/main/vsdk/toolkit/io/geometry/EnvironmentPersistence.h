#ifndef __ENVIRONMENT_PERSISTENCE__
#define __ENVIRONMENT_PERSISTENCE__

#include "java/io/File.h"
#include "java/io/OutputStream.h"
#include "vsdk/toolkit/io/PersistenceElement.h"
class SimpleScene;

class EnvironmentPersistence : public PersistenceElement {
public:
    static void importEnvironment(const File& sceneFile, SimpleScene* scene);
    static void exportEnvironmentObj(OutputStream& outputStream, SimpleScene* scene);
    static void exportEnvironmentGts(OutputStream& outputStream, SimpleScene* scene);
    static void exportEnvironmentVtk(OutputStream& outputStream, SimpleScene* scene);
};

#endif
