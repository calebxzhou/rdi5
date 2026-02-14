# Migrating `client/ui` from Compose Desktop to Compose Multiplatform (Desktop + Android)

Your project has **significant desktop-only dependencies** that make a full migration non-trivial. Here's a step-by-step plan:

---

## Step 1: Restructure to Kotlin Multiplatform (KMP)

Your current `build.gradle.kts` uses `kotlin("jvm")` + `org.jetbrains.compose`. You need to switch to **Kotlin Multiplatform** with Compose Multiplatform.

**`build.gradle.kts` changes:**

```kotlin
plugins {
    kotlin("multiplatform") // was: kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.application") // NEW: Android plugin
}

kotlin {
    // Desktop target
    jvm("desktop")

    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21" // or "17" for Android compatibility
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Shared Compose dependencies
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.1")

                // Shared non-UI deps
                implementation("io.ktor:ktor-client-core:3.3.3")
                implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
                implementation("org.mongodb:bson:5.5.0")
                // ... other cross-platform deps

                implementation(project(":common")) // common must also become KMP
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // Desktop-only deps
                implementation("org.lwjgl:lwjgl:3.3.3")
                implementation("org.lwjgl:lwjgl-glfw:3.3.3")
                implementation("org.lwjgl:lwjgl-opengl:3.3.3")
                // LWJGL natives...
                implementation("io.ktor:ktor-client-okhttp:3.3.3")
                implementation("io.ktor:ktor-server-netty:3.3.3")
                // ... other desktop-only deps (oshi, jsoup, etc.)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-android:3.3.3") // Android HTTP engine
                // Android-specific deps
            }
        }
    }
}

android {
    namespace = "calebxzhou.rdi.client"
    compileSdk = 35

    defaultConfig {
        applicationId = "calebxzhou.rdi.client"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "5.10.8"
    }
}
```

---

## Step 2: Restructure Source Directories

Move from single-platform layout to KMP layout:

```
client/ui/src/
├── commonMain/kotlin/        ← Shared UI code (screens, navigation, viewmodels)
│   └── calebxzhou/rdi/client/
│       ├── App.kt            ← Shared @Composable App() entry point
│       ├── ui2/screen/       ← All screens (most can go here)
│       └── ...
├── desktopMain/kotlin/       ← Desktop-only code
│   └── calebxzhou/rdi/client/
│       ├── Main.kt           ← application { Window(...) { App() } }
│       ├── DesktopPlatform.kt
│       └── ...
├── androidMain/kotlin/       ← Android-only code
│   └── calebxzhou/rdi/client/
│       ├── MainActivity.kt   ← ComponentActivity with setContent { App() }
│       ├── AndroidPlatform.kt
│       └── ...
├── commonMain/resources/     ← Shared resources (icons, etc.)
├── desktopMain/resources/
└── androidMain/
    └── AndroidManifest.xml   ← Required for Android
```

---

## Step 3: Create `expect`/`actual` Abstractions for Desktop-Only APIs

Your code uses these **desktop-only APIs** that need platform abstraction:

| Desktop-Only API | Where Used | Replacement Strategy |
|---|---|---|
| `application { Window(...) }` | `Main.kt` | Split entry points: Desktop `main()` vs Android `MainActivity` |
| `java.awt.Toolkit.screenSize` | `Main.kt` | `expect fun getScreenSize(): DpSize` |
| `java.awt.dnd.*` (drag & drop) | `Main.kt` | `expect`/`actual` — Android uses `Intent`/content resolver |
| `javax.swing.JFileChooser` | `ModpackUploadScreen.kt` | `expect fun pickFile(): File?` — Android uses `ActivityResultLauncher` |
| `java.awt.Desktop` (clipboard) | `RegisterScreen.kt` | `expect fun copyToClipboard(text: String)` — Android uses `ClipboardManager` |
| `java.awt.Toolkit` (clipboard) | `RegisterScreen.kt` | Same as above |
| `platform.Font(file)` | `Main.kt` | `expect fun loadCustomFont(): FontFamily` |
| `decodeToImageBitmap()` | `Main.kt` | Use `compose.components.resources` for cross-platform resource loading |
| LWJGL (OpenGL/GLFW) | build.gradle | Desktop-only, not needed on Android |
| Ktor Server (Netty) | build.gradle | Desktop-only (if used for local server) |

**Example `expect`/`actual`:**

```kotlin
// commonMain
expect fun copyToClipboard(text: String)

// desktopMain
actual fun copyToClipboard(text: String) {
    val selection = StringSelection(text)
    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
}

// androidMain
actual fun copyToClipboard(text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("label", text))
}
```

---

## Step 4: Create Android Entry Point

```kotlin
// androidMain/kotlin/calebxzhou/rdi/client/MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App() // Same shared composable as desktop
        }
    }
}
```

```xml
<!-- androidMain/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:label="RDI 5"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## Step 5: Migrate `common` Module to KMP Too

Your `common` module is also JVM-only. It needs the same treatment:

```kotlin
// common/build.gradle.kts
plugins {
    kotlin("multiplatform") // was: kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    jvm("desktop")
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            // Cross-platform deps only
            implementation("io.ktor:ktor-client-core:3.3.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            // ...
        }
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:3.3.3")
                // JVM-only deps: jsoup, logback, etc.
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-android:3.3.3")
            }
        }
    }
}
```

---

## Step 6: Handle the Biggest Blockers

These are the **hardest parts** specific to your app:

1. **LWJGL (OpenGL/GLFW)** — This is desktop-only game rendering. On Android, you'd use OpenGL ES directly or skip it entirely. If this is for Minecraft launching, it's inherently desktop-only and should stay in `desktopMain`.

2. **Ktor Server (Netty)** — If you run a local HTTP server on the client, this is desktop-only. On Android, consider removing it or using a lightweight alternative.

3. **File system operations** (`ZipFile`, `Files`, `JFileChooser`) — Android has a sandboxed filesystem. Use `expect`/`actual` with Android's `ContentResolver` and `SAF` (Storage Access Framework).

4. **`calebxzhou.mykotutils`** — Your custom library also needs to be KMP-compatible, or you need to provide `expect`/`actual` wrappers.

5. **Minecraft launching logic** — If the app's core purpose is launching Minecraft on desktop, the Android version would likely be a **companion app** (server browser, account management, chat) rather than a full launcher.

---

## Step 7: Gradle Setup for Android SDK

Add to `gradle.properties`:

```properties
android.useAndroidX=true
```

Add to `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()       // NEW
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Make sure you have Android SDK installed and `local.properties` pointing to it:

```properties
sdk.dir=C:\\Users\\calebxzhou\\AppData\\Local\\Android\\Sdk
```

---

## Summary: Migration Difficulty Assessment

| Area | Difficulty | Reason |
|------|-----------|--------|
| Build system (Gradle KMP) | 🟡 Medium | Mechanical restructuring |
| Compose UI screens | 🟢 Easy | Most Compose code is already cross-platform |
| Navigation | 🟢 Easy | `navigation-compose` works on both |
| File pickers / clipboard | 🟡 Medium | Need `expect`/`actual` per platform |
| LWJGL / Minecraft launch | 🔴 Hard / N/A | Fundamentally desktop-only |
| `common` module migration | 🟡 Medium | Depends on how many JVM-only APIs it uses |
| `mykotutils` library | 🔴 Hard | External dependency must also become KMP |

**Recommended approach**: Start by making the **build system** multiplatform, move all pure-Compose screens to `commonMain`, and keep all Minecraft/game-specific logic in `desktopMain`. The Android app can initially be a "lite" version with server browsing, account management, and social features — without the game launching parts.
