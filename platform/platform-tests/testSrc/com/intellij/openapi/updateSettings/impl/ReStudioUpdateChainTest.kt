// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.updateSettings.impl

import com.intellij.openapi.updateSettings.UpdateStrategyCustomization
import com.intellij.openapi.util.BuildNumber
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Checks that the IDE chains the patches of the `updates.xml` that `fork/make-updates-xml.sh` writes.
 */
class ReStudioUpdateChainTest {
  private val updatesXml = """
    <products>
      <product name="ReStudio">
        <code>IC</code>
        <channel id="RS-RELEASE" status="release" licensing="release">
          <build number="263.7307.1018" version="0.1.2" releaseDate="20261110">
            <message>ReStudio 0.1.2 is available.</message>
            <blogPost url="https://example.invalid/v0.1.2"/>
            <button name="Download" url="https://example.invalid/v0.1.2" download="true"/>
            <patch from="263.7307.1017" size="4" exclusions="win"/>
          </build>
          <build number="263.7307.1017" version="0.1.1" releaseDate="20261110">
            <message>ReStudio 0.1.1 is available.</message>
            <blogPost url="https://example.invalid/v0.1.1"/>
            <button name="Download" url="https://example.invalid/v0.1.1" download="true"/>
            <patch from="263.7307.1001" size="300" exclusions="win"/>
          </build>
        </channel>
      </product>
    </products>""".trimIndent()

  @Test
  fun `an older build updates through a chain of patches`() {
    val result = check("IC-263.7307.1001")
    assertThat(result.newBuild.number.asStringWithoutProductCode()).isEqualTo("263.7307.1018")
    assertThat(result.patches?.chain).isEqualTo(listOf("263.7307.1001", "263.7307.1017", "263.7307.1018").map(BuildNumber::fromString))
    assertThat(result.patches?.size).isEqualTo("304")
  }

  @Test
  fun `the previous build updates with one patch`() {
    val result = check("IC-263.7307.1017")
    assertThat(result.patches?.chain?.map { it.asStringWithoutProductCode() }).containsExactly("263.7307.1017", "263.7307.1018")
  }

  @Test
  fun `a build outside the chain gets only the download`() {
    val result = check("IC-263.7307.999")
    assertThat(result.newBuild.number.asStringWithoutProductCode()).isEqualTo("263.7307.1018")
    assertThat(result.patches).isNull()
  }

  private fun check(currentBuild: String): PlatformUpdates.Loaded {
    val settings = UpdateSettings()
    settings.selectedChannelStatus = ChannelStatus.RELEASE
    val result = UpdateStrategy(
      BuildNumber.fromString(currentBuild)!!,
      parseUpdateData(updatesXml, "IC"),
      settings,
      UpdateStrategyCustomization(),
    ).checkForUpdates()
    assertThat(result).isInstanceOf(PlatformUpdates.Loaded::class.java)
    return result as PlatformUpdates.Loaded
  }
}
