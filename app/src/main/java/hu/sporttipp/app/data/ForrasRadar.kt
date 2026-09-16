package hu.sporttipp.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class NyersMeccs(
    val datum: String,
    val liga: String,
    val hazai: String,
    val vendeg: String,
    val hazaiGol: Int? = null,
    val vendegGol: Int? = null,
    val forras: String
)

data class ForrasAllapot(
    val nev: String,
    val sikeres: Boolean,
    val rekordok: Int,
    val uzenet: String
)

data class RadarEredmeny(
    val meccsek: List<NyersMeccs>,
    val forrasok: List<ForrasAllapot>
)

interface NyiltForrasAdapter {
    val nev: String
    suspend fun betolt(): List<NyersMeccs>
}

private object Halozat {
    suspend fun get(url: String): String =
        withContext(Dispatchers.IO) {
            val kapcsolat =
                URL(url).openConnection() as HttpURLConnection

            try {
                kapcsolat.connectTimeout = 12000
                kapcsolat.readTimeout = 18000
                kapcsolat.requestMethod = "GET"
                kapcsolat.setRequestProperty(
                    "User-Agent",
                    "SportTipp/2.0 Android"
                )

                val kod = kapcsolat.responseCode

                if (kod !in 200..299) {
                    error("HTTP $kod")
                }

                kapcsolat.inputStream
                    .bufferedReader()
                    .use { it.readText() }

            } finally {
                kapcsolat.disconnect()
            }
        }
}

class FootballDataAdapter(
    private val kod: String,
    private val liga: String
) : NyiltForrasAdapter {

    override val nev = "Football-Data / $liga"

    override suspend fun betolt(): List<NyersMeccs> {

        val url =
            "https://www.football-data.co.uk/mmz4281/2627/$kod.csv"

        val text = Halozat.get(url)

        val sorok =
            text.lines().filter { it.isNotBlank() }

        if (sorok.size < 2) {
            return emptyList()
        }

        val fejlec = csv(sorok.first())

        val date = fejlec.indexOf("Date")
        val home = fejlec.indexOf("HomeTeam")
        val away = fejlec.indexOf("AwayTeam")
        val hg = fejlec.indexOf("FTHG")
        val ag = fejlec.indexOf("FTAG")

        if (date < 0 || home < 0 || away < 0) {
            return emptyList()
        }

        return sorok.drop(1).mapNotNull { sor ->

            val c = csv(sor)

            if (c.size <= maxOf(date, home, away)) {
                return@mapNotNull null
            }

            val datum =
                datumIso(c[date])
                    ?: return@mapNotNull null

            NyersMeccs(
                datum = datum,
                liga = liga,
                hazai = c[home],
                vendeg = c[away],
                hazaiGol =
                    if (hg >= 0 && hg < c.size)
                        c[hg].toIntOrNull()
                    else null,
                vendegGol =
                    if (ag >= 0 && ag < c.size)
                        c[ag].toIntOrNull()
                    else null,
                forras = nev
            )
        }
    }

    private fun datumIso(value: String): String? {
        val raw = value.trim()
        if (raw.isBlank()) return null

        val tiszta = raw
            .substringBefore("T")
            .substringBefore(" ")
            .trim()

        val mintak = listOf(
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("d.M.yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("d-M-yyyy")
        )

        for (minta in mintak) {
            try {
                return java.time.LocalDate.parse(tiszta, minta).toString()
            } catch (_: Exception) {
            }
        }

        val szamok = Regex("""\d+""")
            .findAll(raw)
            .map { it.value.toIntOrNull() }
            .filterNotNull()
            .toList()

        if (szamok.size >= 3) {
            try {
                val (ev, honap, nap) =
                    if (szamok[0] >= 1900) {
                        Triple(szamok[0], szamok[1], szamok[2])
                    } else if (szamok[2] >= 1900) {
                        Triple(szamok[2], szamok[1], szamok[0])
                    } else {
                        return null
                    }

                return java.time.LocalDate.of(ev, honap, nap).toString()
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun csv(line: String): List<String> {

        val eredmeny = mutableListOf<String>()
        val aktualis = StringBuilder()

        var idezet = false
        var i = 0

        while (i < line.length) {

            val ch = line[i]

            when {

                ch == '"' -> {

                    if (
                        idezet &&
                        i + 1 < line.length &&
                        line[i + 1] == '"'
                    ) {
                        aktualis.append('"')
                        i++
                    } else {
                        idezet = !idezet
                    }
                }

                ch == ',' && !idezet -> {
                    eredmeny += aktualis.toString()
                    aktualis.clear()
                }

                else -> aktualis.append(ch)
            }

            i++
        }

        eredmeny += aktualis.toString()

        return eredmeny
    }
}

object ForrasRadar {

    private val adapterek =
        listOf<NyiltForrasAdapter>(

            FootballDataAdapter(
                "E0",
                "Premier League"
            ),

            FootballDataAdapter(
                "D1",
                "Bundesliga"
            ),

            FootballDataAdapter(
                "SP1",
                "La Liga"
            ),

            FootballDataAdapter(
                "I1",
                "Serie A"
            ),

            FootballDataAdapter(
                "F1",
                "Ligue 1"
            ),

            FootballDataAdapter(
                "N1",
                "Eredivisie"
            ),

            FootballDataAdapter(
                "P1",
                "Primeira Liga"
            ),

            FootballDataAdapter(
                "B1",
                "Belga élvonal"
            )
        )

    suspend fun keres(): RadarEredmeny =
        withContext(Dispatchers.IO) {

            val meccsek =
                mutableListOf<NyersMeccs>()

            val allapot =
                mutableListOf<ForrasAllapot>()

            adapterek.forEach { adapter ->

                try {

                    val adatok =
                        adapter.betolt()

                    meccsek += adatok

                    allapot += ForrasAllapot(
                        nev = adapter.nev,
                        sikeres = true,
                        rekordok = adatok.size,
                        uzenet = "OK"
                    )

                } catch (e: Exception) {

                    allapot += ForrasAllapot(
                        nev = adapter.nev,
                        sikeres = false,
                        rekordok = 0,
                        uzenet =
                            e.message ?: "Ismeretlen hiba"
                    )
                }
            }

            RadarEredmeny(
                meccsek = egyesit(meccsek),
                forrasok = allapot
            )
        }

    private fun normalizal(x: String): String =
        x.lowercase()
            .replace(" ", "")
            .replace("-", "")
            .replace(".", "")
            .trim()

    private fun egyesit(
        lista: List<NyersMeccs>
    ): List<NyersMeccs> {

        return lista
            .groupBy {
                "${it.datum}|" +
                normalizal(it.hazai) +
                "|" +
                normalizal(it.vendeg)
            }
            .map { (_, lista) ->

                val alap = lista.first()

                alap.copy(
                    forras =
                        lista.map { it.forras }
                            .distinct()
                            .joinToString(" + ")
                )
            }
            .sortedWith(
                compareBy<NyersMeccs> { it.datum }
                    .thenBy { it.liga }
            )
    }
}
