#include "programs/httpd/Httpd.h"
#include "kernel/Idle.h"
#include "kernel/KernelLog.h"
#include "kernel/Smp.h"
#include "programs/httpd/Feeds.h"
#include "programs/httpd/StaticFile.h"
#include "programs/httpd/WebSocket.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "net/NetFlag.h"
#include "net/NetInterfaceInfo.h"
#include "kernel/ArgList.h"

class Httpd::Page {
public:
    explicit Page(size_t limit) : data(new char[limit]), capacity(limit), used(0), full(false) { data[0] = 0; }
    ~Page() { delete[] data; }
    Page(const Page &) = delete;
    Page &operator=(const Page &) = delete;

    void add(const char *text) {
        size_t n = String::length(text);
        if (used + n >= capacity) {
            full = true;
            return;
        }
        Memory::copy(data + used, text, n + 1);
        used += n;
    }
    void addEscaped(const char *text) {
        for (; *text; text++) {
            switch (*text) {
            case '&': add("&amp;"); break;
            case '<': add("&lt;"); break;
            case '>': add("&gt;"); break;
            default: {
                char one[2] = {*text, 0};
                add(one);
            }
            }
        }
    }
    template <class... Args> void addFormatted(const char *format, Args... args) {
        char line[256];
        Format::print(line, sizeof(line), format, args...);
        add(line);
    }
    const char *text() const { return data; }
    size_t length() const { return used; }
    bool truncated() const { return full; }

private:
    char *data;
    size_t capacity;
    size_t used;
    bool full;
};

int
Httpd::main(const ArgList &args) {
    if (args.count() == 2 && String::equals(args.at(0), "-c")) {
        int64_t socket = 0;
        for (const char *c = args.at(1); *c >= '0' && *c <= '9'; c++) {
            socket = socket * 10 + (*c - '0');
        }
        return serveConnection(socket);
    }
    return serverLoop();
}

int
Httpd::serverLoop() {
    int64_t listener = Syscalls::tcpListen(PORT);
    if (listener < 0) {
        kprintf("httpd: cannot listen on port %u (error %d)\n", (unsigned)PORT, (int)-listener);
        return 1;
    }
    kprintf("httpd: listening on TCP port %u\n", (unsigned)PORT);
    for (;;) {
        int64_t connection = Syscalls::tcpAccept(listener, 1000);
        int status;
        while (Syscalls::waitpid(-1, &status, Syscalls::WNOHANG) > 0) {}
        if (connection == -Syscalls::ERR_TIMEDOUT) {
            continue;
        }
        if (connection < 0) {
            kprintf("httpd: accept failed (error %d)\n", (int)-connection);
            return 1;
        }
        char handle[16];
        Format::print(handle, sizeof(handle), "%u", (unsigned)connection);
        ArgList child;
        child.add("-c");
        child.add(handle);
        if (Syscalls::run("httpd", child) < 0) {
            kprintf("httpd: cannot start a process for the connection\n");
            Syscalls::tcpClose(connection);
        }
    }
}

void
Httpd::sendHead(int64_t socket, int status, const char *reason, const char *type, size_t length, const char *extra) {
    char head[320];
    Format::print(head, sizeof(head),
                  "HTTP/1.1 %d %s\r\nContent-Type: %s\r\nContent-Length: %u\r\n%sCache-Control: no-cache\r\n"
                  "Connection: close\r\n\r\n",
                  status, reason, type, (unsigned)length, extra ? extra : "");
    Syscalls::tcpSend(socket, head, String::length(head));
}

void
Httpd::respond(int64_t socket, int status, const char *reason, const char *body, const char *extra) {
    size_t length = String::length(body);
    sendHead(socket, status, reason, "text/html; charset=utf-8", length, extra);
    if (length) {
        Syscalls::tcpSend(socket, body, length);
    }
}

bool
Httpd::header(const char *headers, const char *name, char *out, size_t size) {
    size_t nameLength = String::length(name);
    for (const char *line = headers; *line;) {
        bool match = true;
        for (size_t i = 0; i < nameLength && match; i++) {
            char c = line[i];
            if (c >= 'A' && c <= 'Z') {
                c += 'a' - 'A';
            }
            match = c == name[i];
        }
        if (match && line[nameLength] == ':') {
            const char *value = line + nameLength + 1;
            while (*value == ' ' || *value == '\t') {
                value++;
            }
            size_t n = 0;
            while (value[n] && value[n] != '\r' && value[n] != '\n' && n + 1 < size) {
                out[n] = value[n];
                n++;
            }
            while (n > 0 && (out[n - 1] == ' ' || out[n - 1] == '\t')) {
                n--;
            }
            out[n] = 0;
            return true;
        }
        while (*line && *line != '\n') {
            line++;
        }
        if (*line) {
            line++;
        }
    }
    return false;
}

