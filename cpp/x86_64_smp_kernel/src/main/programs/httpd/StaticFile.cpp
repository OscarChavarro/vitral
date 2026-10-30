#include <stdio.h>
#include "programs/httpd/StaticFile.h"

void *
StaticFile::open(const char *path, uint64_t &size) {
    FILE *file = fopen(path, "rb");
    if (!file) {
        return nullptr;
    }
    if (fseek(file, 0, SEEK_END) != 0) {
        fclose(file);
        return nullptr;
    }
    long length = ftell(file);
    if (length < 0 || fseek(file, 0, SEEK_SET) != 0) {
        fclose(file);
        return nullptr;
    }
    size = (uint64_t)length;
    return file;
}

size_t
StaticFile::read(void *file, void *buffer, size_t size) { return fread(buffer, 1, size, (FILE *)file); }

void
StaticFile::close(void *file) { fclose((FILE *)file); }
