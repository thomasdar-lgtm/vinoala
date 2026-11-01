package com.koala.vinoala

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
private fun TopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f), maxLines = 1)
        actions()
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(14.dp)).background(Navy2).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = Yellow, fontWeight = FontWeight.Black, fontSize = 14.sp)
        content()
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    if (value.isBlank()) return
    Row {
        Text(label, color = Muted, fontSize = 14.sp, modifier = Modifier.width(110.dp))
        Text(value, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BigPhoto(repo: Repo, b: Bottle) {
    var full by remember(b.id, b.majLe) { mutableStateOf<String?>(null) }
    LaunchedEffect(b.id, b.majLe) { if (b.hasPhoto) full = repo.photo(b.id) }
    val src = full ?: b.thumb
    val bmp = remember(src) { if (src.isBlank()) null else Photos.toBitmap(src) }
    if (bmp != null) Image(
        bmp.asImageBitmap(), null,
        Modifier.fillMaxWidth().heightIn(max = 380.dp).clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Fit,
    )
}

/* ---------- Fiche ---------- */

@Composable
fun DetailScreen(
    repo: Repo, b: Bottle,
    onBack: () -> Unit, onEdit: () -> Unit, onTaste: () -> Unit, onRebuy: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        TopBar(b.title, onBack) {
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Modifier") }
            IconButton(onClick = { confirm = true }) { Icon(Icons.Default.Delete, "Supprimer") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            BigPhoto(repo, b)
            Spacer(Modifier.height(10.dp))
            Text(b.title, fontWeight = FontWeight.Black, fontSize = 22.sp)
            val sub = b.subtitle()
            if (sub.isNotBlank()) Text(sub, color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                if (b.couleur.isNotBlank()) Pill(b.couleur, couleurColor(b.couleur))
                b.apogee()?.let { Pill(it.label, apogeeColor(it)) }
            }
            if (b.drunk) Section("Dégustation${if (b.dateBue.isNotBlank()) " du ${b.dateBue}" else ""}") {
                if (b.note.isNotBlank()) Text("${fmtNote(b.note)} / 10", color = Yellow, fontWeight = FontWeight.Black, fontSize = 26.sp)
                if (b.commentaire.isNotBlank()) Text(b.commentaire)
                if (b.noteThomas.isNotBlank() || b.comThomas.isNotBlank()) {
                    Text("Thomas${if (b.noteThomas.isNotBlank()) " : ${fmtNote(b.noteThomas)} / 10" else ""}", fontWeight = FontWeight.Bold)
                    if (b.comThomas.isNotBlank()) Text(b.comThomas, color = Muted)
                }
                if (b.noteCindy.isNotBlank() || b.comCindy.isNotBlank()) {
                    Text("Cindy${if (b.noteCindy.isNotBlank()) " : ${fmtNote(b.noteCindy)} / 10" else ""}", fontWeight = FontWeight.Bold)
                    if (b.comCindy.isNotBlank()) Text(b.comCindy, color = Muted)
                }
            }
            Section("Le vin") {
                InfoLine("Domaine", b.domaine)
                InfoLine("Appellation", b.appellation)
                InfoLine("Région", listOf(b.region, b.pays).filter { it.isNotBlank() }.joinToString(", "))
                InfoLine("Millésime", b.millesime)
                InfoLine("Cépages", b.cepages)
                InfoLine("Degré", if (b.degre.isBlank()) "" else "${b.degre} %")
                InfoLine("Contenance", b.contenance)
                InfoLine("Code-barres", b.codeBarres)
            }
            if (listOf(b.gardeDe, b.gardeA, b.temperature, b.accords).any { it.isNotBlank() }) Section("Garde et service") {
                InfoLine("À boire", when {
                    b.gardeDe.isNotBlank() && b.gardeA.isNotBlank() -> "de ${b.gardeDe} à ${b.gardeA}"
                    b.gardeDe.isNotBlank() -> "à partir de ${b.gardeDe}"
                    b.gardeA.isNotBlank() -> "avant fin ${b.gardeA}"
                    else -> ""
                })
                InfoLine("Température", if (b.temperature.isBlank()) "" else "${b.temperature} °C")
                InfoLine("Accords", b.accords)
            }
            if (listOf(b.prix, b.lieu, b.dateAchat).any { it.isNotBlank() }) Section("Achat") {
                InfoLine("Prix", if (b.prix.isBlank()) "" else "${b.prix} €")
                InfoLine("Lieu", b.lieu)
                InfoLine("Date", b.dateAchat)
            }
            Spacer(Modifier.height(90.dp))
        }
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onRebuy, modifier = Modifier.weight(1f).height(50.dp)) { Text("Racheter") }
            YellowButton(if (b.drunk) "Modifier la note" else "On l'a bue !", Modifier.weight(1.4f), onClick = onTaste)
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Supprimer cette bouteille ?") },
        text = { Text("Elle disparaîtra aussi du téléphone de l'autre.") },
        confirmButton = { TextButton(onClick = { confirm = false; repo.delete(b); onBack() }) { Text("Supprimer", color = Color(0xFFFF6B6B)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Annuler") } },
    )
}

/* ---------- Recherche Open Food Facts ---------- */

@Composable
fun LookupScreen(code: String, onResult: (Bottle, String?) -> Unit) {
    LaunchedEffect(code) {
        val info = runCatching { Off.lookup(code) }.getOrNull()
        onResult(info?.bottle ?: Bottle(codeBarres = code, dateAchat = Bottle.today()), info?.imageUrl)
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Recherche de la bouteille…", color = Muted)
        Text(code, color = Muted, fontSize = 12.sp)
    }
}

/* ---------- Saisie / modification ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    initial: Bottle, isNew: Boolean, photoUrl: String?, notice: String?,
    onSave: (List<Bottle>, Photo?) -> Unit, onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var b by remember { mutableStateOf(initial) }
    var photo by remember { mutableStateOf<Photo?>(null) }
    var photoBusy by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(1) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(photoUrl) {
        if (photoUrl != null && photo == null && !initial.hasPhoto) {
            photoBusy = true
            photo = runCatching { Photos.fromUrl(photoUrl) }.getOrNull()
            photoBusy = false
        }
    }
    LaunchedEffect(notice) { if (notice != null) toast(ctx, notice) }

    fun load(uri: Uri?) {
        if (uri == null) return
        photoBusy = true
        scope.launch {
            val p = runCatching { Photos.fromUri(ctx, uri) }.getOrNull()
            photoBusy = false
            if (p == null) toast(ctx, "Photo illisible") else photo = p
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) load(captureUri) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { load(it) }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        TopBar(if (isNew) "Nouvelle bouteille" else "Modifier", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Section("Photo") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                        Thumb(photo?.thumb ?: b.thumb, b.couleur, 96)
                        if (photoBusy) CircularProgressIndicator(Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            val u = Photos.newCaptureUri(ctx); captureUri = u; camera.launch(u)
                        }) { Text("Prendre une photo") }
                        OutlinedButton(onClick = {
                            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text("Depuis la galerie") }
                    }
                }
            }
            Section("Le vin") {
                Field("Nom du vin", b.name) { b = b.copy(name = it) }
                Field("Domaine / château", b.domaine) { b = b.copy(domaine = it) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Bottle.COULEURS.forEach { c ->
                        FilterChip(
                            selected = b.couleur == c, onClick = { b = b.copy(couleur = if (b.couleur == c) "" else c) },
                            label = { Text(c) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = couleurColor(c), selectedLabelColor = Navy),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Millésime", b.millesime, Modifier.weight(1f), number = true) { b = b.copy(millesime = it) }
                    Field("Degré %", b.degre, Modifier.weight(1f), decimal = true) { b = b.copy(degre = it) }
                }
                Field("Appellation", b.appellation) { b = b.copy(appellation = it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Région", b.region, Modifier.weight(1f)) { b = b.copy(region = it) }
                    Field("Pays", b.pays, Modifier.weight(1f)) { b = b.copy(pays = it) }
                }
                Field("Cépages", b.cepages) { b = b.copy(cepages = it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Contenance", b.contenance, Modifier.weight(1f)) { b = b.copy(contenance = it) }
                    Field("Code-barres", b.codeBarres, Modifier.weight(1f), number = true) { b = b.copy(codeBarres = it) }
                }
            }
            Section("Garde et service") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("À boire dès (année)", b.gardeDe, Modifier.weight(1f), number = true) { b = b.copy(gardeDe = it) }
                    Field("Jusqu'à (année)", b.gardeA, Modifier.weight(1f), number = true) { b = b.copy(gardeA = it) }
                }
                Field("Température de service °C", b.temperature, number = true) { b = b.copy(temperature = it) }
                Field("Accords mets-vins", b.accords, lines = 2) { b = b.copy(accords = it) }
            }
            Section("Achat") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Prix €", b.prix, Modifier.weight(1f), decimal = true) { b = b.copy(prix = it) }
                    Field("Date", b.dateAchat, Modifier.weight(1f)) { b = b.copy(dateAchat = it) }
                }
                Field("Lieu d'achat", b.lieu) { b = b.copy(lieu = it) }
                if (isNew) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Nombre de bouteilles", modifier = Modifier.weight(1f))
                    Stepper(count) { count = it }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        YellowButton(
            if (isNew && count > 1) "Ajouter $count bouteilles" else "Enregistrer",
            Modifier.fillMaxWidth().padding(14.dp), enabled = !photoBusy,
        ) {
            if (b.name.isBlank() && b.domaine.isBlank()) { toast(ctx, "Indique au moins le nom ou le domaine"); return@YellowButton }
            val list = if (isNew) List(count) { i -> if (i == 0) b else b.asNewPurchase().copy(
                prix = b.prix, lieu = b.lieu, dateAchat = b.dateAchat) } else listOf(b)
            onSave(list, photo)
        }
    }
}

@Composable
private fun Field(
    label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(),
    number: Boolean = false, decimal: Boolean = false, lines: Int = 1, onChange: (String) -> Unit,
) = OutlinedTextField(
    value, onChange, label = { Text(label) }, modifier = modifier,
    singleLine = lines == 1, minLines = lines,
    keyboardOptions = KeyboardOptions(
        keyboardType = when { decimal -> KeyboardType.Decimal; number -> KeyboardType.Number; else -> KeyboardType.Text },
    ),
)

/* ---------- Dégustation ---------- */

@Composable
fun TasteScreen(initial: Bottle, onSave: (Bottle) -> Unit, onBack: () -> Unit) {
    var b by remember { mutableStateOf(if (initial.dateBue.isBlank()) initial.copy(dateBue = Bottle.today()) else initial) }
    var separate by remember { mutableStateOf(initial.noteThomas.isNotBlank() || initial.noteCindy.isNotBlank() || initial.comThomas.isNotBlank() || initial.comCindy.isNotBlank()) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        TopBar(initial.title, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Section("Notre note") {
                NoteSlider(b.note) { b = b.copy(note = it) }
                Field("Commentaire", b.commentaire, lines = 3) { b = b.copy(commentaire = it) }
                Field("Bue le", b.dateBue) { b = b.copy(dateBue = it) }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Pas d'accord ? Une note chacun", modifier = Modifier.weight(1f))
                Switch(separate, { separate = it })
            }
            if (separate) {
                Section("Thomas") {
                    NoteSlider(b.noteThomas) { b = b.copy(noteThomas = it) }
                    Field("Commentaire de Thomas", b.comThomas, lines = 2) { b = b.copy(comThomas = it) }
                }
                Section("Cindy") {
                    NoteSlider(b.noteCindy) { b = b.copy(noteCindy = it) }
                    Field("Commentaire de Cindy", b.comCindy, lines = 2) { b = b.copy(comCindy = it) }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        YellowButton("Enregistrer la dégustation", Modifier.fillMaxWidth().padding(14.dp)) {
            val out = if (separate) b else b.copy(noteThomas = "", comThomas = "", noteCindy = "", comCindy = "")
            onSave(out.copy(status = Bottle.BUE))
        }
    }
}

@Composable
private fun NoteSlider(value: String, onChange: (String) -> Unit) {
    var v by remember { mutableFloatStateOf(value.toFloatOrNull() ?: -1f) }
    Column {
        Text(
            if (v < 0) "Pas encore notée" else "${fmtNote(v.toString())} / 10",
            color = if (v < 0) Muted else Yellow, fontWeight = FontWeight.Black, fontSize = 24.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
        Slider(
            value = if (v < 0) 5f else v,
            onValueChange = { x -> v = Math.round(x * 2) / 2f; onChange(v.toString()) },
            valueRange = 0f..10f, steps = 19,
            colors = SliderDefaults.colors(thumbColor = Yellow, activeTrackColor = Yellow),
        )
        if (v >= 0) TextButton(onClick = { v = -1f; onChange("") }) { Text("Effacer la note", color = Muted) }
    }
}
