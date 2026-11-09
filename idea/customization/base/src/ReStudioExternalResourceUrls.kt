// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.idea.customization.base

import com.intellij.platform.ide.impl.customization.BaseJetBrainsExternalProductResourceUrls
import com.intellij.platform.ide.customization.FeedbackReporter
import com.intellij.util.Url
import com.intellij.util.Urls

/**
 * The web links of ReStudio. The bug reports and the downloads go to the ReStudio repository, not to JetBrains.
 * The IDE has no JetBrains support form, feedback form or "What's New" page.
 * The help topics stay on the IntelliJ IDEA help site, because the IDE features are the same.
 * The base class reads the update metadata URL from the `idea.updates.url` property. `ReStudioBranding` in `build/src` sets it.
 */
internal class ReStudioExternalResourceUrls : BaseJetBrainsExternalProductResourceUrls() {
  private val repository: Url = Urls.newFromEncoded("https://github.com/onuragtas/ReStudio")

  override val basePatchDownloadUrl: Url
    get() = repository.resolve("releases/download/")

  override val youtrackProjectId: String
    get() = ""

  override val shortProductNameUsedInForms: String?
    get() = null

  override val productPageUrl: Url
    get() = repository

  override val baseWebHelpUrl: Url
    get() = baseWebSiteUrl.resolve("help/idea/")

  override val bugReportUrl: ((String) -> Url)
    get() = { description -> repository.resolve("issues/new").addParameters(mapOf("body" to description)) }

  override val technicalSupportUrl: ((description: String) -> Url)?
    get() = null

  override val feedbackReporter: FeedbackReporter?
    get() = null

  override val downloadPageUrl: Url
    get() = repository.resolve("releases")

  override val whatIsNewPageUrl: Url?
    get() = null
}
