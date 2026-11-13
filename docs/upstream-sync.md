# Upstream senkron süreci

Fork, JetBrains'in `master` branch'ini takip etmez. Yeni özellik ve düzeltmeler, JetBrains'in **sürüm tag'leri** (`idea/<build>`) `main` branch'ine merge edilerek alınır. Her merge ayrı bir `sync/<build>` branch'inde yapılır ve pull request ile `main`'e girer.

## Branch modeli

| Branch | İçerik | Kim değiştirir |
|---|---|---|
| `master` | JetBrains `master`'ının yansıması. Bu branch'e commit atılmaz. | Sadece `git pull upstream master` |
| `main` | Fork'un kendisi: son merge edilen tag + marka ve paketleme değişiklikleri | Pull request ile |
| `sync/<build>` | Bir tag'in `main`'e merge'ü; PR açılır, sonra silinir | `fork/upstream-sync.sh` |

Şu an hangi tag'in taban olduğu [fork/UPSTREAM_BASE](../fork/UPSTREAM_BASE) dosyasında yazar. Script bu dosyayı her merge'de günceller; elle değiştirmeyin.

## Akış

1. **Haber:** [fork-upstream-check](../.github/workflows/fork-upstream-check.yml) workflow'u her pazartesi upstream'i kontrol eder. Aynı sürüm serisinde (ör. `263`) daha yeni bir tag varsa "Upstream sync: idea/…" başlıklı bir issue açar. Elle `fork/upstream-sync.sh status` ile de bakılabilir.
2. **Merge:** Yerelde, temiz bir çalışma diziniyle:
   ```
   fork/upstream-sync.sh merge            # serinin en yeni tag'i
   fork/upstream-sync.sh merge idea/263.8000.10   # belirli bir tag
   ```
   Script tag'i çeker ve `main`'den `sync/<build>` branch'ini açar. Sonuç, yeni tag'in ağacı ve üstünde fork'un farkıdır (`<taban tag>..main`). Fark `git apply --3way` ile uygulanır. Commit'in iki ebeveyni var: `main` ve tag.
   - Neden düz `git merge` değil: Yeni bir build hattı (ör. `262.10968` → `262.11879`) önceki tag'in commit'lerini içermez. Merge'ün ortak atası taban tag'den eskidir ve git tag'e özel commit'leri ikinci kez uygular. Deneme (9 Kasım 2026, `upstream/263` branch'i): düz merge 114 çakışma verdi; çakışmaları dosya dosya çözmek de birbirine uymayan dosyalar bıraktı (`EditorsSplitters.kt` derlenmedi). Yeni yöntemde çakışma 0, sonuç upstream'den sadece fork'un 50 dosyasında ayrılıyor.
   - Aynı build hattındaki tag'ler (ör. `263.7307.112` → `263.7307.200`) birbirinin üstüne kurulur; orada iki yöntem de aynı sonucu verir.
3. **`android/` klonu:** Build, `android/` dizinini ister ve onu aynı tag'de bekler. Yeni tag bir `android/` modülüne başvurur, eski klonda yoksa Bazel derlemeden önce düşer. `finish` komutu gereken git komutunu yazar. CI, klonu `fork/UPSTREAM_BASE`'deki tag ile kendisi alır.
4. **Çakışma varsa:** Çakışma sadece fork'un değiştirdiği dosyalarda çıkar. Script bu dosyaları listeler ve durur. Çözün, `git add` yapın, sonra:
   ```
   fork/upstream-sync.sh finish idea/263.8000.10
   ```
   Vazgeçmek için: `fork/upstream-sync.sh abort`.
5. **API değişiklikleri:** Upstream, fork kodunun kullandığı bir API'yi değiştirebilir. Çakışma olmasa da derleme düşer. Deneme örneği: `renderAdditionalJvmArguments`, `vendorName` ve `pathsSelector`'ı `ProductJvmArguments`'tan alıp parametre yaptı; `ReStudioPluginsTest` güncellenmeliydi. Bu yüzden kontroller derlemeyi ve `ReStudio*` testlerini içerir.
6. **Kontroller** (aşağıda) geçince branch'i push edin, `main`'e PR açın, merge edin, issue'yu kapatın.

## Kontroller

Her sync PR'ı merge edilmeden önce:

- [ ] Fork modülleri ve testleri derleniyor: `./bazel.cmd build //build:i_build_target //idea/customization/base:base //platform/platform-impl:ide-impl //platform/core-impl:core-impl` ve `./tests.cmd --module intellij.idea.community.build.tests --test 'org.jetbrains.intellij.build.ReStudio*'`
- [ ] CI (`fork-release.yml`, tag'siz) sync branch'inde geçiyor; smoke test bundled plugin'leri yüklüyor
- [ ] IDE kaynaktan derleniyor ve açılıyor (`./installers.cmd -Dintellij.build.target.os=current` veya IDE'deki "IDEA" run configuration'ı)
- [ ] Marka doğru: ürün adı, ikonlar, About ekranı; "IntelliJ" ve JetBrains logoları görünmüyor
- [ ] Bundled plugin'ler (dil plugin'leri, Flutter, Dart, Python) yükleniyor; `sinceBuild`/`untilBuild` yeni build ile uyumlu
- [ ] `testdata/` örnek projeleri açılıyor; her dil için completion, run ve debug çalışıyor

## Çakışmaları az tutma kuralları

Çakışma maliyeti, JetBrains dosyalarına ne kadar dokunduğumuza bağlıdır.

- Fork'a özel her şey **yeni dosyalarda** durur: `fork/`, `docs/upstream-sync.md`, `.github/workflows/fork-*.yml`, marka kaynakları. Upstream bu yollara dokunmaz.
- JetBrains dosyası değiştirmek zorunluysa değişikliği küçük tutun ve aşağıdaki "Dokunulan upstream dosyaları" tablosuna ekleyin. Çakışma çıkarsa ilk bakılacak yer burasıdır.
- Merge commit'leri olduğu gibi kalır; `main` üzerinde rebase yapılmaz.

### Dokunulan upstream dosyaları

| Dosya | Neden | Eklendiği sync |
|---|---|---|
| `build/src/org/jetbrains/intellij/build/IdeaCommunityProperties.kt` | Üç çağrı `ReStudioPlugins`'e gider (fork dosyası, aynı dizinde): `getAdditionalPluginPaths`, `getAdditionalContextDependentIdeJvmArguments` ve `copyAdditionalFiles` içindeki bir satır. `out/restudio-plugins` yoksa build upstream ile aynıdır. Marka değerleri `ReStudioBranding`'den gelir: görsel dizini, VM seçenekleri, dizin adı, artifact adı, lisans dosyaları ve üç customizer'daki ad, dizin, bundle ID ve URL scheme. Bir satır daha: `createCommunityBuildContext`, `ReStudioMacSigning`'in imza aracını verir. Bir satır daha: `bundledPluginModules`'a `ReStudioPlugins.SOURCE_PLUGIN_MODULES` eklenir (Lombok plugin'i). | — (ilk fork değişikliği) |
| `build/src/OpenSourceCommunityInstallersBuildTarget.kt` | Bir satır: `ReStudioMacSigning.enabledSteps()`, build Mac'inde sertifika varsa `mac_sign` ve `mac_notarize` adımlarını atlanacaklar listesinden çıkarır. | — (Faz 7) |
| `community-resources/resources/idea/IdeaApplicationInfo.xml` | Şirket adı, ürün adı, motto, splash ve ikon yolları ReStudio'ya ait. Görseller aynı dizinde `restudio*` adlı yeni dosyalarda. `script="idea"` değişmedi; launcher adları `idea` olarak kalır. | — (Faz 7) |
| `build/conf/nsis/idea.nsi` | Üç satır: `CompanyName` ve `Publisher` `${MANUFACTURER}` kullanır, `URLInfoAbout` ReStudio reposunu gösterir. | — (Faz 7) |
| `platform/platform-impl/src/com/intellij/openapi/updateSettings/impl/pluginsAdvertisement/PluginsAdvertiser.kt` | Bir satır: `promo.ignore.suggested.ide` sistem özelliği de JetBrains IDE önerisini kapatır. Launcher bu özelliği verir. | — (Faz 7) |
| `platform/platform-impl/src/com/intellij/ui/AppUIUtil.kt` | Bir satır: `loadConsentsForEditing` consents listesinin kopyasını alır. Vendor JetBrains değilse ve `consents-<vendor>.json` yoksa liste boş ve değiştirilemez; `removeIf` hata verir ve build'in `traverseUI` adımı düşer. Upstream'e de gönderilebilir. | — (Faz 7) |
| `platform/core-impl/src/com/intellij/ide/plugins/PluginUtils.kt` | `isUpdateable`, `plugin.path` ile gelen bir plugin için `false` döner. Böyle bir plugin aynı ID'li kurulu güncellemeyi geçersiz kılar, bu yüzden Marketplace güncellemesi işe yaramaz. Test: `platform/platform-tests/.../ReStudioPluginPathUpdateTest.kt`. | — (Faz 7) |
| `idea/customization/base/resources/intellij.idea.customization.base.xml` | Bir satır: `ExternalProductResourceUrls` servisi `ReStudioExternalResourceUrls`'e (fork dosyası, aynı modül) gider. Hata raporu ve indirme linkleri ReStudio reposuna gider; JetBrains destek, feedback ve What's New linkleri kapanır. Bir satır daha: `ReStudioErrorReportSubmitter` `order="first"` ile kayıtlı ve `ITNReporter`'ın yerine geçer. Bir satır daha: `ReStudioAboutDescription` About ekranına kaynak ve marka notunu ekler. | — (Faz 7) |
| `idea/customization/base/intellij.idea.customization.base.iml` (ve üretilen `BUILD.bazel`) | Bir satır: `intellij.platform.core` bağımlılığı. `ReStudioErrorReportSubmitter` `ApplicationInfo` ve `PluginAware` için bunu ister. | — (Faz 7) |
| `platform/platform-impl/src/com/intellij/diagnostic/HeapDumpAnalysisSupport.kt` | Bir satır: heap dump raporu `findExtension` (tam sınıf `ITNReporter`) yerine `findFirstAssignableExtension` ile submitter seçer. Böylece rapor JetBrains'e değil, `ReStudioErrorReportSubmitter`'a gider. | — (Faz 7) |
| `platform/build-scripts/src/org/jetbrains/intellij/build/impl/productInfo/ProductLaunchRenderer.kt` | `IDE_HOME_PLACEHOLDER` sabiti ve bir satır: ek JVM argümanlarındaki yer tutucuyu işletim sisteminin IDE dizini makrosuyla değiştirir. `-Dplugin.path` buna ihtiyaç duyar. | — (ilk fork değişikliği) |
| `plugins/ide-startup/importSettings/src/com/intellij/ide/startup/importSettings/jb/JbImportServiceImpl.kt` | İki satır: ilk açılış sihirbazı `JetBrains` vendor dizinini de tarar, plugin dizinini `vendorPluginsDir` ile bulur. Vendor `ReStudio` olduğu için sihirbaz IntelliJ, Rider gibi IDE'leri bulmuyordu. Yardımcılar `ReStudioJbVendorDirs.kt`'de (fork dosyası, aynı paket). | — (Faz 7) |

