// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.updateSettings.impl

import com.intellij.diagnostic.rethrowControlFlowException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.io.NioFiles
import com.intellij.platform.ide.customization.ExternalProductResourceUrls
import com.intellij.util.Restarter
import com.intellij.util.io.HttpRequests
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import org.jdom.Element
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * One installer of a build in `updates.xml`. `fork/make-updates-xml.sh` writes it in the `<build>` element as
 * `<restudio-package os="mac" arch="aarch64" url="..." sha256="..." size="..."/>`.
 * The platform parser ignores this element.
 */
@ApiStatus.Internal
data class ReStudioUpdatePackage(val os: String, val arch: String, val url: String, val sha256: String, val size: Long?)

/**
 * The way the IDE updates to a new build.
 */
@ApiStatus.Internal
enum class ReStudioUpdatePath {
  /** The platform applies a chain of patches. */
  PATCH_CHAIN,

  /** The IDE downloads the full installer, replaces the installation and restarts. */
  FULL_INSTALLER,

  /** The IDE opens the download page. */
  DOWNLOAD_PAGE,

  /** The IDE cannot write the installation directory. It tells the user and opens the download page. */
  DOWNLOAD_PAGE_NOT_WRITABLE,
}

/**
 * The update of a build that has no chain of patches. The IDE downloads the full installer of the new build,
 * verifies the SHA-256 of `updates.xml`, unpacks the installer next to the installation and checks it.
 * After the IDE exits, the restarter moves the old installation aside, moves the new one into its place and starts it.
 * The next start of the IDE deletes the old installation ([cleanupAfterUpdate]).
 *
 * On macOS, the new app must have a valid signature of the same Developer ID team as the running app.
 * The IDE never runs a command with elevated rights. If it cannot write the installation directory, it opens the download page.
 */
@ApiStatus.Internal
object ReStudioFullUpdate {
  const val PACKAGE_ELEMENT: String = "restudio-package"

  private val LOG = logger<ReStudioFullUpdate>()
  private const val OLD_SUFFIX = ".restudio-old"
  private const val NEW_SUFFIX = ".restudio-new"
  private const val COMMAND_TIMEOUT_MS = 10 * 60 * 1000
  /** The IDE downloads a full installer only from the ReStudio releases. A Linux installer has no signature to check. */
  private const val DOWNLOAD_PREFIX = "https://github.com/onuragtas/ReStudio/releases/download/"
  private val SHA256_PATTERN = Regex("[0-9a-f]{64}")
  private val BUILD_NUMBER_PATTERN = Regex("\"buildNumber\"\\s*:\\s*\"([^\"]+)\"")
  private val TEAM_PATTERN = Regex("^TeamIdentifier=(\\S+)$", RegexOption.MULTILINE)

  /** The `os` value of [ReStudioUpdatePackage] for this OS, or `null` if the IDE cannot replace itself on this OS. */
  fun currentOs(): String? = when (OS.CURRENT) {
    OS.macOS -> "mac"
    OS.Linux -> "linux"
    else -> null
  }

  /** The `arch` value of [ReStudioUpdatePackage] for this CPU. */
  fun currentArch(): String = when (CpuArch.CURRENT) {
    CpuArch.ARM64 -> "aarch64"
    CpuArch.X86_64 -> "x64"
    else -> CpuArch.CURRENT.name.lowercase()
  }

  /**
   * Reads the [PACKAGE_ELEMENT] elements of [build] for [productCode] from the root element of `updates.xml`.
   */
  @VisibleForTesting
  fun parsePackages(metadata: Element, productCode: String, build: BuildNumber): List<ReStudioUpdatePackage> {
    val wanted = build.withoutProductCode().asString()
    return metadata.getChildren("product")
      .filter { product -> product.getChildren("code").any { it.value.trim() == productCode } }
      .flatMap { it.getChildren("channel") }
      .flatMap { it.getChildren("build") }
      .filter { BuildNumber.fromString(it.getAttributeValue("number") ?: "")?.withoutProductCode()?.asString() == wanted }
      .flatMap { it.getChildren(PACKAGE_ELEMENT) }
      .mapNotNull { node ->
        val os = node.getAttributeValue("os") ?: return@mapNotNull null
        val arch = node.getAttributeValue("arch") ?: return@mapNotNull null
        val url = node.getAttributeValue("url") ?: return@mapNotNull null
        val sha256 = node.getAttributeValue("sha256")?.trim()?.lowercase() ?: return@mapNotNull null
        ReStudioUpdatePackage(os, arch, url, sha256, node.getAttributeValue("size")?.toLongOrNull())
      }
  }

