# SportTipp v1 architektúra

## Folyamat
Napi meccs radar
→ nyilvános webes forrásadapterek
→ történelmi/H2H adatok
→ aktuális forma
→ piacspecifikus saját modellek
→ saját vélemény rögzítése
→ külső tippek konszenzusa
→ ellentmondásvizsgálat
→ Tipp Gate
→ végső megjelenítés
→ napló
→ meccs utáni kiértékelés

## Kötelező elkülönítés
A külső tippek nem lehetnek a saját modell bemenetei.
A saját modell eredményét előbb kell rögzíteni, és csak utána történhet összevetés.

## Piacok
- 1X2 / dupla esély
- Over/Under gól
- BTTS
- szöglet
- lap

## Forrás Radar
Minden weboldal külön adapter. Egy adapter hibája nem állíthatja le a keresést.
CAPTCHA, bejelentkezés, fizetőfal vagy technikai hozzáférés-védelem megkerülése nincs.

## Tipp Gate
A v1-ben csak megfelelő adatminőség és jel-erősség mellett legyen saját tipp.
Ellenkező esetben NO TIP.

## Visszamérés
Külön mérjük:
- saját modell
- külső források
- külső konszenzus
- saját + külső egyezés
- piac és liga szerinti teljesítmény
