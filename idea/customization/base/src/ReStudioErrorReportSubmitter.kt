// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.idea.customization.base

import com.intellij.diagnostic.ITNReporter
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import java.awt.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Reports the errors of the platform and the JetBrains plugins as a GitHub issue in the ReStudio repository.
 * The submitter replaces [ITNReporter], so no report goes to JetBrains.
 * It extends [ITNReporter], because the IDE shows the platform errors of a release build only for other submitters.
 * The submitter only opens the browser. The user reads the issue and submits it on GitHub.
 */
internal class ReStudioErrorReportSubmitter : ITNReporter() {
  override fun getReportActionText(): String = ReStudioDiagnosticBundle.message("error.report.action")

  override fun getPrivacyNoticeText(): String = ReStudioDiagnosticBundle.message("error.report.notice")

  override fun submit(
    events: Array<IdeaLoggingEvent>,
    additionalInfo: String?,
    parentComponent: Component,
    consumer: Consumer<in SubmittedReportInfo>,
  ): Boolean {
    val event = events.firstOrNull() ?: return false
    val attachments = event.attachments.filter { it.isIncluded }.map { "--- ${it.path} ---\n${it.displayText}" }
    val url = ReStudioIssueUrl.build(event.throwableText, attachments, additionalInfo, environment(event))
    BrowserUtil.browse(url)
    consumer.consume(SubmittedReportInfo(url, null, SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
    return true
  }

  private fun environment(event: IdeaLoggingEvent): List<String> {
    val appInfo = ApplicationInfo.getInstance()
    val lines = mutableListOf(
      "${appInfo.fullApplicationName}, build ${appInfo.build.asString()}",
      "OS: ${SystemInfo.OS_NAME} ${SystemInfo.OS_VERSION} (${SystemInfo.OS_ARCH})",
      "Java: ${System.getProperty("java.runtime.version")} (${System.getProperty("java.vendor")})",
    )
    event.problematicPluginInfo?.let { lines.add("Plugin: ${it.name} ${it.version} (${it.pluginId.idString})") }
    return lines
  }
}

/**
 * Makes the URL of a new GitHub issue. The details are the stack trace and the attachments that the user included.
 * GitHub refuses a URL longer than about 8 KB, so the URL drops the end of the details.
 */
internal object ReStudioIssueUrl {
  const val NEW_ISSUE_URL: String = "https://github.com/onuragtas/ReStudio/issues/new"
  const val MAX_URL_LENGTH: Int = 8000
  private const val MAX_TITLE_LENGTH = 120

  fun build(throwableText: String, attachments: List<String>, additionalInfo: String?, environment: List<String>): String {
    val title = throwableText.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(MAX_TITLE_LENGTH) ?: "Error"
    val trace = (listOf(throwableText) + attachments).joinToString("\n").trimEnd().lines()
    var count = trace.size
    while (true) {
      val shown = trace.take(count) + (if (count < trace.size) listOf("\t... ${trace.size - count} more lines") else emptyList())
      val url = url(title, body(additionalInfo, environment, shown))
      if (url.length <= MAX_URL_LENGTH || count <= 1) return url
      count = minOf(count - 1, count * 3 / 4)
    }
  }

  private fun body(additionalInfo: String?, environment: List<String>, trace: List<String>): String = buildString {
    append("### What did you do before the error?\n\n")
    append(additionalInfo?.takeIf { it.isNotBlank() }?.trim() ?: "")
    append("\n\n### Environment\n\n")
    environment.forEach { append("- ").append(it).append('\n') }
    append("\n### Details\n\n```\n")
    trace.forEach { append(it).append('\n') }
    append("```\n")
  }

  private fun url(title: String, body: String): String = "$NEW_ISSUE_URL?title=${encode(title)}&body=${encode(body)}"

  private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}