  /**
   * The package for [os] and [arch]. The URL must use HTTPS, and the SHA-256 must have 64 hex digits.
   * Without such a package, the result is `null`.
   */
  @VisibleForTesting
  fun selectPackage(packages: List<ReStudioUpdatePackage>, os: String?, arch: String): ReStudioUpdatePackage? {
    if (os == null) return null
    return packages.singleOrNull { it.os == os && it.arch == arch }
      ?.takeIf { it.url.startsWith(DOWNLOAD_PREFIX) && !it.url.contains("..") && SHA256_PATTERN.matches(it.sha256) }
  }

  /**
   * The decision between a chain of patches, the full installer and the download page.
   * The chain of patches always wins. The full installer needs a package for this OS, a restart-capable IDE and a writable
   * installation.
   */
  @VisibleForTesting
  fun decide(hasPatchChain: Boolean, pkg: ReStudioUpdatePackage?, restartCapable: Boolean, installWritable: Boolean): ReStudioUpdatePath = when {
    hasPatchChain -> ReStudioUpdatePath.PATCH_CHAIN
    pkg == null || !restartCapable -> ReStudioUpdatePath.DOWNLOAD_PAGE
    !installWritable -> ReStudioUpdatePath.DOWNLOAD_PAGE_NOT_WRITABLE
    else -> ReStudioUpdatePath.FULL_INSTALLER
  }

  /**
   * The directory that the update replaces: the `.app` bundle on macOS, the IDE home on Linux.
   */
  @VisibleForTesting
  fun installRoot(home: Path, os: String?): Path? = when (os) {
    "mac" -> home.parent?.takeIf { home.name == "Contents" && it.name.endsWith(".app") }
    "linux" -> home
    else -> null
  }

  @VisibleForTesting
  fun isWritable(root: Path): Boolean {
    val parent = root.parent ?: return false
    return Files.isWritable(root) && Files.isWritable(parent)
  }

  /**
   * Starts the update to [update] with the full installer, if this OS supports it.
   * Returns `false` if the caller must open the download page itself.
   * The task opens the download page itself when `updates.xml` has no package for this OS.
   */
  @JvmStatic
  fun start(project: Project?, update: PlatformUpdates.Loaded): Boolean {
    if (update.patches != null) return false
    val os = currentOs() ?: return false
    if (!ApplicationManager.getApplication().isRestartCapable) return false
    val root = installRoot(PathManager.getHomeDir(), os) ?: return false

    IdeUpdateWidgetState.getInstance().onDownloadStarted()
    @Suppress("UsagesOfObsoleteApi")
    object : Task.Backgroundable(project, ReStudioUpdateBundle.message("restudio.update.full.title"), true, PerformInBackgroundOption.DEAF) {
      override fun run(indicator: ProgressIndicator) {
        val command = try {
          prepare(update, os, root, indicator) ?: return
        }
        catch (e: Throwable) {
          rethrowControlFlowException(e)
          LOG.warn(e)
          deleteStaging(root)
          notifyFailure(project, update, e.message ?: e.javaClass.name)
          return
        }
        offerRestart(project, command, indicator)
      }

      override fun onCancel() {
        deleteStaging(root)
      }

      override fun onFinished() {
        IdeUpdateWidgetState.getInstance().onDownloadFinished()
      }
    }.queue()
    return true
  }

  /**
   * Deletes the old installation that the last update moved aside, and the rest of a failed update.
   * The IDE calls it at the start.
   */
  @JvmStatic
  fun cleanupAfterUpdate() {
    val root = installRoot(PathManager.getHomeDir(), currentOs()) ?: return
    for (dir in listOf(oldRoot(root), staging(root))) {
      if (Files.exists(dir)) {
        try {
          LOG.info("deleting ${dir}")
          NioFiles.deleteRecursively(dir)
        }
        catch (e: IOException) {
          LOG.warn(e)
        }
      }
    }
  }

  private fun oldRoot(root: Path): Path = root.resolveSibling(".${root.name}${OLD_SUFFIX}")

  private fun staging(root: Path): Path = root.resolveSibling(".${root.name}${NEW_SUFFIX}")

  private fun tempDir(): Path = PathManager.getTempDir().resolve("restudio-full-update")

  private fun deleteStaging(root: Path) {
    try {
      NioFiles.deleteRecursively(staging(root))
      NioFiles.deleteRecursively(tempDir())
    }
    catch (e: IOException) {
      LOG.warn(e)
    }
  }

