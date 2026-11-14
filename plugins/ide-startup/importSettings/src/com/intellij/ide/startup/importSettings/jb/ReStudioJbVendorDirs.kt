// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.startup.importSettings.jb

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ReStudioOldPluginsDir
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
 * The restart after a full import uses the same rule in `ConfigImportHelper`, through [ReStudioOldPluginsDir].
 */
internal fun vendorPluginsDir(confDir: Path): Path =
  ReStudioOldPluginsDir.forVendorOf(confDir, Path.of(PathManager.getDefaultPluginPathFor(confDir.name)))
