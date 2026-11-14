// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.application

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

/**
 * Finds the plugins directory of an IDE from another vendor.
 *
 * The default plugins path of a settings selector uses the vendor directory of ReStudio.
 * When the import reads the settings of a JetBrains IDE, the plugins of that IDE are under the `JetBrains` directory.
 * On Linux, this directory is not under the settings directory, for example `~/.local/share/JetBrains/IdeaIC2026.3`.
 */
@ApiStatus.Internal
object ReStudioOldPluginsDir {
  /** Puts the vendor directory of [oldConfigDir] in place of the ReStudio vendor directory in [defaultPluginsDir]. */
  @JvmStatic
  fun forVendorOf(oldConfigDir: Path, defaultPluginsDir: Path): Path {
    val ownVendor = Path.of(PathManager.getDefaultConfigPathFor("X")).parent?.fileName?.toString() ?: return defaultPluginsDir
    return forVendorOf(oldConfigDir, defaultPluginsDir, ownVendor)
  }

  @JvmStatic
  fun forVendorOf(oldConfigDir: Path, defaultPluginsDir: Path, ownVendor: String): Path {
    val selector = oldConfigDir.fileName?.toString() ?: return defaultPluginsDir
    val vendor = oldConfigDir.parent?.fileName?.toString() ?: return defaultPluginsDir
    if (vendor == ownVendor) return defaultPluginsDir

    val names = defaultPluginsDir.map { it.toString() }.toMutableList()
    val index = names.indexOf(selector)
    if (index < 1 || names[index - 1] != ownVendor) return defaultPluginsDir
    names[index - 1] = vendor
    val root = defaultPluginsDir.root ?: return defaultPluginsDir
    return names.fold(root) { path, name -> path.resolve(name) }
  }
}
