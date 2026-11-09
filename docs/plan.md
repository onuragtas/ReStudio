# Açık Kaynak Çok Dilli IDE (intellij-community fork) — Proje Planı

Nov 7, 2026 · @Onur Ağtaş

## Amaç ve kapsam

intellij-community fork'lanacak; Java ve Kotlin'in yanına .NET, Go, PHP, Flutter ve frontend desteği eklenerek kendi markasında, GitHub'da public yayınlanan bir IDE çıkarılacak. Dil zekâsı sıfırdan yazılmayacak: açık kaynak language server'lar ve debugger'lar entegre edilecek, açık kaynak plugin'i olan diller doğrudan pakete eklenecek.

Önemli kısıt: JetBrains'in ücretsiz katmandaki bazı özellikleri (2026.1'deki JS/TS/HTML/CSS/React desteği, temel Spring desteği) kapalı kaynak; fork'un açık kaynak build'ine girmiyor. Bu yüzden frontend de LSP ile kurulacak.

| Dil / teknoloji | Fork'ta nereden gelir | Language server | Debugger | Bizim iş |
| --- | --- | --- | --- | --- |
| Java, Kotlin | intellij-community | Yerleşik | Yerleşik | Yok |
| Python | intellij-community'deki açık kaynak Python plugin'i | Yerleşik | Yerleşik | Pakete eklemek |
| Flutter, Dart | Google'ın Flutter plugin'i + Dart plugin'i | Dart analysis server | Yerleşik | Pakete eklemek |
| C#, .NET | Bizim plugin | Roslyn LS | netcoredbg | Tam paket |
| Go | Bizim plugin | gopls | Delve (`dlv dap`) | Tam paket |
| JS/TS, HTML/CSS, React, Vue, Angular | Bizim plugin | vtsls, vscode-langservers-extracted, Vue LS, Angular LS, Tailwind LS, ESLint | vscode-js-debug | Tam paket |
| PHP | Bizim plugin | Phpactor | Xdebug (vscode-php-debug adaptörü) | Tam paket |
| Spring | Yok (JetBrains'in Spring desteği kapalı kaynak) | — | Java debugger | İlk sürümde kapsam dışı |

**Kapsamda**

- Her "tam paket" dil için: completion, go-to-definition, find usages, rename, diagnostics, formatting
- Proje modeli: `.sln`/`.csproj`, `go.mod`/`go.work`, `composer.json`, `package.json` okunup modüllere eşlenmesi
- Run configuration'lar: `dotnet`, `go`, PHP script/server, npm/pnpm script'leri
- Debug: breakpoint, step, değişken, watch (DAP üzerinden)
- Test runner: xUnit/NUnit/MSTest, `go test`, PHPUnit/Pest, Vitest/Jest sonuçlarının IntelliJ test penceresinde gösterilmesi
- Paket yönetimi: NuGet arayüzü; diğer dillerde CLI komutları için run configuration
- Kendi adı ve ikonuyla IDE dağıtımı: GitHub'da public kaynak + GitHub Releases'ta macOS/Linux installer (Windows yok, karar: 9 Kasım 2026)

**Kapsam dışı**

- Rider, GoLand, PhpStorm, WebStorm seviyesinde inspection ve refactoring kataloğu
- Framework'lerin derin desteği (Spring, Laravel, Next.js vb.); temel dil desteği üzerinden çalışırlar
- Razor, Blazor, XAML, MAUI, Unity; F# ve VB.NET
- Ticari dağıtım, ücretli sürüm veya JetBrains Marketplace'e plugin yayını (ilk aşamada)

**Başarı ölçütü:** Her hedef dil için bir örnek proje (ASP.NET Core API, Go servisi, Laravel uygulaması, Flutter uygulaması, React ve Vue uygulaması) IDE'de açılıp yazılabiliyor, çalıştırılabiliyor, test edilebiliyor ve debug edilebiliyor; bunun için başka bir IDE açmaya gerek kalmıyor.

## Mimari

Karar: intellij-community fork'lanıp kendi markanla bir IDE olarak yayınlanacak. Dil desteği ise fork'un içinde değil, ayrı bir plugin reposunda geliştirilip fork'a bundled plugin olarak gömülecek. Böylece fork ince kalır (marka + paketleme), asıl iş hızlı derlenen plugin reposunda yapılır.

**İki repo, iki görev**

|  | Fork reposu (`<isim>-ide`) | Plugin reposu (`<isim>-langs`) |
| --- | --- | --- |
| İçerik | intellij-community + marka, ikonlar, ürün tanımı, bundled plugin listesi | Ortak dil altyapısı + .NET, Go, PHP ve frontend plugin'leri |
| Build | IntelliJ ile kaynaktan derleme, `installers.cmd` ile installer; uzun ve ağır | Gradle, dakikalar; `runIde` ile anında deneme |
| Değişim sıklığı | JetBrains sürümü başına bir upstream merge + marka değişiklikleri | Günlük geliştirme |
| Çıktı | GitHub Releases'ta macOS/Linux installer | Her dil için plugin `.zip`'i; fork build'i bunları içine alır, istenirse resmi IntelliJ'ye de ayrıca kurulabilir |

Neden kodu fork'un içine yazmıyoruz: intellij-community'de her değişiklik büyük bir JPS build'i tetikler ve upstream merge'lerinde senin kodunla JetBrains kodu çakışır. Ayrı plugin reposu hem geliştirmeyi hızlandırır hem de upstream merge'lerini sadece marka dosyalarına indirger.

**Katmanlar**

| Katman | Bileşen | Görevi |
| --- | --- | --- |
| IDE | intellij-community fork'u (Apache 2.0) | Editör, proje görünümü, run/debug/test arayüzü; Java, Kotlin |
| Paketlenen açık kaynak plugin'ler | Python, Flutter, Dart, [LSP4IJ](https://github.com/redhat-developer/lsp4ij) | Kendi dilleri; LSP4IJ, LSP ve DAP mesajlarını IntelliJ özelliklerine eşler |
| Ortak altyapı (bizim) | `core` modülü | SDK bulma, language server indirme/sürüm sabitleme, ortak DAP debug ve test runner adaptörleri |
| Dil paketleri (bizim) | `dotnet`, `go`, `web`, `php` modülleri | Proje modeli, run configuration'lar, dile özel LSP mesajları |
| Harici araçlar | Language server'lar, debug adapter'lar, SDK CLI'ları (`dotnet`, `go`, `php`, `node`, `flutter`) | Dil zekâsı, debug, build, test |

**Lisans notları**

- JetBrains'in "ücretsiz ama kapalı kaynak" özellikleri (JS/TS desteği, temel Spring desteği vb.) fork'ta yok; kopyalanmayacak, decompile edilmeyecek.
- Microsoft'un vsdbg debugger'ı lisans gereği sadece Microsoft ürünlerinde kullanılabilir; .NET için netcoredbg kullanılacak.
- PHP'de Intelephense'in premium özellikleri kapalı kaynak ve lisanslı; varsayılan Phpactor (MIT) olacak.
- Paketlenecek her bileşenin lisansı (LSP4IJ EPL 2.0, Flutter BSD-3, Dart, language server'lar, debug adapter'lar) Faz 7'de NOTICE'e işlenecek.
- Ürün adında "IntelliJ", JetBrains ürün adları veya logoları kullanılmayacak.

## Faz 0: Keşif ve fork kurulumu (1-2 hafta)

Kod yazmadan önce mevcut parçaların kutudan ne verdiğini ölç; eksik listesi sonraki fazların gerçek kapsamını belirler.

**Fork'u ayağa kaldır**

- [x] [JetBrains/intellij-community](https://github.com/JetBrains/intellij-community)'yi GitHub'da fork'la; upstream merge'leri için geçmişi tut (`--depth 1` değil, gerekirse `--filter=blob:none`). Fork: `onuragtas/ReStudio`, tam geçmişle.
- [ ] Windows'ta klonlamadan önce `git config --global core.longpaths true` (sadece Windows'ta build alınırsa)
- [x] Son kararlı sürüm tag'inden kendi `main` branch'ini aç; JetBrains'in `master`'ını takip etme. `main`, `idea/263.7307.200` üzerinde. Bu tag `idea/2026.3` ile aynı commit; base `fork/UPSTREAM_BASE`'de yazar.
- [ ] Kaynağı IntelliJ ile aç, JetBrains Runtime SDK'sını kur, "IDEA" run configuration'ı ile çalıştır
  - Ertelendi. Bu iş sadece fork'un kaynağında çalışırken gerekli (Faz 6-7). Dil işleri plugin reposunda yapılıyor ve installer build'i (yukarıda) fork'un derlendiğini zaten kanıtlıyor.
- [x] `./installers.cmd -Dintellij.build.target.os=current` ile kendi işletim sisteminde installer üret; build süresini, disk ve RAM kullanımını not et
  - Ölçüm (8 Kasım 2026, M1 Pro, 10 çekirdek, 32 GB RAM, soğuk cache): toplam 37,5 dk. Bazel derlemesi 12,5 dk (6.535 action), paketleme ve `.dmg` 25 dk.
  - En yüksek RAM: yaklaşık 10 GB (Bazel + Java worker'lar). Disk: yaklaşık 28 GB (`out/` 14 GB, Bazel cache 11 GB, `build/download` 2 GB, `android/` 0,9 GB).
  - Çıktı (`out/idea-ce/artifacts/`): `-aarch64.dmg` (816 MB) ve Intel için `.dmg` (828 MB), ayrıca `.sit`, SHA ve SPDX dosyaları. Tek komut iki mimariyi de üretiyor. İmzasız; ad hâlâ "IntelliJ IDEA OSS".
  - Paketleme sırasında build iki kez birkaç dakika dosya erişiminde bekledi. Aynı anda VS Code'un `rg` aramaları ve Spotlight çalışıyordu. `out/`, `android/`, `bazel-*` dizinlerini VS Code `search.exclude`/`files.watcherExclude` ve Spotlight dışında tut.
  - Build, repo kökünde `android/` checkout'u ister; yoksa `intellij.android.adb.iml` bulunamadı hatasıyla durur. `getPlugins.sh` eski `git.jetbrains.org` adresini kullanır, bunun yerine: `git clone --depth 1 --branch "$(cat fork/UPSTREAM_BASE)" https://github.com/JetBrains/android.git android` (~940 MB). `JetBrains/android`'de aynı tag var.
  - Her upstream merge'den sonra `android/` da yeni tag'e geçirilmeli.

**Dil bileşenlerini dene**

Geliştirme makinesine kurulanlar (8 Kasım 2026, Homebrew, `go install`, `npm -g`, `dotnet tool -g`, `pecl`). GitHub release'lerinden indirilenler `~/.restudio-dev/tools/<araç>/<sürüm>/` altında:

| Dil | SDK | Language server | Debugger |
| --- | --- | --- | --- |
| .NET | .NET SDK 10.0.403 | roslyn-language-server 5.13.0-1.26514.10, csharp-ls 0.28.0 | netcoredbg 3.2.0-1092 (`osx-arm64`) |
| Go | Go 1.27.3 | gopls 0.24.0 | Delve 1.27.2 |
| Frontend | Node.js 26.13.0, TypeScript 7.0.2 | vtsls 0.3.0, typescript-language-server 6.0.3, vscode-langservers-extracted 4.10.0, Vue LS 3.3.14, Angular LS 22.2.5, Tailwind LS 0.16.0 | vscode-js-debug 1.140.0 |
| PHP | PHP 8.5.12, Composer 2.11.1 | Phpactor 2026.10.07.0 (`.phar`) | Xdebug 3.5.3, vscode-php-debug 1.40.2 |
| Flutter | Flutter 3.47.9 | Dart analysis server | Flutter plugin |
| Java | OpenJDK 21.0.13 (keg-only, `JAVA_HOME=/opt/homebrew/opt/openjdk@21`) | | |

TypeScript 7 Go ile yeniden yazılmış sürüm ve `tsserver.js` içermez. Vite'ın React şablonu TS 7, Vue şablonu TS 6.0.3 kullanıyor. Sonuçlar aşağıdaki boşluk tablosunda.

- [ ] Resmi IntelliJ IDEA üzerine [LSP4IJ](https://github.com/redhat-developer/lsp4ij) kur; hazır şablonları (Go, HTML, CSS, TypeScript vb.) ve [user-defined language server](https://www.github.com/redhat-developer/lsp4ij/blob/main/docs/UserDefinedLanguageServer.md) ayarını kullan (fork'u beklemeye gerek yok)
- [x] .NET: `dotnet tool install --global roslyn-language-server --prerelease` ([NuGet](https://www.nuget.org/packages/roslyn-language-server.linux-x64)); yedek olarak [csharp-ls](https://github.com/razzmatazz/csharp-language-server); debug için [netcoredbg](https://github.com/samsung/netcoredbg)
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): Roslyn ve netcoredbg kullanılıyor. csharp-ls yedeği yok.
  - Yazıldı (10 Kasım 2026, ReStudio-langs 4a23591), derlendi, test edilmedi: Ayar: Auto, Roslyn LS ya da csharp-ls.
- [x] Go: `gopls` + `dlv dap`
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): `go/lsp/GoplsServerFactory.kt`, `go/debug/DelveDescriptorFactory.kt` ve probe kayıtları var.
- [x] Frontend: vtsls (veya typescript-language-server), vscode-langservers-extracted, Vue LS, Angular LS, ESLint; debug için vscode-js-debug
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): probe kayıtları ve `testdata/web/*` var. Plugin, vscode-langservers-extracted'dan sadece ESLint'i kullanıyor.
- [x] PHP: Phpactor + vscode-php-debug adaptörüyle Xdebug
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): `php/lsp/PhpServerFactory.kt`, `php/debug/PhpDebugDescriptorFactory.kt` ve probe kayıtları var.
- [x] Flutter: fork build'inde Flutter ve Dart plugin'lerinin kurulup çalıştığını doğrula
  - Marketplace bu build için Dart 511.0.0 ve Flutter 98.0.0 veriyor. İkisi de hatasız yükleniyor. Dart SDK 3.13.7 bulunuyor ve analysis server çalışıyor.
  - Dart plugin'i platformun yerleşik LSP API'sini (`com.intellij.platform.lsp`) kullanıyor. Bu API açık kaynak build'de var. Faz 1'de LSP4IJ'ye alternatif olarak değerlendirilmeli.
  - macOS masaüstü debug'ı tam Xcode ve CocoaPods ister. Sadece Command Line Tools varken `flutter run` "[1/1] MacOS SDK" satırında hata vermeden takılıyor. IDE bu durumu algılayıp mesaj göstermeli (Faz 6). Chrome (web) cihazı Xcode'suz çalışır.
- [x] Her dil için bir örnek proje seç: ASP.NET Core solution (5-10 proje), çok paketli Go servisi, Laravel uygulaması, React (Vite) ve Vue uygulaması, Angular ve Tailwind uygulaması, Flutter uygulaması
  - `~/projects/ReStudio-langs/testdata/` (yerel repo, push edilmedi). Hepsi şablondan üretildi ve build/test'leri geçiyor: `dotnet/` (7 proje, `Shop.sln`), `go/` (`go.work`, 2 modül), `php/` (Laravel), `web/react/` (TS 7), `web/vue/` (TS 6, ESLint ve Vitest eklendi), `web/angular/` (Angular 22, TS 6), `web/tailwind/` (Tailwind 4, Vite), `flutter/`.
- [ ] Boşluk analizi: dil × özellik (completion, navigation, diagnostics, run, debug, test) için "çalışıyor / kısmen / yok" tablosu
  - Language server, debug ve test runner katmanları ölçüldü (aşağıda). LSP4IJ'nin IDE içinde denemesi sürüyor: fork build'i `~/.restudio-dev/ide/` altında, LSP4IJ 0.21.0 ile açıldı.

**Boşluk analizi: language server katmanı** (8 Kasım 2026)

Ölçüm aracı: `ReStudio-langs/tools/lsp-probe/` (stdio üzerinden başsız bir LSP istemcisi). Her server bir örnek dosyada başlatıldı; tüm trafik `.lsp-traces/<case>.jsonl`'a kaydedildi. ✅ çalışıyor, ⚠️ kısmen, ❌ yok, — server'ın işi değil.

| Server (proje) | Definition | Hover | Completion | References | Rename | Formatting | Diagnostics | Süre |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| gopls (Go, `go.work`) | ✅ diğer modüle | ✅ | ✅ | ✅ 3 dosya | ✅ 3 dosya | ✅ | ✅ push | 2 sn |
| Roslyn LS, `solution/open` yok (.NET) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ⚠️ sadece tek dosya | 95 sn |
| Roslyn LS, `solution/open` ile (.NET) | ✅ diğer projeye | ✅ | ✅ | ✅ 3 dosya | ✅ | ✅ | ✅ pull | 12 sn |
| csharp-ls `--solution` (.NET) | ✅ | ✅ | ✅ | ✅ 3 dosya | ✅ | ✅ | ✅ | 6 sn |
| vtsls (React, TS 7) | ✅ `@types/react` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 2 sn |
| typescript-language-server (React, TS 7) | ❌ başlamıyor | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | — |
| TS 7 `tsc --lsp --stdio` (React) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 2 sn |
| vtsls (Vue, `.ts` dosyası) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ `.vue` import'unu bulamıyor | 2 sn |
| Vue LS 3 (Vue, `.vue` dosyası) | ❌ zaman aşımı | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | — |
| CSS LS | — | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 6 sn |
| HTML LS | — | ✅ | ✅ | — | ✅ etiket çifti | ✅ | ✅ | 6 sn |
| ESLint LS (Vue, `.ts`) | — | — | — | — | — | — | ✅ pull | 3 sn |
| Phpactor (Laravel) | ✅ `vendor/`'a | ✅ | ✅ | ✅ | ⚠️ `vendor/`'u değiştiriyor | ❌ | ⚠️ yanlış alarm | 26 sn |
| Dart analysis server (Flutter) | ✅ Flutter SDK'ya | ✅ | ✅ | ✅ | ✅ SDK sembolünü reddediyor | ✅ | ✅ | 6 sn |
| Angular LS 22 (template `.html`) | ✅ component'e | ✅ | ✅ | ✅ HTML + TS | ✅ HTML + TS | ❌ | ? dosya temiz, ölçülmedi | 2 sn |
| Tailwind LS 0.16 (`.html`) | — | ✅ class | ✅ 23.632 class | — | — | — | ? dosya temiz, ölçülmedi | 8 sn |

Bulgular ve sonraki fazlara etkisi:

- **Roslyn LS (Faz 2):** `initialized`'dan sonra `solution/open` bildirimi şart. Bildirim olmadan dosya "misc file" kalır ve hiçbir özellik çalışmaz. Bildirimle her şey 12 sn'de hazır. Diagnostic mesajları sistem dilinde (Türkçe) geliyor; dil IDE'nin diline göre ayarlanmalı.
- **csharp-ls:** `--solution` ile özel mesaj gerekmeden aynı sonucu daha hızlı veriyor (6 sn). Roslyn LS'e karşı gerçek bir yedek.
- **TypeScript (Faz 4):** typescript-language-server kendi TS'ini getirmediği için TS 7 projelerinde başlamıyor; plandan çıkarılmalı. vtsls kendi TS 5.9.3'üne geri dönüyor ve çalışıyor. TS 7'nin `tsc --lsp` modu tam çalışıyor; TS 7 projelerinde tercih edilebilir.
- **Vue (Faz 4):** Vue LS 3, TypeScript işlerini istemciye `tsserver/request` bildirimiyle gönderiyor ve cevap bekliyor. Plugin; Vue LS'i ve `@vue/typescript-plugin` yüklü vtsls'yi birlikte başlatıp bu mesajları köprülemeli (`tsserver/response`). Köprü olmadan Vue desteği hiç çalışmıyor.
- **ESLint (Faz 4):** typescript-eslint, TS 7'yi desteklemiyor (peer: `>=4.8.4 <6.1.0`). Vite'ın React şablonu ESLint yerine oxlint getiriyor. oxlint desteği değerlendirilmeli.
- **Formatting:** Server'lar istemcinin gönderdiği girinti ayarını kullanıyor. Ayar `.editorconfig` veya Prettier'dan gelmeli; aksi hâlde formatlama tüm dosyayı değiştiriyor.
- **PHP (Faz 5):** Phpactor rename, `vendor/` altındaki Laravel dosyalarını düzenliyor; plugin `vendor/`'a düşen düzenlemeleri engellemeli. Formatting yok (php-cs-fixer entegrasyonu gerekli). `view()` helper'ı için "Function not found" yanlış alarmı veriyor; Composer `autoload.files` indekslenmiyor olabilir.
- **Angular (Faz 4):** `ngserver`, `@angular/language-service` paketini `--ngProbeLocations` dizinlerinde arar. Bu paket LS'in kendi kurulumunda durur. Parametreye hem LS'in `node_modules`'u hem projenin `node_modules`'u verilmeli. Aksi hâlde server açılışta düşer. Formatting yok; Prettier gerekli.
- **Tailwind (Faz 4):** v4'te config dosyası yok. LS, `@import "tailwindcss"` içeren CSS'i kendisi buluyor. Completion listesi çok büyük (23 bin öğe); istemci listeyi filtreleyip sınırlamalı.

**Boşluk analizi: debug katmanı** (8 Kasım 2026)

Ölçüm aracı: `ReStudio-langs/tools/dap-probe/` (başsız bir DAP istemcisi, stdio ve TCP). Her adapter örnek kodu başlattı, breakpoint'te durdu, değişkeni okudu, evaluate etti, bir step over yaptı ve programı sonuna kadar çalıştırdı.

| Adapter (örnek) | Breakpoint | Değişken | Evaluate | Step over | Bitiş | Not |
| --- | --- | --- | --- | --- | --- | --- |
| Delve `dlv dap` (Go testi) | ✅ | ✅ `name` | ✅ | ✅ 8→9 | ✅ | `mode: test` |
| netcoredbg 3.2.0 (.NET konsol) | ✅ | ✅ `rate` | ✅ | ✅ 11→12 | ✅ | Gerçek `dotnet` host'u gerekli |
| netcoredbg, `async` metot | ✅ | ✅ `rate` | ✅ | ✅ `await` 17→18 | ✅ | Step başka thread'de bitiyor |
| vscode-js-debug 1.140.0 (Node) | ✅ | ✅ `prices` | ✅ | ✅ 3→4 | ✅ | Alt oturum (`startDebugging`) |
| vscode-php-debug 1.40.2 + Xdebug 3.5.3 | ✅ | ✅ `$prices` | ✅ | ✅ 10→11 | ✅ | Xdebug argümanları gerekli |
| Dart `flutter debug-adapter --test` | ✅ | ✅ `tester` | ✅ | ✅ `await` satırında iki durak | ✅ | Önce entry'de duruyor |

Bulgular ve sonraki fazlara etkisi:

- **macOS geliştirici modu (tüm fazlar):** Delve ve netcoredbg `task_for_pid` izni ister. Geliştirici modu kapalıyken macOS bir şifre penceresi açar ve debug sessizce takılır. IDE bu durumu algılamalı ve kullanıcıya `sudo DevToolsSecurity -enable` önermeli.
- **.NET (Faz 2):** Homebrew'un `dotnet`'i bir bash betiği. netcoredbg bu betiği başlatınca `configurationDone` 0x80004005 ile düşüyor. SDK bulucu gerçek host'u (`libexec/dotnet`) ve `DOTNET_ROOT`'u vermeli. Breakpoint'ler önce doğrulanmamış (`verified: false`) gelir ve modül yüklenince doğrulanır. `await` sonrası step başka bir thread'de durur; IDE `stopped` olayındaki thread'i seçmeli.
- **Frontend (Faz 4):** js-debug asıl hedefi bir alt oturumda açar (`startDebugging` isteği, aynı TCP sunucusuna yeni bağlantı). LSP4IJ DAP istemcisinin bu isteği desteklediği doğrulanmalı; yoksa Node debug hiç durmaz.
- **PHP (Faz 5):** php-debug, PHP'yi Xdebug ayarı olmadan başlatıyor. Run configuration `-dxdebug.mode=debug -dxdebug.start_with_request=yes -dxdebug.client_port=<port>` argümanlarını eklemeli.
- **Flutter (Faz 6):** Dart adapter önce `entry` sebebiyle durur ve kendi kendine devam eder. `await` satırında step over aynı satırda ikinci kez durur; bu Dart VM davranışı, hata değil.
- **Test aracı dersi:** Bir `stopped` olayı tek bir bekleyene verilmeli. Aksi hâlde istemci eski olayı tekrar okur ve program çalışırken `stackTrace` ister.

**Boşluk analizi: test runner katmanı** (8 Kasım 2026)

Ölçüm aracı: `ReStudio-langs/tools/test-probe/run-all.sh`. Her runner üç kez çalıştı: tüm testler, isimle tek test ve geçici olarak eklenen bozuk bir test. IDE'nin test ağacı makine okunur çıktıya ihtiyaç duyar.

| Runner (örnek) | Makine okunur çıktı | Tüm testler | Tek test | Hata konumu | Süre |
| --- | --- | --- | --- | --- | --- |
| `go test -json` (`go.work`, 2 modül) | ✅ JSON satırları | ✅ 2 paket | ✅ `-run '^TestX$'` | ⚠️ sadece çıktı metninde (`file.go:6`) | 2 sn |
| `dotnet test` + xUnit (2 test projesi) | ✅ TRX dosyası | ✅ | ✅ `--filter FullyQualifiedName=` | ✅ stack'te `file.cs:line 8` | 4 sn |
| Vitest 5 (Vue) | ✅ JSON dosyası | ✅ | ✅ `-t` (diğerleri `skipped`) | ⚠️ sadece hata mesajında | 2 sn |
| PHPUnit (Laravel) | ✅ TeamCity mesajları | ✅ | ✅ `--filter` | ✅ `locationHint` + `file.php:11` | 1 sn |
| `flutter test --machine` | ✅ JSON olayları | ✅ | ✅ `--plain-name` | ✅ testin `url`/`line`'ı ve stack | 2 sn |

Bulgular ve sonraki fazlara etkisi:

- **PHPUnit (Faz 5):** `--teamcity` çıktısı IntelliJ'nin kendi test ağacının (SM runner) formatı. `php_qn://` konum ipuçları da hazır. En az iş bu runner'da.
- **Diğer runner'lar (Faz 1):** Go, TRX, Vitest ve Flutter çıktısını TeamCity mesajlarına çeviren ortak bir dönüştürücü gerekli. Bu ortak test katmanının ana işi.
- **Go (Faz 3):** `go.work` kökünden her modül ayrı desenle verilmeli (`./lib/... ./api/...`). Tek bir modül dizininden `./...` diğer modülü atlıyor. Hata konumu yapısal değil; `file.go:N:` satırı çıktıdan ayrıştırılmalı.
- **.NET (Faz 2):** TRX canlı değil, test bitince yazılıyor. Canlı test ağacı için bir VSTest logger'ı veya Microsoft.Testing.Platform değerlendirilmeli. CLI çıktısı sistem dilinde (Türkçe) geliyor; `DOTNET_CLI_UI_LANGUAGE=en` verilmeli.
- **Vitest (Faz 4):** JSON'da test konumu yok; `includeTaskLocation` ayarı veya özel bir reporter gerekli. JSON sadece `--outputFile` ile güvenilir bir dosyaya yazılıyor.

**Bitti kriteri:** Fork kendi makinende derlenip installer üretiyor, hangi language server'ın kullanılacağı seçilmiş, boşluk tablosu hazır, sonraki fazların kapsamı bu tabloya göre güncellenmiş.

Roslyn LS'in bilinen tuhaflığı: standart LSP dışında, proje yükleme ve `restore` gerektiğini bildiren özel mesajlar kullanıyor. Generic bir LSP istemcisinde bunlar karşılanmadığı için dış kütüphanelerde hover/diagnostic eksik kalabilir; plugin'in ilk somut işi bu mesajları ele almak olacak.

## Geliştirme fazları

Toplam tahmin akşam/hafta sonu temposunda 7-9 ay. Faz 1-5 plugin reposunda, Faz 6-7 fork reposunda yapılır. Her dil ayrı bir sürüm olarak çıkabilir: ilk public sürüm (v0.1) Faz 1, 2, 6 ve 7 ile Java, Kotlin, Python, Flutter ve .NET'i kapsar; Go, frontend ve PHP sonraki sürümlerde eklenir.

### Faz 1: Ortak dil altyapısı (3 hafta)

- [x] Gradle multi-module plugin reposu: `core` + dil modülleri; LSP4IJ'ye bağımlılık
  - 8 Kasım 2026: `core` ve `go` modülleri derleniyor, 9 test geçiyor. `core` her dil plugin'ine gömülü (embedded) content module olarak giriyor; `plugin.xml` içindeki `<content>` girdisi şart.
- [x] SDK bulucu: `dotnet`, `go`, `php`, `node`, `flutter` yollarını PATH'ten bul, ayar ekranında değiştirilebilsin
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): `core/sdk/ToolLocator.kt` PATH'i ve sabit dizinleri arıyor. Ayar ekranı ve Flutter bulucusu yok.
  - Yazıldı (10 Kasım 2026, ReStudio-langs fe516fb), derlendi, test edilmedi: Settings > Languages & Frameworks > ReStudio Tools her aracın yolunu gösterir ve değiştirir. Diller araçlarını `toolProvider` extension point'iyle kaydeder. Flutter SDK bulucusu eklendi.
