# AntiThiefPRO 2.1

Darmowa aplikacja Android z alarmem ruchowym i lokalnymi narzędziami diagnostycznymi.

## Funkcje

- Alarm ruchowy z 5-sekundowym odliczaniem.
- Regulowana czułość czujnika przyspieszenia.
- Oddzielne ustawienia dźwięku i wibracji.
- PIN aplikacji do rozbrajania alarmu i zmiany ustawień PIN.
- Historia zdarzeń z limitem 200 wpisów.
- Szyfrowanie lokalnej historii algorytmem AES-GCM z kluczem przechowywanym w Android Keystore.
- Zdarzenie w historii po restarcie Androida, jeśli system dostarczy odbiorcy chroniony broadcast.
- Ekran diagnostyczny: wersja, Android/API, czujnik ruchu, połączenie sieciowe, wolne miejsce i status administratora urządzenia.
- Eksport historii do pliku CSV.
- Opcjonalna zgoda administratora urządzenia Android z polityką obserwowania błędnych prób odblokowania (`watch-login`). Po aktywacji aplikacja zapisuje wykryte nieudane próby kodu blokady urządzenia w historii i próbuje wyświetlić powiadomienie.
- Brak reklam, analityki i automatycznego wysyłania danych do zewnętrznych serwerów.

## Budowanie APK

Push do gałęzi `main` automatycznie uruchamia GitHub Actions. Po udanym buildzie otwórz kartę **Actions**, wybierz najnowsze udane uruchomienie i pobierz artefakt `AntiThiefPRO-v2.1-debug`. Rozpakuj ZIP, aby uzyskać APK.

Artefakty kolejnych buildów są teraz przechowywane przez 1 dzień. Starsze artefakty z wcześniejszych uruchomień nie są usuwane wstecz przez tę zmianę i mogą pozostać do upływu ich dotychczasowego terminu ważności.

## Ograniczenia i prywatność

- Alarm ruchowy działa tylko, gdy aplikacja pozostaje otwarta. Ta wersja nie utrzymuje stałego monitorowania ruchu w tle.
- Wykrywanie nieudanego kodu blokady urządzenia wymaga ręcznego włączenia administratora urządzenia w systemowym oknie Androida. Działa tylko dla blokady urządzenia/profilu, a nie dla PIN-u wewnątrz tej aplikacji. Android lub ustawienia powiadomień mogą ograniczyć widoczność alertu.
- Automatyczne zdjęcia, nagrywanie wideo, pobieranie lokalizacji oraz wysyłanie e-mailem/na Google Drive nie są jeszcze zaimplementowane. Na Androidzie 14–16 dostęp do aparatu i lokalizacji w tle jest ograniczony; sama zgoda administratora nie omija tych zabezpieczeń. Wysyłka do Gmaila/Drive wymaga dodatkowo konfiguracji autoryzacji Google.
- Historia jest szyfrowana lokalnie; eksport CSV zapisuje czytelny plik w lokalizacji wybranej przez użytkownika.
- Domyślny PIN to `1234`. Zmień go przed użyciem.
- To debug APK do testów, nie wersja ze sklepu Google Play.

## Historia zmian

### 2.1
- Dodano wykrywanie nieudanych prób odblokowania urządzenia po włączeniu administratora (`watch-login`).
- Dodano wpis do historii i powiadomienie bezpieczeństwa po wykryciu błędnej próby.
- Aplikacja prosi o zgodę na powiadomienia w Androidzie 13 i nowszym.

### 2.0
- Dodano szyfrowaną lokalną historię zdarzeń.
- Dodano ekran diagnostyczny.
- Dodano eksport historii do CSV.
- Dodano zapis zdarzenia restartu systemu.
- Dodano opcjonalną aktywację administratora urządzenia Android.
- Zmieniono retencję nowych artefaktów buildów na 1 dzień.

### 1.1
- Przeprojektowany ciemny interfejs.
- Odliczanie przed uzbrojeniem.
- Regulowana czułość czujnika.
- Oddzielne przełączniki dźwięku i wibracji.
- PIN i zapis ustawień na urządzeniu.
