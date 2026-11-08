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

- [ ] Resmi IntelliJ IDEA üzerine [LSP4IJ](https://github.com/redhat-developer/lsp4ij) kur; hazır şablonları (Go, HTML, CSS, TypeScript vb.) ve [user-defined language server](https://www.github.com/redhat-developer/lsp4ij/blob/main/docs/UserDefinedLanguageServer.md) ayarını kullan (fork'u beklemeye gerek yok)
- [ ] .NET: `dotnet tool install --global roslyn-language-server --prerelease` ([NuGet](https://www.nuget.org/packages/roslyn-language-server.linux-x64)); yedek olarak [csharp-ls](https://github.com/razzmatazz/csharp-language-server); debug için [netcoredbg](https://github.com/samsung/netcoredbg)
- [ ] Go: `gopls` + `dlv dap`
- [ ] Frontend: vtsls (veya typescript-language-server), vscode-langservers-extracted, Vue LS, Angular LS, ESLint; debug için vscode-js-debug
- [ ] PHP: Phpactor + vscode-php-debug adaptörüyle Xdebug
- [ ] Flutter: fork build'inde Flutter ve Dart plugin'lerinin kurulup çalıştığını doğrula
- [ ] Her dil için bir örnek proje seç: ASP.NET Core solution (5-10 proje), çok paketli Go servisi, Laravel uygulaması, React (Vite) ve Vue uygulaması, Flutter uygulaması
- [ ] Boşluk analizi: dil × özellik (completion, navigation, diagnostics, run, debug, test) için "çalışıyor / kısmen / yok" tablosu

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

- [ ] TS/JS için vtsls (veya typescript-language-server); HTML, CSS, JSON için vscode-langservers-extracted
- [ ] Vue LS (TypeScript server'la birlikte çalışacak şekilde), Angular LS, Tailwind CSS LS
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
