/**
`java.lang.System.getProperty` boundary for the base package.

The base package must not touch the host process, and this runtime has no JVM
system-property table, so every property reads as unset. This mirrors the
`java.lang.Boolean.getBoolean` port, which always answers `false`.
*/
export function platformGetProperty(name: string): string | null {
    if (name.length === 0) {
        return null;
    }
    return null;
}
