import { Double } from "../lang/Double.js";
import { IllegalStateException } from "../lang/IllegalStateException.js";
import { Reader } from "./Reader.js";

/**
Port of `java.io.StreamTokenizer`.

The previous TypeScript file of this name was a regular-expression
approximation: it had no character-type table, no configurable comment or
quote characters, no `lineno()` and no `eolIsSignificant` mode, so the scene
and geometry readers could not be ported against it. This is the JDK algorithm
instead, character type table included, so that a reader configured exactly as
its Java counterpart splits a file into exactly the same token stream.

Only the byte-oriented deprecated `StreamTokenizer(InputStream)` constructor is
left out; the platform-neutral `Reader` hierarchy is the supported input.
*/
export class StreamTokenizer {
    private static readonly NEED_CHAR = 2147483647;
    private static readonly SKIP_LF = 2147483646;

    private static readonly CT_WHITESPACE = 1;
    private static readonly CT_DIGIT = 2;
    private static readonly CT_ALPHA = 4;
    private static readonly CT_QUOTE = 8;
    private static readonly CT_COMMENT = 16;

    /** A constant indicating that the end of the stream has been read. */
    public static readonly TT_EOF = -1;

    /** A constant indicating that the end of the line has been read. */
    public static readonly TT_EOL = 10;

    /** A constant indicating that a number token has been read. */
    public static readonly TT_NUMBER = -2;

    /** A constant indicating that a word token has been read. */
    public static readonly TT_WORD = -3;

    /** A constant indicating that no token has been read, used for initializing ttype. */
    private static readonly TT_NOTHING = -4;

    private reader: Reader | null = null;

    private buf: string[] = new Array<string>(20);

    /**
    The next character to be considered by the nextToken method. May also be
    NEED_CHAR to indicate that a new character should be read, or SKIP_LF to
    indicate that a new character should be read and, if it is a '\n'
    character, it should be discarded and a second new character should be
    read.
    */
    private peekc: number = StreamTokenizer.NEED_CHAR;

    private pushedBack = false;
    private forceLower = false;
    /** The line number of the last token read */
    private LINENO = 1;

    private eolIsSignificantP = false;
    private slashSlashCommentsP = false;
    private slashStarCommentsP = false;

    private ctype: number[] = new Array<number>(256).fill(0);

    /**
    After a call to the nextToken method, this field contains the type of the
    token just read.
    */
    public ttype: number = StreamTokenizer.TT_NOTHING;

    /**
    If the current token is a word token, this field contains a string giving
    the characters of the word token.
    */
    public sval: string | undefined;

    /**
    If the current token is a number, this field contains the value of that
    number.
    */
    public nval = 0;

    public constructor(r: Reader) {
        this.wordChars("a".charCodeAt(0), "z".charCodeAt(0));
        this.wordChars("A".charCodeAt(0), "Z".charCodeAt(0));
        this.wordChars(128 + 32, 255);
        this.whitespaceChars(0, " ".charCodeAt(0));
        this.commentChar("/".charCodeAt(0));
        this.quoteChar('"'.charCodeAt(0));
        this.quoteChar("'".charCodeAt(0));
        this.parseNumbers();

        if (r === null) {
            throw new Error("NullPointerException");
        }
        this.reader = r;
    }

    /** Resets this tokenizer's syntax table so that all characters are "ordinary." */
    public resetSyntax(): void {
        for (let i = this.ctype.length; --i >= 0;) {
            this.ctype[i] = 0;
        }
    }

    /** Specifies that all characters c in the range low <= c <= high are word constituents. */
    public wordChars(low: number, hi: number): void {
        if (low < 0) {
            low = 0;
        }
        if (hi >= this.ctype.length) {
            hi = this.ctype.length - 1;
        }
        while (low <= hi) {
            this.ctype[low] = this.ctype[low]! | StreamTokenizer.CT_ALPHA;
            low++;
        }
    }

    /** Specifies that all characters c in the range low <= c <= high are white space characters. */
    public whitespaceChars(low: number, hi: number): void {
        if (low < 0) {
            low = 0;
        }
        if (hi >= this.ctype.length) {
            hi = this.ctype.length - 1;
        }
        while (low <= hi) {
            this.ctype[low++] = StreamTokenizer.CT_WHITESPACE;
        }
    }

