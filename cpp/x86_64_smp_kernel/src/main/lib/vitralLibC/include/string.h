#ifndef __VITRAL_LIBC_STRING__
#define __VITRAL_LIBC_STRING__
#include <stddef.h>
void *
memcpy(void *destination, const void *source, size_t count) asm("memcpy");
void *
memmove(void *destination, const void *source, size_t count) asm("memmove");
void *
memset(void *destination, int byte, size_t count) asm("memset");
int
memcmp(const void *a, const void *b, size_t count) asm("memcmp");
void *
memchr(const void *block, int byte, size_t count);
size_t
strlen(const char *text);
size_t
strnlen(const char *text, size_t limit);
int
strcmp(const char *a, const char *b);
int
strncmp(const char *a, const char *b, size_t count);
int
strcasecmp(const char *a, const char *b);
int
strncasecmp(const char *a, const char *b, size_t count);
int
strcoll(const char *a, const char *b);
char *
strcpy(char *destination, const char *source);
char *
strncpy(char *destination, const char *source, size_t count);
char *
strcat(char *destination, const char *source);
char *
strncat(char *destination, const char *source, size_t count);
char *
strchr(const char *text, int character);
char *
strrchr(const char *text, int character);
char *
strstr(const char *text, const char *needle);
char *
strpbrk(const char *text, const char *set);
size_t
strspn(const char *text, const char *set);
size_t
strcspn(const char *text, const char *set);
char *
strtok(char *text, const char *delimiters);
char *
strtok_r(char *text, const char *delimiters, char **state);
char *
strdup(const char *text);
char *
strndup(const char *text, size_t count);
char *
strerror(int code);

#endif
