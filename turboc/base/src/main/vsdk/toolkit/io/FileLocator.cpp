#include <stdio.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/io/FileLocator.h"

void
FileLocator::clearSearchPaths()
{
    searchPaths.clear();
}

void
FileLocator::addSearchPath(const char *path)
{
    searchPaths.add(String(path));
}

const ArrayList<String> &
FileLocator::getSearchPaths() const
{
    return searchPaths;
}

File *
FileLocator::locate(const char *filename) const
{
    File *candidate = new File(filename);
    if (candidate->canRead()) {
        return candidate;
    }
    delete candidate;

    for (long int i = 0; i < searchPaths.size(); i++) {
        char pathname[512];
        snprintf(pathname, sizeof(pathname), "%s/%s", searchPaths.get(i).toCString(), filename);
        candidate = new File(pathname);
        if (candidate->canRead()) {
            return candidate;
        }
        delete candidate;
    }
    return NULL;
}