    /** Specifies that all characters c in the range low <= c <= high are "ordinary" in this tokenizer. */
    public ordinaryChars(low: number, hi: number): void {
        if (low < 0) {
            low = 0;
        }
        if (hi >= this.ctype.length) {
            hi = this.ctype.length - 1;
        }
        while (low <= hi) {
            this.ctype[low++] = 0;
        }
    }

    /** Specifies that the character argument is "ordinary" in this tokenizer. */
    public ordinaryChar(ch: number): void {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = 0;
        }
    }

    /** Specified that the character argument starts a single-line comment. */
    public commentChar(ch: number): void {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = StreamTokenizer.CT_COMMENT;
        }
    }

    /** Specifies that matching pairs of this character delimit string constants in this tokenizer. */
    public quoteChar(ch: number): void {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = StreamTokenizer.CT_QUOTE;
        }
    }

    /** Specifies that numbers should be parsed by this tokenizer. */
    public parseNumbers(): void {
        for (let i = "0".charCodeAt(0); i <= "9".charCodeAt(0); i++) {
            this.ctype[i] = this.ctype[i]! | StreamTokenizer.CT_DIGIT;
        }
        this.ctype[".".charCodeAt(0)] = this.ctype[".".charCodeAt(0)]! | StreamTokenizer.CT_DIGIT;
        this.ctype["-".charCodeAt(0)] = this.ctype["-".charCodeAt(0)]! | StreamTokenizer.CT_DIGIT;
    }

    /** Determines whether or not ends of line are treated as tokens. */
    public eolIsSignificant(flag: boolean): void {
        this.eolIsSignificantP = flag;
    }

    /** Determines whether or not the tokenizer recognizes C-style comments. */
    public slashStarComments(flag: boolean): void {
        this.slashStarCommentsP = flag;
    }

    /** Determines whether or not the tokenizer recognizes C++-style comments. */
    public slashSlashComments(flag: boolean): void {
        this.slashSlashCommentsP = flag;
    }

    /** Determines whether or not word token are automatically lowercased. */
    public lowerCaseMode(fl: boolean): void {
        this.forceLower = fl;
    }

    /** Read the next character */
    private read(): number {
        if (this.reader !== null) {
            return this.reader.read();
        }
        throw new IllegalStateException();
    }

    /**
    Parses the next token from the input stream of this tokenizer. The type of
    the next token is returned in the ttype field. Additional information about
    the token may be in the nval field or the sval field of this tokenizer.
    */
    public nextToken(): number {
        if (this.pushedBack) {
            this.pushedBack = false;
            return this.ttype;
        }
        const ct: number[] = this.ctype;
        this.sval = undefined;

        let c: number = this.peekc;
        if (c < 0) {
            c = StreamTokenizer.NEED_CHAR;
        }
        if (c === StreamTokenizer.SKIP_LF) {
            c = this.read();
            if (c < 0) {
                return (this.ttype = StreamTokenizer.TT_EOF);
            }
            if (c === "\n".charCodeAt(0)) {
                c = StreamTokenizer.NEED_CHAR;
            }
        }
        if (c === StreamTokenizer.NEED_CHAR) {
            c = this.read();
            if (c < 0) {
                return (this.ttype = StreamTokenizer.TT_EOF);
            }
        }
        this.ttype = c; /* Just to be safe */

        /* Set peekc so that the next invocation of nextToken will read
         * another character unless peekc is reset in this invocation
         */
        this.peekc = StreamTokenizer.NEED_CHAR;

        let ctype: number = c < 256 ? ct[c]! : StreamTokenizer.CT_ALPHA;
        while ((ctype & StreamTokenizer.CT_WHITESPACE) !== 0) {
            if (c === "\r".charCodeAt(0)) {
                this.LINENO++;
                if (this.eolIsSignificantP) {
                    this.peekc = StreamTokenizer.SKIP_LF;
                    return (this.ttype = StreamTokenizer.TT_EOL);
                }
                c = this.read();
                if (c === "\n".charCodeAt(0)) {
                    c = this.read();
                }
            } else {
                if (c === "\n".charCodeAt(0)) {
                    this.LINENO++;
                    if (this.eolIsSignificantP) {
                        return (this.ttype = StreamTokenizer.TT_EOL);
                    }
                }
                c = this.read();
            }
            if (c < 0) {
                return (this.ttype = StreamTokenizer.TT_EOF);
            }
            ctype = c < 256 ? ct[c]! : StreamTokenizer.CT_ALPHA;
        }

        if ((ctype & StreamTokenizer.CT_DIGIT) !== 0) {
            let neg = false;
            if (c === "-".charCodeAt(0)) {
                c = this.read();
                if (c !== ".".charCodeAt(0) && (c < "0".charCodeAt(0) || c > "9".charCodeAt(0))) {
                    this.peekc = c;
                    return (this.ttype = "-".charCodeAt(0));
                }
                neg = true;
            }
            let v = 0;
            let decexp = 0;
            let seendot = 0;
            for (;;) {
                if (c === ".".charCodeAt(0) && seendot === 0) {
                    seendot = 1;
                } else if ("0".charCodeAt(0) <= c && c <= "9".charCodeAt(0)) {
                    v = v * 10 + (c - "0".charCodeAt(0));
                    decexp += seendot;
                } else {
                    break;
                }
                c = this.read();
            }
            this.peekc = c;
            if (decexp !== 0) {
                let denom = 10;
                decexp--;
                while (decexp > 0) {
                    denom *= 10;
                    decexp--;
                }
                /* Do one division of a likely-to-be-more-accurate number */
                v = v / denom;
            }
            this.nval = neg ? -v : v;
            return (this.ttype = StreamTokenizer.TT_NUMBER);
        }

        if ((ctype & StreamTokenizer.CT_ALPHA) !== 0) {
            let i = 0;
            do {
                if (i >= this.buf.length) {
                    this.buf = this.buf.concat(new Array<string>(this.buf.length));
                }
                this.buf[i++] = String.fromCharCode(c);
                c = this.read();
                ctype = c < 0 ? StreamTokenizer.CT_WHITESPACE : c < 256 ? ct[c]! : StreamTokenizer.CT_ALPHA;
            } while ((ctype & (StreamTokenizer.CT_ALPHA | StreamTokenizer.CT_DIGIT)) !== 0);
            this.peekc = c;
            this.sval = this.buf.slice(0, i).join("");
            if (this.forceLower) {
                this.sval = this.sval.toLowerCase();
            }
            return (this.ttype = StreamTokenizer.TT_WORD);
        }

        if ((ctype & StreamTokenizer.CT_QUOTE) !== 0) {
            this.ttype = c;
            let i = 0;
            /* Invariants (because \Octal needs a lookahead):
             *   (i)  c contains char value
             *   (ii) d contains the lookahead
             */
            let d: number = this.read();
            while (d >= 0 && d !== this.ttype && d !== "\n".charCodeAt(0) && d !== "\r".charCodeAt(0)) {
                if (d === "\\".charCodeAt(0)) {
                    c = this.read();
                    const first: number = c; /* To allow \377, but not \477 */
                    if (c >= "0".charCodeAt(0) && c <= "7".charCodeAt(0)) {
                        c = c - "0".charCodeAt(0);
                        let c2: number = this.read();
                        if ("0".charCodeAt(0) <= c2 && c2 <= "7".charCodeAt(0)) {
                            c = (c << 3) + (c2 - "0".charCodeAt(0));
                            c2 = this.read();
                            if ("0".charCodeAt(0) <= c2 && c2 <= "7".charCodeAt(0) && first <= "3".charCodeAt(0)) {
                                c = (c << 3) + (c2 - "0".charCodeAt(0));
                                d = this.read();
                            } else {
                                d = c2;
                            }
                        } else {
                            d = c2;
                        }
                    } else {
                        switch (c) {
                            case "a".charCodeAt(0):
                                c = 0x7;
                                break;
                            case "b".charCodeAt(0):
                                c = "\b".charCodeAt(0);
                                break;
                            case "f".charCodeAt(0):
                                c = 0xc;
                                break;
                            case "n".charCodeAt(0):
                                c = "\n".charCodeAt(0);
                                break;
                            case "r".charCodeAt(0):
                                c = "\r".charCodeAt(0);
                                break;
                            case "t".charCodeAt(0):
                                c = "\t".charCodeAt(0);
                                break;
                            case "v".charCodeAt(0):
                                c = 0xb;
                                break;
                            default:
                                break;
                        }
                        d = this.read();
                    }
                } else {
                    c = d;
                    d = this.read();
                }
                if (i >= this.buf.length) {
                    this.buf = this.buf.concat(new Array<string>(this.buf.length));
                }
                this.buf[i++] = String.fromCharCode(c);
            }

            /* If we broke out of the loop because we found a matching quote
             * character then arrange to read a new character next time
             * around; otherwise, save the character.
             */
            this.peekc = d === this.ttype ? StreamTokenizer.NEED_CHAR : d;

            this.sval = this.buf.slice(0, i).join("");
            return this.ttype;
        }

        if (c === "/".charCodeAt(0) && (this.slashSlashCommentsP || this.slashStarCommentsP)) {
            c = this.read();
            if (c === "*".charCodeAt(0) && this.slashStarCommentsP) {
                let prevc = 0;
                while ((c = this.read()) !== "/".charCodeAt(0) || prevc !== "*".charCodeAt(0)) {
                    if (c === "\r".charCodeAt(0)) {
                        this.LINENO++;
                        c = this.read();
                        if (c === "\n".charCodeAt(0)) {
                            c = this.read();
                        }
                    } else {
                        if (c === "\n".charCodeAt(0)) {
                            this.LINENO++;
                            c = this.read();
                        }
                    }
                    if (c < 0) {
                        return (this.ttype = StreamTokenizer.TT_EOF);
                    }
                    prevc = c;
                }
                return this.nextToken();
            } else if (c === "/".charCodeAt(0) && this.slashSlashCommentsP) {
                while ((c = this.read()) !== "\n".charCodeAt(0) && c !== "\r".charCodeAt(0) && c >= 0);
                this.peekc = c;
                return this.nextToken();
            } else {
                /* Now see if it is still a single line comment */
                if ((ct["/".charCodeAt(0)]! & StreamTokenizer.CT_COMMENT) !== 0) {
                    while ((c = this.read()) !== "\n".charCodeAt(0) && c !== "\r".charCodeAt(0) && c >= 0);
                    this.peekc = c;
                    return this.nextToken();
                } else {
                    this.peekc = c;
                    return (this.ttype = "/".charCodeAt(0));
                }
            }
        }

        if ((ctype & StreamTokenizer.CT_COMMENT) !== 0) {
            while ((c = this.read()) !== "\n".charCodeAt(0) && c !== "\r".charCodeAt(0) && c >= 0);
            this.peekc = c;
            return this.nextToken();
        }

        return (this.ttype = c);
    }

    /**
    Causes the next call to the nextToken method of this tokenizer to return
    the current value in the ttype field, and not to modify the value in the
    nval or sval field.
    */
    public pushBack(): void {
        if (this.ttype !== StreamTokenizer.TT_NOTHING) {
            /* No-op if nextToken() not called */
            this.pushedBack = true;
        }
    }

    /** Return the current line number. */
    public lineno(): number {
        return this.LINENO;
    }

    /** Returns the string representation of the current stream token and the line number it occurs on. */
    public toString(): string {
        let ret: string;
        switch (this.ttype) {
            case StreamTokenizer.TT_EOF:
                ret = "EOF";
                break;
            case StreamTokenizer.TT_EOL:
                ret = "EOL";
                break;
            case StreamTokenizer.TT_WORD:
                ret = this.sval as string;
                break;
            case StreamTokenizer.TT_NUMBER:
                ret = "n=" + Double.toString(this.nval);
                break;
            case StreamTokenizer.TT_NOTHING:
                ret = "NOTHING";
                break;
            default: {
                if (this.ttype < 256 && (this.ctype[this.ttype]! & StreamTokenizer.CT_QUOTE) !== 0) {
                    ret = this.sval as string;
                    break;
                }
                const s: string[] = new Array<string>(3);
                s[0] = s[2] = "'";
                s[1] = String.fromCharCode(this.ttype);
                ret = s.join("");
                break;
            }
        }
        return "Token[" + ret + "], line " + this.LINENO;
    }
}
