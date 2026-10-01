import {
    KeyEvent,
    MouseEvent as VitralMouseEvent,
    RGBAImageUncompressed,
    type ColorRgb,
} from "@vitral/base";

export interface WebKeyEvent {
    keycode: string;
    unicodeId: string;
    modifierMask: number;
}

export interface WebMouseEvent {
    x: number;
    y: number;
    button: number;
    buttons: number;
    modifiers: number;
    clicks: number;
}

/**
Gives the toolkit access to the GUI operations of a browser: the counterpart
of Java's `vsdk.toolkit.gui.AwtSystem`.

Two families of event conversions live here:
  - `web2vsdk*` give string-keyed events, used by the WebGL examples.
  - `toVitral*Event` give the toolkit's own `KeyEvent` and `MouseEvent`, as
    `AwtSystem.awt2vsdkEvent` does from AWT events, for code ported from Java
    that works over them (i.e. the interaction techniques of the viewports).
    The DOM event is first read the way AWT reports the same key press or
    mouse action (its key character, key code and extended modifiers), and
    then mapped with Java's rules.

`calculateLabelImage` rasterizes a text with a 2D canvas, as `AwtSystem` does
with Java2D.
*/
export class WebSystem {
    public static readonly MASK_CTRL = 0x0001;
    public static readonly MASK_ALT = 0x0008;
    public static readonly MASK_SHIFT = 0x0080;
    public static readonly MASK_META = 0x0400;

    private static readonly KEY_BY_CODE: ReadonlyMap<string, string> = new Map([
        ["Escape", "KEY_ESC"],
        ["Backspace", "KEY_BACKSPACE"],
        ["Tab", "KEY_TAB"],
        ["Enter", "KEY_ENTER"],
        ["NumpadEnter", "KEY_NUMENTER"],
        ["Space", "KEY_SPACE"],
        ["Insert", "KEY_INSERT"],
        ["Delete", "KEY_DELETE"],
        ["Home", "KEY_HOME"],
        ["End", "KEY_END"],
        ["PageUp", "KEY_PAGEUP"],
        ["PageDown", "KEY_PAGEDOWN"],
        ["ArrowUp", "KEY_UP"],
        ["ArrowDown", "KEY_DOWN"],
        ["ArrowLeft", "KEY_LEFT"],
        ["ArrowRight", "KEY_RIGHT"],
        ["ShiftLeft", "KEY_LSHIFT"],
        ["ShiftRight", "KEY_RSHIFT"],
        ["AltLeft", "KEY_LALT"],
        ["AltRight", "KEY_RALT"],
        ["ControlLeft", "KEY_LCTRL"],
        ["ControlRight", "KEY_RCTRL"],
        ["CapsLock", "KEY_CAPSLOCK"],
        ["NumLock", "KEY_NUMLOCK"],
        ["PrintScreen", "KEY_PRINTSCREEN"],
        ["Comma", "KEY_COMMA"],
        ["Period", "KEY_PERIOD"],
        ["NumpadDecimal", "KEY_NUMPERIOD"],
        ["NumpadDivide", "KEY_NUMSLASH"],
        ["NumpadMultiply", "KEY_NUMASTERISK"],
        ["NumpadSubtract", "KEY_NUMMINUS"],
        ["NumpadAdd", "KEY_NUMPLUS"],
    ]);

    public static web2vsdkKeyEvent(event: KeyboardEvent): WebKeyEvent {
        return {
            keycode: WebSystem.normalizeKey(event),
            unicodeId: WebSystem.normalizeUnicode(event),
            modifierMask: WebSystem.modifierMask(event),
        };
    }

    public static web2vsdkMouseEvent(event: MouseEvent, target?: HTMLElement): WebMouseEvent {
        const rect = target?.getBoundingClientRect();
        const x = rect ? event.clientX - rect.left : event.offsetX;
        const y = rect ? event.clientY - rect.top : event.offsetY;

        return {
            x,
            y,
            button: event.button,
            buttons: event.buttons,
            modifiers: WebSystem.modifierMask(event),
            clicks: 0,
        };
    }

    public static web2vsdkWheelEvent(event: WheelEvent, target?: HTMLElement): WebMouseEvent {
        return {
            ...WebSystem.web2vsdkMouseEvent(event, target),
            clicks: event.deltaY,
        };
    }

    private static modifierMask(event: MouseEvent | KeyboardEvent): number {
        let mask = 0;
        if (event.shiftKey) mask |= WebSystem.MASK_SHIFT;
        if (event.ctrlKey) mask |= WebSystem.MASK_CTRL;
        if (event.altKey) mask |= WebSystem.MASK_ALT;
        if (event.metaKey) mask |= WebSystem.MASK_META;
        return mask;
    }