- [ ] Language server yöneticisi: sürümü sabitlenmiş server'ları ilk kullanımda kur (`dotnet tool`, `go install`, `npm`, `.phar`), güncellemeyi bilinçli yap
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): kurulum kodu yok. Araçlar sadece PATH'te ya da `~/.restudio-dev/tools` altında aranıyor.
  - Yazıldı (10 Kasım 2026, ReStudio-langs fe516fb), derlendi, test edilmedi: `ToolInstaller` sabit sürümü ilk kullanımda kurar (npm, `go install`, `dotnet tool`, SHA-256 kontrollü indirme); önce kullanıcıya sorar. Henüz hiçbir dil modülü çağırmıyor.
- [x] Ortak debug katmanı: bir run configuration'dan DAP adapter'ı başlatma (LSP4IJ DAP üzerine)
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): `core/dap/DapLaunch.kt` ve `FileDebugAdapterFactory.kt`; dört dil de kullanıyor.
- [ ] Ortak test adaptörü: dış test çıktısını (TRX, `go test -json`, JUnit XML) IntelliJ test penceresine bağlama
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): TRX, Vitest ve PHPUnit translator'ları yazılmış. Eksik: JUnit XML ve Flutter.
  - Ortak katman yazıldı (8 Kasım 2026): `core`'daki `TestEventTranslator` bir runner satırını id tabanlı TeamCity mesajlarına çeviriyor. Her dil sadece kendi translator'ını veriyor.
  - Go bitti: `go test -json` translator'ı alt testleri, skip, panic ve derleme hatasını işliyor. Go Test run configuration'ı LSP4IJ DAP configuration'ından türüyor. Run test penceresini açıyor, Debug Delve'i başlatıyor.
  - Açık: TRX, Vitest, PHPUnit ve Flutter translator'ları. Test penceresinden koda gitme (test locator).
  - Yazıldı (10 Kasım 2026, ReStudio-langs fe516fb), derlendi, test edilmedi: `JUnitXmlTranslator` eklendi. Flutter translator'ı yok.