## Yeni sürüm serisine geçiş

Script ve workflow sadece **aynı seri** içindeki tag'leri önerir (ör. `263.x` → `263.y`). `status`, upstream'de yeni bir seri (ör. `271`) açıldığında bunu ayrıca bildirir. Seriler arası geçiş büyük bir güncellemedir:

1. Yeni serinin ilk **kararlı** tag'ini bekleyin (EAP veya RC değil). Build numarasını JetBrains sürüm notlarından doğrulayın.
2. Bundled plugin'lerin (Flutter, Dart, dil plugin'leri) yeni seri için sürümü çıkmış olmalı.
3. Merge aynı komutla yapılır: `fork/upstream-sync.sh merge idea/271.xxxx.yy`.
4. Plugin reposunda hedef platform sürümünü ve `sinceBuild` değerini güncelleyin.

## İlk kurulum

Bir kez yapılır:

```
git remote add upstream https://github.com/JetBrains/intellij-community.git   # yoksa
git fetch --no-tags upstream refs/tags/idea/263.7307.200:refs/tags/idea/263.7307.200
git switch -c main idea/263.7307.200
# fork/, docs/upstream-sync.md ve .github/workflows/fork-upstream-check.yml dosyalarını commit edin
git push -u origin main
```

GitHub'da yapılacaklar:

- Fork'un varsayılan branch'ini `main` yapın.
- **Issues**'ı açın (fork'larda varsayılan olarak kapalıdır). Kapalı kalırsa workflow issue açamaz.
- Actions'ı açın. Upstream'den gelen workflow'ları (`IntelliJ_IDEA.yml`, `PyCharm.yml` vb.) Actions sekmesinden **disable** edin. Bunlar JetBrains'in altyapısı için yazılmıştır ve fork'ta boşuna çalışır.
