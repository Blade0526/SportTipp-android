package hu.sporttipp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hu.sporttipp.app.data.ForrasAllapot
import hu.sporttipp.app.model.*
import hu.sporttipp.app.repository.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState) {
            SportTippApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportTippApp() {

    val repo =
        remember {
            SportTippRepository()
        }

    val scope =
        rememberCoroutineScope()

    var tippek by remember {
        mutableStateOf<List<VegsoTipp>>(
            emptyList()
        )
    }

    var forrasok by remember {
        mutableStateOf<List<ForrasAllapot>>(
            emptyList()
        )
    }

    var betolt by remember {
        mutableStateOf(false)
    }

    var uzenet by remember {
        mutableStateOf(
            "Nyomd meg a Tippkeresés gombot."
        )
    }

    var oldal by remember {
        mutableIntStateOf(0)
    }

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {

        Scaffold(

            topBar = {
                TopAppBar(
                    title = {
                        Text("SportTipp v2")
                    }
                )
            },

            bottomBar = {

                NavigationBar {

                    val nevek =
                        listOf(
                            "TOP",
                            "Meccsek",
                            "Források",
                            "Napló"
                        )

                    val ikonok =
                        listOf(
                            "⭐",
                            "⚽",
                            "🌐",
                            "📜"
                        )

                    nevek.forEachIndexed {
                        index,
                        nev ->

                        NavigationBarItem(
                            selected =
                                oldal == index,

                            onClick = {
                                oldal = index
                            },

                            icon = {
                                Text(
                                    ikonok[index]
                                )
                            },

                            label = {
                                Text(nev)
                            }
                        )
                    }
                }
            }

        ) { padding ->

            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(12.dp)
                    .fillMaxSize(),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    "Nyílt forrású sportelemző rendszer",
                    style =
                        MaterialTheme.typography.bodySmall
                )

                Button(
                    enabled = !betolt,

                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {

                        betolt = true
                        uzenet =
                            "Forrás Radar fut..."

                        scope.launch {

                            try {

                                val eredmeny =
                                    repo.keres()

                                tippek =
                                    eredmeny.tippek

                                forrasok =
                                    eredmeny.forrasok

                                uzenet =
                                    "Források: " +
                                    "${eredmeny.forrasok.count { it.sikeres }}/" +
                                    "${eredmeny.forrasok.size} • " +
                                    "összes rekord: " +
                                    "${eredmeny.osszesMeccs} • " +
                                    "mai meccsek: " +
                                    "${eredmeny.maiMeccsek} • " +
                                    "elemzések: " +
                                    "${eredmeny.tippek.size}"

                            } catch (
                                e: Exception
                            ) {

                                uzenet =
                                    "Adatgyűjtési hiba: " +
                                    (
                                        e.message
                                            ?: "ismeretlen hiba"
                                    )
                            }

                            betolt = false
                        }
                    }
                ) {

                    Text(
                        if (betolt)
                            "⏳ Adatgyűjtés..."
                        else
                            "🔎 Tippkeresés indítása"
                    )
                }

                Card(
                    Modifier.fillMaxWidth()
                ) {

                    Text(
                        uzenet,
                        Modifier.padding(12.dp)
                    )
                }

                when (oldal) {

                    0 ->
                        TippLista(
                            tippek.filter {
                                !it.noTip
                            }
                        )

                    1 ->
                        TippLista(tippek)

                    2 ->
                        ForrasLista(forrasok)

                    else ->
                        NaploLista(
                            repo.naplo()
                        )
                }
            }
        }
    }
}

@Composable
fun TippLista(
    tippek: List<VegsoTipp>
) {

    if (tippek.isEmpty()) {

        Card(
            Modifier.fillMaxWidth()
        ) {

            Text(
                "Nincs megjeleníthető tipp. " +
                "Ez jelentheti azt is, hogy " +
                "nincs elegendő adat vagy " +
                "egyetlen lehetőség sem ment át " +
                "a Tipp Gate-en.",

                Modifier.padding(16.dp)
            )
        }

        return
    }

    LazyColumn(
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        items(tippek) { tipp ->

            Card(
                Modifier.fillMaxWidth()
            ) {

                Column(
                    Modifier.padding(14.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        "${tipp.meccs.hazai} – " +
                        tipp.meccs.vendeg,

                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )

                    Text(
                        "${tipp.meccs.liga} • " +
                        tipp.meccs.kezdes
                    )

                    HorizontalDivider()

                    Text(
                        "${tipp.modell.piac.cimke}: " +
                        tipp.modell.tipp
                    )

                    Text(
                        "Saját modell: " +
                        "${(tipp.modell.valoszinuseg * 100).toInt()}%"
                    )

                    Text(
                        "Adatminőség: " +
                        tipp.meccs.adatMinoseg
                    )

                    Text(
                        "Final Tipp Score: " +
                        "${tipp.score}/100"
                    )

                    Text(
                        if (tipp.odds.edge == null)
                            "Value: nincs ellenőrzött odds"
                        else
                            "Becsült edge: " +
                            "${(tipp.odds.edge * 100).toInt()}%"
                    )

                    Text(
                        if (tipp.noTip)
                            "⚠️ NO TIP – " +
                            tipp.gateIndok
                        else
                            "✅ " +
                            tipp.gateIndok
                    )

                    tipp.modell.indokok
                        .forEach {

                            Text(
                                "• $it",

                                style =
                                    MaterialTheme.typography
                                        .bodySmall
                            )
                        }
                }
            }
        }
    }
}

@Composable
fun ForrasLista(
    forrasok: List<ForrasAllapot>
) {

    if (forrasok.isEmpty()) {

        Text(
            "A Forrás Radar még nem futott."
        )

        return
    }

    LazyColumn(
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        items(forrasok) { f ->

            Card(
                Modifier.fillMaxWidth()
            ) {

                Column(
                    Modifier.padding(12.dp)
                ) {

                    Text(
                        if (f.sikeres)
                            "✅ ${f.nev}"
                        else
                            "❌ ${f.nev}"
                    )

                    Text(
                        if (f.sikeres)
                            "${f.rekordok} rekord"
                        else
                            f.uzenet,

                        style =
                            MaterialTheme.typography
                                .bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun NaploLista(
    naplo: List<TippNaplo>
) {

    LazyColumn(
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        items(
            naplo.reversed()
        ) { elem ->

            Card(
                Modifier.fillMaxWidth()
            ) {

                Text(
                    "${elem.piac}: " +
                    "${elem.tipp} • " +
                    "Score ${elem.score} • " +
                    if (elem.noTip)
                        "NO TIP"
                    else
                        "elfogadva",

                    Modifier.padding(12.dp)
                )
            }
        }
    }
}