    private static normalizeKey(event: KeyboardEvent): string {
        const mapped = WebSystem.KEY_BY_CODE.get(event.code);
        if (mapped) {
            return mapped;
        }

        if (/^Key[A-Z]$/.test(event.code)) {
            const letter = event.code.substring(3);
            return event.shiftKey ? `KEY_${letter}` : `KEY_${letter.toLowerCase()}`;
        }

        if (/^Digit[0-9]$/.test(event.code)) {
            return `KEY_${event.code.substring(5)}`;
        }

        if (/^Numpad[0-9]$/.test(event.code)) {
            return `KEY_NUM${event.code.substring(6)}`;
        }

        if (/^F([1-9]|1[0-2])$/.test(event.code)) {
            return `KEY_${event.code}`;
        }

        return "KEY_NONE";
    }

    private static normalizeUnicode(event: KeyboardEvent): string {
        if (event.key.length === 1) {
            return event.key;
        }
        if (event.code === "Space") {
            return " ";
        }
        return "";
    }

    //= Vitral events, as AwtSystem gives them =============================

    /// AWT's `getModifiersEx` masks
    private static readonly AWT_SHIFT_DOWN_MASK: number = 64;
    private static readonly AWT_CTRL_DOWN_MASK: number = 128;
    private static readonly AWT_META_DOWN_MASK: number = 256;
    private static readonly AWT_ALT_DOWN_MASK: number = 512;
    private static readonly AWT_BUTTON1_DOWN_MASK: number = 1024;
    private static readonly AWT_BUTTON2_DOWN_MASK: number = 2048;
    private static readonly AWT_BUTTON3_DOWN_MASK: number = 4096;

    /**
    @param event a DOM mouse or keyboard event
    @return the extended modifiers AWT would report for it
    (`InputEvent.getModifiersEx`)
    */
    public static awtModifiersEx(event: MouseEvent | KeyboardEvent): number {
        let mask: number = 0;
        if (event.shiftKey) mask |= WebSystem.AWT_SHIFT_DOWN_MASK;
        if (event.ctrlKey) mask |= WebSystem.AWT_CTRL_DOWN_MASK;
        if (event.metaKey) mask |= WebSystem.AWT_META_DOWN_MASK;
        if (event.altKey) mask |= WebSystem.AWT_ALT_DOWN_MASK;
        if (event instanceof MouseEvent) {
            // DOM `buttons`: 1 left, 2 right, 4 middle; AWT: 1 left, 2 middle, 3 right
            if ((event.buttons & 1) !== 0) mask |= WebSystem.AWT_BUTTON1_DOWN_MASK;
            if ((event.buttons & 4) !== 0) mask |= WebSystem.AWT_BUTTON2_DOWN_MASK;
            if ((event.buttons & 2) !== 0) mask |= WebSystem.AWT_BUTTON3_DOWN_MASK;
        }
        return mask;
    }

    /**
    @param event a DOM mouse event
    @return the button AWT would report (`MouseEvent.getButton`): 0 for none,
    1 left, 2 middle, 3 right
    */
    public static awtButton(event: MouseEvent): number {
        if (event.type === "pointermove" || event.type === "mousemove" || event.type === "wheel" ||
            event.type === "pointerenter" || event.type === "pointerleave") {
            return 0;
        }
        switch (event.button) {
            case 0: return 1;
            case 1: return 2;
            case 2: return 3;
            default: return 0;
        }
    }

    /**
    Java's `AwtSystem.awt2vsdkEvent(java.awt.event.MouseEvent)`.
    @param event DOM mouse event
    @param target element whose upper left corner is the origin of the
    coordinates (the AWT component); by default, the target of the event
    @return the vitral event, with coordinates in CSS pixels of the target
    */
    public static toVitralMouseEvent(event: MouseEvent, target?: HTMLElement): VitralMouseEvent {
        const evsdk: VitralMouseEvent = new VitralMouseEvent();
        const element: Element | null = target ?? (event.currentTarget instanceof Element ? event.currentTarget : null);
        const rect: DOMRect | null = element !== null ? element.getBoundingClientRect() : null;

        evsdk.setX(Math.round(rect !== null ? event.clientX - rect.left : event.offsetX));
        evsdk.setY(Math.round(rect !== null ? event.clientY - rect.top : event.offsetY));
        evsdk.setButton(WebSystem.awtButton(event));
        evsdk.setModifiers(WebSystem.awtModifiersEx(event));

        return evsdk;
    }

