# SportTipp v1 – integrált architektúra

Meccs Radar → történelmi/H2H → aktuális forma → piacspecifikus saját modellek → saját predikció lezárása → Forrás Radar → konszenzus/ellentmondás → odds/value → Tipp Gate → Final Tipp Score → napló → visszamérés.

## Alapszabályok

- A NO TIP teljes értékű eredmény.
- A frissebb adatok nagyobb súlyt kapnak.
- A külső tippek nem lehetnek a saját modell bemenetei.
- Az egymástól függő forrásokat nem kezeljük független szavazatként.
- A valószínűség és a value külön fogalom.
- Ellenőrzött odds nélkül nincs kitalált value.
- CAPTCHA, belépés, fizetőfal vagy technikai védelem megkerülése nincs.

A v1 integrált offline mag 1X2, gól, BTTS, szöglet és lap modelleket tartalmaz.
Az adatmodell előkészíti a dupla esély, félidő, csapatgól és hendikep későbbi modelljeit.
