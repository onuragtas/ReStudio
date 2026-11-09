// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import kotlinx.collections.immutable.PersistentMap
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Signs the macOS distribution with the local `codesign` tool of the build Mac.
 *
 * The signing starts only on macOS, and only when the `RESTUDIO_MAC_SIGN_IDENTITY` environment variable holds the
 * name of a "Developer ID Application" certificate in the keychain. Without it, the build makes unsigned installers.
 *
 * The notarization uses the notary client of the build scripts. It starts when the `APPLE_ISSUER_ID`, `APPLE_KEY_ID`
 * and `APPLE_PRIVATE_KEY` environment variables are set. `APPLE_PRIVATE_KEY` holds the content of the `.p8` file.
 *
 * The `restudio-release` skill sets these variables.
 */
object ReStudioMacSigning {
  const val IDENTITY_VARIABLE: String = "RESTUDIO_MAC_SIGN_IDENTITY"

  private val NOTARY_VARIABLES = listOf("APPLE_ISSUER_ID", "APPLE_KEY_ID", "APPLE_PRIVATE_KEY")

  private val identity: String?
    get() = System.getenv(IDENTITY_VARIABLE)?.takeIf { it.isNotBlank() && OsFamily.currentOs == OsFamily.MACOS }

  /** The build tools with the local sign tool, or [ProprietaryBuildTools.DUMMY] when the signing is off. */
  fun proprietaryBuildTools(): ProprietaryBuildTools {
    val identity = identity ?: return ProprietaryBuildTools.DUMMY
    return ProprietaryBuildTools.DUMMY.copy(signTool = LocalCodesignTool(identity))
  }

  /** The build steps that the build must not skip, because the signing or the notarization is on. */
  fun enabledSteps(): Set<String> {
    if (identity == null) {
      return emptySet()
    }
    val notarize = NOTARY_VARIABLES.all { !System.getenv(it).isNullOrBlank() }
    return if (notarize) setOf(BuildOptions.MAC_SIGN_STEP, BuildOptions.MAC_NOTARIZE_STEP) else setOf(BuildOptions.MAC_SIGN_STEP)
  }
}

private class LocalCodesignTool(private val identity: String) : SignTool {
  override val macOsCodesignIdentity: MacOsCodesignIdentity = parseIdentity(identity)

  /**
   * [SignNativeFileMode.ENABLED] downloads presigned native libraries from a JetBrains server.
   * [SignNativeFileMode.PREPARE] extracts the native libraries and gives them to [signFiles].
   */
  override val signNativeFileMode: SignNativeFileMode
    get() = SignNativeFileMode.PREPARE

  override fun signFiles(files: List<Path>, context: BuildContext?, options: PersistentMap<String, String>) {
    if (files.isEmpty()) {
      return
    }
    val entitlements = options["mac_codesign_entitlements"]
    when (options["contentType"]) {
      "application/x-mac-app-bin" -> codesign(files, entitlements)
      "application/x-mac-app-zip" -> files.forEach { signAppArchive(it, entitlements) }
      // ReStudio has no Windows distribution, and the build gives no other content type for macOS.
      else -> {}
    }
  }

  /** Signs the nested bundles from the deepest to the top, then the application bundle, and packs the archive again. */
  private fun signAppArchive(archive: Path, entitlements: String?) {
    val dir = Files.createTempDirectory("restudio-sign")
    try {
      run(listOf("ditto", "-x", "-k", archive.toString(), dir.toString()))
      val app = dir.listDirectoryEntries("*.app").single()
      val nested = Files.walk(app).use { paths ->
        paths.filter { it != app && it.isDirectory() && isBundle(it) }.toList()
      }
      for (bundle in nested.sortedByDescending { it.nameCount }) {
        codesign(listOf(bundle), entitlements)
      }
      codesign(listOf(app), entitlements)
      run(listOf("codesign", "--verify", "--deep", "--strict", app.toString()))
      Files.delete(archive)
      run(listOf("ditto", "-c", "-k", "--sequesterRsrc", "--keepParent", app.toString(), archive.toString()))
    }
    finally {
      dir.toFile().deleteRecursively()
    }
  }

  private fun isBundle(dir: Path): Boolean =
    dir.extension in setOf("app", "framework", "bundle", "plugin") ||
    (dir.name == "jbr" && Files.isRegularFile(dir.resolve("Contents/Info.plist")))

  private fun codesign(files: List<Path>, entitlements: String?) {
    val command = mutableListOf("codesign", "--sign", identity, "--options", "runtime", "--timestamp", "--force")
    if (entitlements != null) {
      command += listOf("--entitlements", entitlements)
    }
    files.chunked(100).forEach { chunk -> run(command + chunk.map(Path::toString)) }
  }

  override fun signFilesWithGpg(files: List<Path>, context: BuildContext) {}

  override fun getPresignedLibraryFile(path: String, libName: String, libVersion: String, context: BuildContext): Path? =
    error("The local sign tool uses SignNativeFileMode.PREPARE and has no presigned libraries")

  override fun commandLineClient(context: BuildContext, os: OsFamily, arch: JvmArchitecture): Path? = null
}

private val IDENTITY_PATTERN = Regex("""Developer ID Application: (.+) \(([A-Z0-9]+)\)""")

private fun parseIdentity(identity: String): MacOsCodesignIdentity {
  val match = IDENTITY_PATTERN.matchEntire(identity)
                ?: error("${ReStudioMacSigning.IDENTITY_VARIABLE} must be 'Developer ID Application: <team name> (<team ID>)', not '$identity'")
  return MacOsCodesignIdentity(teamName = match.groupValues[1], teamID = match.groupValues[2])
}

private fun run(command: List<String>) {
  val process = ProcessBuilder(command).redirectErrorStream(true).start()
  val output = process.inputStream.bufferedReader().readText()
  check(process.waitFor(30, TimeUnit.MINUTES)) { "Timeout: ${command.take(3)}" }
  check(process.exitValue() == 0) { "${command.first()} failed with exit code ${process.exitValue()}:\n$output" }
}
