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

data class Tipp(
    val meccs: String,
    val piac: String,
    val internet: String,
    val sajat: String,
    val egyezes: String,
    val pont: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SportTippApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportTippApp() {
    var keresve by remember { mutableStateOf(false) }
    var ful by remember { mutableIntStateOf(0) }

    val tippek = listOf(
        Tipp("Minta FC – Példa United", "Over 2,5 gól",
            "8/11 minta-forrás támogatja", "Saját modell: 74/100",
            "Erős egyezés", 82),
        Tipp("Teszt City – Demo Athletic", "Over 8,5 szöglet",
            "4/9 minta-forrás támogatja", "Saját modell: 81/100",
            "Saját modell erősebb", 76),
        Tipp("Sample SC – Prototype FC", "BTTS – Igen",
            "7/10 minta-forrás támogatja", "Saját modell: 52/100",
            "Ellentmondás – NO TIP", 41)
    )

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("SportTipp v0.1") }) }
        ) { pad ->
            Column(
                Modifier.padding(pad).padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Foci • API-kulcs nélküli első prototípus",
                    style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { keresve = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🔎 Mai tippek keresése") }

                if (!keresve) {
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            "Indíts tippkeresést. A v0.1 demonstrációs adatokkal mutatja be " +
                            "a saját elemzés, az internetes tippek és az egyezések szétválasztását.",
                            Modifier.padding(16.dp)
                        )
                    }
                } else {
                    TabRow(selectedTabIndex = ful) {
                        listOf("🌐 Internet", "🧠 Saját", "🤝 Egyezés").forEachIndexed { i, t ->
                            Tab(selected = ful == i, onClick = { ful = i }, text = { Text(t) })
                        }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(tippek) { t ->
                            TippKartya(t, ful)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TippKartya(t: Tipp, ful: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(t.meccs, style = MaterialTheme.typography.titleMedium)
            Text(t.piac, style = MaterialTheme.typography.titleSmall)
            when (ful) {
                0 -> Text("🌐 ${t.internet}")
                1 -> Text("🧠 ${t.sajat}")
                else -> {
                    Text("🤝 ${t.egyezes}")
                    Text("Végső Tipp Score: ${t.pont}/100")
                }
            }
            if (t.pont < 50) {
                Text("⚠️ NO TIP – nincs elég erős megerősítés")
            }
        }
    }
}
