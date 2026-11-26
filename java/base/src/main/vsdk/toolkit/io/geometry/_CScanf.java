package vsdk.toolkit.io.geometry;

/**
Java-only helper of the readers ported from cpp: the subset of C `sscanf`,
`strtod` and `strtol` that the MIT scene, altimetry and mesh readers use,
with the same return values (assigned conversions, or -1 when the input ends
before the first assigned conversion, as glibc does), so the ported parsing code keeps its control
flow. Supported directives: white space, `%d`, `%lf`, `%f`, `%s` and `%Ns`,
each optionally suppressed with `*`. Numbers follow the C locale grammar:
decimal and hexadecimal floating point, `inf`, `infinity` and `nan`.
Outputs are one-element holders: `int[]` for `%d`, `double[]` for `%lf`
and `String[]` for `%s`.
*/
final class _CScanf
{
    private _CScanf()
    {
    }

    private static boolean isSpace(char c)
    {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000b' ||
            c == '\f' || c == '\r';
    }

    private static boolean isDigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    private static boolean isHexDigit(char c)
    {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean matchesIgnoreCase(String s, int at, String word)
    {
        return s.regionMatches(true, at, word, 0, word.length());
    }

    /**
    Length of the longest floating point prefix of `s` starting at `start`,
    or 0 when there is none. `value[0]` receives the parsed number.
    */
    static int floatPrefix(String s, int start, double[] value)
    {
        return floatPrefix(s, start, value, false);
    }

    /**
    With `scanfExponent`, an exponent marker without digits is consumed as
    glibc `scanf` does ("1e" reads as 1), while `strtod` leaves it unread.
    */
    static int floatPrefix(String s, int start, double[] value, boolean scanfExponent)
    {
        int i = start;
        int n = s.length();
        boolean negative = false;
        if ( i < n && (s.charAt(i) == '+' || s.charAt(i) == '-') ) {
            negative = s.charAt(i) == '-';
            i++;
        }
        if ( matchesIgnoreCase(s, i, "infinity") ) {
            value[0] = negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            return i + 8 - start;
        }
        if ( matchesIgnoreCase(s, i, "inf") ) {
            value[0] = negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            return i + 3 - start;
        }
        if ( matchesIgnoreCase(s, i, "nan") ) {
            int end = i + 3;
            if ( end < n && s.charAt(end) == '(' ) {
                int k = end + 1;
                while ( k < n && (Character.isLetterOrDigit(s.charAt(k)) || s.charAt(k) == '_') ) k++;
                if ( k < n && s.charAt(k) == ')' ) end = k + 1;
            }
            value[0] = Double.NaN;
            return end - start;
        }
        if ( i + 1 < n && s.charAt(i) == '0' &&
             (s.charAt(i+1) == 'x' || s.charAt(i+1) == 'X') ) {
            int k = i + 2;
            int digits = 0;
            StringBuilder mantissa = new StringBuilder();
            while ( k < n && isHexDigit(s.charAt(k)) ) { mantissa.append(s.charAt(k)); k++; digits++; }
            StringBuilder fraction = new StringBuilder();
            if ( k < n && s.charAt(k) == '.' ) {
                int f = k + 1;
                while ( f < n && isHexDigit(s.charAt(f)) ) { fraction.append(s.charAt(f)); f++; digits++; }
                if ( digits > 0 ) k = f;
            }
            if ( digits > 0 ) {
                String exponent = "0";
                if ( k < n && (s.charAt(k) == 'p' || s.charAt(k) == 'P') ) {
                    int e = k + 1;
                    int signAt = e;
                    if ( e < n && (s.charAt(e) == '+' || s.charAt(e) == '-') ) e++;
                    int digitsStart = e;
                    while ( e < n && isDigit(s.charAt(e)) ) e++;
                    if ( e > digitsStart ) {
                        exponent = s.substring(signAt, e);
                        k = e;
                    }
                }
                String text = "0x" + (mantissa.length() > 0 ? mantissa : "0") + "." +
                    fraction + "p" + exponent;
                double v = Double.parseDouble(text);
                value[0] = negative ? -v : v;
                return k - start;
            }
            // "0x" without hexadecimal digits: the "0" alone is the number.
        }
        int k = i;
        int digits = 0;
        while ( k < n && isDigit(s.charAt(k)) ) { k++; digits++; }
        if ( k < n && s.charAt(k) == '.' ) {
            int f = k + 1;
            int fractionDigits = 0;
            while ( f < n && isDigit(s.charAt(f)) ) { f++; fractionDigits++; }
            if ( digits + fractionDigits > 0 ) {
                k = f;
                digits += fractionDigits;
            }
        }
        if ( digits == 0 ) return 0;
        if ( k < n && (s.charAt(k) == 'e' || s.charAt(k) == 'E') ) {
            int e = k + 1;
            if ( e < n && (s.charAt(e) == '+' || s.charAt(e) == '-') ) e++;
            int digitsStart = e;
            while ( e < n && isDigit(s.charAt(e)) ) e++;
            if ( e > digitsStart || scanfExponent ) k = e;
        }
        String text = s.substring(i, k);
        if ( scanfExponent ) {
            // Drop an exponent marker (and sign) that has no digits.
            int cut = text.length();
            while ( cut > 0 && !isDigit(text.charAt(cut-1)) && text.charAt(cut-1) != '.' ) cut--;
            text = text.substring(0, cut);
        }
        if ( text.endsWith(".") ) text = text + "0";
        if ( text.startsWith(".") ) text = "0" + text;
        double v = Double.parseDouble(text);
        value[0] = negative ? -v : v;
        return k - start;
    }

    /**
    Length of the longest decimal integer prefix (optional sign, digits) of
    `s` starting at `start`, or 0 when there is none.
    */
    static int integerPrefix(String s, int start, long[] value)
    {
        int i = start;
        int n = s.length();
        boolean negative = false;
        if ( i < n && (s.charAt(i) == '+' || s.charAt(i) == '-') ) {
            negative = s.charAt(i) == '-';
            i++;
        }
        int k = i;
        long v = 0;
        while ( k < n && isDigit(s.charAt(k)) ) {
            if ( v < Long.MAX_VALUE / 10 ) v = v*10 + (s.charAt(k) - '0');
            k++;
        }
        if ( k == i ) return 0;
        value[0] = negative ? -v : v;
        return k - start;
    }

    /**
    C `strtod(s + start, &end)`: skips leading white space and parses the
    longest floating point prefix into `value[0]`.
    @return the index just after the number (C `end`), or `start` when no
    conversion is performed (then `value[0]` is 0)
    */
    static int strtod(String s, int start, double[] value)
    {
        int i = start;
        while ( i < s.length() && isSpace(s.charAt(i)) ) i++;
        int length = floatPrefix(s, i, value);
        if ( length == 0 ) {
            value[0] = 0.0;
            return start;
        }
        return i + length;
    }

    /**
    C `strtod` over a whole token: the number when the token is entirely a
    floating point number, otherwise null.
    */
    static Double strtodWhole(String token)
    {
        int i = 0;
        while ( i < token.length() && isSpace(token.charAt(i)) ) i++;
        double[] value = {0};
        int length = floatPrefix(token, i, value);
        if ( length == 0 || i + length != token.length() ) return null;
        return value[0];
    }

    /** C `sscanf`. */
    static int sscanf(String input, String format, Object... outputs)
    {
        int in = 0;
        int out = 0;
        int assigned = 0;
        int n = input.length();
        for ( int f = 0; f < format.length(); f++ ) {
            char fc = format.charAt(f);
            if ( isSpace(fc) ) {
                while ( in < n && isSpace(input.charAt(in)) ) in++;
                continue;
            }
            if ( fc != '%' ) {
                if ( in >= n ) return assigned > 0 ? assigned : -1;
                if ( input.charAt(in) != fc ) return assigned;
                in++;
                continue;
            }
            f++;
            boolean suppress = false;
            if ( format.charAt(f) == '*' ) { suppress = true; f++; }
            int width = 0;
            while ( isDigit(format.charAt(f)) ) { width = width*10 + (format.charAt(f) - '0'); f++; }
            boolean longModifier = false;
            if ( format.charAt(f) == 'l' ) { longModifier = true; f++; }
            char conversion = format.charAt(f);
            while ( in < n && isSpace(input.charAt(in)) ) in++;
            if ( in >= n ) return assigned > 0 ? assigned : -1;
            String scope = width > 0 && in + width < n ? input.substring(0, in + width) : input;
            if ( conversion == 's' ) {
                int k = in;
                while ( k < scope.length() && !isSpace(scope.charAt(k)) ) k++;
                if ( !suppress ) {
                    ((String[])outputs[out++])[0] = input.substring(in, k);
                    assigned++;
                }
                in = k;
            }
            else if ( conversion == 'd' ) {
                long[] value = {0};
                int length = integerPrefix(scope, in, value);
                if ( length == 0 ) return assigned;
                if ( !suppress ) {
                    ((int[])outputs[out++])[0] = (int)value[0];
                    assigned++;
                }
                in += length;
            }
            else if ( conversion == 'f' ) {
                double[] value = {0};
                int length = floatPrefix(scope, in, value, true);
                if ( length == 0 ) return assigned;
                if ( !suppress ) {
                    if ( !longModifier ) {
                        throw new IllegalArgumentException("only %lf conversions are supported");
                    }
                    ((double[])outputs[out++])[0] = value[0];
                    assigned++;
                }
                in += length;
            }
            else {
                throw new IllegalArgumentException("unsupported conversion %" + conversion);
            }
        }
        return assigned;
    }
}