- [x] Ortak gutter ikonları (▶ Run / Debug): LSP4IJ, LSP dillerinde PSI kurmadığı için bu ikonları koymuyor. `textDocument/documentSymbol` cevabından giriş noktalarını ve testleri bul. `RunLineMarkerContributor` ile ikonu koy, `RunConfigurationProducer` ile run configuration'ı üret. Her dil sadece kuralı verir: Go'da `package main` + `func main` ve `func TestX(t *testing.T)`, C#'ta `Main` ve `[Fact]`, PHP'de `test_` metotları, Dart'ta `main` ve `test()`.
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): dört dilin dedektörü `core/entrypoints/` üzerinde çalışıyor.
  - Fork build'inde denendi (8 Kasım 2026): LSP4IJ ile Go dosyasında ▶ yok. Kullanıcı DAP run configuration'ını elle ekleyip launch JSON'unu yazmak zorunda. Delve şablonunun varsayılanı `"program": "${file}"`; kütüphane dosyasında bu ayar çalışmıyor.
  - Go prototipi yazıldı (8 Kasım 2026). Giriş noktalarını şimdilik regex buluyor, `documentSymbol` değil. Sebep: ikon gopls başlamadan görünmeli.
  - TextMate dosyası tek bir PSI leaf: TextMate parser'ı boş bir lexer kullanıyor. `RunLineMarkerContributor` bu yüzden sadece ilk satıra ikon koyabilir. Çözüm: `core`'daki `EntryPointLineMarkerProvider` her giriş noktası için ayrı bir marker ekliyor. Run aksiyonları giriş noktasını `EntryPointLocation` içinde taşıyor.
  - Platform testlerinde tam highlighting çalıştırılmamalı. LSP4IJ adımları test ortamında gopls cevabını sonsuza kadar bekliyor. Testler provider'ı doğrudan çağırıyor.
  - Fork IDE'sinde elle denendi (8 Kasım 2026): Go'da ▶ ikonu, Run ile test penceresi ve Debug ile breakpoint çalışıyor.
  - Debug için üç LSP4IJ kuralı öğrenildi:
    - Delve, `go test -c` komutunu kendi çalışma klasöründe çalıştırıyor. Her launch `dlvCwd` vermeli.
    - Editör bir breakpoint'e ancak bir debug adapter dosyayı kabul ederse izin veriyor. Delve, `debugAdapterServer` olarak kayıtlı.
    - Oturum bir breakpoint'i ancak run configuration'ın `serverId` değeri doluysa gönderiyor. Boş `serverId` breakpoint'i çizik gösteriyor.
  - Açık: diğer dillerin kuralları.
