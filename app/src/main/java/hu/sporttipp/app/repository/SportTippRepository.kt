package hu.sporttipp.app.repository

import hu.sporttipp.app.engine.*
import hu.sporttipp.app.model.*

class SportTippRepository {

    private val naplo = mutableListOf<TippNaplo>()

    private val modellek = listOf(
        EredmenyModell(),
        GolModell(),
        BttsModell(),
        SzogletModell(),
        LapModell()
    )

    private fun demoMeccsek() = listOf(
        Meccs(
            "demo1",
            "Demó Liga",
            "19:00",
            "Minta FC",
            "Példa United",
            Forma("Minta FC",82,76,84,1.9,0.9,5.8,2.1),
            Forma("Példa United",61,65,58,1.4,1.5,4.7,2.5),
            H2H(10,2.9,0.70),
            AdatMinoseg.JO
        ),
        Meccs(
            "demo2",
            "Demó Liga",
            "21:00",
            "Teszt City",
            "Demo Athletic",
            Forma("Teszt City",69,71,77,1.6,1.1,6.1,2.8),
            Forma("Demo Athletic",67,64,60,1.5,1.3,5.5,3.0),
            H2H(8,2.5,0.63),
            AdatMinoseg.KOZEPES
        )
    )

    private fun demoKulsoTippek() = listOf(
        KulsoTipp("Demó A",Piac.GOL,"Over 2,5",0.72,"A"),
        KulsoTipp("Demó B",Piac.GOL,"Over 2,5",0.66,"B"),
        KulsoTipp("Demó A",Piac.BTTS,"Igen",0.70,"A"),
        KulsoTipp("Demó C",Piac.BTTS,"Igen",0.64,"C")
    )

    fun keres(): List<VegsoTipp> {
        val eredmenyek = mutableListOf<VegsoTipp>()

        for (meccs in demoMeccsek()) {

            // A saját modell eredménye előbb készül el.
            // A külső tippek nem módosítják visszamenőleg.
            val sajatPredikciok =
                modellek.map { it.elemez(meccs) }

            val kulso = demoKulsoTippek()

            for (modell in sajatPredikciok) {

                val konszenzus =
                    KonszenzusMotor.szamol(modell, kulso)

                // Ellenőrzött élő odds hiányában nem találunk ki oddsot.
                val odds =
                    ValueMotor.szamol(modell.valoszinuseg, null)

                val gate =
                    TippGate.dont(meccs, modell, konszenzus, odds)

                val score =
                    ScoreMotor.score(meccs, modell, konszenzus, odds)

                eredmenyek += VegsoTipp(
                    meccs,
                    modell,
                    konszenzus,
                    odds,
                    score,
                    gate.first,
                    gate.second
                )

                naplo += TippNaplo(
                    meccs.id,
                    System.currentTimeMillis(),
                    modell.piac.cimke,
                    modell.tipp,
                    modell.valoszinuseg,
                    score,
                    gate.first
                )
            }
        }

        return eredmenyek.sortedWith(
            compareBy<VegsoTipp> { it.noTip }
                .thenByDescending { it.score }
        )
    }

    fun naplo(): List<TippNaplo> = naplo.toList()
}
