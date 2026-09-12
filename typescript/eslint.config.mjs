import eslint from "@eslint/js";
import prettier from "eslint-config-prettier";
import tseslint from "typescript-eslint";

export default tseslint.config(
    {
        ignores: ["**/dist/**", "**/node_modules/**", "**/artifacts/**", "**/.angular/**"],
    },
    eslint.configs.recommended,
    ...tseslint.configs.recommended,
    {
        files: ["**/*.ts"],
        rules: {
            "max-len": ["error", { code: 200, tabWidth: 4, ignoreUrls: true }],
            "no-tabs": "error",
            // The Java->TypeScript ports keep Java's "declare at the top of
            // the method, assign later" idiom, so single-assignment locals
            // are intentionally `let`.
            "prefer-const": "off",
            "no-loss-of-precision": "off",
            "no-useless-escape": "off",
            "@typescript-eslint/no-empty-object-type": "off",
            "@typescript-eslint/no-explicit-any": "off",
            "@typescript-eslint/no-namespace": "off",
            "@typescript-eslint/no-require-imports": "off",
            "@typescript-eslint/no-this-alias": "off",
            "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_", varsIgnorePattern: "^_" }],
        },
    },
    prettier,
);
