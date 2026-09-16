package hu.sporttipp.app.repository

import hu.sporttipp.app.data.*
import hu.sporttipp.app.engine.*
import hu.sporttipp.app.model.*
import java.time.LocalDate

data class KeresesiEredmeny(
    val tippek: List<VegsoTipp>,
    val forrasok: List<ForrasAllapot>,
    val osszesMeccs: Int,
    val maiMeccsek: Int
)

class SportTippRepository {

    private val naplo = mutableListOf<TippNaplo>()

    private val modellek = listOf(
        EredmenyModell(),
        GolModell(),
        BttsModell(),
        SzogletModell(),
        LapModell()
    )

    suspend fun keres(): KeresesiEredmeny {

        val radar = ForrasRadar.keres()
        val ma = LocalDate.now().toString()

        val lezart = radar.meccsek.filter {
            it.hazaiGol != null &&
            it.vendegGol != null &&
            it.datum < ma
        }

        val mai = radar.meccsek.filter {
            it.datum == ma &&
            (it.hazaiGol == null || it.vendegGol == null)
        }

        val tippek = mutableListOf<VegsoTipp>()

        for (nyers in mai) {

            val meccs = felepitMeccs(nyers, lezart)
                ?: continue

            val sajatPredikciok =
                modellek.map { it.elemez(meccs) }

            for (modell in sajatPredikciok) {

                /*
                 * Külső prediction adapterek külön rétegben érkeznek.
                 * Amíg nincs ellenőrzött külső adat, üres konszenzust
                 * használunk. Nem találunk ki külső tippeket.
                 */
                val konszenzus =
                    KonszenzusMotor.szamol(
                        modell,
                        emptyList()
                    )

                /*
                 * Ellenőrzött odds nélkül nincs kitalált value.
                 */
                val odds =
                    ValueMotor.szamol(
                        modell.valoszinuseg,
                        null
                    )

                val gate =
                    TippGate.dont(
                        meccs,
                        modell,
                        konszenzus,
                        odds
                    )

                val score =
                    ScoreMotor.score(
                        meccs,
                        modell,
                        konszenzus,
                        odds
                    )

                val vegso = VegsoTipp(
                    meccs = meccs,
                    modell = modell,
                    konszenzus = konszenzus,
                    odds = odds,
                    score = score,
                    noTip = gate.first,
                    gateIndok = gate.second
                )

                tippek += vegso

                naplo += TippNaplo(
                    meccsId = meccs.id,
                    idopont = System.currentTimeMillis(),
                    piac = modell.piac.cimke,
                    tipp = modell.tipp,
                    valoszinuseg = modell.valoszinuseg,
                    score = score,
                    noTip = gate.first
                )
            }
        }

        return KeresesiEredmeny(
            tippek = tippek.sortedWith(
                compareBy<VegsoTipp> { it.noTip }
                    .thenByDescending { it.score }
            ),
            forrasok = radar.forrasok,
            osszesMeccs = radar.meccsek.size,
            maiMeccsek = mai.size
        )
    }

    fun naplo(): List<TippNaplo> =
        naplo.toList()

    private fun felepitMeccs(
        aktualis: NyersMeccs,
        tortenet: List<NyersMeccs>
    ): Meccs? {

        val hazaiMult =
            csapatMeccsek(
                aktualis.hazai,
                tortenet
            )

        val vendegMult =
            csapatMeccsek(
                aktualis.vendeg,
                tortenet
            )

        if (hazaiMult.size < 3 ||
            vendegMult.size < 3) {
            return null
        }

        val hazaiForma =
            forma(
                aktualis.hazai,
                hazaiMult,
                true
            )

        val vendegForma =
            forma(
                aktualis.vendeg,
                vendegMult,
                false
            )

        val h2h =
            h2h(
                aktualis.hazai,
                aktualis.vendeg,
                tortenet
            )

        val minoseg =
            adatMinoseg(
                hazaiMult.size,
                vendegMult.size,
                h2h.meccsek
            )

        return Meccs(
            id =
                "${aktualis.datum}_" +
                normalizal(aktualis.hazai) +
                "_" +
                normalizal(aktualis.vendeg),

            liga = aktualis.liga,
            kezdes = aktualis.datum,
            hazai = aktualis.hazai,
            vendeg = aktualis.vendeg,
            hazaiForma = hazaiForma,
            vendegForma = vendegForma,
            h2h = h2h,
            adatMinoseg = minoseg
        )
    }

    private fun csapatMeccsek(
        csapat: String,
        lista: List<NyersMeccs>
    ): List<NyersMeccs> {

        val n = normalizal(csapat)

        return lista
            .filter {
                normalizal(it.hazai) == n ||
                normalizal(it.vendeg) == n
            }
            .sortedByDescending { it.datum }
    }

