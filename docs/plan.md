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
- Kendi adı ve ikonuyla IDE dağıtımı: GitHub'da public kaynak + GitHub Releases'ta Windows/macOS/Linux installer

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
| Çıktı | GitHub Releases'ta Windows/macOS/Linux installer | Her dil için plugin `.zip`'i; fork build'i bunları içine alır, istenirse resmi IntelliJ'ye de ayrıca kurulabilir |

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
- [ ] .NET: `dotnet tool install --global roslyn-language-server --prerelease` ([NuGet](https://www.nuget.org/packages/roslyn-language-server.linux-x64)); yedek olarak [csharp-ls](https://github.com/razzmatazz/csharp-language-server); debug için [netcoredbg](https://github.com/samsung/netcoredbg)
- [ ] Go: `gopls` + `dlv dap`
- [ ] Frontend: vtsls (veya typescript-language-server), vscode-langservers-extracted, Vue LS, Angular LS, ESLint; debug için vscode-js-debug
- [ ] PHP: Phpactor + vscode-php-debug adaptörüyle Xdebug
- [ ] Flutter: fork build'inde Flutter ve Dart plugin'lerinin kurulup çalıştığını doğrula
- [x] Her dil için bir örnek proje seç: ASP.NET Core solution (5-10 proje), çok paketli Go servisi, Laravel uygulaması, React (Vite) ve Vue uygulaması, Flutter uygulaması
  - `~/projects/ReStudio-langs/testdata/` (yerel repo, push edilmedi). Hepsi şablondan üretildi ve build/test'leri geçiyor: `dotnet/` (7 proje, `Shop.sln`), `go/` (`go.work`, 2 modül), `php/` (Laravel), `web/react/` (TS 7), `web/vue/` (TS 6, ESLint eklendi), `flutter/`. Angular örneği yok.
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

Bulgular ve sonraki fazlara etkisi:

- **Roslyn LS (Faz 2):** `initialized`'dan sonra `solution/open` bildirimi şart. Bildirim olmadan dosya "misc file" kalır ve hiçbir özellik çalışmaz. Bildirimle her şey 12 sn'de hazır. Diagnostic mesajları sistem dilinde (Türkçe) geliyor; dil IDE'nin diline göre ayarlanmalı.
- **csharp-ls:** `--solution` ile özel mesaj gerekmeden aynı sonucu daha hızlı veriyor (6 sn). Roslyn LS'e karşı gerçek bir yedek.
- **TypeScript (Faz 4):** typescript-language-server kendi TS'ini getirmediği için TS 7 projelerinde başlamıyor; plandan çıkarılmalı. vtsls kendi TS 5.9.3'üne geri dönüyor ve çalışıyor. TS 7'nin `tsc --lsp` modu tam çalışıyor; TS 7 projelerinde tercih edilebilir.
- **Vue (Faz 4):** Vue LS 3, TypeScript işlerini istemciye `tsserver/request` bildirimiyle gönderiyor ve cevap bekliyor. Plugin; Vue LS'i ve `@vue/typescript-plugin` yüklü vtsls'yi birlikte başlatıp bu mesajları köprülemeli (`tsserver/response`). Köprü olmadan Vue desteği hiç çalışmıyor.
- **ESLint (Faz 4):** typescript-eslint, TS 7'yi desteklemiyor (peer: `>=4.8.4 <6.1.0`). Vite'ın React şablonu ESLint yerine oxlint getiriyor. oxlint desteği değerlendirilmeli.
- **Formatting:** Server'lar istemcinin gönderdiği girinti ayarını kullanıyor. Ayar `.editorconfig` veya Prettier'dan gelmeli; aksi hâlde formatlama tüm dosyayı değiştiriyor.
- **PHP (Faz 5):** Phpactor rename, `vendor/` altındaki Laravel dosyalarını düzenliyor; plugin `vendor/`'a düşen düzenlemeleri engellemeli. Formatting yok (php-cs-fixer entegrasyonu gerekli). `view()` helper'ı için "Function not found" yanlış alarmı veriyor; Composer `autoload.files` indekslenmiyor olabilir.
- **Kapsam dışı kalanlar:** Tailwind LS (örnek projede Tailwind yok), Angular LS (Angular örneği yok).

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

- [ ] Gradle multi-module plugin reposu: `core` + dil modülleri; LSP4IJ'ye bağımlılık
- [ ] SDK bulucu: `dotnet`, `go`, `php`, `node`, `flutter` yollarını PATH'ten bul, ayar ekranında değiştirilebilsin
- [ ] Language server yöneticisi: sürümü sabitlenmiş server'ları ilk kullanımda kur (`dotnet tool`, `go install`, `npm`, `.phar`), güncellemeyi bilinçli yap
- [ ] Ortak debug katmanı: bir run configuration'dan DAP adapter'ı başlatma (LSP4IJ DAP üzerine)
- [ ] Ortak test adaptörü: dış test çıktısını (TRX, `go test -json`, JUnit XML) IntelliJ test penceresine bağlama
- [ ] Ortak gutter ikonları (▶ Run / Debug): LSP4IJ, LSP dillerinde PSI kurmadığı için bu ikonları koymuyor. `textDocument/documentSymbol` cevabından giriş noktalarını ve testleri bul. `RunLineMarkerContributor` ile ikonu koy, `RunConfigurationProducer` ile run configuration'ı üret. Her dil sadece kuralı verir: Go'da `package main` + `func main` ve `func TestX(t *testing.T)`, C#'ta `Main` ve `[Fact]`, PHP'de `test_` metotları, Dart'ta `main` ve `test()`.
  - Fork build'inde denendi (8 Kasım 2026): LSP4IJ ile Go dosyasında ▶ yok. Kullanıcı DAP run configuration'ını elle ekleyip launch JSON'unu yazmak zorunda. Delve şablonunun varsayılanı `"program": "${file}"`; kütüphane dosyasında bu ayar çalışmıyor.
- [ ] Status bar'da server durumu, LSP/DAP trace log ekranı

**Bitti kriteri:** Yeni bir dil eklemek = server tanımı + kurulum tarifi + run/debug/test adaptörü; ortak kod tekrar yazılmıyor.

### Faz 2: C# ve .NET (8-10 hafta)

- [ ] Roslyn LS entegrasyonu; özel mesajlar (solution/proje yükleme, `restore` isteği)
- [ ] `.sln`/`.slnx`/`.csproj` okuma (`dotnet msbuild -getProperty/-getItem`), proje → modül eşleme, solution görünümü
- [ ] `dotnet build`/`run` run configuration'ı, `launchSettings.json`, tıklanabilir build hataları
- [ ] netcoredbg ile debug ve attach
- [ ] `dotnet test` + TRX ile xUnit/NUnit/MSTest test penceresi, test gutter ikonları
- [ ] NuGet tool window (arama, ekleme, güncelleme)

**Bitti kriteri:** ASP.NET Core solution'ı yazılıyor, çalışıyor, test ediliyor, debug ediliyor; async metotlarda step doğru ilerliyor.

### Faz 3: Go (3 hafta)

- [ ] gopls entegrasyonu; `go.mod`/`go.work` → modüller
- [ ] `go build`/`go run` run configuration'ı
- [ ] `go test -json` ile test penceresi ve gutter ikonları
- [ ] Delve (`dlv dap`) ile debug
- [ ] `gofmt`/`goimports` formatlama (gopls üzerinden)

**Bitti kriteri:** `go.work` kullanan çok paketli bir Go servisi çalışıyor, test ve debug ediliyor.

### Faz 4: Frontend (4-5 hafta)

- [ ] TS/JS için vtsls; TS 7 projelerinde projenin kendi `tsc --lsp` server'ı. HTML, CSS, JSON için vscode-langservers-extracted
- [ ] Vue LS: `tsserver/request` mesajlarını `@vue/typescript-plugin` yüklü vtsls'ye köprüle (Faz 0 bulgusu). Angular LS, Tailwind CSS LS
- [ ] ESLint LS ve Prettier ile formatlama
- [ ] `package.json` script'leri için run configuration (npm, pnpm, yarn, bun); `node_modules/.bin` çözümü
- [ ] vscode-js-debug ile Node ve Chrome debug
- [ ] Vitest/Jest sonuçlarını test penceresine bağlama

**Bitti kriteri:** React (Vite) ve Vue uygulamasında yazma, lint, format, dev server, tarayıcıda breakpoint ve testler çalışıyor.

### Faz 5: PHP (3 hafta)

- [ ] Phpactor entegrasyonu; Intelephense kullanıcı isterse opsiyonel
- [ ] `composer.json` → autoload ve kaynak kökleri
- [ ] PHP script, built-in server ve `artisan` run configuration'ları
- [ ] PHPUnit/Pest: JUnit XML çıktısıyla test penceresi
- [ ] Xdebug: vscode-php-debug adaptörüyle debug

**Bitti kriteri:** Laravel projesinde completion, navigation, testler ve Xdebug breakpoint'leri çalışıyor.

### Faz 6: Flutter, Dart ve Python paketleme (1 hafta)

- [ ] Google'ın Flutter plugin'i ve Dart plugin'ini fork build'ine bundled ekle
- [ ] intellij-community içindeki açık kaynak Python plugin'ini bundled ekle
- [ ] Plugin sürümlerinin fork'un IDE sürümüyle uyumunu doğrula; uyumsuzlukta plugin'i kaynaktan derle

**Bitti kriteri:** Flutter projesi ek kurulum olmadan açılıyor, hot reload ve debug çalışıyor; Python projesi çalışıyor.

### Faz 7: Fork, marka ve public dağıtım (3-4 hafta)

- [ ] Ürün adı, ikonlar, splash, About ekranı ve ürün tanım dosyaları; arayüzde ve installer'da "IntelliJ" ve JetBrains logoları kalmasın
- [ ] Kendi config, cache ve log dizinleri; resmi IntelliJ ile yan yana kurulup birbirinin ayarlarını bozmasın
- [ ] Bundled plugin listesi: dil plugin'leri, LSP4IJ, Flutter, Dart, Python; ihtiyaç olmayanlar (ör. Android) çıkarılsın
- [ ] Güncelleme kontrolü GitHub Releases'a baksın (veya ilk sürümde kapatılsın)
- [ ] Desteklenen dillerde plugin önerisi bandını kapat. Fork build'inde `.go` dosyası açılınca IDE "*.go files are supported by IntelliJ IDEA" diyor ve kapalı kaynak Go plugin'ini öneriyor.
- [ ] GitHub Actions ile Windows/macOS/Linux installer build'i; standart runner yetmezse self-hosted runner
  - Upstream'in `.github/workflows/ide_build_and_upload.yml`'ı bunu zaten yapıyor: `ubuntu-latest` üzerinde Docker ile build, `-aarch64.sit` üretimi, sonra `macos-latest` üzerinde `-aarch64.dmg`. Çıktılar imzasız.
  - Fork'ta değişmeden çalışmaz: `.github/actions/build_ide`, `JetBrains/android`'i `github.ref` ile checkout ediyor; bizim branch ve tag'lerimiz orada yok. Ref, `fork/UPSTREAM_BASE`'deki upstream tag'ine sabitlenmeli.
  - Upstream dosyalarına dokunmamak için ayrı bir `fork-release.yml` yaz: build adımlarını ondan çağır, `.dmg`'yi `gh release create` ile GitHub Releases'a yükle.
  - Upstream workflow sadece Apple Silicon (aarch64) `.dmg`'yi yüklüyor. Yerel build Intel `.dmg`'yi de üretiyor; Intel Mac gerekiyorsa workflow'a onun yükleme adımı eklenmeli.
  - Upstream'in günlük `schedule` tetikleyicisi fork'ta gereksiz dakika harcar; fork'ta kapalı kalsın.
- [ ] macOS imzalama ve notarization (yapılmazsa README'de Gatekeeper notu: `xattr -dr com.apple.quarantine`). İmza için Apple Developer ID sertifikası ve notarization bilgileri GitHub secret olarak eklenmeli.
- [ ] `LICENSE` (Apache 2.0), `NOTICE` ve üçüncü taraf lisans listesi
- [ ] Upstream senkron süreci: her JetBrains sürüm tag'ine bir merge; adımlar `docs/upstream-sync.md`'de
- [ ] README: ne olduğu, JetBrains ile bağlantısı olmadığı, desteklenen diller, kurulum
- [ ] Fork'un kökündeki `AGENTS.md`'yi fork'a göre sadeleştir: silinen `.agents/skills/` dosyalarına referanslar kalmasın, fork kuralları (JetBrains koduna dokunma, sadece marka/paketleme) eklensin

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
| Dil | Kotlin, JDK 21 |
| Build | Gradle + IntelliJ Platform Gradle Plugin 2.x |
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

- [ ] IDE'nin adı ne olacak? (JetBrains markalarına benzemeyen, GitHub'da ve domain olarak boş bir isim)
- [ ] Hangi işletim sistemleri için installer çıkacak? İlk sürüm tek platformla sınırlanabilir.
- [ ] Dil sırası doğru mu? (Öneri: .NET → Go → frontend → PHP)
- [ ] Roslyn LS mi csharp-ls mi; vtsls mi typescript-language-server mı? Faz 0 sonunda karar verilecek.
- [ ] Language server'lar installer'a gömülecek mi, ilk kullanımda mı indirilecek? (lisans, installer boyutu, Node.js bağımlılığı)
- [ ] Dart plugin'inin ve intellij-community'deki Python plugin'inin fork'a bundled eklenmesinin lisans ve build açısından doğrulanması
- [ ] Fork'tan JetBrains Marketplace'e erişim açık kalacak mı? Kullanım koşulları kontrol edilmeli.
- [ ] Bir plugin `.zip`'ini fork'un installer build'ine bundled eklemenin doğru yolu; Faz 6 başında intellij-community build script'lerinden doğrulanacak.
- [ ] Fork reposunda `.agents/skills/` silindi. Bu silme kasıtlı mı? Upstream merge'lerinde bu dosyalar tekrar gelir; her merge'de silmek mi, yoksa tutmak mı daha az çakışma yaratır?

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