    /**
    Java's `AwtSystem.awt2vsdkEvent(java.awt.event.MouseWheelEvent)`. AWT
    reports whole notches (`getWheelRotation`, positive towards the user);
    the DOM reports a distance, whose sign is the notch direction.
    @param event DOM wheel event
    @param target element whose upper left corner is the origin of the
    coordinates
    @return the vitral event
    */
    public static toVitralWheelEvent(event: WheelEvent, target?: HTMLElement): VitralMouseEvent {
        const evsdk: VitralMouseEvent = WebSystem.toVitralMouseEvent(event, target);

        evsdk.setClicks(Math.sign(event.deltaY));
        return evsdk;
    }

    /**
    The character AWT reports for a key press (`KeyEvent.getKeyChar`): the
    character of the key, a control character for Ctrl+letter chords, the
    control characters of enter, backspace, tab, escape and delete, or
    `KEY_NONE` (`CHAR_UNDEFINED`) for keys without a character. With Alt,
    the letter of the key is used (browsers on macOS give the Option
    character instead, which AWT does not).
    */
    private static awtKeyChar(event: KeyboardEvent): number {
        if (event.ctrlKey && /^Key[A-Z]$/.test(event.code)) {
            return event.code.charCodeAt(3) & 0x1f;
        }
        if (event.altKey && /^Key[A-Z]$/.test(event.code)) {
            const letter: string = event.code.substring(3);
            return (event.shiftKey ? letter : letter.toLowerCase()).charCodeAt(0);
        }
        switch (event.key) {
            case "Enter": return 10;
            case "Backspace": return 8;
            case "Tab": return 9;
            case "Escape": return 27;
            case "Delete": return 127;
            default:
                break;
        }
        if (event.key.length === 1) {
            return event.key.charCodeAt(0);
        }
        return KeyEvent.KEY_NONE;
    }

    /**
    Java's `AwtSystem.awt2vsdkEvent(java.awt.event.KeyEvent)`. As in AWT, the
    character of a Ctrl+letter chord is a control one (Ctrl+Z gives 0x1A),
    and such a chord gets no key code.
    @param event DOM keyboard event (a key press)
    @return the vitral event
    */
    public static toVitralKeyEvent(event: KeyboardEvent): KeyEvent {
        const evsdk: KeyEvent = new KeyEvent();
        const unicodeId: number = WebSystem.awtKeyChar(event);
        const code: string = event.code;

        //-----------------------------------------------------------------
        if (event.altKey) {
            evsdk.modifierMask |= KeyEvent.MASK_ALT;
        }
        if (event.ctrlKey) {
            evsdk.modifierMask |= KeyEvent.MASK_CTRL;
        }
        if (event.shiftKey) {
            evsdk.modifierMask |= KeyEvent.MASK_SHIFT;
        }

        //-----------------------------------------------------------------
        // AWT gives these keys their key code and returns before taking the
        // character, so their `unicodeId` stays `KEY_NONE`
        if (event.key === "Escape") {
            evsdk.keycode = KeyEvent.KEY_ESC;
            return evsdk;
        }
        switch (code) {
            case "NumpadDivide":
                evsdk.keycode = KeyEvent.KEY_NUMSLASH;
                return evsdk;
            case "NumpadMultiply":
                evsdk.keycode = KeyEvent.KEY_NUMASTERISK;
                return evsdk;
            case "NumpadSubtract":
                evsdk.keycode = KeyEvent.KEY_NUMMINUS;
                return evsdk;
            case "NumpadAdd":
                evsdk.keycode = KeyEvent.KEY_NUMPLUS;
                return evsdk;
            case "NumpadDecimal":
                evsdk.keycode = KeyEvent.KEY_NUMPERIOD;
                return evsdk;
            default:
                break;
        }
        if (/^Numpad[0-9]$/.test(code) && event.key.length === 1 && /[0-9]/.test(event.key)) {
            evsdk.keycode = KeyEvent.KEY_NUM0 + (code.charCodeAt(6) - 48);
            return evsdk;
        }
        switch (event.key) {
            case "Enter":
                evsdk.keycode = code === "NumpadEnter" ? KeyEvent.KEY_NUMENTER : KeyEvent.KEY_ENTER;
                break;
            case "Backspace":
                evsdk.keycode = KeyEvent.KEY_BACKSPACE;
                break;
            case "Tab":
                evsdk.keycode = KeyEvent.KEY_TAB;
                break;
            case "Delete":
                evsdk.keycode = KeyEvent.KEY_DELETE;
                break;
            default:
                break;
        }
        if (code === "Equal") {
            evsdk.keycode = KeyEvent.KEY_EQUALS;
        }
        else if (code === "Minus") {
            evsdk.keycode = KeyEvent.KEY_MINUS;
        }

        evsdk.unicodeId = unicodeId;
        if (unicodeId === KeyEvent.KEY_NONE) {
            evsdk.unicodeId = KeyEvent.KEY_NONE;
            const f: RegExpMatchArray | null = /^F([1-9]|1[0-2])$/.exec(event.key);
            if (f !== null) {
                evsdk.keycode = KeyEvent.KEY_F1 + (Number(f[1]) - 1);
            }
            switch (event.key) {
                case "ArrowUp": evsdk.keycode = KeyEvent.KEY_UP; break;
                case "ArrowDown": evsdk.keycode = KeyEvent.KEY_DOWN; break;
                case "ArrowLeft": evsdk.keycode = KeyEvent.KEY_LEFT; break;
                case "ArrowRight": evsdk.keycode = KeyEvent.KEY_RIGHT; break;
                case "PageUp": evsdk.keycode = KeyEvent.KEY_PAGEUP; break;
                case "PageDown": evsdk.keycode = KeyEvent.KEY_PAGEDOWN; break;
                case "Alt": evsdk.keycode = KeyEvent.KEY_LALT; break;
                case "Control": evsdk.keycode = KeyEvent.KEY_LCTRL; break;
                default: break;
            }
        }
        else {
            const c: string = String.fromCharCode(unicodeId);
            if (c >= "A" && c <= "Z") {
                evsdk.keycode = KeyEvent.KEY_A + (unicodeId - 65);
            }
            else if (c >= "a" && c <= "z") {
                evsdk.keycode = KeyEvent.KEY_a + (unicodeId - 97);
            }
            else if (c >= "0" && c <= "9") {
                evsdk.keycode = KeyEvent.KEY_0 + (unicodeId - 48);
            }
            else {
                switch (c) {
                    case ",": evsdk.keycode = KeyEvent.KEY_COMMA; break;
                    case ".": evsdk.keycode = KeyEvent.KEY_PERIOD; break;
                    case " ": evsdk.keycode = KeyEvent.KEY_SPACE; break;
                    case "\n": evsdk.keycode = KeyEvent.KEY_ENTER; break;
                    case "\b": evsdk.keycode = KeyEvent.KEY_BACKSPACE; break;
                    default: break;
                }
            }
        }

        return evsdk;
    }

