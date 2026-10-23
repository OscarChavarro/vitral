#include <ctype.h>
#include <locale.h>
#include <string.h>
// First: the X11R6 headers (i.e. Debian 1.1) do not include it themselves
#include <X11/Xlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/StringBuilder.h"
#include "vsdk/toolkit/gui/XtTextEncoding.h"

#ifdef VITRAL_X_LOCALE
// Xlib with its own locale implementation (X_LOCALE): its locale is set
// with _Xsetlocale, whose LC_CTYPE is the one of <X11/Xlocale.h>
extern "C" char* _Xsetlocale(int category, const char* name);
static const int X_LOCALE_LC_CTYPE = 2;
#endif

static BOOLEAN utf8Locale = TRUE;

/**
@return true if the locale name tells a UTF-8 encoding (i.e. "C.UTF-8",
"en_US.utf8")
*/
static BOOLEAN isUtf8LocaleName(const char* name)
{
    if ( name == NULL ) {
        return FALSE;
    }
    for ( const char* p = name; *p != '\0'; p++ ) {
        if ( tolower((unsigned char)p[0]) == 'u' &&
             tolower((unsigned char)p[1]) == 't' &&
             tolower((unsigned char)p[2]) == 'f' ) {
            const char* digits = p + 3;
            if ( *digits == '-' || *digits == '_' ) {
                digits++;
            }
            return digits[0] == '8' && !isdigit((unsigned char)digits[1]);
        }
    }
    return FALSE;
}

static BOOLEAN selectUtf8Locale(const char* name)
{
    const char* selected = setlocale(LC_CTYPE, name);
    return selected != NULL && isUtf8LocaleName(selected) &&
        XSupportsLocale();
}

BOOLEAN XtTextEncoding::selectLocale(const char* overrideLocale)
{
    if ( overrideLocale != NULL ) {
        const char* selected = setlocale(LC_CTYPE, overrideLocale);
        if ( selected != NULL ) {
            utf8Locale = isUtf8LocaleName(selected) && XSupportsLocale();
            return utf8Locale;
        }
    }
    // UI text is UTF-8 regardless of the selected JSON language. A neutral
    // Unicode locale lets English labels such as "Español" and future
    // translations share one FontSet encoding.
    if ( selectUtf8Locale("C.UTF-8") || selectUtf8Locale("") ) {
        utf8Locale = TRUE;
        return TRUE;
    }

    // No UTF-8 locale: Latin-1, whose fonts X always has
    utf8Locale = FALSE;
#ifdef VITRAL_X_LOCALE
    _Xsetlocale(X_LOCALE_LC_CTYPE, "en_US.ISO8859-1");
#else
    if ( setlocale(LC_CTYPE, "en_US.ISO8859-1") == NULL &&
         setlocale(LC_CTYPE, "en_US.ISO-8859-1") == NULL ) {
        setlocale(LC_CTYPE, "");
    }
#endif
    return FALSE;
}

BOOLEAN XtTextEncoding::isUtf8()
{
    return utf8Locale;
}

String XtTextEncoding::toLocale(const String& text)
{
    if ( utf8Locale ) {
        return text;
    }
    const unsigned char* p = (const unsigned char*)text.c_str();
    StringBuilder latin1;
    while ( *p != '\0' ) {
        unsigned int c = *p;
        int length = 1;
        unsigned long codePoint = c;
        if ( c >= 0xC0 && c < 0xE0 && (p[1] & 0xC0) == 0x80 ) {
            codePoint = ((c & 0x1F) << 6) | (p[1] & 0x3F);
            length = 2;
        }
        else if ( c >= 0xE0 && c < 0xF0 && (p[1] & 0xC0) == 0x80 &&
                  (p[2] & 0xC0) == 0x80 ) {
            codePoint = 0x10000;
            length = 3;
        }
        else if ( c >= 0xF0 && (p[1] & 0xC0) == 0x80 &&
                  (p[2] & 0xC0) == 0x80 && (p[3] & 0xC0) == 0x80 ) {
            codePoint = 0x10000;
            length = 4;
        }
        // Bytes of invalid sequences are kept as they are
        latin1.append((char)(codePoint <= 0xFF ? codePoint : '?'));
        p += length;
    }
    return latin1.toString();
}

String XtTextEncoding::fromLocale(const String& text)
{
    if ( utf8Locale ) {
        return text;
    }
    const unsigned char* p = (const unsigned char*)text.c_str();
    StringBuilder utf8;
    for ( ; *p != '\0'; p++ ) {
        if ( *p < 0x80 ) {
            utf8.append((char)*p);
        }
        else {
            utf8.append((char)(0xC0 | (*p >> 6)));
            utf8.append((char)(0x80 | (*p & 0x3F)));
        }
    }
    return utf8.toString();
}
