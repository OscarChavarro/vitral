#ifndef __VSDK_TOOLKIT_IO_GEOMETRY_READEROBJ_H__
#define __VSDK_TOOLKIT_IO_GEOMETRY_READEROBJ_H__

#include "vsdk/toolkit/io/PersistenceElement.h"
#include "vsdk/toolkit/java/io/File.h"

class SimpleScene;

class ReaderObj : public PersistenceElement {
public:
    static void importEnvironment(const java::File& sceneFile, SimpleScene* scene);
};

#endif
