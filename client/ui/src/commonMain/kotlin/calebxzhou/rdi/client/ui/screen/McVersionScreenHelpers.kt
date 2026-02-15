package calebxzhou.rdi.client.ui.screen

import calebxzhou.rdi.client.service.ModpackLocalDir
import calebxzhou.rdi.common.model.Task
import java.io.File

/**
 * Desktop-only helper functions used by McVersionScreen.
 * These are only called behind `isDesktop` guard.
 * Android provides no-op stubs.
 */

expect fun selectRdiPackFiles(): List<File>?

expect fun buildImportPackTask(zipFile: File): Task

expect suspend fun importRdiModpack(onProgress: (String) -> Unit): Task

expect suspend fun exportRdiModpack(
    packdir: ModpackLocalDir,
    onProgress: (String) -> Unit
): Result<Unit>

expect suspend fun exportLogsPack(packdir: ModpackLocalDir): Result<Unit>
