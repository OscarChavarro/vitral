/**
Browser-safe counterpart of `System.out.print` / `System.out.println` for
diagnostic output in the platform-neutral base package.

Node-like runtimes expose a writable standard output, so `print` preserves the
Java "no line terminator" behavior there. Browsers have no standard output
stream; the text is then sent to `console.log`, which always terminates the
entry.
*/
type WritableStandardOutput = { write(text: string): unknown };

function standardOutput(): WritableStandardOutput | null {
    const runtime = globalThis as { process?: { stdout?: WritableStandardOutput } };
    const stdout = runtime.process?.stdout;
    return stdout !== undefined && typeof stdout.write === "function" ? stdout : null;
}

/** `System.out.print(text)`. */
export function platformPrint(text: string): void {
    const stdout = standardOutput();
    if (stdout !== null) {
        stdout.write(text);
        return;
    }
    console.log(text);
}

/** `System.out.println(text)`. */
export function platformPrintln(text: string): void {
    const stdout = standardOutput();
    if (stdout !== null) {
        stdout.write(text + "\n");
        return;
    }
    console.log(text);
}
