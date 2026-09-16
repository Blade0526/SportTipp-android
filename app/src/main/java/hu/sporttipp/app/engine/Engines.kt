package hu.sporttipp.app.engine

import hu.sporttipp.app.model.*
import kotlin.math.abs
import kotlin.math.roundToInt

interface PiacModell {
    val piac: Piac
    fun elemez(meccs: Meccs): ModellTipp
}

class EredmenyModell : PiacModell {
    override val piac = Piac.EREDMENY

    override fun elemez(meccs: Meccs): ModellTipp {
        val kulonbseg =
            meccs.hazaiForma.forma5 -
            meccs.vendegForma.forma5 +
            (meccs.hazaiForma.hazaiIdegen -
             meccs.vendegForma.hazaiIdegen) * 0.6

        val tipp = when {
            kulonbseg > 15 -> "1"
            kulonbseg < -15 -> "2"
            else -> "X"
        }

        val p = (0.45 + abs(kulonbseg) / 180.0).coerceAtMost(0.78)

        return ModellTipp(
            piac,
            tipp,
            p,
            listOf(
                "friss forma",
                "hazai/idegen teljesítmény",
                "H2H alacsonyabb súllyal"
            )
        )
    }
}

class GolModell : PiacModell {
    override val piac = Piac.GOL

    override fun elemez(meccs: Meccs): ModellTipp {
        val varhato =
            (meccs.hazaiForma.gol +
             meccs.vendegForma.gol +
             meccs.hazaiForma.kapottGol +
             meccs.vendegForma.kapottGol) / 2.0

        val p = (0.50 + abs(varhato - 2.5) * 0.12)
            .coerceIn(0.50, 0.82)

        return ModellTipp(
            piac,
            if (varhato >= 2.55) "Over 2,5" else "Under 2,5",
            p,
            listOf("góltrend", "kapott gól trend", "H2H")
        )
    }
}

class BttsModell : PiacModell {
    override val piac = Piac.BTTS

    override fun elemez(meccs: Meccs): ModellTipp {
        val igen =
            (meccs.h2h.bttsArany * 0.25 +
             ((meccs.hazaiForma.gol +
               meccs.vendegForma.gol) / 2.0) / 3.0 * 0.75)
                .coerceIn(0.35, 0.82)

        val tippIgen = igen >= 0.55

        return ModellTipp(
            piac,
            if (tippIgen) "Igen" else "Nem",
            if (tippIgen) igen else 1.0 - igen,
            listOf("aktuális támadótrend", "H2H BTTS")
        )
    }
}

class SzogletModell : PiacModell {
    override val piac = Piac.SZOGLET

    override fun elemez(meccs: Meccs): ModellTipp {
        val atlag =
            meccs.hazaiForma.szoglet +
            meccs.vendegForma.szoglet

        val p = (0.50 + abs(atlag - 9.5) * 0.045)
            .coerceIn(0.50, 0.80)

        return ModellTipp(
            piac,
            if (atlag >= 9.5) "Over 9,5" else "Under 9,5",
            p,
            listOf("csapatok szöglettrendje")
        )
    }
}

class LapModell : PiacModell {
    override val piac = Piac.LAP

    override fun elemez(meccs: Meccs): ModellTipp {
        val atlag =
            meccs.hazaiForma.lap +
            meccs.vendegForma.lap

        val p = (0.50 + abs(atlag - 4.5) * 0.055)
            .coerceIn(0.50, 0.80)

        return ModellTipp(
            piac,
            if (atlag >= 4.5) "Over 4,5" else "Under 4,5",
            p,
            listOf(
                "csapatok laptrendje",
                "játékvezető-adat később adapterből"
            )
        )
    }
}

object KonszenzusMotor {
    fun szamol(
        sajat: ModellTipp,
        kulso: List<KulsoTipp>
    ): Konszenzus {

        val relevans = kulso.filter { it.piac == sajat.piac }

        if (relevans.isEmpty()) {
            return Konszenzus(0, 0, 0.0, false)
        }

        val csoportok =
            relevans.groupBy { it.fuggetlensegiCsoport }

        var tamogatas = 0.0
        var osszesSuly = 0.0

        csoportok.values.forEach { csoport ->
            val suly = csoport.maxOf { it.megbizhatosag }
            osszesSuly += suly

            if (csoport.count { it.tipp == sajat.tipp } >=
                (csoport.size + 1) / 2) {
                tamogatas += suly
            }
        }

        val arany =
            if (osszesSuly == 0.0) 0.0
            else tamogatas / osszesSuly

        return Konszenzus(
            relevans.count { it.tipp == sajat.tipp },
            relevans.size,
            arany,
            arany < 0.35 && relevans.size >= 2
        )
    }
}

object ValueMotor {
    fun szamol(
        modellValoszinuseg: Double,
        odds: Double?
    ): OddsAdat {

        if (odds == null || odds <= 1.0) {
            return OddsAdat(null, null, null)
        }

        val implied = 1.0 / odds

        return OddsAdat(
            odds,
            implied,
            modellValoszinuseg - implied
        )
    }
}

object TippGate {
    fun dont(
        meccs: Meccs,
        modell: ModellTipp,
        konszenzus: Konszenzus,
        odds: OddsAdat
    ): Pair<Boolean, String> {

        if (meccs.adatMinoseg == AdatMinoseg.GYENGE)
            return true to "Gyenge adatminőség"

        if (modell.valoszinuseg < 0.58)
            return true to "Kevés modellbizonyosság"

        if (konszenzus.erosEllentmondas)
            return true to "Erős külső ellentmondás"

        if (odds.edge != null && odds.edge < 0.02)
            return true to "Nincs megfelelő becsült value"

        return false to "A Tipp Gate feltételei teljesültek"
    }
}

object ScoreMotor {
    fun score(
        meccs: Meccs,
        modell: ModellTipp,
        konszenzus: Konszenzus,
        odds: OddsAdat
    ): Int {

        val adat = when (meccs.adatMinoseg) {
            AdatMinoseg.KIVALO -> 100
            AdatMinoseg.JO -> 85
            AdatMinoseg.KOZEPES -> 68
            AdatMinoseg.GYENGE -> 40
        }

        val konszenzusPont =
            if (konszenzus.osszesForras == 0) 55.0
            else konszenzus.sulyozottTamogatas * 100.0

        val valuePont =
            odds.edge?.let {
                (50.0 + it * 250.0).coerceIn(0.0, 100.0)
            } ?: 55.0

        return (
            modell.valoszinuseg * 100.0 * 0.48 +
            adat * 0.22 +
            konszenzusPont * 0.18 +
            valuePont * 0.12
        ).roundToInt().coerceIn(0, 100)
    }
}
