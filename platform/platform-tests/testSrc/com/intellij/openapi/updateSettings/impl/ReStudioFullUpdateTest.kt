// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.updateSettings.impl

import com.intellij.openapi.updateSettings.UpdateStrategyCustomization
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.util.JDOMUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

/**
 * Checks the choice between a chain of patches, the full installer and the download page for the `updates.xml`
 * that `fork/make-updates-xml.sh` writes.
 */
class ReStudioFullUpdateTest {
  @get:Rule
  val temp: TemporaryFolder = TemporaryFolder()

  private val macSha = "a".repeat(64)
  private val linuxSha = "b".repeat(64)
  private val linuxArmSha = "c".repeat(64)
  private val base = "https://github.com/onuragtas/ReStudio/releases/download/restudio/v0.1.3/"

  private val updatesXml = """
    <products>
      <product name="ReStudio">
        <code>IC</code>
        <channel id="RS-RELEASE" status="release" licensing="release">
          <build number="263.7307.1019" version="0.1.3" releaseDate="20261114">
            <message>ReStudio 0.1.3 is available.</message>
            <blogPost url="https://example.invalid/v0.1.3"/>
            <button name="Download" url="https://example.invalid/v0.1.3" download="true"/>
            <patch from="263.7307.1018" size="4" exclusions="win"/>
            <restudio-package os="mac" arch="aarch64" url="${base}ReStudio-263.7307.1019-aarch64.sit" sha256="${macSha.uppercase()}" size="100"/>
            <restudio-package os="linux" arch="x64" url="${base}ReStudio-263.7307.1019.tar.gz" sha256="$linuxSha" size="200"/>
            <restudio-package os="linux" arch="aarch64" url="http://example.invalid/ReStudio-263.7307.1019-aarch64.tar.gz" sha256="$linuxArmSha"/>
          </build>
          <build number="263.7307.1018" version="0.1.2" releaseDate="20261110">
            <message>ReStudio 0.1.2 is available.</message>
            <blogPost url="https://example.invalid/v0.1.2"/>
            <button name="Download" url="https://example.invalid/v0.1.2" download="true"/>
            <restudio-package os="mac" arch="aarch64" url="${base}old.sit" sha256="${"d".repeat(64)}"/>
          </build>
        </channel>
      </product>
    </products>""".trimIndent()

  private val newBuild = BuildNumber.fromString("IC-263.7307.1019")!!

  private fun packages(build: BuildNumber = newBuild) = ReStudioFullUpdate.parsePackages(JDOMUtil.load(updatesXml), "IC", build)

  @Test
  fun `the packages of the new build are read`() {
    val packages = packages()
    assertThat(packages.map { "${it.os}/${it.arch}" }).containsExactly("mac/aarch64", "linux/x64", "linux/aarch64")
    assertThat(packages.first().sha256).isEqualTo(macSha)
    assertThat(packages.first().size).isEqualTo(100L)
    assertThat(packages[2].size).isNull()
  }

  @Test
  fun `another product code has no packages`() {
    assertThat(ReStudioFullUpdate.parsePackages(JDOMUtil.load(updatesXml), "IU", newBuild)).isEmpty()
  }

  @Test
  fun `the package matches the OS and the CPU`() {
    val mac = ReStudioFullUpdate.selectPackage(packages(), "mac", "aarch64")
    assertThat(mac?.url).isEqualTo("${base}ReStudio-263.7307.1019-aarch64.sit")
    assertThat(mac?.sha256).isEqualTo(macSha)

    val linux = ReStudioFullUpdate.selectPackage(packages(), "linux", "x64")
    assertThat(linux?.url).isEqualTo("${base}ReStudio-263.7307.1019.tar.gz")
    assertThat(linux?.sha256).isEqualTo(linuxSha)
  }

