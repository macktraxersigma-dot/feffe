# Pulse Visual: мод для Minecraft 1.21.11 (Fabric)

Меню открывается на **Right Shift** (ЛКМ — вкл/выкл модуль, ПКМ — настройки). Настройки сохраняются в `config/pulse-visual.properties`.

## Как получить .jar (проще всего: GitHub соберёт сам)
1. Создай на github.com новый репозиторий (Private).
2. Загрузи в него всё содержимое этой папки (Add file → Upload files), включая папку `.github`.
3. Открой вкладку **Actions** → build → **Run workflow**. Через пару минут внутри запуска появится **pulse-visual-jar**, скачай и распакуй.
4. Перетащи `pulse-visual-*.jar` в окно лаунчера (вкладка Mods). Fabric и Fabric API лаунчер поставит сам.

Если у тебя есть JDK 21 и Gradle: `python tools/resolve_versions.py` и `gradle build`, jar будет в `build/libs`.

## Если сборка упала
Пришли текст ошибки. Весь код, который обращается к Minecraft, собран в трёх файлах: `McPlatform.java`, `PulseClient.java`, `PulseScreen.java`. Всё остальное от версии игры не зависит.
