#ifndef __FILE_H__
#define __FILE_H__

#include <stddef.h>
#include "java/lang/String.h"
#include "java/util/ArrayList.h"

class File {
  private:
String path;

static bool
isValidPath(const char *rawPath);

static bool
canOpenWithMode(const char *rawPath, const char *mode, int *errorCode = NULL);

static bool
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

bool
exists() const;

bool
isDirectory() const;

bool
isFile() const;

bool
canRead() const;

bool
canWrite() const;

bool
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
