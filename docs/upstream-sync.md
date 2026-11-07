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
   Script tag'i çeker, `main`'den `sync/<build>` branch'ini açar ve merge eder.
3. **Çakışma varsa:** Script çakışan dosyaları listeler ve durur. Çözün, `git add` yapın, sonra:
   ```
   fork/upstream-sync.sh finish idea/263.8000.10
   ```
   Vazgeçmek için: `fork/upstream-sync.sh abort`.
4. **Kontroller** (aşağıda) geçince branch'i push edin, `main`'e PR açın, merge edin, issue'yu kapatın.

## Kontroller

Her sync PR'ı merge edilmeden önce:

- [ ] IDE kaynaktan derleniyor ve açılıyor (`./installers.cmd -Dintellij.build.target.os=current` veya IDE'deki "IDEA" run configuration'ı)
- [ ] Marka doğru: ürün adı, ikonlar, About ekranı; "IntelliJ" ve JetBrains logoları görünmüyor
- [ ] Bundled plugin'ler (dil plugin'leri, Flutter, Dart, Python) yükleniyor; `sinceBuild`/`untilBuild` yeni build ile uyumlu
- [ ] `testdata/` örnek projeleri açılıyor; her dil için completion, run ve debug çalışıyor

## Çakışmaları az tutma kuralları

Çakışma maliyeti, JetBrains dosyalarına ne kadar dokunduğumuza bağlıdır.

- Fork'a özel her şey **yeni dosyalarda** durur: `fork/`, `docs/upstream-sync.md`, `.github/workflows/fork-*.yml`, marka kaynakları. Upstream bu yollara dokunmaz.
- JetBrains dosyası değiştirmek zorunluysa değişikliği küçük tutun ve aşağıdaki "Dokunulan upstream dosyaları" tablosuna ekleyin. Çakışma çıkarsa ilk bakılacak yer burasıdır.
- Script merge'leri `git rerere` açık çalıştırır. Bir çakışmayı bir kez çözünce, aynı çakışma sonraki merge'lerde otomatik çözülür. Bunun için çözümü `finish` ile commit edin.
- Merge commit'leri olduğu gibi kalır; `main` üzerinde rebase yapılmaz.

### Dokunulan upstream dosyaları

| Dosya | Neden | Eklendiği sync |
|---|---|---|
| _(henüz yok)_ | | |

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