  /**
   * Downloads, verifies and unpacks the installer. Returns the command that swaps the installations,
   * or `null` if the IDE opened the download page.
   */
  private fun prepare(update: PlatformUpdates.Loaded, os: String, root: Path, indicator: ProgressIndicator): Array<String>? {
    indicator.text = ReStudioUpdateBundle.message("restudio.update.full.metadata")
    val metadataUrl = ExternalProductResourceUrls.getInstance().updateMetadataUrl ?: return openDownloadPage(update, false)
    val metadata = HttpRequests.request(metadataUrl).productNameAsUserAgent().connect { JDOMUtil.load(it.getReader(indicator)) }
    val packages = parsePackages(metadata, ApplicationInfo.getInstance().build.productCode, update.newBuild.number)
    val selected = selectPackage(packages, os, currentArch())
    val pkg = when (decide(false, selected, true, isWritable(root))) {
      ReStudioUpdatePath.PATCH_CHAIN, ReStudioUpdatePath.FULL_INSTALLER -> checkNotNull(selected)
      ReStudioUpdatePath.DOWNLOAD_PAGE -> return openDownloadPage(update, false)
      ReStudioUpdatePath.DOWNLOAD_PAGE_NOT_WRITABLE -> return openDownloadPage(update, true)
    }

    val temp = tempDir()
    if (PathManager.isUnderHomeDirectory(temp)) throw IOException("Temp directory inside installation: ${temp}")
    NioFiles.deleteRecursively(temp)
    Files.createDirectories(temp)

    indicator.text = ReStudioUpdateBundle.message("restudio.update.full.downloading")
    val file = temp.resolve(pkg.url.substringAfterLast('/').ifEmpty { "installer" })
    LOG.info("downloading ${pkg.url}")
    HttpRequests.request(pkg.url).productNameAsUserAgent().gzip(false).saveToFile(file, indicator)

    indicator.text = ReStudioUpdateBundle.message("restudio.update.full.verifying")
    indicator.isIndeterminate = true
    val actual = sha256(file, indicator)
    if (actual != pkg.sha256) {
      NioFiles.deleteRecursively(file)
      throw IOException("SHA-256 mismatch for ${pkg.url}: expected ${pkg.sha256}, got ${actual}")
    }

    indicator.text = ReStudioUpdateBundle.message("restudio.update.full.unpacking")
    val staging = staging(root)
    NioFiles.deleteRecursively(staging)
    Files.createDirectories(staging)
    val newRoot = when (os) {
      "mac" -> unpackMac(file, staging, root, indicator)
      else -> unpackLinux(file, staging, indicator)
    }
    checkBuildNumber(newRoot, os, update.newBuild.number)
    val starter = Restarter.getIdeStarter() ?: throw IOException("No launcher in ${root}")
    if (!Files.isExecutable(newRoot.resolve(root.relativize(starter)))) throw IOException("No launcher in the new build: ${newRoot}")
    if (os == "mac") {
      // The checks passed. The app may start without the Gatekeeper dialog now.
      run(indicator, "/usr/bin/xattr", "-r", "-d", "com.apple.quarantine", newRoot.toString())
    }
    NioFiles.deleteRecursively(file)

    val script = temp.resolve("restudio-swap.sh")
    Files.writeString(script, SWAP_SCRIPT)
    // The restarter is inside the installation that the command replaces. The platform copies it out first.
    Restarter.setCopyRestarterFiles()
    return arrayOf("/bin/sh", script.toString(), root.toString(), newRoot.toString(), oldRoot(root).toString())
  }

  private fun unpackMac(file: Path, staging: Path, root: Path, indicator: ProgressIndicator): Path {
    runOrThrow(indicator, "/usr/bin/ditto", "-x", "-k", file.toString(), staging.toString())
    val app = staging.listDirectoryEntries("*.app").singleOrNull { it.isDirectory() }
              ?: throw IOException("The installer has no single .app bundle")

    indicator.text = ReStudioUpdateBundle.message("restudio.update.full.signature")
    runOrThrow(indicator, "/usr/bin/codesign", "--verify", "--deep", "--strict", app.toString())
    val newSignature = run(indicator, "/usr/bin/codesign", "-d", "--verbose=2", app.toString())
    val newTeam = TEAM_PATTERN.find(newSignature.stderr)?.groupValues?.get(1)
    val currentTeam = TEAM_PATTERN.find(run(indicator, "/usr/bin/codesign", "-d", "--verbose=2", root.toString()).stderr)?.groupValues?.get(1)
    if (newTeam == null || newTeam == "not" || currentTeam == null || newTeam != currentTeam) {
      throw IOException("The Developer ID team of the new app (${newTeam}) does not match the running app (${currentTeam})")
    }
    if (!newSignature.stderr.contains("Authority=Developer ID Application:")) {
      throw IOException("The new app has no Developer ID signature")
    }
    runOrThrow(indicator, "/usr/sbin/spctl", "--assess", "--type", "execute", app.toString())
    return app
  }

