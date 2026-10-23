#ifndef __FILES__
#define __FILES__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"

/**
Static helpers of `java.nio.file.Files` used by the port.
*/
class Files {
  public:
    /**
    Emulation of `Files.readString`: reads a whole text file.
    @param path name of the file
    @param outText the contents of the file
    @return false if the file could not be read (Java throws IOException)
    */
    static BOOLEAN readString(const String &path, String &outText);

  private:
    Files();
};

#endif
