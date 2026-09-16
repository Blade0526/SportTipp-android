#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "$HOME/SportTipp-android"
git branch "backup-v0.1-$(date +%Y%m%d-%H%M%S)" 2>/dev/null || true
BASE="app/src/main/java/hu/sporttipp/app"
mkdir -p "$BASE/model" "$BASE/engine" "$BASE/repository"

cat > "$BASE/model/Models.kt" <<'EOF'
package hu.sporttipp.app.model
enum class Piac(val cimke:String){EREDMENY("1X2"),DUPLA("Dupla esély"),GOL("Gólok"),BTTS("BTTS"),SZOGLET("Szögletek"),LAP("Lapok"),FELIDO("Félidő"),CSAPATGOL("Csapatgól"),HANDICAP("Hendikep")}
enum class AdatMinoseg{GYENGE,KOZEPES,JO,KIVALO}
data class Forma(val csapat:String,val f5:Int,val f10:Int,val hely:Int,val gol:Double,val kapott:Double,val szoglet:Double,val lap:Double)
data class H2H(val n:Int,val atlagGol:Double,val btts:Double)
data class Meccs(val id:String,val liga:String,val kezdes:String,val hazai:String,val vendeg:String,val h:Forma,val v:Forma,val h2h:H2H,val minoseg:AdatMinoseg)
data class ModellTipp(val piac:Piac,val tipp:String,val p:Double,val indok:List<String>)
data class KulsoTipp(val forras:String,val piac:Piac,val tipp:String,val megbizhatosag:Double,val fuggetlenseg:String)
data class Konszenzus(val tamogat:Int,val osszes:Int,val suly:Double,val ellentmondas:Boolean)
data class OddsAdat(val odds:Double?,val implied:Double?,val edge:Double?)
data class VegsoTipp(val meccs:Meccs,val modell:ModellTipp,val konszenzus:Konszenzus,val odds:OddsAdat,val score:Int,val noTip:Boolean,val indok:String)
data class Naplo(val meccsId:String,val ido:Long,val piac:String,val tipp:String,val p:Double,val score:Int,val noTip:Boolean,val eredmeny:Boolean?=null)
EOF

cat > "$BASE/engine/Engines.kt" <<'EOF'
package hu.sporttipp.app.engine
import hu.sporttipp.app.model.*
import kotlin.math.abs
import kotlin.math.roundToInt

interface PiacModell{val piac:Piac;fun elemez(m:Meccs):ModellTipp}
class EredmenyModell:PiacModell{
 override val piac=Piac.EREDMENY
 override fun elemez(m:Meccs):ModellTipp{
  val d=m.h.f5-m.v.f5+(m.h.hely-m.v.hely)*.6
  val t=if(d>15)"1" else if(d < -15)"2" else "X"
  val p=(.45+abs(d)/180).coerceAtMost(.78)
  return ModellTipp(piac,t,p,listOf("friss forma","hazai/idegen bontás","H2H kis súllyal"))
 }}
class GolModell:PiacModell{
 override val piac=Piac.GOL
 override fun elemez(m:Meccs):ModellTipp{
  val x=(m.h.gol+m.v.gol+m.h.kapott+m.v.kapott)/2
  val p=(.50+abs(x-2.5)*.12).coerceIn(.50,.82)
  return ModellTipp(piac,if(x>=2.55)"Over 2,5" else "Under 2,5",p,listOf("góltrend","kapott gól trend","H2H"))
 }}
class BttsModell:PiacModell{
 override val piac=Piac.BTTS
 override fun elemez(m:Meccs):ModellTipp{
  val q=(m.h2h.btts*.25+((m.h.gol+m.v.gol)/2)/3*.75).coerceIn(.35,.82)
  val yes=q>=.55; return ModellTipp(piac,if(yes)"Igen" else "Nem",if(yes)q else 1-q,listOf("támadótrend","H2H BTTS"))
 }}
class SzogletModell:PiacModell{
 override val piac=Piac.SZOGLET
 override fun elemez(m:Meccs):ModellTipp{
  val x=m.h.szoglet+m.v.szoglet; val p=(.50+abs(x-9.5)*.045).coerceIn(.50,.80)
  return ModellTipp(piac,if(x>=9.5)"Over 9,5" else "Under 9,5",p,listOf("szöglettrend"))
 }}
