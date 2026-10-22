#include <limits.h>
#include <stdlib.h>
#include <dirent.h>
#include <sys/stat.h>
// realpath, which old C libraries (i.e. libc 5) declare there
#include <unistd.h>

#include "io/FolderListing.h"
#include "java/util/ArrayList.txx"
#include "java/util/Comparator.h"

bool FolderListing::isFolder(const String& path)
{
    struct stat status;
    return stat(path.c_str(), &status) == 0 && S_ISDIR(status.st_mode);
}

String FolderListing::normalizeFolder(const String& folder)
{
    char resolved[PATH_MAX];
    if ( realpath(folder.c_str(), resolved) != NULL ) {
        return resolved;
    }
    return folder;
}

bool FolderListing::accepts(const ArrayList<FileSuffixFilter>& filters,
                            const String& path)
{
    if ( filters.isEmpty() || isFolder(path) ) {
        return true;
    }
    File file(path.c_str());
    for ( long i = 0; i < filters.size(); i++ ) {
        if ( filters[i].accept(file) ) {
            return true;
        }
    }
    return false;
}

void FolderListing::list(const String& folder,
                         const ArrayList<FileSuffixFilter>& filters,
                         ArrayList<String>& folders,
                         ArrayList<String>& files)
{
    folders.clear();
    files.clear();
    DIR* directory = opendir(folder.c_str());
    if ( directory == NULL ) {
        return;
    }
    struct dirent* entry;
    while ( (entry = readdir(directory)) != NULL ) {
        String name = entry->d_name;
        if ( name.isEmpty() || name[0] == '.' ) {
            continue;
        }
        String path = folder + "/" + name;
        if ( isFolder(path) ) {
            folders.add(name);
        }
        else if ( accepts(filters, path) ) {
            files.add(name);
        }
    }
    closedir(directory);
    folders.sort(NaturalOrderComparator<String>());
    files.sort(NaturalOrderComparator<String>());
}
