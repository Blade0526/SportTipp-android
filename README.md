# SportTipp Android v0.1

Első működő, API-kulcs nélküli prototípus.

## Cél
A SportTipp nem fogad automatikusan és nem kezel pénzt. A felhasználó kézzel indít egy napi
tippkeresést, az alkalmazás pedig elkülönítve jeleníti meg:

- 🌐 Internetes tippek
- 🧠 Saját elemzés
- 🤝 Egyezések
- 📜 Napló

## v0.1 működés
A projekt már lefordítható Android alkalmazássá. A Forrás Radar adapteres felépítésű.
Az első adapterek nyilvános weboldalakra vannak előkészítve, de a parser csak olyan
oldalhoz aktiválandó, amelynek automatizált lekérése az adott oldal feltételeivel összeegyeztethető.

A prototípus ezért demonstrációs mintákkal is működik: ezzel az UI, az elkülönítés,
a pontozás, az egyezésmotor és a napló már tesztelhető anélkül, hogy hamis "élő" adatnak
állítanánk be mintákat.

## Következő lépés
1. ellenőrzött nyilvános forrásadapterek bekötése;
2. meccsazonosítás és csapatnév-normalizálás;
3. történelmi/H2H adatgyűjtés;
4. piacspecifikus saját modellek;
5. eredmény-visszamérés és kalibráció.
