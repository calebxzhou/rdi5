package calebxzhou.rdi.client.service

import java.io.File

/**
 * Link or copy a mod file to the target location.
 * Desktop: creates a symbolic link (faster, saves space).
 * Android: copies the file (symlinks not supported on most Android filesystems).
 */
expect fun linkOrCopyMod(source: File, target: File)