- [x] Status bar'da server durumu, LSP/DAP trace log ekranı
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): LSP trace'i debug log açıkken dosyaya yazılıyor. Status bar widget'ı, trace ekranı ve DAP trace'i yok.
  - Yazıldı (10 Kasım 2026, ReStudio-langs fe516fb), derlendi, test edilmedi: Status bar widget'ı LSP4IJ server'larını gösterir; Help > Diagnostic Tools > Open LSP Trace. DAP trace'i yok.
- [x] New Project sihirbazı: her dil kendi proje şablonunu versin (istek: Onur, 10 Kasım 2026). Python, Dart ve Flutter kendi kayıtlarını yapıyor. ReStudio-langs'teki dört modülün hiçbiri `moduleBuilder` ya da `newProjectWizard.*` kaydetmiyor; bu yüzden Go, .NET, PHP ve web dilleri sihirbazda yok.
  - Yazıldı (10 Kasım 2026, ReStudio-langs a29be74, 4a23591, 87b9118, 08eb06c), derlendi, test edilmedi: Go (`go mod init`), .NET (console, classlib, xunit), web (Vite React/Vue/vanilla, Node.js) ve PHP (Composer, Laravel) generator'ları `newProjectWizard.generator` ile kayıtlı.

**Bitti kriteri:** Yeni bir dil eklemek = server tanımı + kurulum tarifi + run/debug/test adaptörü; ortak kod tekrar yazılmıyor.

### Faz 2: C# ve .NET (8-10 hafta)

- [x] Roslyn LS entegrasyonu; özel mesajlar (solution/proje yükleme, `restore` isteği)
  - Yazıldı (10 Kasım 2026, ReStudio-langs 4a23591), derlendi, test edilmedi: Probe kaydı: Roslyn LS restore isteği göndermiyor, restore'u kendisi yapıyor. İstemci otomatik restore'u açar ve yükleme hatalarını bildirir; Tools > .NET'te elle restore var.
- [ ] `.sln`/`.slnx`/`.csproj` okuma (`dotnet msbuild -getProperty/-getItem`), proje → modül eşleme, solution görünümü
  - Yazıldı (10 Kasım 2026, ReStudio-langs 4a23591), derlendi, test edilmedi: `dotnet msbuild -getProperty/-getItem` cache'li; her proje bir modül, `ProjectReference` modül bağımlılığı. Solution görünümü yok.
- [x] `dotnet build`/`run` run configuration'ı, `launchSettings.json`, tıklanabilir build hataları
  - Yazıldı (10 Kasım 2026, ReStudio-langs 4a23591), derlendi, test edilmedi: `launchSettings.json` profilleri ve tıklanabilir build hataları eklendi.
- [x] netcoredbg ile debug ve attach
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): `dotnet/debug/` ve `DotnetAttach*` dosyaları.
  - İlk dilim (8 Kasım 2026): `dotnet` plugin'i. Roslyn LS `solution/open` ile yükleniyor. `Main` ve top-level statements için ▶ var. Run `dotnet run`, Debug ise `dotnet build` ve netcoredbg ile çalışıyor. Debug fork IDE'sinde elle denendi ve breakpoint'te duruyor.
  - Test debug'ı ve attach yazıldı (8 Kasım 2026). Test debug'ı `dotnet test`'i `VSTEST_HOST_DEBUG=1` ile başlatıyor, sonra netcoredbg bekleyen testhost'a bağlanıyor. "Attach to Process" listesi .NET süreçleri için netcoredbg sunuyor. Test debug'ı fork IDE'sinde elle denendi ve breakpoint'te duruyor.
  - LSP4IJ'de "adapter'ı başlat, sonra attach et" modu yok: ATTACH modu süreç başlatmıyor. Descriptor LSP4IJ'ye LAUNCH, DAP istemcisine ATTACH veriyor.
  - Platform, debug state'ini before-run adımlarından önce istiyor (`AsyncProgramRunner.execute`) ve her adıma environment'ın bir kopyasını veriyor. Before-run adımında oluşan bir değer, `executionId` ile ve oturum başlarken alınmalı.
  - Platform `*.cs` dosyalarını kendi yerleşik "C#" custom file type'ına bağlıyor (`defaultFileTypes.xml`, dil `TEXT`). C# için gutter provider'ı `TEXT` dili için de kayıtlı. Faz 7'de fork bu eşlemeyi kaldırıp C#'ı TextMate'e verebilir.
  - Roslyn code lens komutları (`dotnet.test.run`, `roslyn.client.peekReferences`) IDE aksiyonu olarak karşılanıyor. Başlıkları sistem dilinde geliyor.
- [x] `dotnet test` + TRX ile xUnit/NUnit/MSTest test penceresi, test gutter ikonları
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): `DotnetTestRunConfiguration`, `DotnetConsoleTranslator`, `TrxReport`.
  - İlk dilim: xUnit, NUnit ve MSTest metotlarında ▶ var. Teste çift tıklayınca koda gidiliyor.
  - Test ağacı konsol logger'ının sonuç satırlarından doluyor (`console;verbosity=normal`). VSTest bu satırları çalışma boyunca toplu yazıyor. TRX yedek: konsolda sonuç satırı yoksa ağacı o dolduruyor.
- [x] NuGet tool window (arama, ekleme, güncelleme)
  - Yazıldı (10 Kasım 2026, ReStudio-langs 4a23591), derlendi, test edilmedi: Arama, ekleme, güncelleme, kaldırma. Güncellemede sürüm seçimi yok.

**Bitti kriteri:** ASP.NET Core solution'ı yazılıyor, çalışıyor, test ediliyor, debug ediliyor; async metotlarda step doğru ilerliyor.

### Faz 3: Go (3 hafta)

- [x] gopls entegrasyonu; `go.mod`/`go.work` → modüller
  - Yazıldı (10 Kasım 2026, ReStudio-langs a29be74), derlendi, test edilmedi: `go.work` `use` dizinleri content root olur; `modules.txt`'li `vendor/` exclude edilir.
