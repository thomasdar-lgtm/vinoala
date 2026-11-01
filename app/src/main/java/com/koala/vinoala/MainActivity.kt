package com.koala.vinoala

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

sealed interface Screen {
    data object Home : Screen
    data object Scan : Screen
    data class Lookup(val code: String) : Screen
    data class Edit(val bottle: Bottle, val isNew: Boolean, val photoUrl: String? = null, val notice: String? = null) : Screen
    data class Detail(val id: String) : Screen
    data class Taste(val id: String) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val repo = Repo.get(this)
        repo.listen()
        setContent {
            VinoalaTheme {
                Surface(Modifier.fillMaxSize(), color = Navy) { App(repo) }
            }
        }
    }
}

@Composable
fun App(repo: Repo) {
    val ctx = LocalContext.current
    var signedIn by remember { mutableStateOf(repo.signedIn) }
    if (!signedIn) {
        LoginScreen(repo) { signedIn = true }
        return
    }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var tab by remember { mutableIntStateOf(0) }
    val state by repo.state.collectAsState()
    val upd = remember { UpdateState() }
    LaunchedEffect(Unit) { upd.info = runCatching { Updater.check() }.getOrNull() }
    fun bottle(id: String) = state.bottles.firstOrNull { it.id == id }

    fun back() {
        screen = when (val s = screen) {
            is Screen.Edit -> if (s.isNew) Screen.Home else Screen.Detail(s.bottle.id)
            is Screen.Taste -> Screen.Detail(s.id)
            else -> Screen.Home
        }
    }
    BackHandler(enabled = screen != Screen.Home) { back() }

    Box(Modifier.fillMaxSize()) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(
                repo, state, upd, tab, { tab = it },
                onOpen = { screen = Screen.Detail(it) },
                onScan = { screen = Screen.Scan },
                onSignOut = { repo.signOut(); signedIn = false },
            )

            Screen.Scan -> ScannerScreen(
                onCode = { code ->
                    val prev = repo.known(code)
                    screen = if (prev.isEmpty()) Screen.Lookup(code) else {
                        val notes = prev.mapNotNull { it.score() }
                        Screen.Edit(
                            prev.first().asNewPurchase(), isNew = true,
                            notice = buildString {
                                append("Déjà acheté ${prev.size} fois")
                                if (notes.isNotEmpty()) append(", note moyenne ${fmtNote("%.1f".format(java.util.Locale.US, notes.average()))}/10")
                            },
                        )
                    }
                },
                onManual = { screen = Screen.Edit(Bottle(dateAchat = Bottle.today()), isNew = true) },
                onClose = { back() },
            )

            is Screen.Lookup -> LookupScreen(s.code) { b, url ->
                val found = b.name.isNotBlank() || b.domaine.isNotBlank()
                screen = Screen.Edit(
                    b.copy(dateAchat = Bottle.today()), isNew = true, photoUrl = url,
                    notice = if (found) null else "Bouteille inconnue : complète la fiche à la main",
                )
            }

            is Screen.Edit -> EditScreen(
                s.bottle, s.isNew, s.photoUrl, s.notice,
                onSave = { list, photo ->
                    list.forEach { repo.save(it, photo) }
                    if (list.size > 1) toast(ctx, "${list.size} bouteilles ajoutées à la cave")
                    tab = 0
                    screen = Screen.Detail(list.first().id)
                },
                onBack = { back() },
            )

            is Screen.Detail -> {
                val b = bottle(s.id)
                if (b == null) LaunchedEffect(s) { kotlinx.coroutines.delay(2000); screen = Screen.Home }
                else DetailScreen(
                    repo, b,
                    onBack = { back() },
                    onEdit = { screen = Screen.Edit(b, isNew = false) },
                    onTaste = { screen = Screen.Taste(b.id) },
                    onRebuy = { screen = Screen.Edit(b.asNewPurchase(), isNew = true) },
                )
            }

            is Screen.Taste -> {
                val b = bottle(s.id)
                if (b == null) LaunchedEffect(s) { kotlinx.coroutines.delay(2000); screen = Screen.Home }
                else TasteScreen(
                    b,
                    onSave = { repo.save(it); tab = 1; screen = Screen.Detail(it.id) },
                    onBack = { back() },
                )
            }
        }
    }
}
