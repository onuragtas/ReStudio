// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ReStudioOldPluginsDirTest {
  private val linuxDefault = Path.of("/home/u/.local/share/ReStudio/IdeaIC2026.3")

  @Test
  fun `the plugins of a JetBrains IDE are under the JetBrains directory`() {
    val config = Path.of("/home/u/.config/JetBrains/IdeaIC2026.3")
    assertThat(ReStudioOldPluginsDir.forVendorOf(config, linuxDefault, "ReStudio"))
      .isEqualTo(Path.of("/home/u/.local/share/JetBrains/IdeaIC2026.3"))
  }

  @Test
  fun `the plugins of an older ReStudio stay under the ReStudio directory`() {
    val config = Path.of("/home/u/.config/ReStudio/IdeaIC2026.3")
    assertThat(ReStudioOldPluginsDir.forVendorOf(config, linuxDefault, "ReStudio")).isEqualTo(linuxDefault)
  }

  @Test
  fun `a path without the vendor directory does not change`() {
    val config = Path.of("/home/u/.config/JetBrains/IdeaIC2026.3")
    val custom = Path.of("/opt/plugins/IdeaIC2026.3")
    assertThat(ReStudioOldPluginsDir.forVendorOf(config, custom, "ReStudio")).isEqualTo(custom)
  }
}
