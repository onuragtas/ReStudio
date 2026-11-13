// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.startup.importSettings.jb

import com.intellij.openapi.application.PathManager
import java.nio.file.Path
import kotlin.io.path.name

/**
 * The vendor directory of the JetBrains IDEs.
 *
 * ReStudio keeps its settings under its own vendor directory.
 * The import wizard also looks in this directory to find the settings of a JetBrains IDE.
 */
internal const val JETBRAINS_VENDOR_DIR: String = "JetBrains"

/**
 * Returns the plugins directory of the IDE that keeps its settings in [confDir].
 *
 * [PathManager.getDefaultPluginPathFor] uses the vendor directory of ReStudio.
 * This function puts the vendor directory of [confDir] in its place.
 */
internal fun vendorPluginsDir(confDir: Path): Path {
  val defaultPath = Path.of(PathManager.getDefaultPluginPathFor(confDir.name))
  val ownVendor = Path.of(PathManager.getDefaultConfigPathFor("X")).parent?.name ?: return defaultPath
  val vendor = confDir.parent?.name ?: return defaultPath
  if (vendor == ownVendor) return defaultPath

  val names = defaultPath.map { it.toString() }.toMutableList()
  val index = names.indexOf(confDir.name)
  if (index < 1 || names[index - 1] != ownVendor) return defaultPath
  names[index - 1] = vendor
  val root = defaultPath.root ?: return defaultPath
  return names.fold(root) { path, name -> path.resolve(name) }
}
