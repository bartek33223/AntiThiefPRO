# AntiThiefPRO 2.0

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
- Opcjonalna, standardowa zgoda administratora urządzenia Android. Nie gwarantuje ona blokady odinstalowania: administratora można wyłączyć w ustawieniach systemu.
- Brak reklam, analityki i wysyłania danych do zewnętrznych serwerów.

## Budowanie APK

Push do gałęzi `main` automatycznie uruchamia GitHub Actions. Po udanym buildzie otwórz kartę **Actions**, wybierz najnowsze udane uruchomienie i pobierz artefakt `AntiThiefPRO-debug`. Rozpakuj ZIP, aby uzyskać APK.

Artefakty kolejnych buildów są teraz przechowywane przez 1 dzień.

## Ograniczenia i prywatność

- Alarm ruchowy działa tylko, gdy aplikacja pozostaje otwarta. Ta wersja nie utrzymuje stałego monitorowania ruchu w tle.
- Android nie udostępnia zwykłym aplikacjom uniwersalnego dostępu do każdej nieudanej próby wpisania kodu blokady.
- Automatyczne fotografowanie, nagrywanie i wysyłanie lokalizacji nie są włączone.
- Historia jest szyfrowana lokalnie; eksport CSV zapisuje czytelny plik w lokalizacji wybranej przez użytkownika.
- Domyślny PIN to `1234`. Zmień go przed użyciem.
- To debug APK do testów, nie wersja ze sklepu Google Play.

## Historia zmian

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
