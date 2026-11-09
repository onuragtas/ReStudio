// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.intellij.build.impl.productInfo.IDE_HOME_PLACEHOLDER
import org.jetbrains.intellij.build.io.copyDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * The prebuilt plugins that `fork/fetch-bundled-plugins.sh` unpacks into `out/restudio-plugins`.
 *
 * The `bundled` directory holds the plugins that the IDE loads as bundled plugins.
 * The `path` directory holds the plugins that the IDE loads through the `plugin.path` property.
 * The platform applies its compatibility dependencies only to a plugin that is not bundled.
 * A legacy plugin from a vendor other than JetBrains needs these dependencies, so it goes into `path`.
 * Without `out/restudio-plugins`, the build is the same as upstream.
 */
@ApiStatus.Internal
object ReStudioPlugins {
  private const val SOURCE_DIRECTORY = "out/restudio-plugins"

  /** The directory in the IDE home that holds the `path` plugins. */
  private const val TARGET_DIRECTORY = "restudio-plugins"

  fun bundledPluginPaths(communityHome: Path): List<Path> = pluginDirectories(communityHome.resolve("$SOURCE_DIRECTORY/bundled"))

  fun copyPathPlugins(communityHome: Path, targetDir: Path) {
    for (plugin in pathPlugins(communityHome)) {
      copyDir(sourceDir = plugin, targetDir = targetDir.resolve(TARGET_DIRECTORY).resolve(plugin.fileName.toString()))
    }
  }

  /** The `plugin.path` argument for the IDE launchers, or nothing if there are no `path` plugins. */
  fun jvmArguments(communityHome: Path): List<String> {
    val plugins = pathPlugins(communityHome)
    if (plugins.isEmpty()) return emptyList()
    return listOf("-Dplugin.path=" + plugins.joinToString(",") { "$IDE_HOME_PLACEHOLDER/$TARGET_DIRECTORY/${it.fileName}" })
  }

  private fun pathPlugins(communityHome: Path): List<Path> = pluginDirectories(communityHome.resolve("$SOURCE_DIRECTORY/path"))

  private fun pluginDirectories(directory: Path): List<Path> {
    if (!Files.isDirectory(directory)) return emptyList()
    return Files.list(directory).use { entries -> entries.filter { Files.isDirectory(it) }.sorted().toList() }
  }
}
