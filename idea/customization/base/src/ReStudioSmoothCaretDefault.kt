// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.idea.customization.base

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.InitialConfigImportState

/** The key that `SmoothCaretAutoEnableActivity` reads. When the key has a value, the activity does nothing. */
internal const val SMOOTH_CARET_AUTO_ENABLE_REASON_KEY: String = "editor.smooth.caret.auto.enable.reason"

/**
 * Keeps smooth caret movement off for a new user.
 *
 * Upstream turns on smooth caret movement for each new user. The animated caret moves to a click or a typed character a few
 * milliseconds late, and users feel this as lag. This listener runs before the projects open. It records the reason `NONE`,
 * so the upstream activity does not change the settings. A user can still turn the option on in the editor settings.
 */
internal class ReStudioSmoothCaretDefault : ApplicationInitializedListener {
  override suspend fun execute() {
    if (!InitialConfigImportState.isNewUser()) return
    val properties = PropertiesComponent.getInstance()
    if (!properties.isValueSet(SMOOTH_CARET_AUTO_ENABLE_REASON_KEY)) {
      properties.setValue(SMOOTH_CARET_AUTO_ENABLE_REASON_KEY, "NONE")
    }
  }
}
