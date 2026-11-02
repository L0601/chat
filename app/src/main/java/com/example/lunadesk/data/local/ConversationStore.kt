package com.example.lunadesk.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.lunadesk.data.model.ChatMessageUi
import com.example.lunadesk.data.model.CallMetrics
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ConversationSummary(val id: String, val title: String, val updatedAt: Long)

interface ConversationStore {
    suspend fun list(): List<ConversationSummary>
    suspend fun load(id: String): List<ChatMessageUi>
    suspend fun save(id: String, messages: List<ChatMessageUi>)
    suspend fun rename(id: String, title: String)
    suspend fun delete(id: String)
}

class SqliteConversationStore(context: Context) : SQLiteOpenHelper(context, "conversations.db", null, 2), ConversationStore {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE conversations (id TEXT PRIMARY KEY, title TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE messages (id TEXT PRIMARY KEY, conversation_id TEXT NOT NULL, position INTEGER NOT NULL, role TEXT NOT NULL, content TEXT NOT NULL, error TEXT, metrics TEXT)")
        db.execSQL("CREATE INDEX messages_conversation ON messages(conversation_id, position)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE messages ADD COLUMN metrics TEXT")
    }

    override suspend fun list() = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT id, title, updated_at FROM conversations ORDER BY updated_at DESC", null).use { c ->
            buildList { while (c.moveToNext()) add(ConversationSummary(c.getString(0), c.getString(1), c.getLong(2))) }
        }
    }
    override suspend fun load(id: String) = withContext(Dispatchers.IO) {
        readableDatabase.query("messages", arrayOf("id", "role", "content", "error", "metrics"), "conversation_id=?", arrayOf(id), null, null, "position").use { c ->
            buildList { while (c.moveToNext()) add(ChatMessageUi(c.getString(0), c.getString(1), c.getString(2), errorText = c.getString(3), metrics = c.getString(4)?.let { raw -> runCatching { Json.decodeFromString<CallMetrics>(raw) }.getOrNull() })) }
        }
    }
    override suspend fun save(id: String, messages: List<ChatMessageUi>) = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            val title = messages.firstOrNull { it.role == "user" }?.content?.take(40) ?: "新对话"
            db.execSQL("INSERT OR IGNORE INTO conversations(id,title,updated_at) VALUES(?,?,?)", arrayOf(id, title, System.currentTimeMillis()))
            db.execSQL("UPDATE conversations SET updated_at=? WHERE id=?", arrayOf(System.currentTimeMillis(), id))
            db.delete("messages", "conversation_id=?", arrayOf(id))
            messages.forEachIndexed { index, m ->
                db.insertOrThrow("messages", null, ContentValues().apply {
                    put("id", m.id); put("conversation_id", id); put("position", index)
                    put("role", m.role); put("content", m.content); put("error", m.errorText)
                    put("metrics", m.metrics?.let { Json.encodeToString(it) })
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    override suspend fun rename(id: String, title: String) = withContext(Dispatchers.IO) {
        writableDatabase.update("conversations", ContentValues().apply { put("title", title) }, "id=?", arrayOf(id)); Unit
    }
    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try { db.delete("messages", "conversation_id=?", arrayOf(id)); db.delete("conversations", "id=?", arrayOf(id)); db.setTransactionSuccessful() }
        finally { db.endTransaction() }
    }
}