- [x] `go build`/`go run` run configuration'ı
  - Yazıldı (10 Kasım 2026, ReStudio-langs a29be74), derlendi, test edilmedi: Yeni "Go Application" tipi `go build` ile çalıştırır; `file.go:line` link olur. Debug Delve ile kalır.
- [x] `go test -json` ile test penceresi ve gutter ikonları
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): `go/test/` dosyaları ve `GoLineMarkerProvider`.
- [x] Delve (`dlv dap`) ile debug
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): `DelveDescriptorFactory.kt`, `GoLaunches.kt`.
- [x] `gofmt`/`goimports` formatlama (gopls üzerinden)
  - Yazıldı (10 Kasım 2026, ReStudio-langs a29be74), derlendi, test edilmedi: Kaydederken gopls `source.organizeImports`; Optimize Imports da çalışır.

**Bitti kriteri:** `go.work` kullanan çok paketli bir Go servisi çalışıyor, test ve debug ediliyor.

### Faz 4: Frontend (4-5 hafta)

- [x] TS/JS için vtsls; TS 7 projelerinde projenin kendi `tsc --lsp` server'ı. HTML, CSS, JSON için vscode-langservers-extracted
  - Yazıldı (10 Kasım 2026, ReStudio-langs 87b9118), derlendi, test edilmedi: HTML, CSS, JSON server'ları; TypeScript 7 projesinde `tsc --lsp` (Vue projesi vtsls'de kalır).
- [x] Vue LS: `tsserver/request` mesajlarını `@vue/typescript-plugin` yüklü vtsls'ye köprüle (Faz 0 bulgusu). Angular LS, Tailwind CSS LS
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): `web/lsp/VueBridge.kt`, `AngularServerFactory`, `TailwindServerFactory`. JSX'te Tailwind açık.
- [x] ESLint LS ve Prettier ile formatlama
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): `web/lsp/EslintSupport.kt`, `web/ide/PrettierFormattingService.kt`.
- [x] `package.json` script'leri için run configuration (npm, pnpm, yarn, bun); `node_modules/.bin` çözümü
  - Yazıldı (10 Kasım 2026, ReStudio-langs 87b9118), derlendi, test edilmedi: `node_modules/.bin` PATH'te ilk sırada.
- [x] vscode-js-debug ile Node ve Chrome debug
  - Yazıldı (10 Kasım 2026, ReStudio-langs 87b9118), derlendi, test edilmedi: "Chrome (js-debug)" run configuration'ı. Gutter'dan üretilmiyor.
- [x] Vitest/Jest sonuçlarını test penceresine bağlama
  - Yazıldı (10 Kasım 2026, ReStudio-langs 87b9118), derlendi, test edilmedi: Jest run configuration'ı, gutter ve TeamCity reporter'ı.

- İlk dilim (9 Kasım 2026): `web` plugin'i. vtsls (TS/JS), Angular LS ve Tailwind LS (sadece o projelerde, document matcher ile), `package.json` script'leri için ▶ (Run paket yöneticisiyle, Debug js-debug ile), Vitest için ▶ (Run canlı test ağacı, Debug js-debug ile). Fork IDE'sinde elle denendi: vtsls hover ve completion, `npm run dev`, Vitest Run ve Debug çalışıyor.
  - js-debug her Node sürecini `startDebugging` ile alt oturum olarak açıyor; LSP4IJ bunu destekliyor (probe: js-debug-npm, js-debug-vitest).
  - Vitest 5, `-t` filtresini `" > "` ile birleşmiş adlarla eşliyor; JSON reporter ise adları boşlukla yazıyor.
  - Vitest test ağacı, eklentiyle gelen bir reporter'dan (`restudio-reporter.mjs`) id tabanlı TeamCity mesajlarıyla doluyor.
  - LSP4IJ, plugin'lerin sunucularına bakmadan şablon önerisi gösteriyor ("*.ts files are supported by language servers"). `core`, bu öneriyi ReStudio dillerinin dosyalarında gizleyen bir sağlayıcıyla değiştiriyor.
  - Vue (9 Kasım 2026): `.vue` dosyası vtsls'e (`@vue/typescript-plugin` ile) ve Vue LS'e gidiyor. Vue LS'in `tsserver/request` mesajları vtsls'e köprüleniyor. Renk için plugin, vuejs/language-tools'un TextMate grammar'ını (MIT) taşıyor. Fork IDE'sinde elle denendi: hover, definition ve template completion çalışıyor.
  - Köprü hatası: lsp4j tek parametreli bir bildirimi bir dizi daha sarıyor ve kendi trace'i bunu göstermiyor. Ham bayt kaydı (`stdio-tee.mjs`, debug seviyesinde) bunu buldu.
  - Açık: ESLint/Prettier, Chrome debug, Jest, HTML/CSS/JSON server'ları, JSX'te Tailwind.

**Bitti kriteri:** React (Vite) ve Vue uygulamasında yazma, lint, format, dev server, tarayıcıda breakpoint ve testler çalışıyor.

### Faz 5: PHP (3 hafta)

- [x] Phpactor entegrasyonu; Intelephense kullanıcı isterse opsiyonel
  - Yazıldı (10 Kasım 2026, ReStudio-langs 08eb06c), derlendi, test edilmedi: Ayarla Phpactor ya da Intelephense; server ID `restudio.php`. `vendor/` içine rename düzenlemeleri atılır.
- [x] `composer.json` → autoload ve kaynak kökleri
  - Yazıldı (10 Kasım 2026, ReStudio-langs 08eb06c), derlendi, test edilmedi: psr-4, psr-0 ve classmap kaynak ve test kökü olur; `vendor/` exclude edilir ve library olarak eklenir.
- [x] PHP script, built-in server ve `artisan` run configuration'ları
  - Yazıldı (10 Kasım 2026, ReStudio-langs 08eb06c), derlendi, test edilmedi: `php -S` (Xdebug ile Debug) ve `artisan` run configuration'ları. `artisan serve` Debug'ı web isteklerinde durmaz.
- [x] PHPUnit/Pest: JUnit XML çıktısıyla test penceresi
  - Yazıldı (10 Kasım 2026, ReStudio-langs 08eb06c), derlendi, test edilmedi: Pest run configuration'ı, gutter ve locator. PHPUnit JUnit XML yerine `--teamcity` kullanıyor.
- [x] Xdebug: vscode-php-debug adaptörüyle debug
  - Kodla karşılaştırıldı (10 Kasım 2026, ReStudio-langs edabdd0): `PhpDebugDescriptorFactory.kt`; `PhpLaunches.kt` `-dxdebug.*` argümanlarını veriyor.

- İlk dilim (8 Kasım 2026): `php` plugin'i. Phpactor, PHPUnit testleri ve script'ler için ▶, script Run/Debug, PHPUnit Run (`--teamcity` ile test ağacı, `php_qn` locator) ve Debug (Xdebug). Beşi de fork IDE'sinde elle denendi.
  - `--teamcity` ad tabanlı mesaj yazıyor; test konsolu ad tabanlı modda çalışıyor.
  - Açık: `artisan` ve built-in server configuration'ları, Pest, `vendor/`'a düşen rename düzenlemelerini engelleme, php-cs-fixer ile formatting.

**Bitti kriteri:** Laravel projesinde completion, navigation, testler ve Xdebug breakpoint'leri çalışıyor.

### Faz 6: Flutter, Dart ve Python paketleme (1 hafta)

- [x] Google'ın Flutter plugin'i ve Dart plugin'ini fork build'ine bundled ekle
- [x] intellij-community içindeki açık kaynak Python plugin'ini bundled ekle
  - 9 Kasım 2026: `fork/bundled-plugins.tsv` plugin'leri, Marketplace sürümlerini ve SHA-256 değerlerini tutuyor. `fork/fetch-bundled-plugins.sh` zip'leri indirip `out/restudio-plugins/` altına açıyor. Build bu dizini `build/src/.../ReStudioPlugins.kt` ile okuyor.
  - Python, kaynaktan derlenmiyor. Marketplace'teki `PythonCore` 263.7307.200 kullanılıyor; bu sürüm fork'un build'iyle aynı.
  - Dart, Flutter ve LSP4IJ bundled olarak yüklenemiyor. Platform, uyumluluk bağımlılıklarını sadece bundled olmayan eski stil plugin'lere ekliyor (`ProductPluginInitContext.kt`). Bundled Dart, `XLineBreakpointTypeBase` sınıfını bulamadı.
  - Bu üç plugin `Contents/restudio-plugins/` altında duruyor. Launcher onları `-Dplugin.path` ile yüklüyor, Marketplace kurulumunda olduğu gibi. `ProductLaunchRenderer.kt`'deki `IDE_HOME_PLACEHOLDER`, yolu işletim sisteminin makrosuna çeviriyor. Python bundled kalıyor.
  - Doğrulandı (macOS aarch64 `.dmg`): dört plugin de yükleniyor ve log'da `SEVERE` yok.
  - Linux `tar.gz` ve Windows dağıtımı macOS'ta derlendi. Yerleşim doğru. `idea.sh` ve Linux `product-info.json` `$IDE_HOME`, `idea.bat` `%IDE_HOME%` kullanıyor. Windows `.exe` ve zip adımları macOS'ta atlanıyor.
  - Doğrulandı (CI run 42612172475, 9 Kasım 2026): `fork/smoke-test.sh`, `ubuntu-latest` (Xvfb) ve `windows-latest` (sessiz kurulum) üzerinde IDE'yi bir Python ve Dart projesiyle açtı. Dört plugin de yüklendi; plugin hatası yok. Bu run marka değişikliklerinden önceki kodu kullandı.
  - Doğrulandı (CI run 42620657587, 9 Kasım 2026): markalı kod (commit 72b0ff28fb84) ile aynı smoke test Linux ve Windows üzerinde geçti. Dosya adları `ReStudio-263.7307.1003*`; build numarası `-Dbuild.number` ile geldi. Help menüsü linkleri (8b6b012be9b9) bu run içinde yok.
  - `build/tests/.../ReStudioPluginsTest.kt`: dizin kuralları ve üç işletim sisteminin makrosu için 6 test.
  - Kısıt: `plugin.path` ile gelen bir plugin, aynı ID'li daha yeni bir sürümü de geçersiz kılıyor (`pluginInitContextSelectCandidateSubset.kt`). Kullanıcı bu üç plugin'i Marketplace'ten güncelleyemez; güncelleme yeni bir ReStudio sürümüyle gelir. Faz 7'de Plugins ekranında güncelleme önerisi gizlenmeli.
