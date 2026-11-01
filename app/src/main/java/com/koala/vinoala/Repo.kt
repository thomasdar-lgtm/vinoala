package com.koala.vinoala

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { c ->
    addOnSuccessListener { c.resume(it) }
    addOnFailureListener { c.resumeWithException(it) }
}

data class CaveState(
    val loaded: Boolean = false,
    val bottles: List<Bottle> = emptyList(),
    val pending: Boolean = false,
    val error: String? = null,
)

/**
 * Données dans Firestore (projet vinoala-5fdde), avec cache local illimité :
 * l'appli marche hors ligne, les modifications partent dès que le réseau revient.
 * Une copie JSON est aussi gardée dans les fichiers de l'appli.
 */
class Repo private constructor(private val ctx: Context) {
    private val app: FirebaseApp = FirebaseApp.getApps(ctx).firstOrNull() ?: FirebaseApp.initializeApp(
        ctx,
        FirebaseOptions.Builder()
            .setApiKey("AIzaSyDyZEwrLTQX769uklH-MD7cEY4mrLJejOQ")
            .setApplicationId("1:346549208842:web:3b2cb92dff01798e855cf3")
            .setProjectId("vinoala-5fdde")
            .setStorageBucket("vinoala-5fdde.firebasestorage.app")
            .setGcmSenderId("346549208842")
            .build(),
    )
    private val auth = FirebaseAuth.getInstance(app)
    private val db = FirebaseFirestore.getInstance(app).apply {
        runCatching {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED).build()
                ).build()
        }
    }
    private val bottles get() = db.collection("bouteilles")
    private val photos get() = db.collection("photos")
    private val backup = File(ctx.filesDir, "sauvegarde.json")

    private val _state = MutableStateFlow(CaveState())
    val state: StateFlow<CaveState> = _state
    private var reg: ListenerRegistration? = null

    val signedIn get() = auth.currentUser != null

    suspend fun signIn(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email.trim(), pass).awaitTask()
        listen()
    }

    fun signOut() {
        reg?.remove(); reg = null
        auth.signOut()
        _state.value = CaveState()
    }

    fun listen() {
        if (reg != null || !signedIn) return
        reg = bottles.addSnapshotListener { snap, err ->
            if (err != null) {
                _state.value = _state.value.copy(loaded = true, error = err.message)
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            val list = snap.documents.map { Bottle.fromMap(it.id, it.data ?: emptyMap()) }
            _state.value = CaveState(true, list, snap.metadata.hasPendingWrites(), null)
            writeBackup(list)
        }
    }

    fun save(b: Bottle, photo: Photo? = null) {
        val now = System.currentTimeMillis()
        val bb = if (photo != null) b.copy(thumb = photo.thumb, hasPhoto = true, majLe = now) else b.copy(majLe = now)
        if (photo != null) photos.document(bb.id).set(mapOf("data" to photo.full))
        bottles.document(bb.id).set(bb.toMap())
    }

    fun delete(b: Bottle) {
        bottles.document(b.id).delete()
        if (b.hasPhoto) photos.document(b.id).delete()
    }

    /** Grande photo : cache d'abord, serveur sinon. */
    suspend fun photo(id: String): String? {
        val cached = runCatching { photos.document(id).get(Source.CACHE).awaitTask().getString("data") }.getOrNull()
        return cached ?: runCatching { photos.document(id).get().awaitTask().getString("data") }.getOrNull()
    }

    /** Dernière bouteille déjà enregistrée avec ce code-barres. */
    fun known(code: String): List<Bottle> =
        _state.value.bottles.filter { it.codeBarres == code }.sortedByDescending { it.creeLe }

    /** Fiches + miniatures. Les grandes photos restent dans Firebase. */
    fun exportJson(): String {
        val arr = JSONArray()
        _state.value.bottles.forEach { b -> arr.put(JSONObject(b.toMap()).put("id", b.id)) }
        return JSONObject().put("app", "vinoala").put("version", 1).put("bouteilles", arr).toString()
    }

    /** Ajoute les bouteilles absentes et met à jour celles qui sont plus récentes dans le fichier. */
    fun importJson(text: String): Int {
        val t = text.trim()
        val arr = if (t.startsWith("[")) JSONArray(t) else JSONObject(t).getJSONArray("bouteilles")
        val current = _state.value.bottles.associateBy { it.id }
        var n = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id")
            if (id.isBlank()) continue
            val m = o.keys().asSequence().associateWith { k -> o.get(k) }
            val b = Bottle.fromMap(id, m)
            val cur = current[id]
            if (cur == null || b.majLe > cur.majLe) { bottles.document(id).set(b.toMap()); n++ }
        }
        return n
    }

    private fun writeBackup(list: List<Bottle>) = runCatching {
        val arr = JSONArray()
        list.forEach { b -> arr.put(JSONObject(b.toMap()).put("id", b.id)) }
        val tmp = File(backup.path + ".tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(backup)
    }

    companion object {
        @Volatile private var inst: Repo? = null
        fun get(ctx: Context) = inst ?: synchronized(this) { inst ?: Repo(ctx.applicationContext).also { inst = it } }
    }
}
