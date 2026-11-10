# ReStudio

ReStudio is a free, open-source IDE for many languages. It is a fork of the
[IntelliJ Platform open-source code](https://github.com/JetBrains/intellij-community).

> ReStudio is not a JetBrains product. JetBrains does not make, support or endorse it.
> IntelliJ and IntelliJ IDEA are trademarks of JetBrains s.r.o.

## Status

ReStudio is in an early preview. Expect bugs.

## Languages

| Language | Support | Comes from |
|---|---|---|
| Java, Kotlin | Full | The IntelliJ Platform |
| Python | Full | The open-source Python plugin |
| Dart, Flutter | Full | The Dart and Flutter plugins |
| Go, C# (.NET), PHP, web (TypeScript, JavaScript, Vue, Angular, HTML, CSS) | Through language servers | [ReStudio-langs](https://github.com/onuragtas/ReStudio-langs) |
| Other languages | Through language servers | [LSP4IJ](https://github.com/redhat-developer/lsp4ij) |

The language servers and debug adapters are separate tools. Settings > Tools > ReStudio Tools shows the tools that ReStudio found.

ReStudio does not contain the closed-source features of the JetBrains IDEs.
For example, it does not have the JetBrains support for JavaScript, TypeScript or Spring.

## Install

Download the installer for your system from [GitHub Releases](https://github.com/onuragtas/ReStudio/releases).

- **Linux:** extract the `.tar.gz` file and run `bin/restudio.sh`.
- **macOS (Apple silicon):** open the `.dmg` file and drag the app into Applications.
  The app is signed and notarized. ReStudio has no installer for Intel Macs.

ReStudio keeps its settings apart from the JetBrains IDEs. You can install both on one computer.

## JetBrains Marketplace

The Plugins dialog connects to JetBrains Marketplace. The
[JetBrains Marketplace terms](https://plugins.jetbrains.com/legal/terms-of-use) apply to that use.
ReStudio puts Dart, Flutter, LSP4IJ and the ReStudio language plugins into the IDE. You get the updates of these plugins with a new ReStudio release.

## Build from source

The build steps are the same as for the IntelliJ Platform. See the [upstream README](../README.md).
Before the build, run `fork/fetch-bundled-plugins.sh`. It downloads the plugins that ReStudio puts into the IDE.

```
fork/fetch-bundled-plugins.sh
./installers.cmd -Dintellij.build.target.os=current
```

The installers are in `out/idea-ce/artifacts/`.

## License

The code of ReStudio is under the [Apache License 2.0](../fork/legal/LICENSE.txt).
The bundled plugins keep their own licenses. See [NOTICE](../fork/legal/NOTICE.txt).
The `LICENSE.txt` file at the root of this repository contains the JetBrains terms for the JetBrains builds.
These terms do not apply to the ReStudio builds.

## Upstream

ReStudio follows the JetBrains release tags. See [docs/upstream-sync.md](../docs/upstream-sync.md).
