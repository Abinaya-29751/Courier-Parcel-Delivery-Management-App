package com.abi.project.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.abi.project.model.User
import com.abi.project.model.Courier
import com.abi.project.utils.NotificationHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 3  // Increment this
        private const val DATABASE_NAME = "courier_app.db"
        // Users table
        private const val TABLE_USERS = "users"
        private const val COLUMN_USER_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_PHONE = "phone"
        private const val COLUMN_IS_ADMIN = "is_admin"

        // Couriers table
        private const val TABLE_COURIERS = "couriers"
        private const val COLUMN_COURIER_ID = "id"
        private const val COLUMN_COURIER_NUMBER = "courier_number"
        private const val COLUMN_STATUS = "status"
        private const val COLUMN_PLACE = "place"
        private const val COLUMN_DELIVERY_PERSON_NAME = "delivery_person_name"
        private const val COLUMN_DELIVERY_PERSON_ID = "delivery_person_id"
        private const val COLUMN_USER_USERNAME = "user_username"

        // Courier Location table
        private const val TABLE_COURIER_LOCATION = "courier_location"
        private const val COLUMN_LOCATION_ID = "id"
        private const val COLUMN_LOCATION_COURIER_ID = "courier_id"
        private const val COLUMN_LOCATION = "location"

        // Deliveries Table
        private const val TABLE_DELIVERY_PERSON= "deliveries"
        private const val COLUMN_PERSON_ID = "id"
        private const val COLUMN_DELIVERY_COURIER_ID = "courier_id"
        private const val COLUMN_PERSON_RATINGS = "ratings"
        private const val COLUMN_PERSON_NAME = "person_name"


    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE,
                $COLUMN_PASSWORD TEXT,
                $COLUMN_PHONE TEXT,
                $COLUMN_IS_ADMIN INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createCouriersTable = """
            CREATE TABLE $TABLE_COURIERS (
        $COLUMN_COURIER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $COLUMN_COURIER_NUMBER TEXT UNIQUE,
        $COLUMN_STATUS TEXT,
        $COLUMN_PLACE TEXT,
        $COLUMN_DELIVERY_PERSON_NAME TEXT,
        $COLUMN_DELIVERY_PERSON_ID TEXT,
        $COLUMN_USER_USERNAME TEXT,
        FOREIGN KEY($COLUMN_USER_USERNAME) REFERENCES $TABLE_USERS($COLUMN_USERNAME)
    )
        """.trimIndent()

        val createCourierLocationTable = """
            CREATE TABLE $TABLE_COURIER_LOCATION (
        $COLUMN_LOCATION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $COLUMN_LOCATION_COURIER_ID INTEGER,
        $COLUMN_LOCATION TEXT,
        FOREIGN KEY($COLUMN_LOCATION_COURIER_ID) REFERENCES $TABLE_COURIERS($COLUMN_COURIER_ID) ON DELETE CASCADE,
        UNIQUE($COLUMN_LOCATION_COURIER_ID)
    )
        """.trimIndent()
        val createDeliveryPersonTable = """
        CREATE TABLE $TABLE_DELIVERY_PERSON (
            $COLUMN_PERSON_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_PERSON_NAME TEXT NOT NULL,
            $COLUMN_PERSON_RATINGS REAL DEFAULT 0.0
        )
    """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createCouriersTable)
        db.execSQL(createCourierLocationTable)
        db.execSQL(createDeliveryPersonTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COURIERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COURIER_LOCATION")
        onCreate(db)
    }

    // ✅ Add a new user
    fun addUser(username: String, password: String, phone: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_PHONE, phone)
            put(COLUMN_IS_ADMIN, 0)  // Default as regular user
        }
        val result = db.insert(TABLE_USERS, null, values)
        if (result == -1L) {
            Log.e("DatabaseHelper", "Failed to insert user: $username")
        }
        return result
    }

    // ✅ Check if a user exists
    fun checkUser(username: String, password: String): User? {
        val db = readableDatabase
        val cursor: Cursor? = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(username, password)
        )

        var user: User? = null
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID))
                val phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE))
                val isAdmin = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ADMIN)) == 1

                user = User(id, username, password, phone, isAdmin)
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error checking user: ${e.message}")
        } finally {
            cursor?.close()
        }
        return user
    }

    // ✅ Insert a new courier
    fun insertCourier(
        courierNumber: String,
        username: String,
        status: String,
        place: String,
        deliveryPerson: String,
        deliveryPersonId: String,
        locationUrl: String
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COURIER_NUMBER, courierNumber)
            put(COLUMN_USER_USERNAME, username)
            put(COLUMN_STATUS, status)
            put(COLUMN_PLACE, place)
            put(COLUMN_DELIVERY_PERSON_NAME, deliveryPerson)
            put(COLUMN_DELIVERY_PERSON_ID, deliveryPersonId)
        }

        val courierId = db.insert(TABLE_COURIERS, null, values)

        if (courierId != -1L) {
            val locationValues = ContentValues().apply {
                put(COLUMN_LOCATION_COURIER_ID, courierId)
                put(COLUMN_LOCATION, locationUrl)
            }
            db.insertWithOnConflict(TABLE_COURIER_LOCATION, null, locationValues, SQLiteDatabase.CONFLICT_REPLACE)
        }

        return courierId
    }

    // ✅ Retrieve courier location
    fun getCourierLocation(courierId: Int): String? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_LOCATION FROM $TABLE_COURIER_LOCATION WHERE $COLUMN_LOCATION_COURIER_ID = ?",
            arrayOf(courierId.toString())
        )
        return if (cursor.moveToFirst()) {
            cursor.getString(0)
        } else {
            null
        }.also { cursor.close() }
    }

    // ✅ Get all couriers
    fun getAllCouriers(): List<Courier> {
        val couriers = mutableListOf<Courier>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT couriers.*, courier_location.location FROM couriers " +
                    "LEFT JOIN courier_location ON couriers.id = courier_location.courier_id",
            null
        )

        if (cursor.moveToFirst()) {
            do {
                couriers.add(cursorToCourier(cursor))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return couriers
    }

    // ✅ Convert cursor to courier object
    private fun cursorToCourier(cursor: Cursor): Courier {
        return Courier(
            id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
            courierNumber = cursor.getString(cursor.getColumnIndexOrThrow("courier_number")) ?: "",
            status = cursor.getString(cursor.getColumnIndexOrThrow("status")) ?: "",
            place = cursor.getString(cursor.getColumnIndexOrThrow("place")) ?: "",
            deliveryPersonName = cursor.getString(cursor.getColumnIndexOrThrow("delivery_person_name")) ?: "N/A",
            deliveryPersonId = cursor.getString(cursor.getColumnIndexOrThrow("delivery_person_id")) ?: "N/A",
            userUsername = cursor.getString(cursor.getColumnIndexOrThrow("user_username")) ?: "Unknown",
            locationUrl = cursor.getString(cursor.getColumnIndexOrThrow("location")) ?: ""
        )
    }

    // ✅ Insert admin account if not exists
    fun insertAdminAccount() {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, "admin")
            put(COLUMN_PASSWORD, "admin123")
            put(COLUMN_PHONE, "1234567890")
            put(COLUMN_IS_ADMIN, 1)
        }
        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun updateCourierStatus(courierId: Int, newStatus: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", newStatus)
        }
        val result = db.update("couriers", values, "id=?", arrayOf(courierId.toString()))
        db.close()
        return result > 0
    }

    @SuppressLint("Range")
    fun getAllCouriersForUser(username: String): List<Courier> {
        val couriers = mutableListOf<Courier>()
        val db = readableDatabase

        // First get the couriers
        val cursor = db.rawQuery(
            "SELECT c.*, cl.location FROM $TABLE_COURIERS c " +
                    "LEFT JOIN $TABLE_COURIER_LOCATION cl ON c.$COLUMN_COURIER_ID = cl.$COLUMN_LOCATION_COURIER_ID " +
                    "WHERE c.$COLUMN_USER_USERNAME = ?",
            arrayOf(username)
        )

        if (cursor.moveToFirst()) {
            do {
                // Safely get location with null check
                val locationColumnIndex = cursor.getColumnIndex(COLUMN_LOCATION)
                val locationUrl = if (locationColumnIndex != -1) {
                    cursor.getString(locationColumnIndex) ?: ""
                } else {
                    ""
                }

                val courier = Courier(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COURIER_ID)),
                    courierNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COURIER_NUMBER)),
                    userUsername = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_USERNAME)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)),
                    place = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLACE)),
                    deliveryPersonName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DELIVERY_PERSON_NAME)),
                    deliveryPersonId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DELIVERY_PERSON_ID)),
                    locationUrl = locationUrl
                )
                couriers.add(courier)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return couriers
    }
    data class DeliveryPerson(val name: String, val contact: String)

    fun getDeliveryPersonForCourier(courierId: Int): DeliveryPerson? {
        val db = readableDatabase
        // Changed the query to get data from couriers table instead
        val query = """
        SELECT $COLUMN_DELIVERY_PERSON_NAME, $COLUMN_DELIVERY_PERSON_ID 
        FROM $TABLE_COURIERS 
        WHERE $COLUMN_COURIER_ID = ?
    """
        val cursor = db.rawQuery(query, arrayOf(courierId.toString()))

        return if (cursor.moveToFirst()) {
            val name = cursor.getString(0) ?: "N/A"
            val contact = cursor.getString(1) ?: "N/A"
            cursor.close()
            DeliveryPerson(name, contact)
        } else {
            cursor.close()
            null
        }
    }







}
