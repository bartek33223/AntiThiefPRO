# AntiThiefPRO 5.0

AntiThiefPRO to aplikacja Android do rejestrowania wybranych zdarzeń związanych z bezpieczeństwem telefonu. Projekt buduje testowy plik APK przez GitHub Actions.

## Co jest obecnie zaimplementowane

- Odświeżony interfejs z ekranem głównym, aparatem, historią, ochroną i ustawieniami.
- Ręczne robienie zdjęć aparatem aplikacji i zapisywanie ich w galerii, w folderze Pictures/AntiThiefPRO.
- Odbieranie przez administratora urządzenia zdarzeń o nieudanych próbach odblokowania, o ile Android je dostarczy.
- Lokalny dziennik zdarzeń z limitem 200 wpisów.
- Szyfrowanie dziennika przez AES-GCM z kluczem przechowywanym w Android Keystore.
- Informacja o stanie administratora urządzenia i uprawnienia do aparatu.

## Ważne ograniczenia

- Automatyczne robienie zdjęcia po błędnym PIN-ie na systemowym ekranie blokady nie jest w tej wersji zaimplementowane. Otrzymanie zdarzenia administratora urządzenia nie daje automatycznie prawa do użycia aparatu w tle. Zdjęcie ręczne działa, gdy aplikacja jest otwarta i ma zgodę na aparat.
- Nie ma obecnie wysyłania zdjęć ani lokalizacji na zewnętrzny serwer. Nie ma też działającego alarmu ruchowego z poprzednich wersji opisanych w starym README.
- To kompilacja debug do testów, a nie wydanie sklepowe.

## Budowanie APK

Każdy push do gałęzi main uruchamia workflow GitHub Actions. Po udanym buildzie otwórz kartę Actions, wybierz najnowsze udane uruchomienie i pobierz artefakt AntiThiefPRO-v5-debug. Rozpakuj pobrany ZIP, aby uzyskać APK.

## Narzędzia kompilacji

Workflow korzysta ze stabilnych wydań akcji GitHub działających na Node.js 24, Gradle 9.7.1, Android Gradle Plugin 9.3.3, Kotlin 2.4.21, Activity 1.13.0, Core 1.19.1 i CameraX 1.6.2. Kompilacja używa JDK 17, Android SDK API 36 i celu aplikacji API 36.
