#ifndef __HTTPD__
#define __HTTPD__
#include "syscalls/Syscalls.h"
#include "kernel/ArgList.h"

class Httpd {
public:
    static int main(const ArgList &args);

private:
    static constexpr uint16_t PORT = 80;
    static constexpr uint32_t REQUEST_TIMEOUT_MS = 5000;
    static constexpr size_t REQUEST_MAX = 8192;
    static constexpr size_t JSON_MAX = 4096;
    static constexpr const char *WEB_ROOT = "/media/ramdisk/www";

    class Page;

    static int serverLoop();
    static int serveConnection(int64_t socket);
    static void handle(int64_t socket, char *request, char *headers);
    static void sendHead(int64_t socket, int status, const char *reason, const char *type, size_t length, const char *extra);
    static void respond(int64_t socket, int status, const char *reason, const char *body, const char *extra = nullptr);
    static void serveFile(int64_t socket, const char *target);
    static void serveSystem(int64_t socket);
    static void upgrade(int64_t socket, const char *topic, char *headers);
    static bool header(const char *headers, const char *name, char *out, size_t size);
    static bool containsWord(const char *text, const char *word);
    static const char *contentType(const char *path);
};

#endif
