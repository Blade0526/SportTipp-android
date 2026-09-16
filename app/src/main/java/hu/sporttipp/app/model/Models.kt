package hu.sporttipp.app.model

enum class Piac(val cimke: String) {
    EREDMENY("1X2"),
    DUPLA("Dupla esély"),
    GOL("Gólok"),
    BTTS("Mindkét csapat szerez gólt"),
    SZOGLET("Szögletek"),
    LAP("Lapok"),
    FELIDO("Félidő"),
    CSAPATGOL("Csapatgól"),
    HANDICAP("Hendikep")
}

enum class AdatMinoseg { GYENGE, KOZEPES, JO, KIVALO }

data class Forma(
    val csapat: String,
    val forma5: Int,
    val forma10: Int,
    val hazaiIdegen: Int,
    val gol: Double,
    val kapottGol: Double,
    val szoglet: Double,
    val lap: Double
)

data class H2H(
    val meccsek: Int,
    val atlagGol: Double,
    val bttsArany: Double
)

data class Meccs(
    val id: String,
    val liga: String,
    val kezdes: String,
    val hazai: String,
    val vendeg: String,
    val hazaiForma: Forma,
    val vendegForma: Forma,
    val h2h: H2H,
    val adatMinoseg: AdatMinoseg
)

data class ModellTipp(
    val piac: Piac,
    val tipp: String,
    val valoszinuseg: Double,
    val indokok: List<String>
)

data class KulsoTipp(
    val forras: String,
    val piac: Piac,
    val tipp: String,
    val megbizhatosag: Double,
    val fuggetlensegiCsoport: String
)

data class Konszenzus(
    val tamogatoForrasok: Int,
    val osszesForras: Int,
    val sulyozottTamogatas: Double,
    val erosEllentmondas: Boolean
)

data class OddsAdat(
    val odds: Double?,
    val impliedProbability: Double?,
    val edge: Double?
)

data class VegsoTipp(
    val meccs: Meccs,
    val modell: ModellTipp,
    val konszenzus: Konszenzus,
    val odds: OddsAdat,
    val score: Int,
    val noTip: Boolean,
    val gateIndok: String
)

data class TippNaplo(
    val meccsId: String,
    val idopont: Long,
    val piac: String,
    val tipp: String,
    val valoszinuseg: Double,
    val score: Int,
    val noTip: Boolean,
    val eredmeny: Boolean? = null
)