bool
Httpd::containsWord(const char *text, const char *word) {
    size_t wordLength = String::length(word);
    for (const char *c = text; *c;) {
        while (*c == ' ' || *c == ',') {
            c++;
        }
        size_t n = 0;
        bool match = true;
        while (c[n] && c[n] != ',' && c[n] != ' ') {
            char x = c[n];
            if (x >= 'A' && x <= 'Z') {
                x += 'a' - 'A';
            }
            if (n >= wordLength || x != word[n]) {
                match = false;
            }
            n++;
        }
        if (match && n == wordLength) {
            return true;
        }
        c += n;
    }
    return false;
}

int
Httpd::serveConnection(int64_t socket) {
    char *request = new char[REQUEST_MAX + 1];
    size_t length = 0;
    bool complete = false;
    int64_t failure = 0;
    while (length < REQUEST_MAX) {
        int64_t n = Syscalls::tcpReceive(socket, request + length, REQUEST_MAX - length, REQUEST_TIMEOUT_MS);
        if (n <= 0) {
            failure = n;
            break;
        }
        size_t from = length > 3 ? length - 3 : 0;
        length += (size_t)n;
        request[length] = 0;
        for (size_t i = from + 3; i < length && !complete; i++) {
            complete = request[i - 3] == '\r' && request[i - 2] == '\n' && request[i - 1] == '\r' && request[i] == '\n';
        }
        if (complete) {
            break;
        }
    }
    request[length] = 0;

    if (!complete && failure == -Syscalls::ERR_TIMEDOUT) {
        respond(socket, 408, "Request Timeout", "<!DOCTYPE html><title>408</title><h1>408 Request Timeout</h1>");
    } else if (!complete) {
        if (failure == 0 && length > 0) {
            respond(socket, 400, "Bad Request", "<!DOCTYPE html><title>400</title><h1>400 Bad Request</h1>");
        }
    } else {
        char *lineEnd = request;
        while (*lineEnd != '\r') {
            lineEnd++;
        }
        char *headers = lineEnd + 2;
        *lineEnd = 0;
        handle(socket, request, headers);
    }
    delete[] request;
    Syscalls::tcpClose(socket);
    return 0;
}

void
Httpd::handle(int64_t socket, char *request, char *headers) {
    char *method = request;
    char *target = method;
    while (*target && *target != ' ') {
        target++;
    }
    if (*target) {
        *target++ = 0;
    }
    char *version = target;
    while (*version && *version != ' ') {
        version++;
    }
    if (*version) {
        *version++ = 0;
    }
    bool wellFormed = *method && *target == '/' && version[0] == 'H' && version[1] == 'T' && version[2] == 'T' &&
                      version[3] == 'P' && version[4] == '/' && version[5] == '1';
    if (!wellFormed) {
        respond(socket, 400, "Bad Request", "<!DOCTYPE html><title>400</title><h1>400 Bad Request</h1>");
        return;
    }
    if (!String::equals(method, "GET")) {
        respond(socket, 405, "Method Not Allowed", "<!DOCTYPE html><title>405</title><h1>405 Method Not Allowed</h1>",
                "Allow: GET\r\n");
        return;
    }
    for (char *c = target; *c; c++) {
        if (*c == '?' || *c == '#') {
            *c = 0;
            break;
        }
    }

    if (target[1] == 'w' && target[2] == 's' && target[3] == '/') {
        upgrade(socket, target + 4, headers);
    } else if (String::equals(target, "/api/system")) {
        serveSystem(socket);
    } else {
        serveFile(socket, target);
    }
}

void
Httpd::upgrade(int64_t socket, const char *topic, char *headers) {
    char value[96];
    char key[64];
    if (!Feeds::knowsTopic(topic)) {
        respond(socket, 404, "Not Found", "<!DOCTYPE html><title>404</title><h1>404 Not Found</h1>");
        return;
    }
    bool upgrading = header(headers, "upgrade", value, sizeof(value)) && containsWord(value, "websocket") &&
                     header(headers, "connection", value, sizeof(value)) && containsWord(value, "upgrade") &&
                     header(headers, "sec-websocket-key", key, sizeof(key));
    if (!upgrading) {
        respond(socket, 426, "Upgrade Required", "<!DOCTYPE html><title>426</title><h1>426 Upgrade Required</h1>",
                "Upgrade: websocket\r\n");
        return;
    }
    if (!header(headers, "sec-websocket-version", value, sizeof(value)) || !String::equals(value, "13")) {
        respond(socket, 400, "Bad Request", "<!DOCTYPE html><title>400</title><h1>400 Bad Request</h1>",
                "Sec-WebSocket-Version: 13\r\n");
        return;
    }
    char accept[WebSocket::ACCEPT_SIZE];
    WebSocket::acceptValue(key, accept);
    char reply[192];
    Format::print(reply, sizeof(reply),
                  "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: %s\r\n\r\n",
                  accept);
    if (Syscalls::tcpSend(socket, reply, String::length(reply)) < 0) {
        return;
    }
    WebSocket websocket(socket);
    Feeds::serve(websocket, topic);
}

