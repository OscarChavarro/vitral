import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

export default defineConfig({
    resolve: {
        alias: {
            vsdk: fileURLToPath(new URL("./src/main/vsdk", import.meta.url)),
            java: fileURLToPath(new URL("./src/main/java", import.meta.url)),
        },
    },
});
