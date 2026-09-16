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
import hu.sporttipp.app.model.*
import hu.sporttipp.app.repository.SportTippRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SportTippApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportTippApp() {

    val repo = remember { SportTippRepository() }

    var tippek by remember {
        mutableStateOf<List<VegsoTipp>>(emptyList())
    }

    var oldal by remember { mutableIntStateOf(0) }

    MaterialTheme(colorScheme = darkColorScheme()) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SportTipp v1.0") }
                )
            },

            bottomBar = {
                NavigationBar {
                    val nevek =
                        listOf("TOP tippek", "Mai meccsek", "Napló")

                    val ikonok =
                        listOf("⭐", "⚽", "📜")

                    nevek.forEachIndexed { index, nev ->
                        NavigationBarItem(
                            selected = oldal == index,
                            onClick = { oldal = index },
                            icon = { Text(ikonok[index]) },
                            label = { Text(nev) }
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
                    "Elemző alkalmazás • nem helyez el fogadást",
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = { tippek = repo.keres() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔎 Tippkeresés indítása")
                }

                when (oldal) {
                    0 -> TippLista(tippek.filter { !it.noTip })
                    1 -> TippLista(tippek)
                    else -> NaploLista(repo.naplo())
                }
            }
        }
    }
}

@Composable
fun TippLista(tippek: List<VegsoTipp>) {

    if (tippek.isEmpty()) {
        Card(Modifier.fillMaxWidth()) {
            Text(
                "Nincs megjeleníthető tipp. Indíts tippkeresést. " +
                "A NO TIP is érvényes eredmény.",
                Modifier.padding(16.dp)
            )
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        items(tippek) { tipp ->

            Card(Modifier.fillMaxWidth()) {

                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        "${tipp.meccs.hazai} – ${tipp.meccs.vendeg}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "${tipp.meccs.liga} • ${tipp.meccs.kezdes}"
                    )

                    HorizontalDivider()

                    Text(
                        "${tipp.modell.piac.cimke}: ${tipp.modell.tipp}"
                    )

                    Text(
                        "Saját modell: " +
                        "${(tipp.modell.valoszinuseg * 100).toInt()}%"
                    )

                    Text(
                        "Külső konszenzus: " +
                        "${tipp.konszenzus.tamogatoForrasok}/" +
                        "${tipp.konszenzus.osszesForras}"
                    )

                    Text(
                        "Adatminőség: ${tipp.meccs.adatMinoseg}"
                    )

                    Text(
                        "Végső Tipp Score: ${tipp.score}/100"
                    )

                    Text(
                        if (tipp.odds.edge == null)
                            "Odds/value: nincs ellenőrzött odds-adat"
                        else
                            "Becsült edge: " +
                            "${(tipp.odds.edge * 100).toInt()}%"
                    )

                    Text(
                        if (tipp.noTip)
                            "⚠️ NO TIP – ${tipp.gateIndok}"
                        else
                            "✅ ${tipp.gateIndok}"
                    )

                    tipp.modell.indokok.forEach {
                        Text(
                            "• $it",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NaploLista(naplo: List<TippNaplo>) {

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        items(naplo.reversed()) { elem ->

            Card(Modifier.fillMaxWidth()) {
                Text(
                    "${elem.piac}: ${elem.tipp} • " +
                    "Score ${elem.score} • " +
                    if (elem.noTip) "NO TIP" else "elfogadva",
                    Modifier.padding(12.dp)
                )
            }
        }
    }
}
