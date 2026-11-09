// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.intellij.build.BuildPaths.Companion.COMMUNITY_ROOT
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ReStudioBrandingTest {
  private val communityHome: Path = COMMUNITY_ROOT.communityRoot

  @Test
  fun `the legal files of ReStudio replace the root files`(@TempDir target: Path) {
    Files.writeString(target.resolve("LICENSE.txt"), "JETBRAINS OPEN-SOURCE BUILD TERMS")

    ReStudioBranding.copyLegalFiles(communityHome, target)

    assertThat(target.resolve("LICENSE.txt")).content().contains("Apache License", "Version 2.0, January 2004").doesNotContain("JETBRAINS")
    assertThat(target.resolve("NOTICE.txt")).content().contains("ReStudio is not a JetBrains product.")
  }

  @Test
  fun `the images directory has each file that the installers need`() {
    val images = ReStudioBranding.imagesDirectory(communityHome)
    val required = listOf(
      "linux/product_128.png",
      "mac/dmg_background.tiff", "mac/product.icns",
      "win/headerlogo.bmp", "win/install.ico", "win/logo.bmp", "win/product.ico", "win/uninstall.ico",
    )

    assertThat(required.filterNot { Files.isRegularFile(images.resolve(it)) }).isEmpty()
  }

  @Test
  fun `the application info uses the ReStudio names and images`() {
    val resources = communityHome.resolve("community-resources/resources")
    val applicationInfo = Files.readString(resources.resolve("idea/IdeaApplicationInfo.xml"))

    assertThat(applicationInfo)
      .contains("""<company name="ReStudio"""", """fullname="ReStudio"""", """script="${ReStudioBranding.LAUNCHER_NAME}"""")
      .doesNotContain("JetBrains")
    for (image in Regex("""(?:url|svg|svg-small)="/([^"]+)"""").findAll(applicationInfo).map { it.groupValues[1] }) {
      assertThat(resources.resolve(image)).exists()
    }
  }

  @Test
  fun `the ReStudio error report submitter comes before the JetBrains submitter`() {
    val descriptor = Files.readString(communityHome.resolve("idea/customization/base/resources/intellij.idea.customization.base.xml"))

    assertThat(descriptor).contains("""<errorHandler implementation="com.intellij.idea.customization.base.ReStudioErrorReportSubmitter" order="first"/>""")
  }
}