  @Test
  fun `a package without HTTPS, a bad checksum or another OS is not used`() {
    assertThat(ReStudioFullUpdate.selectPackage(packages(), "linux", "aarch64")).isNull()
    assertThat(ReStudioFullUpdate.selectPackage(packages(), "mac", "x64")).isNull()
    assertThat(ReStudioFullUpdate.selectPackage(packages(), null, "x64")).isNull()
    val badSha = ReStudioUpdatePackage("linux", "x64", "https://example.invalid/a.tar.gz", "1234", null)
    assertThat(ReStudioFullUpdate.selectPackage(listOf(badSha), "linux", "x64")).isNull()
    val twice = packages().filter { it.os == "linux" && it.arch == "x64" }.let { it + it }
    assertThat(ReStudioFullUpdate.selectPackage(twice, "linux", "x64")).isNull()
  }

  @Test
  fun `the decision prefers the patch chain, then the full installer`() {
    val pkg = ReStudioFullUpdate.selectPackage(packages(), "linux", "x64")
    assertThat(ReStudioFullUpdate.decide(true, pkg, true, true)).isEqualTo(ReStudioUpdatePath.PATCH_CHAIN)
    assertThat(ReStudioFullUpdate.decide(false, pkg, true, true)).isEqualTo(ReStudioUpdatePath.FULL_INSTALLER)
    assertThat(ReStudioFullUpdate.decide(false, null, true, true)).isEqualTo(ReStudioUpdatePath.DOWNLOAD_PAGE)
    assertThat(ReStudioFullUpdate.decide(false, pkg, false, true)).isEqualTo(ReStudioUpdatePath.DOWNLOAD_PAGE)
    assertThat(ReStudioFullUpdate.decide(false, pkg, true, false)).isEqualTo(ReStudioUpdatePath.DOWNLOAD_PAGE_NOT_WRITABLE)
  }

  @Test
  fun `the previous build uses the patch and an older build uses the full installer`() {
    assertThat(pathFor("IC-263.7307.1018")).isEqualTo(ReStudioUpdatePath.PATCH_CHAIN)
    assertThat(pathFor("IC-263.7307.999")).isEqualTo(ReStudioUpdatePath.FULL_INSTALLER)
  }

  private fun pathFor(currentBuild: String): ReStudioUpdatePath {
    val settings = UpdateSettings()
    settings.selectedChannelStatus = ChannelStatus.RELEASE
    val result = UpdateStrategy(
      BuildNumber.fromString(currentBuild)!!,
      parseUpdateData(updatesXml, "IC"),
      settings,
      UpdateStrategyCustomization(),
    ).checkForUpdates() as PlatformUpdates.Loaded
    val pkg = ReStudioFullUpdate.selectPackage(packages(result.newBuild.number), "linux", "x64")
    return ReStudioFullUpdate.decide(result.patches != null, pkg, true, true)
  }

  @Test
  fun `the install root is the app bundle on macOS and the home on Linux`() {
    assertThat(ReStudioFullUpdate.installRoot(Path.of("/Applications/ReStudio.app/Contents"), "mac"))
      .isEqualTo(Path.of("/Applications/ReStudio.app"))
    assertThat(ReStudioFullUpdate.installRoot(Path.of("/opt/ReStudio/Contents"), "mac")).isNull()
    assertThat(ReStudioFullUpdate.installRoot(Path.of("/opt/ReStudio"), "linux")).isEqualTo(Path.of("/opt/ReStudio"))
    assertThat(ReStudioFullUpdate.installRoot(Path.of("C:/ReStudio"), null)).isNull()
  }

  @Test
  fun `the install root must be writable with its parent`() {
    val root = Files.createDirectory(temp.root.toPath().resolve("ReStudio"))
    assertThat(ReStudioFullUpdate.isWritable(root)).isTrue()
    assertThat(ReStudioFullUpdate.isWritable(root.resolve("missing"))).isFalse()
  }

  @Test
  fun `the checksum is the SHA-256 of the file`() {
    val file = temp.root.toPath().resolve("installer.tar.gz")
    Files.writeString(file, "abc")
    assertThat(ReStudioFullUpdate.sha256(file, null)).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
  }
}
