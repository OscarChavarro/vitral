import { WidgetElement } from "./WidgetElement.js";

/**
Element of a menu tree (composite design pattern). Names are given in the
Windows32 SDK API / Aquynza coded style, see `processSimplifiedName`.

A Java `char` is kept as a one character string here; the "no character"
value `'\0'` is the string `"\0"`.
*/
export abstract class WidgetMenuElement extends WidgetElement {
    public abstract override toString(): string;
    public abstract toStringAtLevel(level: number): string;

    private fromHex(c: string): number {
        let i = -1;

        switch (c) {
            case "0": i = 0; break;
            case "1": i = 1; break;
            case "2": i = 2; break;
            case "3": i = 3; break;
            case "4": i = 4; break;
            case "5": i = 5; break;
            case "6": i = 6; break;
            case "7": i = 7; break;
            case "8": i = 8; break;
            case "9": i = 9; break;
            case "a": case "A": i = 10; break;
            case "b": case "B": i = 11; break;
            case "c": case "C": i = 12; break;
            case "d": case "D": i = 13; break;
            case "e": case "E": i = 14; break;
            case "f": case "F": i = 15; break;
        }
        return i;
    }

    /**
    Given a Windows32 SDK API / Aquynza style coded name, this method
    generates the simplified name, separating from it its mnemonic and
    accelerator if any.

    For example, if codedName has the value <code>&Open\tCtrl+O</code>, the
    simplified name will be "Open", the mnemonic will be 'O' and the
    accelerator will be "Ctrl+O". This method return its simplified name.
    */
    protected processSimplifiedName(codedName: string | null): string | null {
        if (codedName === null) return null;

        let simplifiedName = "";

        let i: number;
        let c: string;

        for (i = 0; i < codedName.length; i++) {
            c = codedName.charAt(i);
            if (c === "&") continue;

            if (c === "\t") {
                break;
            }

            if (c === "#") {
                // Process UNICODE escape sequences...
                const start = i;
                i++;
                let num1 = 0;
                let num2 = 0;
                let num3 = 0;
                let num4: number;
                let num: number;
                for (; i < codedName.length; i++) {
                    c = codedName.charAt(i);
                    if (c === "#") {
                        simplifiedName = simplifiedName + c;
                        break;
                    }
                    num = this.fromHex(c);
                    if (num < 0) {
                        break;
                    }
                    if (i === start + 1) {
                        num1 = num;
                    } else if (i === start + 2) {
                        num2 = num;
                    } else if (i === start + 3) {
                        num3 = num;
                    } else if (i === start + 4) {
                        num4 = num;
                        c = String.fromCharCode((num1 << 12) | (num2 << 8) | (num3 << 4) | num4);
                        simplifiedName = simplifiedName + c;
                        break;
                    }
                }
            } else {
                simplifiedName = simplifiedName + c;
            }
        }

        return simplifiedName;
    }

    /**
    Given a Windows32 SDK API / Aquynza style coded name, this method
    generates the simplified name, separating from it its mnemonic and
    accelerator if any.

    For example, if codedName has the value <code>&Open\tCtrl+O</code>, the
    simplified name will be "Open", the mnemonic will be 'O' and the
    accelerator will be "Ctrl+O". This method return its mnemonic.
    */
    protected processMnemonic(codedName: string | null): string {
        if (codedName === null) return "\0";

        let i: number;
        let c: string;

        for (i = 0; i < codedName.length - 1; i++) {
            c = codedName.charAt(i);
            if (c === "&") {
                i++;
                c = codedName.charAt(i);
                return c;
            }
        }

        return "\0";
    }

    /**
    Given a Windows32 SDK API / Aquynza style coded name, this method
    generates the simplified name, separating from it its mnemonic and
    accelerator if any.

    For example, if codedName has the value <code>Open\tCtrl+O</code>, the
    simplified name will be "Open", the mnemonic will be 'O' and the
    accelerator will be "Ctrl+O". This method return its accelerator,
    or null if it is no accelerator.
    */
    protected processAccelerator(codedName: string | null): string | null {
        if (codedName === null) return null;

        let accelerator = "";

        let i: number;
        let c: string;

        for (i = 0; i < codedName.length; i++) {
            c = codedName.charAt(i);
            if (c === "\t") break;
        }

        for (i++; i < codedName.length; i++) {
            c = codedName.charAt(i);
            accelerator = accelerator + c;
        }

        if (accelerator.length <= 0) return null;

        return accelerator;
    }
}