    //= Label images =======================================================

    /**
    Java's `AwtSystem.calculateLabelImage(label, color, fontSize)`: an image of
    the size of the text (its advance and the height of the font), with the
    text in the color on a transparent background, drawn with an Arial font
    of the given size in pixels and its baseline at the ascent of the font.
    @param label the text
    @param color color of the text
    @param fontSize size of the font, in pixels
    @return the image
    */
    public static calculateLabelImage(label: string, color: ColorRgb, fontSize: number = 14): RGBAImageUncompressed {
        const font: string = fontSize + "px Arial, Helvetica, sans-serif";
        const measureCanvas: OffscreenCanvas = new OffscreenCanvas(1, 1);
        const measure: OffscreenCanvasRenderingContext2D | null = measureCanvas.getContext("2d");
        const labelImage: RGBAImageUncompressed = new RGBAImageUncompressed();

        if (measure === null) {
            labelImage.init(1, 1);
            return labelImage;
        }
        measure.font = font;
        const metrics: TextMetrics = measure.measureText(label);
        const ascent: number = metrics.fontBoundingBoxAscent;
        const descent: number = metrics.fontBoundingBoxDescent;
        const width: number = Math.max(1, Math.ceil(metrics.width));
        const height: number = Math.max(1, Math.ceil(ascent + descent));
        const up: number = Math.ceil(ascent);

        const canvas: OffscreenCanvas = new OffscreenCanvas(width, height);
        const context: OffscreenCanvasRenderingContext2D | null = canvas.getContext("2d");
        labelImage.init(width, height);
        if (context === null) {
            return labelImage;
        }
        context.font = font;
        context.fillStyle = "rgb(" + Math.round(color.r() * 255) + "," + Math.round(color.g() * 255) + "," +
            Math.round(color.b() * 255) + ")";
        context.fillText(label, 0, up);

        const pixels: Uint8ClampedArray = context.getImageData(0, 0, width, height).data;
        const raw: Uint8Array = labelImage.getRawImageDirectBuffer();
        for (let y: number = 0; y < height; y++) {
            // Rows of the image are stored bottom first
            const target: number = (height - 1 - y) * width * 4;
            raw.set(pixels.subarray(y * width * 4, (y + 1) * width * 4), target);
        }
        return labelImage;
    }
}
