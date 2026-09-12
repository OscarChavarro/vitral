export interface WebKeyEvent {
    keycode: string;
    unicode_id: string;
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
            unicode_id: WebSystem.normalizeUnicode(event),
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
}
