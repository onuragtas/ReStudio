// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.updateSettings.impl

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/**
 * The messages of the ReStudio full update ([ReStudioFullUpdate]).
 */
internal object ReStudioUpdateBundle {
  @NonNls
  private const val BUNDLE_FQN = "messages.ReStudioUpdateBundle"
  private val BUNDLE = DynamicBundle(ReStudioUpdateBundle::class.java, BUNDLE_FQN)

  @Nls
  fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE_FQN) key: String, vararg params: Any): String {
    return BUNDLE.getMessage(key, *params)
  }
}