const char *
Httpd::contentType(const char *path) {
    size_t n = String::length(path);
    size_t dot = n;
    while (dot > 0 && path[dot - 1] != '.' && path[dot - 1] != '/') {
        dot--;
    }
    const char *extension = dot > 0 && path[dot - 1] == '.' ? path + dot : "";
    static const class {
    public:
        const char *extension;
        const char *type;
    } TYPES[] = {
        {"html", "text/html; charset=utf-8"},  {"css", "text/css; charset=utf-8"},
        {"js", "text/javascript; charset=utf-8"}, {"json", "application/json"},
        {"svg", "image/svg+xml"},               {"png", "image/png"},
        {"jpg", "image/jpeg"},                  {"ico", "image/x-icon"},
        {"txt", "text/plain; charset=utf-8"},   {"wasm", "application/wasm"},
    };
    for (const auto &t : TYPES) {
        if (String::equals(extension, t.extension)) {
            return t.type;
        }
    }
    return "application/octet-stream";
}

void
Httpd::serveFile(int64_t socket, const char *target) {
    if (String::equals(target, "/")) {
        target = "/index.html";
    }
    bool valid = String::length(target) < 96;
    for (const char *c = target; *c && valid; c++) {
        bool plain = (*c >= 'a' && *c <= 'z') || (*c >= 'A' && *c <= 'Z') || (*c >= '0' && *c <= '9') || *c == '.' || *c == '_' ||
                     *c == '-' || *c == '/';
        if (!plain || (*c == '/' && c[1] == '/') || (*c == '/' && c[1] == '.')) {
            valid = false;
        }
    }
    char path[160];
    uint64_t size = 0;
    void *file = nullptr;
    if (valid) {
        Format::print(path, sizeof(path), "%s%s", WEB_ROOT, target);
        file = StaticFile::open(path, size);
    }
    if (!file) {
        respond(socket, 404, "Not Found", "<!DOCTYPE html><title>404</title><h1>404 Not Found</h1>");
        return;
    }
    sendHead(socket, 200, "OK", contentType(target), (size_t)size, nullptr);
    char *chunk = new char[4096];
    for (;;) {
        size_t n = StaticFile::read(file, chunk, 4096);
        if (n == 0 || Syscalls::tcpSend(socket, chunk, n) < 0) {
            break;
        }
    }
    delete[] chunk;
    StaticFile::close(file);
}

void
Httpd::serveSystem(int64_t socket) {
    Page page(JSON_MAX);
    int64_t started = 0;
    uint32_t seconds = (uint32_t)Syscalls::uptime(&started);
    page.addFormatted("{\"uptimeSeconds\":%u,\"startedAt\":%u,\"cpus\":{\"online\":%u,\"logical\":%u,\"cores\":%u,\"threadsPerCore\":%u},\"idle\":\"",
                      seconds, (uint32_t)started, Smp::onlineCount(), Smp::logicalCount(), Smp::coreCount(), Smp::threadsPerCore());
    page.addEscaped(Idle::name());
    page.add("\",\"interfaces\":[");
    NetInterfaceInfo info;
    for (unsigned i = 0; Syscalls::netInterface(i, &info) == 0; i++) {
        page.addFormatted("%s{\"name\":\"%s\",\"driver\":\"%s\",\"up\":%s,\"running\":%s,\"mtu\":%u,", i ? "," : "", info.name, info.driver,
                          (info.flags & NetFlag::UP) ? "true" : "false", (info.flags & NetFlag::RUNNING) ? "true" : "false", info.mtu);
        page.addFormatted("\"mac\":\"%02x:%02x:%02x:%02x:%02x:%02x\",\"ip\":\"%u.%u.%u.%u\",\"netmask\":\"%u.%u.%u.%u\",",
                          info.mac[0], info.mac[1], info.mac[2], info.mac[3], info.mac[4], info.mac[5], info.address[0], info.address[1],
                          info.address[2], info.address[3], info.netmask[0], info.netmask[1], info.netmask[2], info.netmask[3]);
        page.addFormatted("\"rxPackets\":%u,\"rxBytes\":%u,\"txPackets\":%u,\"txBytes\":%u}", (uint32_t)info.rxPackets,
                          (uint32_t)info.rxBytes, (uint32_t)info.txPackets, (uint32_t)info.txBytes);
    }
    page.add("]}");
    sendHead(socket, 200, "OK", "application/json", page.length(), nullptr);
    Syscalls::tcpSend(socket, page.text(), page.length());
}