    private fun forma(
        csapat: String,
        meccsek: List<NyersMeccs>,
        hazaiBontas: Boolean
    ): Forma {

        val utolso5 = meccsek.take(5)
        val utolso10 = meccsek.take(10)

        val pont5 =
            formaPont(csapat, utolso5)

        val pont10 =
            formaPont(csapat, utolso10)

        val bontott =
            meccsek.filter {
                if (hazaiBontas)
                    normalizal(it.hazai) ==
                        normalizal(csapat)
                else
                    normalizal(it.vendeg) ==
                        normalizal(csapat)
            }.take(10)

        val bontottPont =
            formaPont(csapat, bontott)

        val golok =
            utolso10.map {
                csapatGol(csapat, it)
            }

        val kapott =
            utolso10.map {
                ellenfelGol(csapat, it)
            }

        return Forma(
            csapat = csapat,

            forma5 =
                normalizaltForma(
                    pont5,
                    utolso5.size
                ),

            forma10 =
                normalizaltForma(
                    pont10,
                    utolso10.size
                ),

            hazaiIdegen =
                normalizaltForma(
                    bontottPont,
                    bontott.size
                ),

            gol =
                if (golok.isEmpty())
                    0.0
                else golok.average(),

            kapottGol =
                if (kapott.isEmpty())
                    0.0
                else kapott.average(),

            /*
             * Ezekhez külön részletes statisztikai
             * adapter szükséges. Nulla helyett később
             * nullable adatmodellre állítjuk át.
             */
            szoglet = 0.0,
            lap = 0.0
        )
    }

    private fun formaPont(
        csapat: String,
        lista: List<NyersMeccs>
    ): Int {

        return lista.sumOf { m ->

            val sajat =
                csapatGol(csapat, m)

            val ellenfel =
                ellenfelGol(csapat, m)

            when {
                sajat > ellenfel -> 3
                sajat == ellenfel -> 1
                else -> 0
            }
        }
    }

    private fun normalizaltForma(
        pont: Int,
        db: Int
    ): Int {

        if (db == 0) return 0

        return (
            pont.toDouble() /
            (db * 3.0) *
            100.0
        ).toInt()
    }

    private fun csapatGol(
        csapat: String,
        m: NyersMeccs
    ): Double {

        return if (
            normalizal(m.hazai) ==
            normalizal(csapat)
        ) {
            (m.hazaiGol ?: 0).toDouble()
        } else {
            (m.vendegGol ?: 0).toDouble()
        }
    }

    private fun ellenfelGol(
        csapat: String,
        m: NyersMeccs
    ): Double {

        return if (
            normalizal(m.hazai) ==
            normalizal(csapat)
        ) {
            (m.vendegGol ?: 0).toDouble()
        } else {
            (m.hazaiGol ?: 0).toDouble()
        }
    }

    private fun h2h(
        hazai: String,
        vendeg: String,
        lista: List<NyersMeccs>
    ): H2H {

        val h = normalizal(hazai)
        val v = normalizal(vendeg)

        val meccsek =
            lista.filter {

                val a =
                    normalizal(it.hazai)

                val b =
                    normalizal(it.vendeg)

                (a == h && b == v) ||
                (a == v && b == h)

            }.sortedByDescending {
                it.datum
            }.take(10)

        if (meccsek.isEmpty()) {
            return H2H(
                meccsek = 0,
                atlagGol = 0.0,
                bttsArany = 0.0
            )
        }

        val golAtlag =
            meccsek.map {
                (it.hazaiGol ?: 0) +
                (it.vendegGol ?: 0)
            }.average()

        val btts =
            meccsek.count {
                (it.hazaiGol ?: 0) > 0 &&
                (it.vendegGol ?: 0) > 0
            }.toDouble() /
            meccsek.size.toDouble()

        return H2H(
            meccsek = meccsek.size,
            atlagGol = golAtlag,
            bttsArany = btts
        )
    }

    private fun adatMinoseg(
        h: Int,
        v: Int,
        h2h: Int
    ): AdatMinoseg {

        return when {

            h >= 10 &&
            v >= 10 &&
            h2h >= 5 ->
                AdatMinoseg.KIVALO

            h >= 10 &&
            v >= 10 ->
                AdatMinoseg.JO

            h >= 5 &&
            v >= 5 ->
                AdatMinoseg.KOZEPES

            else ->
                AdatMinoseg.GYENGE
        }
    }

    private fun normalizal(
        x: String
    ): String =
        x.lowercase()
            .replace(" fc", "")
            .replace(" afc", "")
            .replace(" cf", "")
            .replace(" ", "")
            .replace("-", "")
            .replace(".", "")
            .trim()
}
