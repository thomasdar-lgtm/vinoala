package com.koala.vinoala

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.Normalizer

val Navy = Color(0xFF00071A)
val Navy2 = Color(0xFF0D1733)
val Navy3 = Color(0xFF16213F)
val Wine = Color(0xFF8E1B3A)
val Yellow = Color(0xFFFFCC00)
val Muted = Color(0xFFB7C0D8)
val Green = Color(0xFF4CC38A)
val Orange = Color(0xFFFF9F43)

@Composable
fun VinoalaTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Yellow, onPrimary = Navy, secondary = Wine, onSecondary = Color.White,
        background = Navy, onBackground = Color.White, surface = Navy2, onSurface = Color.White,
        surfaceVariant = Navy3, onSurfaceVariant = Muted, surfaceContainer = Navy2,
        error = Color(0xFFFF6B6B),
    ),
    content = content,
)

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

fun couleurColor(c: String): Color = when (c) {
    "Rouge" -> Color(0xFFB0213F)
    "Blanc" -> Color(0xFFF2E3A0)
    "Rosé" -> Color(0xFFF4A3B5)
    "Effervescent" -> Color(0xFFE9D46B)
    "Moelleux" -> Color(0xFFE0A33A)
    else -> Muted
}

fun apogeeColor(a: Apogee): Color = when (a) {
    Apogee.ATTENDRE -> Muted
    Apogee.PRET -> Green
    Apogee.VITE -> Orange
    Apogee.PASSE -> Color(0xFFFF6B6B)
}

private fun norm(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

@Composable
fun Logo(size: Int = 26) = Row(verticalAlignment = Alignment.CenterVertically) {
    Image(
        painterResource(R.drawable.logo), null,
        Modifier.size((size * 1.6f).dp).clip(RoundedCornerShape((size * 0.35f).dp)),
    )
    Spacer(Modifier.width((size * 0.35f).dp))
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFFD23A5E))) { append("Vin") }
            withStyle(SpanStyle(color = Yellow)) { append("oala") }
        },
        fontWeight = FontWeight.Black, fontSize = size.sp,
    )
}

@Composable
fun Thumb(b64: String, couleur: String, size: Int = 56) {
    val bmp = remember(b64) { if (b64.isBlank()) null else Photos.toBitmap(b64) }
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(10.dp)).background(Navy3),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.size((size / 3).dp).clip(CircleShape).background(couleurColor(couleur)))
    }
}

@Composable
fun Pill(text: String, color: Color) = Text(
    text, color = Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold,
    modifier = Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 8.dp, vertical = 2.dp),
)

@Composable
fun LoginScreen(repo: Repo, onDone: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Logo(40)
        Spacer(Modifier.height(8.dp))
        Text("Votre cave, partagée entre vos deux téléphones", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            email, { email = it }, label = { Text("E-mail commun") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            pass, { pass = it }, label = { Text("Mot de passe") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                busy = true
                scope.launch {
                    val r = runCatching { repo.signIn(email, pass) }
                    busy = false
                    r.onSuccess { onDone() }.onFailure {
                        val m = it.message ?: ""
                        toast(ctx, when {
                            "network" in m.lowercase() -> "Pas de connexion Internet"
                            "password" in m.lowercase() || "credential" in m.lowercase() || "no user" in m.lowercase() ->
                                "E-mail ou mot de passe incorrect"
                            else -> "Connexion impossible : $m"
                        })
                    }
                }
            },
            enabled = !busy && email.isNotBlank() && pass.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Navy, strokeWidth = 2.dp)
            else Text("Se connecter", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text("Une seule fois par téléphone : ensuite l'appli marche même sans réseau.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun HomeScreen(
    state: CaveState,
    upd: UpdateState,
    tab: Int,
    onTab: (Int) -> Unit,
    onOpen: (String) -> Unit,
    onScan: () -> Unit,
    onSignOut: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val cave = state.bottles.filter { !it.drunk }
    val bues = state.bottles.filter { it.drunk }
    val q = norm(query.trim())
    val shown = (if (tab == 0) cave else bues)
        .filter { b -> q.isEmpty() || norm("${b.name} ${b.domaine} ${b.appellation} ${b.region} ${b.millesime} ${b.couleur} ${b.cepages}").contains(q) }
        .let { l ->
            if (tab == 0) l.sortedWith(compareBy<Bottle> { it.apogee()?.let { a -> -a.ordinal } ?: 0 }.thenByDescending { it.creeLe })
            else l.sortedByDescending { it.majLe }
        }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Logo()
                Spacer(Modifier.width(10.dp))
                if (state.pending) Text("synchro en attente", color = Orange, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Box {
                    TextButton(onClick = { menu = true }) { Text("v${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 12.sp) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Se déconnecter") }, onClick = { menu = false; onSignOut() })
                    }
                }
            }
            UpdateBanner(upd)
            state.error?.let {
                Text("Synchro impossible : $it", color = Color(0xFFFF6B6B), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            }
            TabRow(selectedTabIndex = tab, containerColor = Navy, contentColor = Yellow) {
                Tab(tab == 0, { onTab(0) }, text = { Text("En cave (${cave.size})") })
                Tab(tab == 1, { onTab(1) }, text = { Text("Historique (${bues.size})") })
            }
            OutlinedTextField(
                query, { query = it }, placeholder = { Text("Rechercher") }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when {
                !state.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                shown.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        when {
                            q.isNotEmpty() -> "Aucun vin ne correspond"
                            tab == 0 -> "Cave vide. Scanne ta première bouteille !"
                            else -> "Les bouteilles bues et notées apparaîtront ici."
                        },
                        color = Muted,
                    )
                }
                else -> LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp)) {
                    items(shown, key = { it.id }) { b -> BottleRow(b) { onOpen(b.id) } }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onScan, containerColor = Yellow, contentColor = Navy,
            icon = { Icon(Icons.Default.Add, null) }, text = { Text("Ajouter", fontWeight = FontWeight.Bold) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }
}

@Composable
private fun BottleRow(b: Bottle, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(Navy2)
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumb(b.thumb, b.couleur)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(b.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = b.subtitle()
            if (sub.isNotBlank()) Text(sub, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (b.couleur.isNotBlank()) Pill(b.couleur, couleurColor(b.couleur))
                b.apogee()?.let { Pill(it.label, apogeeColor(it)) }
                if (b.drunk && b.dateBue.isNotBlank()) Text("bue le ${b.dateBue}", color = Muted, fontSize = 11.sp)
            }
        }
        b.score()?.let {
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(fmtNote(it.toString()), color = Yellow, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("/10", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

/** Petit compteur réutilisable. */
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit) {
    var v by remember { mutableIntStateOf(value) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { if (v > 1) { v--; onChange(v) } }) { Text("−", fontSize = 22.sp) }
        Text("$v", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        TextButton(onClick = { if (v < 24) { v++; onChange(v) } }) { Text("+", fontSize = 22.sp) }
    }
}

@Composable
fun YellowButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) = Button(
    onClick = onClick, enabled = enabled, modifier = modifier.height(50.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
) { Text(text, fontWeight = FontWeight.Bold) }
