# PDF Master - Android Application (Jetpack Compose & Clean Architecture)

Мобільний застосунок для Android для створення PDF-файлів із зображень та редагування наявних PDF.

## 🚀 Стек технологій
- **Мова:** Kotlin 2.0+
- **UI:** Jetpack Compose (Material Design 3 / Material You)
- **Архітектура:** MVVM + Clean Architecture (Presentation, Domain, Data)
- **Нативні API:**
  - `android.graphics.pdf.PdfDocument` — створення та малювання PDF з орієнтацією, полями та нумерацією.
  - `android.graphics.pdf.PdfRenderer` — генерація векторних прев'ю сторінок.
  - Photo Picker API (`ActivityResultContracts.PickMultipleVisualMedia`) — вибір фотографій без застарілих системних дозволів.
  - MediaStore API & Scoped Storage (Android 10-15+) — безпечне збереження у папку "Downloads".
- **Бібліотеки:**
  - `io.coil-kt:coil-compose` — асинхронне завантаження зображень та кешування.
  - `com.github.yalantis:ucrop` — кадрування та поворот фото.
  - `com.tom-roush:pdfbox-android` — розширене редагування, вилучення та мердж PDF.
  - Kotlin Coroutines & Flow — фонова обробка без блокування UI потоку.

## 📦 Як відкрити проєкт в Android Studio
1. Завантажте ZIP-архів через кнопку **"Завантажити ZIP проєкт"**.
2. Розпакуйте архів у потрібну папку.
3. Відкрийте **Android Studio** (рекомендовано Flamingo, Hedgehog, Iguana, Koala або Ladybug).
4. Виберіть **File -> Open...** та вкажіть розпаковану папку.
5. Зачекайте завершення Gradle Sync.
6. Запустіть застосунок на емуляторі або реальному пристрої з Android 8.0+ (API 26-35).
