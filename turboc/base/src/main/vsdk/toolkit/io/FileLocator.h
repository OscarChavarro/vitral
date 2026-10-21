#ifndef __FILE_LOCATOR__
#define __FILE_LOCATOR__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/io/File.h"

class FileLocator {
  private:
    ArrayList<String> searchPaths;

  public:
    void clearSearchPaths();
    void addSearchPath(const char *path);
    const ArrayList<String> &getSearchPaths() const;
    File *locate(const char *filename) const;
};

#endif
