import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

// The specs run the `@vitral/fs` sources against the `@vitral/base` sources,
// so both packages share one instance of every base class.
const baseMain = fileURLToPath(new URL("../base/src/main", import.meta.url));

export default defineConfig({
    resolve: {
        alias: [
            { find: /^@vitral\/base$/, replacement: `${baseMain}/index.ts` },
            { find: /^@vitral\/base\/(.*)$/, replacement: `${baseMain}/$1` },
            { find: /^vsdk\/(.*)$/, replacement: `${baseMain}/vsdk/$1` },
            { find: /^java\/(.*)$/, replacement: `${baseMain}/java/$1` },
        ],
    },
});