  private fun unpackLinux(file: Path, staging: Path, indicator: ProgressIndicator): Path {
    runOrThrow(indicator, "tar", "-xzf", file.toString(), "-C", staging.toString(), "--no-same-owner")
    return staging.listDirectoryEntries().singleOrNull { it.isDirectory() && Files.isDirectory(it.resolve("bin")) }
           ?: throw IOException("The installer has no single IDE directory")
  }

  private fun checkBuildNumber(newRoot: Path, os: String, expected: BuildNumber) {
    val productInfo = if (os == "mac") newRoot.resolve("Contents/Resources/product-info.json") else newRoot.resolve("product-info.json")
    val text = Files.readString(productInfo)
    val actual = BUILD_NUMBER_PATTERN.find(text)?.groupValues?.get(1)?.let(BuildNumber::fromString)
    if (actual == null || actual.withoutProductCode() != expected.withoutProductCode()) {
      throw IOException("The installer has build ${actual}, expected ${expected}")
    }
  }

  @VisibleForTesting
  fun sha256(file: Path, indicator: ProgressIndicator?): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(file).use { input ->
      val buffer = ByteArray(1 shl 16)
      while (true) {
        indicator?.checkCanceled()
        val read = input.read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private fun run(indicator: ProgressIndicator, vararg command: String): ProcessOutput {
    val output = CapturingProcessHandler(GeneralCommandLine(*command)).runProcessWithProgressIndicator(indicator, COMMAND_TIMEOUT_MS)
    if (output.isCancelled) throw ProcessCanceledException()
    return output
  }

  private fun runOrThrow(indicator: ProgressIndicator, vararg command: String) {
    val output = run(indicator, *command)
    if (output.isTimeout || output.exitCode != 0) {
      throw IOException("${command.first()} failed (${output.exitCode}): ${output.stderr.trim()}")
    }
  }

  private fun openDownloadPage(update: PlatformUpdates.Loaded, notWritable: Boolean): Array<String>? {
    val url = downloadUrl(update.newBuild, update.updatedChannel)
    ApplicationManager.getApplication().invokeLater {
      if (notWritable) {
        val title = ReStudioUpdateBundle.message("restudio.update.full.title")
        Messages.showInfoMessage(ReStudioUpdateBundle.message("restudio.update.full.not.writable", PathManager.getHomeDir()), title)
      }
      BrowserUtil.browse(url)
    }
    return null
  }

  private fun notifyFailure(project: Project?, update: PlatformUpdates.Loaded, reason: String) {
    val url = downloadUrl(update.newBuild, update.updatedChannel)
    val title = ReStudioUpdateBundle.message("restudio.update.full.title")
    UpdateChecker.getNotificationGroupForIdeUpdateResults()
      .createNotification(title, ReStudioUpdateBundle.message("restudio.update.full.failed", reason), NotificationType.ERROR)
      .addAction(NotificationAction.createSimpleExpiring(ReStudioUpdateBundle.message("restudio.update.full.open.page")) { BrowserUtil.browse(url) })
      .setDisplayId("restudio.full.update.failed")
      .notify(project)
  }

  private fun offerRestart(project: Project?, command: Array<String>, indicator: ProgressIndicator) {
    IdeUpdateWidgetState.getInstance().onRestartReady(command)
    if (IdeUpdateWidgetState.isWidgetShown()) return
    if (indicator.isShowing) {
      PlatformUpdateDialog.restartLaterAndRunCommand(command)
    }
    else {
      val title = ReStudioUpdateBundle.message("restudio.update.full.title")
      UpdateChecker.getNotificationGroupForIdeUpdateResults()
        .createNotification(title, ReStudioUpdateBundle.message("restudio.update.full.ready", ApplicationNamesInfo.getInstance().fullProductName),
                            NotificationType.INFORMATION)
        .addAction(NotificationAction.createSimpleExpiring(ReStudioUpdateBundle.message("restudio.update.full.restart")) {
          PlatformUpdateDialog.restartLaterAndRunCommand(command)
        })
        .setDisplayId("restudio.full.update.ready")
        .notify(project)
    }
  }

  /**
   * The restarter runs this script after the IDE exits: `restudio-swap.sh <installation> <new installation> <old place>`.
   * If the second move fails, the script moves the old installation back.
   */
  private val SWAP_SCRIPT = """
    #!/bin/sh
    target="${'$'}1"
    new="${'$'}2"
    old="${'$'}3"
    [ -d "${'$'}new" ] || exit 1
    rm -rf "${'$'}old"
    mv "${'$'}target" "${'$'}old" || exit 1
    if ! mv "${'$'}new" "${'$'}target"; then
      mv "${'$'}old" "${'$'}target"
      exit 1
    fi
    rmdir "${'$'}(dirname "${'$'}new")" 2>/dev/null
    exit 0
  """.trimIndent() + "\n"
}
