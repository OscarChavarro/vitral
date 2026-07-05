#ifndef __ENVIRONMENTPERSISTENCE__
#define __ENVIRONMENTPERSISTENCE__

#include "java/io/File.h"
#include "vsdk/toolkit/io/PersistenceElement.h"
class SimpleScene;

class EnvironmentPersistence : public PersistenceElement {
public:
    static void importEnvironment(const java::File& sceneFile, SimpleScene* scene);
};

#endif