- [ ] Plugin sürümlerinin fork'un IDE sürümüyle uyumunu doğrula; uyumsuzlukta plugin'i kaynaktan derle
  - Otomatik deneme (9 Kasım 2026, `performanceTesting` plugin'inin `testscript.filename` script'i): Python script'i Run ve Debug ile 0 koduyla bitiyor. Flutter testi (`testdata/flutter`) Run ile 0 koduyla bitiyor.
  - Flutter test Debug'ı bu araçla doğrulanamadı. Dart debugger VM service'e bağlanıyor, ama test `--start-paused` durumunda kalıyor. Faz 0'daki Marketplace kurulumu da aynı davranıyor; sebep aracın `RunConfigurationCommand`'ındaki NPE. Flutter runner `processStarting` olayını göndermiyor.
  - Açık (elle): Flutter Debug'da breakpoint ve hot reload. Python'da kayıtlı bir SDK gerekli; temiz bir kurulumda SDK yoksa run configuration ayar penceresini açıyor.

**Bitti kriteri:** Flutter projesi ek kurulum olmadan açılıyor, hot reload ve debug çalışıyor; Python projesi çalışıyor.

### Faz 7: Fork, marka ve public dağıtım (3-4 hafta)

- [x] Ürün adı, ikonlar, splash, About ekranı ve ürün tanım dosyaları; arayüzde ve installer'da "IntelliJ" ve JetBrains logoları kalmasın
  - 9 Kasım 2026: Ad "ReStudio", vendor "ReStudio". Değerler `build/src/.../ReStudioBranding.kt` ve `IdeaApplicationInfo.xml`'de. Geçici ikon ve splash'i `fork/branding/make-images.swift` üretiyor; kalıcı logo gelince script tekrar çalıştırılır.
  - Doğrulandı (macOS aarch64 `.dmg`, build 263.7307.1000): `ReStudio.app`, bundle ID `io.github.onuragtas.restudio`, URL scheme `restudio`, artifact adı `ReStudio-<build>`. Smoke test geçti.
  - Vendor JetBrains olmayınca build'in `traverseUI` adımı düştü (boş consents listesi). `AppUIUtil.kt`'de tek satırlık düzeltme var.
  - Launcher adı `restudio` (9 Kasım 2026): `bin/restudio.sh`, `Contents/MacOS/restudio`; ortam değişkenleri `RESTUDIO_PROPERTIES`, `RESTUDIO_VM_OPTIONS`, `RESTUDIO_JDK`. `ReStudioBranding.LAUNCHER_NAME` ve `IdeaApplicationInfo.xml`'deki `script` aynı; `ReStudioBrandingTest` kontrol eder. `bin/idea.properties` dosya adı ve `idea.*` özellikleri platformdan gelir, değişmez.
  - Değiştirilmedi: macOS `Info.plist`'teki telif satırı "Copyright JetBrains s.r.o., (c) 2000-<yıl>". Kodun telif hakkı JetBrains'te, bu yüzden satır doğru. Linux WM class `jetbrains-restudio`: `jetbrains-` öneki build script'te ve IDE'de (`AppUIUtil.kt`) sabit. Kullanıcı bunu görmez; değiştirmek iki JetBrains dosyası ister. Help menüsündeki linkler `ReStudioExternalResourceUrls`'ten geliyor: hata raporu ve indirme GitHub'a gidiyor; JetBrains destek, feedback ve What's New kapalı; yardım konuları IntelliJ IDEA yardım sitesinde kalıyor. Derlendi, kurulu IDE'de denenmedi.
  - Yapıldı (9 Kasım 2026): `ReStudioErrorReportSubmitter` (`idea/customization/base`) `ITNReporter`'ın yerine geçer. Hata penceresindeki düğme "Open a GitHub Issue" oldu. Düğme tarayıcıda yeni bir issue açar: başlık, kullanıcı notu, IDE sürümü, işletim sistemi, Java, plugin ve stack trace. URL 8000 karakteri geçmez; stack trace'in sonu atılır. Heap dump raporu da aynı yolu kullanır. JetBrains'e hiçbir rapor gitmez. Otomatik rapor zaten kapalı, çünkü vendor JetBrains değil.
    - Kontrol edildi: modüller derlendi; URL üreticisi uzun bir stack trace ve özel karakterlerle denendi. `ReStudioBrandingTest` kaydı kontrol eder.
    - CI run 42658743450 (build 1004): Linux ve Windows smoke testleri bu kodla geçti; IDE ve dört plugin hatasız yüklendi.
    - Kontrol edilmedi: kurulu bir IDE'de hata penceresi ve tarayıcı açılışı.
