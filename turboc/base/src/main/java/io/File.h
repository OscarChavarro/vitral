#ifndef __FILE_H__
#define __FILE_H__

#include <stddef.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"

class File {
  private:
String path;

static BOOLEAN
isValidPath(const char *rawPath);

static BOOLEAN
canOpenWithMode(const char *rawPath, const char *mode, int *errorCode = NULL);

static BOOLEAN
isDirectoryByReadProbe(const char *rawPath);

  public:
File();
explicit File(const char *path);
explicit File(const String &path);
~File();

void
dispose();

String
getPath() const;

String
getName() const;

/** Path resolved against the current working directory (not normalized,
as in the JDK). */
String
getAbsolutePath() const;

BOOLEAN
exists() const;

BOOLEAN
isDirectory() const;

BOOLEAN
isFile() const;

BOOLEAN
canRead() const;

BOOLEAN
canWrite() const;

BOOLEAN
mkdirs() const;

/**
Names of the entries of this directory (no "." / ".."), in unspecified
order as in the JDK. Empty list if this is not a readable directory.
*/
ArrayList<String>
list() const;

/** Same as list(), but as File objects; the caller owns them. */
ArrayList<File*>
listFiles() const;
};


#endif
