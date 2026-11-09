// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.plugins

import com.intellij.platform.pluginSystem.parser.impl.PluginDescriptorBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class ReStudioPluginPathUpdateTest {
  @TempDir
  lateinit var home: Path

  private val previous: String? = System.getProperty("plugin.path")

  @AfterEach
  fun restoreProperty() {
    if (previous == null) System.clearProperty("plugin.path") else System.setProperty("plugin.path", previous)
  }

  @Test
  fun `a plugin from the plugin path property is not updateable`() {
    System.setProperty("plugin.path", "${home.resolve("Dart")},${home.resolve("lsp4ij")}")

    assertThat(PluginUtils.isUpdateable(plugin(home.resolve("lsp4ij")))).isFalse()
  }

  @Test
  fun `the plugin path property can use the path separator`() {
    System.setProperty("plugin.path", home.resolve("Dart").toString() + File.pathSeparator + home.resolve("lsp4ij"))

    assertThat(PluginUtils.isUpdateable(plugin(home.resolve("Dart")))).isFalse()
  }

  @Test
  fun `an installed plugin stays updateable`() {
    System.setProperty("plugin.path", home.resolve("Dart").toString())

    assertThat(PluginUtils.isUpdateable(plugin(home.resolve("plugins/Dart")))).isTrue()
  }

  @Test
  fun `an installed plugin is updateable without the property`() {
    System.clearProperty("plugin.path")

    assertThat(PluginUtils.isUpdateable(plugin(home.resolve("Dart")))).isTrue()
  }

  private fun plugin(path: Path): PluginMainDescriptor {
    val raw = PluginDescriptorBuilder.builder().apply {
      id = "com.example.plugin"
      version = "1.0"
    }.build()
    return PluginMainDescriptor(raw = raw, pluginPath = path, isBundled = false)
  }
}
