#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <climits>
#include <cstdlib>
#include <dirent.h>
#include <sys/stat.h>

#include "io/FolderListing.h"

bool FolderListing::isFolder(const java::String& path)
{
    struct stat status;
    return stat(path.c_str(), &status) == 0 && S_ISDIR(status.st_mode);
}

java::String FolderListing::normalizeFolder(const java::String& folder)
{
    char resolved[PATH_MAX];
    if ( realpath(folder.c_str(), resolved) != nullptr ) {
        return resolved;
    }
    return folder;
}

bool FolderListing::accepts(const java::ArrayList<FileSuffixFilter>& filters,
                            const java::String& path)
{
    if ( filters.isEmpty() || isFolder(path) ) {
        return true;
    }
    java::File file(path.c_str());
    for ( size_t i = 0; i < filters.size(); i++ ) {
        if ( filters[i].accept(file) ) {
            return true;
        }
    }
    return false;
}

void FolderListing::list(const java::String& folder,
                         const java::ArrayList<FileSuffixFilter>& filters,
                         java::ArrayList<java::String>& folders,
                         java::ArrayList<java::String>& files)
{
    folders.clear();
    files.clear();
    DIR* directory = opendir(folder.c_str());
    if ( directory == nullptr ) {
        return;
    }
    struct dirent* entry;
    while ( (entry = readdir(directory)) != nullptr ) {
        java::String name = entry->d_name;
        if ( name.isEmpty() || name[0] == '.' ) {
            continue;
        }
        java::String path = folder + "/" + name;
        if ( isFolder(path) ) {
            folders.add(name);
        }
        else if ( accepts(filters, path) ) {
            files.add(name);
        }
    }
    closedir(directory);
    folders.sort(nullptr);
    files.sort(nullptr);
}