class LapModell:PiacModell{
 override val piac=Piac.LAP
 override fun elemez(m:Meccs):ModellTipp{
  val x=m.h.lap+m.v.lap; val p=(.50+abs(x-4.5)*.055).coerceIn(.50,.80)
  return ModellTipp(piac,if(x>=4.5)"Over 4,5" else "Under 4,5",p,listOf("laptrend","játékvezető-adat adapterrel bővíthető"))
 }}

object KonszenzusMotor{
 fun szamol(a:ModellTipp,k:List<KulsoTipp>):Konszenzus{
  val r=k.filter{it.piac==a.piac}; if(r.isEmpty())return Konszenzus(0,0,0.0,false)
  val groups=r.groupBy{it.fuggetlenseg}; var ok=0.0;var all=0.0
  groups.values.forEach{g->val w=g.maxOf{it.megbizhatosag};all+=w;if(g.count{it.tipp==a.tipp}>=(g.size+1)/2)ok+=w}
  val s=if(all==0.0)0.0 else ok/all
  return Konszenzus(r.count{it.tipp==a.tipp},r.size,s,s<.35&&r.size>=2)
 }}
object ValueMotor{fun szamol(p:Double,o:Double?):OddsAdat{if(o==null||o<=1)return OddsAdat(null,null,null);val i=1/o;return OddsAdat(o,i,p-i)}}
object TippGate{
 fun dont(m:Meccs,a:ModellTipp,k:Konszenzus,o:OddsAdat):Pair<Boolean,String>{
  if(m.minoseg==AdatMinoseg.GYENGE)return true to "Gyenge adatminőség"
  if(a.p<.58)return true to "Kevés modellbizonyosság"
  if(k.ellentmondas)return true to "Erős külső ellentmondás"
  if(o.edge!=null&&o.edge<.02)return true to "Nincs megfelelő becsült value"
  return false to "A Tipp Gate feltételei teljesültek"
 }}
object ScoreMotor{
 fun score(m:Meccs,a:ModellTipp,k:Konszenzus,o:OddsAdat):Int{
  val d=when(m.minoseg){AdatMinoseg.KIVALO->100;AdatMinoseg.JO->85;AdatMinoseg.KOZEPES->68;AdatMinoseg.GYENGE->40}
  val c=if(k.osszes==0)55.0 else k.suly*100
  val v=o.edge?.let{(50+it*250).coerceIn(0.0,100.0)}?:55.0
  return (a.p*100*.48+d*.22+c*.18+v*.12).roundToInt().coerceIn(0,100)
 }}
EOF

cat > "$BASE/repository/SportTippRepository.kt" <<'EOF'
package hu.sporttipp.app.repository
import hu.sporttipp.app.engine.*
import hu.sporttipp.app.model.*

class SportTippRepository{
 private val naplo=mutableListOf<Naplo>()
 private val modellek=listOf(EredmenyModell(),GolModell(),BttsModell(),SzogletModell(),LapModell())
 private fun meccsek()=listOf(
  Meccs("demo1","Demó Liga","19:00","Minta FC","Példa United",Forma("Minta FC",82,76,84,1.9,.9,5.8,2.1),Forma("Példa United",61,65,58,1.4,1.5,4.7,2.5),H2H(10,2.9,.70),AdatMinoseg.JO),
  Meccs("demo2","Demó Liga","21:00","Teszt City","Demo Athletic",Forma("Teszt City",69,71,77,1.6,1.1,6.1,2.8),Forma("Demo Athletic",67,64,60,1.5,1.3,5.5,3.0),H2H(8,2.5,.63),AdatMinoseg.KOZEPES))
 private fun kulso()=listOf(
  KulsoTipp("Demó A",Piac.GOL,"Over 2,5",.72,"A"),KulsoTipp("Demó B",Piac.GOL,"Over 2,5",.66,"B"),
  KulsoTipp("Demó A",Piac.BTTS,"Igen",.70,"A"),KulsoTipp("Demó C",Piac.BTTS,"Igen",.64,"C"))
 fun keres():List<VegsoTipp>{
  val out=mutableListOf<VegsoTipp>()
  for(m in meccsek()){
   val sajat=modellek.map{it.elemez(m)} // saját modell lezárva a külső konszenzus előtt
   val ext=kulso()
   for(a in sajat){val k=KonszenzusMotor.szamol(a,ext);val o=ValueMotor.szamol(a.p,null);val g=TippGate.dont(m,a,k,o);val s=ScoreMotor.score(m,a,k,o)
    out+=VegsoTipp(m,a,k,o,s,g.first,g.second);naplo+=Naplo(m.id,System.currentTimeMillis(),a.piac.cimke,a.tipp,a.p,s,g.first)}
  }
  return out.sortedWith(compareBy<VegsoTipp>{it.noTip}.thenByDescending{it.score})
 }
 fun naplo()=naplo.toList()
}
EOF

