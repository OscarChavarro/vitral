#ifndef __VSDK_TOOLKIT_IO_GEOMETRY_ENVIRONMENTPERSISTENCE_H__
#define __VSDK_TOOLKIT_IO_GEOMETRY_ENVIRONMENTPERSISTENCE_H__

#include "vsdk/toolkit/io/PersistenceElement.h"
#include "java/io/File.h"

class SimpleScene;

class EnvironmentPersistence : public PersistenceElement {
public:
    static void importEnvironment(const java::File& sceneFile, SimpleScene* scene);
};

#endif
