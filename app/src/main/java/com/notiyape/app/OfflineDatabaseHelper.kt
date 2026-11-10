package com.notiyape.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

data class CobroPendienteSQLite(
    val idLocal: Long,
    val codigoTienda: String,
    val nombre: String,
    val monto: String,
    val metodo: String,
    val cajero: String,
    val fecha: String,
    val hora: String,
    val timestamp: Long
)

class OfflineDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "yapealdia_offline.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_COBROS = "cobros_locales"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_COBROS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo_tienda TEXT NOT NULL,
                nombre TEXT NOT NULL,
                monto TEXT NOT NULL,
                metodo TEXT NOT NULL,
                cajero TEXT NOT NULL,
                fecha TEXT NOT NULL,
                hora TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                sincronizado INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COBROS")
        onCreate(db)
    }

    fun insertarCobroLocal(
        codigoTienda: String,
        nombre: String,
        monto: String,
        metodo: String,
        cajero: String,
        fecha: String,
        hora: String,
        timestamp: Long,
        sincronizado: Boolean = false
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("codigo_tienda", codigoTienda)
            put("nombre", nombre)
            put("monto", monto)
            put("metodo", metodo)
            put("cajero", cajero)
            put("fecha", fecha)
            put("hora", hora)
            put("timestamp", timestamp)
            put("sincronizado", if (sincronizado) 1 else 0)
        }
        return db.insert(TABLE_COBROS, null, values)
    }

    @Synchronized
    fun marcarComoSincronizado(idLocal: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("sincronizado", 1)
        }
        db.update(TABLE_COBROS, values, "id = ?", arrayOf(idLocal.toString()))
    }

    @Synchronized
    fun marcarComoPendiente(idLocal: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("sincronizado", 0)
        }
        db.update(TABLE_COBROS, values, "id = ?", arrayOf(idLocal.toString()))
    }

    @Synchronized
    fun obtenerCobrosPendientes(): List<CobroPendienteSQLite> {
        val lista = mutableListOf<CobroPendienteSQLite>()
        val db = readableDatabase
        // Solo obtener cobros que llevan más de 25 segundos pendientes para evitar choque con el envío en vivo
        val limiteTiempo = (System.currentTimeMillis() - 25_000L).toString()
        val cursor = db.query(
            TABLE_COBROS,
            null,
            "sincronizado = ? AND timestamp < ?",
            arrayOf("0", limiteTiempo),
            null,
            null,
            "timestamp ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    CobroPendienteSQLite(
                        idLocal = it.getLong(it.getColumnIndexOrThrow("id")),
                        codigoTienda = it.getString(it.getColumnIndexOrThrow("codigo_tienda")),
                        nombre = it.getString(it.getColumnIndexOrThrow("nombre")),
                        monto = it.getString(it.getColumnIndexOrThrow("monto")),
                        metodo = it.getString(it.getColumnIndexOrThrow("metodo")),
                        cajero = it.getString(it.getColumnIndexOrThrow("cajero")),
                        fecha = it.getString(it.getColumnIndexOrThrow("fecha")),
                        hora = it.getString(it.getColumnIndexOrThrow("hora")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        return lista
    }

    fun contarPendientes(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_COBROS WHERE sincronizado = 0", null)
        cursor.use {
            if (it.moveToFirst()) return it.getInt(0)
        }
        return 0
    }

    fun obtenerHistorialComoJsonArray(limite: Int = 50): JSONArray {
        val array = JSONArray()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_COBROS,
            null,
            null,
            null,
            null,
            null,
            "timestamp DESC",
            limite.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                val sync = it.getInt(it.getColumnIndexOrThrow("sincronizado")) == 1
                val metodo = it.getString(it.getColumnIndexOrThrow("metodo"))
                val nombreBase = it.getString(it.getColumnIndexOrThrow("nombre"))
                val obj = JSONObject().apply {
                    put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                    put("nombre", if (nombreBase.contains(metodo)) nombreBase else "$nombreBase ($metodo)")
                    put("monto", it.getString(it.getColumnIndexOrThrow("monto")))
                    put("metodo", metodo)
                    put("cajero", it.getString(it.getColumnIndexOrThrow("cajero")))
                    put("fecha", it.getString(it.getColumnIndexOrThrow("fecha")))
                    put("hora", it.getString(it.getColumnIndexOrThrow("hora")))
                    put("timestamp", it.getLong(it.getColumnIndexOrThrow("timestamp")))
                    put("sincronizado", sync)
                }
                array.put(obj)
            }
        }
        return array
    }

    @Synchronized
    fun limpiarTodoLocal() {
        try {
            writableDatabase.delete(TABLE_COBROS, null, null)
        } catch (e: Exception) {}
    }
}