cat > "$BASE/MainActivity.kt" <<'EOF'
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

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(){
 val repo=remember{SportTippRepository()};var tips by remember{mutableStateOf<List<VegsoTipp>>(emptyList())};var page by remember{mutableIntStateOf(0)}
 MaterialTheme(colorScheme=darkColorScheme()){Scaffold(topBar={TopAppBar(title={Text("SportTipp v1.0")})},bottomBar={NavigationBar{
  listOf("TOP tippek","Mai meccsek","Napló").forEachIndexed{i,n->NavigationBarItem(selected=page==i,onClick={page=i},icon={Text(listOf("⭐","⚽","📜")[i])},label={Text(n)})}
 }}){pad->Column(Modifier.padding(pad).padding(12.dp).fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("Elemző alkalmazás • nem helyez el fogadást",style=MaterialTheme.typography.bodySmall)
  Button(onClick={tips=repo.keres()},modifier=Modifier.fillMaxWidth()){Text("🔎 Tippkeresés indítása")}
  when(page){0->Lista(tips.filter{!it.noTip});1->Lista(tips);else->NaploLista(repo.naplo())}
 }}}
}
@Composable fun Lista(t:List<VegsoTipp>){
 if(t.isEmpty()){Card(Modifier.fillMaxWidth()){Text("Nincs megjeleníthető tipp. Indíts tippkeresést. A NO TIP is érvényes eredmény.",Modifier.padding(16.dp))};return}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(t){x->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
  Text("${x.meccs.hazai} – ${x.meccs.vendeg}",style=MaterialTheme.typography.titleMedium);Text("${x.meccs.liga} • ${x.meccs.kezdes}")
  HorizontalDivider();Text("${x.modell.piac.cimke}: ${x.modell.tipp}");Text("Saját modell: ${(x.modell.p*100).toInt()}%")
  Text("Külső konszenzus: ${x.konszenzus.tamogat}/${x.konszenzus.osszes}");Text("Adatminőség: ${x.meccs.minoseg}");Text("Végső Tipp Score: ${x.score}/100")
  Text("Odds/value: ${if(x.odds.edge==null)"nincs ellenőrzött odds-adat" else "%.1f%%".format(x.odds.edge*100)}")
  Text(if(x.noTip)"⚠️ NO TIP – ${x.indok}" else "✅ ${x.indok}");x.modell.indok.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall)}
 }}}}
}
@Composable fun NaploLista(n:List<Naplo>){LazyColumn{items(n.reversed()){x->Card(Modifier.fillMaxWidth()){Text("${x.piac}: ${x.tipp} • Score ${x.score} • ${if(x.noTip)"NO TIP" else "elfogadva"}",Modifier.padding(12.dp))}}}}
EOF

# Verziószám frissítése a már működő Gradle fájlban.
python - <<'PY'
from pathlib import Path
p=Path("app/build.gradle.kts")
s=p.read_text()
s=s.replace('versionCode = 1','versionCode = 2').replace('versionName = "0.1.0"','versionName = "1.0.0"')
p.write_text(s)
PY

cat > docs/ARCHITEKTURA.md <<'EOF'
# SportTipp v1 – integrált architektúra
Meccs Radar → történelmi/H2H → aktuális forma → piacspecifikus saját modellek → saját predikció lezárása → Forrás Radar → konszenzus/ellentmondás → odds/value → Tipp Gate → Final Tipp Score → napló → visszamérés.

Alapszabályok: NO TIP érvényes eredmény; frissebb adatok nagyobb súlyúak; külső tippek nem lehetnek a saját modell bemenetei; függő források nem teljesen független szavazatok; valószínűség és value külön fogalom; ellenőrzött odds nélkül nincs kitalált value; CAPTCHA, belépés, fizetőfal vagy technikai védelem megkerülése nincs.

A v1 integrált offline mag 1X2, gól, BTTS, szöglet és lap modelleket futtat. Az adatmodell előkészíti a dupla esély, félidő, csapatgól és hendikep bővítést. Élő meccs-, statisztika-, odds- és tippforrások külön, ellenőrzött adapterrétegben kapcsolandók.
EOF

git diff --check
git add .
git commit -m "SportTipp v1 - integralt elemzo mag" || true
git push origin main
echo "KÉSZ – a GitHub Actions build elindult."
