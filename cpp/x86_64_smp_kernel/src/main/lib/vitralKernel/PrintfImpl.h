#ifndef __PRINTF_IMPL__
#define __PRINTF_IMPL__
#include <stdarg.h>
#include <stdint.h>

class PrintfImpl {
public:
    template <class Out>
    static int pad(int count, char c) {
        for (int i = 0; i < count; i++) {
            Out::put(c);
        }
        return count > 0 ? count : 0;
    }

    template <class Out>
    static int emit_num(uint32_t v, unsigned base, bool upper, bool neg, int width, bool zero, bool left) {
        char buf[12];
        int n = 0;
        const char *digits = upper ? "0123456789ABCDEF" : "0123456789abcdef";
        do { buf[n++] = digits[v % base]; v /= base; } while (v);
        int len = n + (neg ? 1 : 0);
        int out = 0;
        if (!left && !zero) {
            out += pad<Out>(width - len, ' ');
        }
        if (neg) {
            Out::put('-');
            out++;
        }
        if (!left && zero) {
            out += pad<Out>(width - len, '0');
        }
        while (n) {
            Out::put(buf[--n]);
            out++;
        }
        if (left) {
            out += pad<Out>(width - len, ' ');
        }
        return out;
    }

    template <class Out>
    static int format(const char *fmt, va_list ap) {
        int count = 0;
        for (; *fmt; fmt++) {
            if (*fmt != '%') { Out::put(*fmt); count++; continue; }
            fmt++;
            bool zero = false;
            bool left = false;
            int width = 0;
            for (;; fmt++) {
                if (*fmt == '0') {
                    zero = true;
                } else if (*fmt == '-') {
                    left = true;
                } else {
                    break;
                }
            }
            while (*fmt >= '0' && *fmt <= '9') {
                width = width * 10 + (*fmt++ - '0');
            }
            switch (*fmt) {
            case 'c':
                if (!left) {
                    count += pad<Out>(width - 1, ' ');
                }
                Out::put((char)va_arg(ap, int));
                count++;
                if (left) {
                    count += pad<Out>(width - 1, ' ');
                }
                break;
            case 's': {
                const char *s = va_arg(ap, const char *);
                if (!s) {
                    s = "(null)";
                }
                int len = 0;
                while (s[len]) {
                    len++;
                }
                if (!left) {
                    count += pad<Out>(width - len, ' ');
                }
                for (; *s; s++, count++) {
                    Out::put(*s);
                }
                if (left) {
                    count += pad<Out>(width - len, ' ');
                }
                break;
            }
            case 'd': case 'i': {
                int32_t v = va_arg(ap, int32_t);
                count += v < 0 ? emit_num<Out>((uint32_t)-v, 10, false, true, width, zero, left)
                               : emit_num<Out>((uint32_t)v, 10, false, false, width, zero, left);
                break;
            }
            case 'u': count += emit_num<Out>(va_arg(ap, uint32_t), 10, false, false, width, zero, left); break;
            case 'x': count += emit_num<Out>(va_arg(ap, uint32_t), 16, false, false, width, zero, left); break;
            case 'X': count += emit_num<Out>(va_arg(ap, uint32_t), 16, true, false, width, zero, left); break;
            case 'p': Out::put('0'); Out::put('x'); count += 2 +
                      emit_num<Out>((uint32_t)(uintptr_t)va_arg(ap, void *), 16, false, false, 8, true, false); break;
            case '%': Out::put('%'); count++; break;
            case '\0': fmt--; break;
            default: Out::put('%'); Out::put(*fmt); count += 2; break;
            }
        }
        return count;
    }
};

#endif
