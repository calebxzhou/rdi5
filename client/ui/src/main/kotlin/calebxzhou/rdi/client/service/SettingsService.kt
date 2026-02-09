package calebxzhou.rdi.client.service

import calebxzhou.rdi.client.AppConfig
import calebxzhou.rdi.common.ProxyConfig
import java.io.File
import java.lang.management.ManagementFactory

object SettingsService {

    data class ValidationResult(
        val success: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Get total physical memory in MB
     */
    fun getTotalPhysicalMemoryMb(): Int {
        val osBean = runCatching {
            ManagementFactory.getOperatingSystemMXBean()
        }.getOrNull()
        val totalBytes = (osBean as? com.sun.management.OperatingSystemMXBean)
            ?.totalPhysicalMemorySize
            ?: return 0
        return (totalBytes / (1024L * 1024L)).toInt()
    }

    /**
     * Validate memory settings
     */
    fun validateMemory(maxMemoryText: String, totalMemoryMb: Int): ValidationResult {
        if (maxMemoryText.isBlank()) {
            return ValidationResult(true)
        }

        val memoryValue = maxMemoryText.trim().toIntOrNull()
            ?: return ValidationResult(false, "最大内存格式不正确")

        if (memoryValue == 0) {
            return ValidationResult(true)
        }

        if (memoryValue <= 4096) {
            return ValidationResult(false, "最大内存必须大于4096MB")
        }

        if (totalMemoryMb > 0 && memoryValue >= totalMemoryMb) {
            return ValidationResult(false, "最大内存必须小于总内存 ${totalMemoryMb}MB")
        }

        return ValidationResult(true)
    }

    /**
     * Validate proxy port
     */
    fun validateProxyPort(proxyPortText: String): ValidationResult {
        if (proxyPortText.isBlank()) {
            return ValidationResult(true)
        }

        val port = proxyPortText.trim().toIntOrNull()
            ?: return ValidationResult(false, "代理端口格式不正确")

        if (port !in 1..65535) {
            return ValidationResult(false, "代理端口必须在1-65535之间")
        }

        return ValidationResult(true)
    }

    /**
     * Validate Java path
     */
    fun validateJavaPath(rawPath: String, expectedMajor: Int): Result<Unit> {
        val resolved = resolveJavaExecutable(rawPath) ?: return Result.failure(
            IllegalStateException("Java 路径无效: $rawPath")
        )
        val version = readJavaMajorVersion(resolved) ?: return Result.failure(
            IllegalStateException("无法识别 Java 版本: ${resolved.absolutePath}")
        )
        if (version != expectedMajor) {
            return Result.failure(
                IllegalStateException("Java 版本应为 $expectedMajor，当前为 $version")
            )
        }
        return Result.success(Unit)
    }

    /**
     * Save settings configuration
     */
    suspend fun saveSettings(
        useMirror: Boolean,
        maxMemoryText: String,
        jre21Path: String,
        jre8Path: String,
        carrier: Int,
        proxyEnabled: Boolean,
        proxySystem: Boolean,
        proxyHost: String,
        proxyPortText: String,
        proxyUsr: String,
        proxyPwd: String
    ): Result<Unit> = runCatching {
        val memoryValue = maxMemoryText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        val jre21 = jre21Path.trim().takeIf { it.isNotEmpty() }
        val jre8 = jre8Path.trim().takeIf { it.isNotEmpty() }
        val proxyPort = proxyPortText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

        val config = AppConfig(
            useMirror = useMirror,
            maxMemory = memoryValue ?: 0,
            jre21Path = jre21,
            jre8Path = jre8,
            carrier = carrier,
            proxyConfig = ProxyConfig(
                enabled = proxyEnabled,
                systemProxy = proxySystem,
                host = proxyHost,
                port = proxyPort ?: 10808,
                usr = proxyUsr.takeIf { it.isNotBlank() },
                pwd = proxyPwd.takeIf { it.isNotBlank() }
            )
        )
        AppConfig.save(config)
    }

    /**
     * Validate profile change
     */
    fun validateProfileChange(name: String, pwd: String, currentName: String): ValidationResult {
        val nameBytes = name.toByteArray(Charsets.UTF_8).size
        if (nameBytes !in 3..24) {
            return ValidationResult(false, "昵称须在3~24个字节，当前为${nameBytes}")
        }

        if (pwd.isNotEmpty() && pwd.length !in 6..16) {
            return ValidationResult(false, "密码长度须在6~16个字符")
        }

        if (name == currentName && pwd.isEmpty()) {
            return ValidationResult(false, "没有修改内容")
        }

        return ValidationResult(true)
    }

    private fun resolveJavaExecutable(rawPath: String): File? {
        val input = File(rawPath.trim())
        if (input.isDirectory) {
            val exe = input.resolve("bin").resolve(if (isWindows()) "java.exe" else "java")
            return exe.takeIf { it.exists() }
        }
        if (input.exists()) {
            val name = input.name.lowercase()
            if (isWindows()) {
                return input.takeIf { name == "java.exe" }
            }
            return input.takeIf { name == "java" }
        }
        val exe = File(rawPath.trim() + if (isWindows()) ".exe" else "")
        return exe.takeIf { it.exists() }
    }

    private fun readJavaMajorVersion(javaExe: File): Int? {
        return runCatching {
            val process = ProcessBuilder(javaExe.absolutePath, "-version")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            val match = Regex("""version "([0-9]+)(?:\.([0-9]+))?.*"""").find(output)
                ?: return@runCatching null
            val major = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return@runCatching null
            if (major == 1) {
                match.groupValues.getOrNull(2)?.toIntOrNull()
            } else {
                major
            }
        }.getOrNull()
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").contains("windows", ignoreCase = true)
    }
}