- [x] Kendi config, cache ve log dizinleri; resmi IntelliJ ile yan yana kurulup birbirinin ayarlarını bozmasın
  - Dizinler `<vendor>/<selector>`: `ReStudio/ReStudio2026.3` (macOS'ta `~/Library/Application Support/ReStudio/ReStudio2026.3`). `product-info.json`'da `dataDirectoryName` `ReStudio2026.3`. Resmi IntelliJ ile yan yana kurulum elle denenmedi.
- [x] Bundled plugin listesi: dil plugin'leri, LSP4IJ, Flutter, Dart, Python; ihtiyaç olmayanlar (ör. Android) çıkarılsın
  - 9 Kasım 2026: Android plugin'i IC build'inde zaten yok. 68 bundled plugin kalıyor. Hepsi yükleniyor, log'da hata yok ve smoke test geçiyor. Bu yüzden hiçbiri çıkarılmadı (karar: Onur, 9 Kasım 2026).
  - "IntelliJ IDEA Customization" plugin'i `implementation-detail="true"`. Plugins ekranı onu göstermiyor; adını değiştirmek gerekmiyor.
- [x] Güncelleme kontrolü GitHub Releases'a baksın (veya ilk sürümde kapatılsın)
  - Launcher `-Didea.updates.url` ile `update-metadata` release'indeki `updates.xml`'i okutuyor. Release workflow'u bu dosyayı `fork/make-updates-xml.sh` ile üretip yüklüyor. `latest` linki kullanılmıyor, çünkü GitHub prerelease'i en son release saymıyor.
  - Release build'leri gerçek bir build numarası alıyor: `263.7307.<1000 + run number>`. SNAPSHOT build'de IDE hiçbir güncellemeyi göstermez.
  - Doğrulandı (yerel `file://` dosyası): daha yüksek build numarasında `onUpdateFound: true`, daha düşükte `false`. Bildirim penceresi elle görülmedi.
  - Plugins ekranında Dart, Flutter ve LSP4IJ için güncelleme önerisi yok: `PluginUtils.isUpdateable`, `plugin.path` ile gelen plugin için `false` döner (test: `ReStudioPluginPathUpdateTest`). Bu değişiklik henüz bir installer'da denenmedi.
- [x] Desteklenen dillerde plugin önerisi bandını kapat. Fork build'inde `.go` dosyası açılınca IDE "*.go files are supported by IntelliJ IDEA" diyor ve kapalı kaynak Go plugin'ini öneriyor.
  - Launcher `-Dpromo.ignore.suggested.ide=true` veriyor; `PluginsAdvertiser.kt` bu özelliği okuyor. IDE önerisi kapanıyor, Marketplace plugin önerileri kalıyor. `.go` dosyasıyla elle denenmedi.
- [x] GitHub Actions ile macOS/Linux installer build'i; standart runner yetmezse self-hosted runner
  - Bitti (10 Kasım 2026): `fork-release.yml` Linux `.tar.gz` ve macOS aarch64 `.dmg`'yi 26 dakikada üretiyor (run 42699952867). Self-hosted runner gerekmedi.
  - Upstream'in `.github/workflows/ide_build_and_upload.yml`'ı bunu zaten yapıyor: `ubuntu-latest` üzerinde Docker ile build, `-aarch64.sit` üretimi, sonra `macos-latest` üzerinde `-aarch64.dmg`. Çıktılar imzasız.
  - Fork'ta değişmeden çalışmaz: `.github/actions/build_ide`, `JetBrains/android`'i `github.ref` ile checkout ediyor; bizim branch ve tag'lerimiz orada yok. Ref, `fork/UPSTREAM_BASE`'deki upstream tag'ine sabitlenmeli.
  - Upstream dosyalarına dokunmamak için ayrı bir `fork-release.yml` yaz: build adımlarını ondan çağır, `.dmg`'yi `gh release create` ile GitHub Releases'a yükle.
  - Yazıldı (8 Kasım 2026): `.github/workflows/fork-release.yml`. `restudio/v*` etiketiyle ya da elle başlıyor. Linux `.tar.gz`, Windows `.exe` ve macOS aarch64 `.dmg` dosyalarını bir prerelease'e yüklüyor.
  - İlk çalıştırma başarılı (8-9 Kasım 2026, yaklaşık 2,5 saat): `restudio/v0.1.0-alpha` prerelease'i yayında. İçerik hâlâ "IntelliJ IDEA OSS" ve dil plugin'leri yok.
  - Upstream `build_ide` action'ı fork'ta çalışmıyor: JetBrains/android reposunu `github.ref` ile çekiyor, ama o repoda `main` dalı yok. Fork'ta her gün çalışan IntelliJ IDEA ve PyCharm build'leri bu yüzden 4 dakikada düşüyor. `fork-release.yml`, android reposunu `fork/UPSTREAM_BASE` etiketiyle çekiyor.
  - Upstream workflow sadece Apple Silicon (aarch64) `.dmg`'yi yüklüyor. Yerel build Intel `.dmg`'yi de üretiyor; Intel Mac gerekiyorsa workflow'a onun yükleme adımı eklenmeli.
  - Upstream'in günlük `schedule` tetikleyicisi fork'ta gereksiz dakika harcar; fork'ta kapalı kalsın.
  - 9 Kasım 2026: `fork-release.yml` build'den önce `fork/fetch-bundled-plugins.sh`'ı çalıştırıyor. Build'den sonra her Linux `tar.gz`'de Dart, Flutter, LSP4IJ ve Python dizinlerini arıyor; biri yoksa release durur. Kontrol yerel arşivlerde geçti. CI'da henüz çalışmadı.
- [ ] Mac'te yerel release (istek: Onur, 10 Kasım 2026). Skill: `.claude/skills/restudio-release/SKILL.md`. Mac, Linux `.tar.gz`, macOS `.sit` ve `.dmg`'yi tek build'de üretir ve `fork/publish-release.sh` ile yayınlar.
  - CI ve yerel release aynı script'leri kullanır: `fork/next-build-number.sh`, `fork/check-bundled-plugins.sh`, `fork/publish-release.sh`.
  - Build numarası yayındaki `updates.xml`'den yüksek olur. CI'da en az 1000 + run numarası olur.
  - Yerel release tag'i oluşturunca `fork-release.yml` başlar. Yeni `guard` job'ı release'i bulur ve build'i atlar.
  - Mac, Linux smoke testini çalıştıramaz. Yerel release'te sadece macOS smoke testi çalışır.
- [ ] IDE içinden otomatik güncelleme ("Update and Restart"), yama dosyalarıyla. Sıra (karar: Onur, 9 Kasım 2026): `v0.1.0` yamasız çıkar; yama ikinci release'te eklenir ve denenir.
  - Yazıldı (10 Kasım 2026), denenmedi: `fork/make-patches.sh` önceki release'in `.tar.gz` ve `.sit` dosyalarını indirir ve `updater` (`./gradlew fatJar`) ile `--strict` yama üretir. macOS'ta kök `Contents`. `fork/publish-release.sh` yamaları `update-metadata`'ya yükler; `make-updates-xml.sh` `<patch from size exclusions>` satırını ekler. `basePatchDownloadUrl` artık `releases/download/update-metadata/`.
  - `ReStudioExternalResourceUrls.basePatchDownloadUrl` → `releases/download/update-metadata/`. IDE yamayı `IC-<eski>-<yeni>-patch[-aarch64]-<os>.jar` adıyla bu adreste arar (`computePatchUrl`).
  - Release job'ı önceki release'in Linux ve macOS dağıtımlarını indirir, açar ve `updater` (`updater/src/com/intellij/updater/Runner.java`, `create` komutu) ile her OS ve mimari için yama üretir. Yamalar `update-metadata` release'ine yüklenir.
  - `fork/make-updates-xml.sh` `<patch from="<eski build>" size="..."/>` satırlarını ekler.
  - Yama sadece önceki sürümden geçerli; daha eski sürümler "Download" ile tam installer alır.
  - Denenecek: macOS imzası eklendikten sonra yamanın imzalı dosyaları taşıması.
- [x] CI build süresini kısalt. Ölçüm (run 42620657587, `distributions` job'ı 57 dakika):
  - Bitti (10 Kasım 2026): Windows kalktı ve disk cache eklendi. Toplam run 57 dk'dan 26 dk'ya indi.
  - Bazel analiz 3 dk, Bazel derleme 24 dk (6535 action, cache yok).
  - Paketleme 22 dk. Bunun 16-18 dakikası Windows NSIS `.exe` sıkıştırması (`SetCompressor lzma`); x64 ve aarch64 aynı anda çalışır. Linux 3 dk, macOS 2 dk, traverseUI 1 dk.
  - Yapıldı (9 Kasım 2026): Seçenek 1'in disk cache kısmı. `fork-release.yml` container'ın `~/.cache` dizinini `$RUNNER_TEMP/ide-cache`'e bağlar ve `JetBrains/monorepo-bazel-cache`'i `actions/cache` ile saklar. Repository cache (indirmeler, yerelde 7,4 GB) sınıra sığmadığı için dışarıda. İlk run (42690626069) cache'i doldurdu: CI'daki disk cache 3,1 GB, kayıt 16 sn sürdü. İkinci run (42699952867) cache'i 19 sn'de geri yükledi. Build adımı 40,5 dk'dan 11,3 dk'ya, `distributions` job'ı 46 dk'dan yaklaşık 20 dk'ya indi.
  - Seçenek 1: Bazel disk ve repository cache'ini `actions/cache` ile run'lar arasında sakla. Fork commit'lerinde derleme büyük ölçüde atlanır; upstream merge sonrası tam derleme olur. Önce cache boyutu ölçülmeli (GitHub cache sınırı repo başına 10 GB; yereldeki repository cache 7,4 GB).
  - Seçenek 2: Tag'siz test run'larında NSIS `zlib` kullansın, release'te `lzma` kalsın (`idea.nsi`'de `$%RESTUDIO_NSIS_COMPRESSOR%` ile; JetBrains dosyasında 3-4 satır). Tahmini kazanç 10 dk; test installer'ı daha büyük olur.
  - Seçenek 3: Daha büyük GitHub runner (ücretli, dakika başına).
  - `intellij.build.target.arch` tek bir değer alır; sadece Windows aarch64'ü atlamak mümkün değil.
  - Yapıldı (9 Kasım 2026, karar: Onur): ReStudio Windows dağıtımı yapmaz. Build `-Dintellij.build.skip.build.steps=windows_artifacts` ile Windows `.exe` ve zip adımını atlar; Windows smoke testi kalktı. Beklenen kazanç 16-18 dk. Seçenek 2 (NSIS `zlib`) artık gereksiz.
  - Ölçüldü (run 42690626069, cache yok): `distributions` job'ı 46 dk (önce 57 dk); Build adımı 40,5 dk. Log `stepsToSkip=windows_artifacts` gösterir. Linux smoke testi `bin/restudio.sh` ve `RESTUDIO_PROPERTIES` ile geçti.
- [ ] (En sona bırakıldı, 9 Kasım 2026) macOS imzalama ve notarization (yapılmazsa README'de Gatekeeper notu: `xattr -dr com.apple.quarantine`). İmza için Apple Developer ID sertifikası ve notarization bilgileri GitHub secret olarak eklenmeli.
  - Yazıldı (10 Kasım 2026): `build/src/.../ReStudioMacSigning.kt` yerel `codesign` ile imzalıyor (`SignNativeFileMode.PREPARE`). `RESTUDIO_MAC_SIGN_IDENTITY` yoksa build imzasız kalır. Notarization build'in kendi notary istemcisiyle yapılıyor (`APPLE_ISSUER_ID`, `APPLE_KEY_ID`, `APPLE_PRIVATE_KEY`). Ayarlar `~/.restudio-dev/signing.env`'de, repoda değil. JetBrains dosyalarında iki satır: `IdeaCommunityProperties.kt`, `OpenSourceCommunityInstallersBuildTarget.kt`.
  - Denendi: macOS aarch64 build'inde imza adımı hatasız geçti; `.sit` Apple'a gönderildi. Notarization sonucu, staple ve `spctl` kontrolü henüz yok.
  - Karar: imza sadece yerel release'te (Mac). CI imzasız kalır; sertifika GitHub'a gitmez.
- [x] `LICENSE` (Apache 2.0), `NOTICE` ve üçüncü taraf lisans listesi
  - Kökteki `LICENSE.txt` Apache 2.0 değil: JetBrains, Haziran 2026'da onu kendi build'lerinin kullanım koşullarıyla değiştirdi. IDE'ye `fork/legal/LICENSE.txt` (Apache 2.0) ve `fork/legal/NOTICE.txt` giriyor. Üçüncü taraf kütüphane listesi build'in `license/third-party-libraries.json` dosyası.
  - Açık: Flutter plugin'i JxBrowser (TeamDev, ticari lisans) içeriyor. Plugin değişmeden dağıtılıyor ve NOTICE'te yazıyor; dağıtım hakkı ayrıca kontrol edilmeli.
- [ ] Upstream senkron süreci: her JetBrains sürüm tag'ine bir merge; adımlar `docs/upstream-sync.md`'de
  - Deneme (9 Kasım 2026): `263` serisinde taban tag'den (`idea/263.7307.200`) yeni bir tag yok. Bu yüzden JetBrains'in `upstream/263` branch'i (534 yeni commit) geçici bir branch'te denendi, sonra silindi.
  - Düz `git merge` 114 çakışma verdi; fork dosyalarından sadece `IdeaApplicationInfo.xml` çakıştı. Çakışmaları dosya dosya çözmek birbirine uymayan dosyalar bıraktı (`EditorsSplitters.kt` derlenmedi).
  - `fork/upstream-sync.sh` artık upstream ağacını alıp fork farkını `git apply --3way` ile uyguluyor. Sonuç: 0 çakışma; ağaç upstream'den sadece fork'un 50 dosyasında ayrılıyor. Fork modülleri derlendi.
  - Bulunan iki elle iş: `android/` klonu aynı tag'e alınmalı; `ReStudioPluginsTest` upstream'in `renderAdditionalJvmArguments` imza değişikliğine uyarlanmalıydı. Uyarlamadan sonra 10 ReStudio testi geçti. İkisi de `docs/upstream-sync.md`'de.
  - Açık: gerçek bir tag ile `merge` ve `finish` komutları uçtan uca çalıştırılmadı. Yeni tag gelince `fork-upstream-check` issue açar.
- [x] README: ne olduğu, JetBrains ile bağlantısı olmadığı, desteklenen diller, kurulum
  - `.github/README.md`. GitHub bu dosyayı kökteki README'den önce gösteriyor; böylece upstream README'si değişmiyor ve merge çakışması olmuyor.
- [x] Fork'un kökündeki `AGENTS.md`'yi fork'a göre sadeleştir: silinen `.agents/skills/` dosyalarına referanslar kalmasın, fork kuralları (JetBrains koduna dokunma, sadece marka/paketleme) eklensin
  - 9 Kasım 2026: `.agents/skills/` silinmemiş; `AGENTS.md`'nin başvurduğu dosyaların hepsi var. `AGENTS.md` değişmedi.
  - Fork kuralları `.ai/local.md`'de. `AGENTS.md` bu dosyayı `@./.ai/local.md` ile içe alıyor; upstream'de bu dosya yok, bu yüzden merge çakışması olmaz.

**Bitti kriteri:** GitHub Releases'ta indirilebilir installer var; kurulunca kendi adıyla açılıyor, desteklenen diller hazır geliyor, resmi IntelliJ ile yan yana çalışıyor ve bir upstream merge'ü baştan sona yapılmış.

## Repo yapısı ve Claude ile çalışma

İki public GitHub reposu: ince bir fork ve asıl işin yapıldığı plugin reposu. Claude Code günlük olarak plugin reposunda, Faz 6-7 ve upstream merge'lerinde fork reposunda çalışır.

```
<isim>-langs/                  # plugin reposu (Faz 1-5)
  settings.gradle.kts         # multi-module
  CLAUDE.md                   # mimari kurallar, komutlar, aktif faz; plana link
  core/                       # SDK bulucu, server yöneticisi, ortak DAP ve test adaptörleri
  dotnet/                     # Roslyn LS, solution modeli, netcoredbg, NuGet
  go/                         # gopls, go.mod modeli, Delve, go test
  web/                        # TS/JS, Vue, Angular, CSS, ESLint, js-debug, Vitest/Jest
  php/                        # Phpactor, composer modeli, Xdebug, PHPUnit/Pest
  testdata/                   # her dil için Faz 0'da seçilen örnek proje

<isim>-ide/                    # intellij-community fork'u = bu repo (ReStudio) (Faz 6-7)
  CLAUDE.md                   # fork kuralları: JetBrains kodu değiştirilmez, sadece marka/paketleme
  docs/plan.md                # bu plan; tek kopya burada, tik atılan görevler
  docs/upstream-sync.md       # upstream merge adımları
  .github/workflows/          # installer build'leri, release
  ... (intellij-community'nin kendisi; değişiklikler ürün tanımı, marka dosyaları ve bundled plugin listesiyle sınırlı)
```

Her dil modülü ayrı bir plugin `.zip`'i üretir ve `core`'a bağımlıdır; böylece resmi IntelliJ kullananlar da sadece istedikleri dili kurabilir.

**Teknoloji yığını**

| Parça | Seçim |
| --- | --- |
| Dil | Kotlin 2.4, JDK 25 (platform 263 en az Java 25 ister: `product-info.json`, `minRequiredJavaVersion`) |
| Build | Gradle 9.8 + IntelliJ Platform Gradle Plugin 2.19 |
| Hedef IDE | Plugin: platform 263 (2026.3) ve üstü · Fork: `idea/263.7307.200` (= `idea/2026.3`) tag'inden başlayan `main` |
| LSP/DAP istemcisi | LSP4IJ (bundled plugin) |
| Language server'lar | Roslyn LS, gopls, vtsls, vscode-langservers-extracted, Vue LS, Angular LS, Tailwind LS, ESLint LS, Phpactor |
| Debug adapter'lar | netcoredbg, Delve (`dlv dap`), vscode-js-debug, vscode-php-debug |
| Test | IntelliJ platform test framework + `testdata/` üzerinde dil başına entegrasyon testleri |

**Claude ile çalışma kuralları**

- `CLAUDE.md`'de: platform API kuralları (EDT'de ağır iş yok, read/write action kullanımı), paket yapısı, build/test komutları, aktif faz.
- Her görev = tek oturum, tek PR. Önce test (fixture veya testdata solution), sonra implementasyon.
- Platform API'lerinde Claude'un bildiği eski API'leri kullanma riski var: deprecated uyarılarını build'de hata say, `runIde` ile her görevin sonunda elle dene.
- Language server ve debug adapter davranışını tahmin ettirme; LSP/DAP trafiğini loglayıp (LSP4IJ trace: verbose) gerçek mesajları Claude'a ver.
- Faz sonunda fork reposundaki `docs/plan.md`'de görevleri işaretle, bitti kriterini elle doğrula. Plugin reposunda planın kopyası tutulmaz.

Fork reposunda Claude'a verilecek ana kural: JetBrains'in kaynak dosyalarına dokunma, değişiklikleri marka ve paketleme dosyalarında tut. Her dokunulan upstream dosyası bir sonraki merge'de çakışma demek.

## Riskler ve açık sorular

En büyük risk kapsam: dört dil paketi ve bir fork'un sürekli bakımı tek kişi için ağır; bu yüzden dil dil sürüm çıkarmak şart. Rider ticari olmayan kullanımda ücretsiz olduğundan, projenin değeri açık kaynak bir alternatif sunmak ve öğrenmek.

| Risk | Etki | Önlem |
| --- | --- | --- |
| Kapsam: 4 dil paketi + fork, tek kişi için büyük | Yüksek | Dil dil sürüm çıkar (v0.1 = .NET); her fazın bitti kriteri sağlanmadan sonrakine geçme |
| Upstream merge maliyeti: JetBrains her sürümde binlerce dosya değiştiriyor | Yüksek | Sadece sürüm tag'lerine merge; JetBrains dosyalarına dokunma |
| Çok sayıda harici language server ve debug adapter'ın sürüm değişiklikleri | Yüksek | Hepsini sürüm sabitleyerek kur; güncellemeyi dil başına test ederek yap |
| Roslyn LS ve Vue LS gibi server'lar VS Code'a göre tasarlanmış, özel mesajlar kullanıyor | Orta | Faz 0'da LSP trace ile gerçek mesajları kaydet; yedek server'ları (csharp-ls, typescript-language-server) hazır tut |
| Frontend desteği JetBrains'in kapalı kaynak JS/TS desteğinin gerisinde kalır | Orta | README'de beklentiyi net yaz; LSP'nin verdiğiyle yetin |
| Fork build'i ağır; GitHub Actions standart runner'ları yetmeyebilir | Orta | Faz 0'da ölç; gerekirse self-hosted runner |
| Marka ihlali: "IntelliJ", JetBrains logoları veya benzer isim | Yüksek | Ayırt edici bir isim; arayüz, installer ve README'de JetBrains ile bağlantı olmadığını belirt |
| Paketlenen bileşenlerin lisans yükümlülükleri | Orta | Faz 7'de NOTICE ve lisans listesi; sorunlu olanları ilk açılışta indir |
| Public projeye gelen issue/PR yükü | Orta | Desteklenen diller ve platformları README'de net yaz; issue şablonu |

**Açık sorular**

- [x] IDE'nin adı ne olacak? (JetBrains markalarına benzemeyen, GitHub'da ve domain olarak boş bir isim)
  - Cevap (9 Kasım 2026): ReStudio.
