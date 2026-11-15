// Locations the reader specs share: the cpp tests' VITRAL_REPOSITORY_ROOT and
// testing::TempDir().
import * as os from "node:os";
import { fileURLToPath } from "node:url";

export const VITRAL_REPOSITORY_ROOT = fileURLToPath(new URL("../../../..", import.meta.url)).replace(/\/$/, "");

export const TEMP_DIR = os.tmpdir() + "/";
