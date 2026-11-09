// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.intellij.build.impl.productInfo.ProductJvmArguments
import org.jetbrains.intellij.build.impl.productInfo.renderAdditionalJvmArguments
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ReStudioPluginsTest {
  @TempDir
  lateinit var home: Path

  @Test
  fun `no plugin directory gives the upstream build`() {
    assertThat(ReStudioPlugins.bundledPluginPaths(home)).isEmpty()
    assertThat(ReStudioPlugins.jvmArguments(home)).isEmpty()
  }

  @Test
  fun `bundled plugins come only from the bundled directory`() {
    val python = plugin("bundled", "python-ce")
    plugin("path", "Dart")

    assertThat(ReStudioPlugins.bundledPluginPaths(home)).containsExactly(python)
  }

  @Test
  fun `path plugins go into one plugin path argument in sorted order`() {
    plugin("path", "lsp4ij")
    plugin("path", "Dart")
    plugin("bundled", "python-ce")

    assertThat(ReStudioPlugins.jvmArguments(home))
      .containsExactly("-Dplugin.path=__IDE_HOME__/restudio-plugins/Dart,__IDE_HOME__/restudio-plugins/lsp4ij")
  }

  @Test
  fun `path plugins are copied into the restudio-plugins directory`() {
    plugin("path", "Dart")
    val target = Files.createDirectories(home.resolve("dist"))

    ReStudioPlugins.copyPathPlugins(home, target)

    assertThat(target.resolve("restudio-plugins/Dart/lib/plugin.jar")).exists()
  }

  @Test
  fun `the renderer replaces the placeholder with the home macro of each operating system`() {
    plugin("path", "Dart")

    assertThat(pluginPathArgument(OsFamily.MACOS)).isEqualTo($$"-Dplugin.path=$APP_PACKAGE/Contents/restudio-plugins/Dart")
    assertThat(pluginPathArgument(OsFamily.MACOS, isPortableDist = true)).isEqualTo($$"-Dplugin.path=$APP_PACKAGE/restudio-plugins/Dart")
    assertThat(pluginPathArgument(OsFamily.LINUX)).isEqualTo($$"-Dplugin.path=$IDE_HOME/restudio-plugins/Dart")
    assertThat(pluginPathArgument(OsFamily.WINDOWS)).isEqualTo("-Dplugin.path=%IDE_HOME%/restudio-plugins/Dart")
  }

  @Test
  fun `the renderer quotes the plugin path argument in a script and leaves other arguments as they are`() {
    plugin("path", "Dart")
    val arguments = render(OsFamily.LINUX, additional = ReStudioPlugins.jvmArguments(home) + "-Dother=value", isScript = true)

    assertThat(arguments).contains($$"\"-Dplugin.path=$IDE_HOME/restudio-plugins/Dart\"", "-Dother=value")
  }

  private fun pluginPathArgument(os: OsFamily, isPortableDist: Boolean = false): String =
    render(os, ReStudioPlugins.jvmArguments(home), isPortableDist = isPortableDist).single { it.startsWith("-Dplugin.path=") }

  private fun render(os: OsFamily, additional: List<String>, isScript: Boolean = false, isPortableDist: Boolean = false): List<String> =
    renderAdditionalJvmArguments(
      jvm = ProductJvmArguments(vendorName = "ReStudio", pathsSelector = "ReStudio", additional = additional),
      os = os,
      arch = JvmArchitecture.aarch64,
      openedPackages = emptyList(),
      isScript = isScript,
      isPortableDist = isPortableDist,
    )

  private fun plugin(mode: String, name: String): Path {
    val directory = Files.createDirectories(home.resolve("out/restudio-plugins/$mode/$name"))
    Files.createDirectories(directory.resolve("lib"))
    Files.writeString(directory.resolve("lib/plugin.jar"), "")
    return directory
  }
}