- [ ] Hangi işletim sistemleri için installer çıkacak? İlk sürüm tek platformla sınırlanabilir.
- [ ] Dil sırası doğru mu? (Öneri: .NET → Go → frontend → PHP)
- [ ] Roslyn LS mi csharp-ls mi; vtsls mi typescript-language-server mı? Faz 0 ölçümüne göre öneri (onay bekliyor):
  - **C#:** Varsayılan Roslyn LS (Microsoft'un resmi server'ı, `solution/open` ile 12 sn'de tam çalışıyor). csharp-ls yedek kalsın: aynı özellikleri özel mesaj gerektirmeden veriyor, ama tek kişilik bir proje.
  - **TypeScript:** vtsls. typescript-language-server TS 7 projelerinde başlamıyor; plandan çıkar. TS 7 projelerinde projenin kendi `tsc --lsp` server'ı kullanılsın.
- [ ] Language server'lar installer'a gömülecek mi, ilk kullanımda mı indirilecek? (lisans, installer boyutu, Node.js bağımlılığı)
- [ ] Dart plugin'inin ve intellij-community'deki Python plugin'inin fork'a bundled eklenmesinin lisans ve build açısından doğrulanması
- [x] Fork'tan JetBrains Marketplace'e erişim açık kalacak mı? Kullanım koşulları kontrol edilmeli.
  - Cevap (9 Kasım 2026): Açık kalıyor. README, Marketplace kullanım koşullarına link veriyor. Koşulların hukuki kontrolü yapılmadı.
- [x] Bir plugin `.zip`'ini fork'un installer build'ine bundled eklemenin doğru yolu; Faz 6 başında intellij-community build script'lerinden doğrulanacak.
  - Cevap (9 Kasım 2026): JetBrains vendor'lı plugin'ler `getAdditionalPluginPaths` ile `plugins/`'e, diğerleri `-Dplugin.path` ile `restudio-plugins/`'e gider (Faz 6).
- [x] Fork reposunda `.agents/skills/` silindi. Bu silme kasıtlı mı? Upstream merge'lerinde bu dosyalar tekrar gelir; her merge'de silmek mi, yoksa tutmak mı daha az çakışma yaratır?
  - Cevap (9 Kasım 2026): Silinmemiş. `main`'de `.agents/skills/` upstream ile aynı; bir şey yapmak gerekmiyor.

## Kaynaklar

- [LSP4IJ](https://github.com/redhat-developer/lsp4ij) ve [geliştirici rehberi](https://www.github.com/redhat-developer/lsp4ij/blob/main/docs/DeveloperGuide.md)
- [roslyn-language-server (NuGet)](https://www.nuget.org/packages/roslyn-language-server.linux-x64)
- [csharp-language-server](https://github.com/razzmatazz/csharp-language-server)
- [netcoredbg](https://github.com/samsung/netcoredbg)
- [The Rough History of .NET Core Debuggers](https://docs.lextudio.com/blog/the-rough-history-of-net-core-debuggers-b9fb206dc4aa)

* [intellij-community: kaynaktan build talimatları](https://github.com/JetBrains/intellij-community)
* [JetBrains: Core JavaScript and TypeScript Features Become Free in IntelliJ IDEA](https://blog.jetbrains.com/idea/2026/03/js-ts-free-support/)
* [Neowin: IntelliJ IDEA goes unified (açık kaynak build'lerin kapsamı)](https://www.neowin.net/news/intellij-idea-goes-unified-what-it-means-for-free-users-and-ultimate-subscribers/)
* [LSP4IJ: user-defined language server ve hazır şablonlar](https://www.github.com/redhat-developer/lsp4ij/blob/main/docs/UserDefinedLanguageServer.md)
* [Flutter plugin for IntelliJ](https://github.com/flutter/flutter-intellij)
