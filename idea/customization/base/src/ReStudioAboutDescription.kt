// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.idea.customization.base

import com.intellij.AbstractBundle
import com.intellij.ide.AboutPopupDescriptionProvider
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.ReStudioAboutBundle"

private object ReStudioAboutBundle : AbstractBundle(BUNDLE) {
  @Nls
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String): String = getMessage(key)
}

/**
 * Adds the origin of ReStudio and the trademark notice to Help > About.
 * The splash screen shows only the product name, so the About dialog is the place for this text.
 */
internal class ReStudioAboutDescription : AboutPopupDescriptionProvider {
  override fun getDescription(): String =
    ReStudioAboutBundle.message("about.origin") + "<br>" + ReStudioAboutBundle.message("about.trademark")

  override fun getExtendedDescription(): String =
    ReStudioAboutBundle.message("about.origin") + "\n" + ReStudioAboutBundle.message("about.trademark")
}
