#include <errno.h>
#include <stdio.h>
#include <string.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/File.h"
#include <dirent.h>
#include <sys/stat.h>
#ifdef __TURBOC__
// getcwd and mkdir, without permissions on DOS
#include <dir.h>
#define mkdir(path, mode) mkdir(path)
#else
#include <unistd.h>
#endif
#include "java/util/ArrayList.txx"

BOOLEAN
File::isValidPath(const char *rawPath) {
    return rawPath != NULL && rawPath[0] != '\0';
}

BOOLEAN
File::canOpenWithMode(const char *rawPath, const char *mode, int *errorCode) {
    errno = 0;
    FILE *probe = fopen(rawPath, mode);
    if ( probe == NULL ) {
        if ( errorCode != NULL ) {
            *errorCode = errno;
        }
        return FALSE;
    }
    fclose(probe);
    if ( errorCode != NULL ) {
        *errorCode = 0;
    }
    return TRUE;
}

BOOLEAN
File::isDirectoryByReadProbe(const char *rawPath) {
    int openError = 0;
    if ( canOpenWithMode(rawPath, "rb", &openError) ) {
        errno = 0;
        FILE *probe = fopen(rawPath, "rb");
        if ( probe == NULL ) {
            return FALSE;
        }
        unsigned char byteProbe = 0;
        errno = 0;
        const size_t readCount = fread(&byteProbe, 1, 1, probe);
        const BOOLEAN isDirectory = (readCount == 0) && (ferror(probe) != 0) && (errno == EISDIR);
        fclose(probe);
        return isDirectory;
    }

    if ( openError == EISDIR ) {
        return TRUE;
    }

    const size_t pathLength = strlen(rawPath);
    char *slashPath = new char[pathLength + 2];
    memcpy(slashPath, rawPath, pathLength);
    slashPath[pathLength] = '/';
    slashPath[pathLength + 1] = '\0';

    int slashError = 0;
    const BOOLEAN slashOpenOk = canOpenWithMode(slashPath, "rb", &slashError);
    delete[] slashPath;
    if ( slashOpenOk ) {
        return TRUE;
    }
    return slashError == EISDIR || slashError == EACCES || slashError == EPERM;
}

File::File():
    path()
{
}

File::File(const char *path):
    path(path)
{
}

File::File(const String &path):
    path(path)
{
}

File::~File() {
    dispose();
}

void
File::dispose() {
    path.dispose();
}

String
File::getPath() const {
    return path;
}

String
File::getAbsolutePath() const {
    if ( path.length() > 0 && path.charAt(0) == '/' ) {
        return path;
    }
    char buffer[4096];
    if ( getcwd(buffer, sizeof(buffer)) == NULL ) {
        return path;
    }
    String cwd(buffer);
    if ( path.length() == 0 ) {
        return cwd;
    }
    return cwd + "/" + path;
}

String
File::getName() const {
    const int separator = path.indexOf('/');
    if ( separator < 0 ) {
        return path;
    }
    int tailStart = separator + 1;
    int lastSeparator = separator;
    while ( TRUE ) {
        const int nextSeparator = path.indexOf('/', tailStart);
        if ( nextSeparator < 0 ) {
            break;
        }
        lastSeparator = nextSeparator;
        tailStart = nextSeparator + 1;
    }
    return path.substring(lastSeparator + 1);
}

BOOLEAN
File::exists() const {
    const char *rawPath = path.toCString();
    if ( !isValidPath(rawPath) ) {
        return FALSE;
    }

    int readError = 0;
    if ( canOpenWithMode(rawPath, "rb", &readError) ) {
        return TRUE;
    }
    if ( readError == EACCES || readError == EPERM || readError == EISDIR ) {
        return TRUE;
    }
    return isDirectoryByReadProbe(rawPath);
}

BOOLEAN
File::isDirectory() const {
    const char *rawPath = path.toCString();
    if ( !isValidPath(rawPath) ) {
        return FALSE;
    }

    return isDirectoryByReadProbe(rawPath);
}

BOOLEAN
File::isFile() const {
    if ( !exists() ) {
        return FALSE;
    }
    return !isDirectory();
}

BOOLEAN
File::canRead() const {
    const char *rawPath = path.toCString();
    if ( !isValidPath(rawPath) ) {
        return FALSE;
    }
    return canOpenWithMode(rawPath, "rb");
}

BOOLEAN
File::canWrite() const {
    const char *rawPath = path.toCString();
    if ( !isValidPath(rawPath) ) {
        return FALSE;
    }

    return canOpenWithMode(rawPath, "ab");
}

BOOLEAN
File::mkdirs() const {
    const char *rawPath = path.toCString();
    if ( !isValidPath(rawPath) ) {
        return FALSE;
    }

    if ( isDirectoryByReadProbe(rawPath) ) {
        return TRUE;
    }

    const size_t len = strlen(rawPath);
    char *tmp = new char[len + 1];
    memcpy(tmp, rawPath, len + 1);

    for ( size_t i = 1; i < len; i++ ) {
        if ( tmp[i] == '/' ) {
            tmp[i] = '\0';
            mkdir(tmp, 0775);
            tmp[i] = '/';
        }
    }

    errno = 0;
    const BOOLEAN created = (mkdir(tmp, 0775) == 0);
    delete[] tmp;

    return created || isDirectoryByReadProbe(rawPath);
}

ArrayList<String>
File::list() const {
    ArrayList<String> names;
    const char *rawPath = path.toCString();
    if ( !isValidPath(rawPath) ) {
        return names;
    }
    DIR *dir = opendir(rawPath);
    if ( dir == NULL ) {
        return names;
    }
    struct dirent *entry;
    while ( (entry = readdir(dir)) != NULL ) {
        if ( strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0 ) {
            continue;
        }
        names.add(String(entry->d_name));
    }
    closedir(dir);
    return names;
}

ArrayList<File*>
File::listFiles() const {
    ArrayList<File*> files;
    ArrayList<String> names = list();
    const String base = path;
    const BOOLEAN needsSlash = base.length() > 0 && base.charAt(base.length() - 1) != '/';
    for ( long int i = 0; i < names.size(); i++ ) {
        size_t total = strlen(base.c_str()) + strlen(names[i].c_str()) + 2;
        char *full = new char[total];
        snprintf(full, total, "%s%s%s", base.c_str(), needsSlash ? "/" : "", names[i].c_str());
        files.add(new File(full));
        delete[] full;
    }
    return files;
}

