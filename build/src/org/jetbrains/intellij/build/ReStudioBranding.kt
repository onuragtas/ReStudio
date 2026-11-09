// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * The names, paths and URLs of the ReStudio distribution. [IdeaCommunityProperties] and its customizers read them.
 * `community-resources/resources/idea/IdeaApplicationInfo.xml` holds the names that the IDE shows.
 */
@ApiStatus.Internal
object ReStudioBranding {
  const val FULL_NAME: String = "ReStudio"

  /** The root directory name of the Linux archive and the Windows installation directory. */
  const val DIRECTORY_NAME: String = "ReStudio"

  const val MAC_BUNDLE_IDENTIFIER: String = "io.github.onuragtas.restudio"

  const val URL_SCHEME: String = "restudio"

  /**
   * The base name of the launchers: `bin/restudio.sh` and `Contents/MacOS/restudio`.
   * The launchers read the `RESTUDIO_PROPERTIES`, `RESTUDIO_VM_OPTIONS` and `RESTUDIO_JDK` environment variables.
   * The `script` attribute in `IdeaApplicationInfo.xml` must have the same value.
   */
  const val LAUNCHER_NAME: String = "restudio"

  const val RELEASES_URL: String = "https://github.com/onuragtas/ReStudio/releases"

  /**
   * The update metadata. The release workflow replaces this file in the `update-metadata` release.
   * A `latest` link does not work, because GitHub does not count a prerelease as the latest release.
   */
  const val UPDATES_URL: String = "$RELEASES_URL/download/update-metadata/updates.xml"

  /**
   * The VM options that the launchers add.
   * - `idea.updates.url`: the IDE reads the ReStudio update metadata, not the JetBrains metadata.
   * - `promo.ignore.suggested.ide`: the editor does not suggest a JetBrains IDE for a file type.
   */
  val VM_OPTIONS: List<String> = listOf("-Didea.updates.url=$UPDATES_URL", "-Dpromo.ignore.suggested.ide=true")

  /** `fork/branding/make-images.swift` makes the files in this directory. */
  fun imagesDirectory(communityHome: Path): Path = communityHome.resolve("fork/branding/images")

  /** The paths selector. It names the config, system and log directories, apart from the JetBrains IDEs. */
  fun systemSelector(appInfo: ApplicationInfoProperties): String = "ReStudio${appInfo.majorVersion}.${appInfo.minorVersionMainPart}"

  fun baseArtifactName(buildNumber: String): String = "ReStudio-$buildNumber"

  /**
   * Replaces `LICENSE.txt` and `NOTICE.txt` in the IDE home with the files in `fork/legal`.
   * The root `LICENSE.txt` holds the JetBrains terms for the JetBrains builds. These terms do not apply to ReStudio.
   */
  fun copyLegalFiles(communityHome: Path, targetDir: Path) {
    for (name in listOf("LICENSE.txt", "NOTICE.txt")) {
      Files.copy(communityHome.resolve("fork/legal/$name"), targetDir.resolve(name), StandardCopyOption.REPLACE_EXISTING)
    }
  }
}
