#include <string.h>
#include <stdlib.h>
#include <ctype.h>

void *
memchr(const void *block, int byte, size_t count) {
    const unsigned char *p = (const unsigned char *)block;
    for (; count; count--, p++) {
        if (*p == (unsigned char)byte) {
            return (void *)p;
        }
    }
    return nullptr;
}

size_t
strlen(const char *text) {
    size_t n = 0;
    while (text[n]) {
        n++;
    }
    return n;
}

size_t
strnlen(const char *text, size_t limit) {
    size_t n = 0;
    while (n < limit && text[n]) {
        n++;
    }
    return n;
}

int
strcmp(const char *a, const char *b) {
    while (*a && *a == *b) {
        a++, b++;
    }
    return (unsigned char)*a - (unsigned char)*b;
}

int
strncmp(const char *a, const char *b, size_t count) {
    for (; count; count--, a++, b++) {
        if (*a != *b) {
            return (unsigned char)*a - (unsigned char)*b;
        }
        if (!*a) {
            return 0;
        }
    }
    return 0;
}

int
strcasecmp(const char *a, const char *b) {
    while (*a && tolower((unsigned char)*a) == tolower((unsigned char)*b)) {
        a++, b++;
    }
    return tolower((unsigned char)*a) - tolower((unsigned char)*b);
}

int
strncasecmp(const char *a, const char *b, size_t count) {
    for (; count; count--, a++, b++) {
        int x = tolower((unsigned char)*a);
        int y = tolower((unsigned char)*b);
        if (x != y) {
            return x - y;
        }
        if (!x) {
            return 0;
        }
    }
    return 0;
}

int
strcoll(const char *a, const char *b) { return strcmp(a, b); }

char *
strcpy(char *destination, const char *source) {
    char *d = destination;
    while ((*d++ = *source++)) {
    }
    return destination;
}

char *
strncpy(char *destination, const char *source, size_t count) {
    size_t i = 0;
    for (; i < count && source[i]; i++) {
        destination[i] = source[i];
    }
    for (; i < count; i++) {
        destination[i] = 0;
    }
    return destination;
}

char *
strcat(char *destination, const char *source) {
    strcpy(destination + strlen(destination), source);
    return destination;
}

char *
strncat(char *destination, const char *source, size_t count) {
    char *d = destination + strlen(destination);
    size_t i = 0;
    for (; i < count && source[i]; i++) {
        d[i] = source[i];
    }
    d[i] = 0;
    return destination;
}

char *
strchr(const char *text, int character) {
    for (;; text++) {
        if (*text == (char)character) {
            return (char *)text;
        }
        if (!*text) {
            return nullptr;
        }
    }
}

char *
strrchr(const char *text, int character) {
    const char *found = nullptr;
    for (;; text++) {
        if (*text == (char)character) {
            found = text;
        }
        if (!*text) {
            return (char *)found;
        }
    }
}

char *
strstr(const char *text, const char *needle) {
    size_t n = strlen(needle);
    if (!n) {
        return (char *)text;
    }
    for (; *text; text++) {
        if (*text == *needle && strncmp(text, needle, n) == 0) {
            return (char *)text;
        }
    }
    return nullptr;
}

char *
strpbrk(const char *text, const char *set) {
    for (; *text; text++) {
        if (strchr(set, *text)) {
            return (char *)text;
        }
    }
    return nullptr;
}

size_t
strspn(const char *text, const char *set) {
    size_t n = 0;
    while (text[n] && strchr(set, text[n])) {
        n++;
    }
    return n;
}

size_t
strcspn(const char *text, const char *set) {
    size_t n = 0;
    while (text[n] && !strchr(set, text[n])) {
        n++;
    }
    return n;
}

static char *tokenState = nullptr;

char *
strtok_r(char *text, const char *delimiters, char **state) {
    if (!text) {
        text = *state;
    }
    if (!text) {
        return nullptr;
    }
    text += strspn(text, delimiters);
    if (!*text) {
        *state = nullptr;
        return nullptr;
    }
    char *end = text + strcspn(text, delimiters);
    if (*end) {
        *end++ = 0;
    }
    *state = *end ? end : nullptr;
    return text;
}

char *
strtok(char *text, const char *delimiters) { return strtok_r(text, delimiters, &tokenState); }

char *
strdup(const char *text) {
    size_t n = strlen(text) + 1;
    char *copy = (char *)malloc(n);
    if (copy) {
        memcpy(copy, text, n);
    }
    return copy;
}

char *
strndup(const char *text, size_t count) {
    size_t n = strnlen(text, count);
    char *copy = (char *)malloc(n + 1);
    if (copy) {
        memcpy(copy, text, n);
        copy[n] = 0;
    }
    return copy;
}

char *
strerror(int code) {
    switch (code) {
    case 0: return (char *)"Success";
    case 2: return (char *)"No such file or directory";
    case 4: return (char *)"Interrupted";
    case 9: return (char *)"Bad file descriptor";
    case 11: return (char *)"Resource temporarily unavailable";
    case 12: return (char *)"Out of memory";
    case 13: return (char *)"Permission denied";
    case 17: return (char *)"File exists";
    case 22: return (char *)"Invalid argument";
    case 38: return (char *)"Function not implemented";
    default: return (char *)"Unknown error";
    }
}
