package com.simplemobiletools.calendar.pro.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.helpers.Converters
import com.simplemobiletools.calendar.pro.helpers.REGULAR_EVENT_TYPE_ID
import com.simplemobiletools.calendar.pro.interfaces.EventTypesDao
import com.simplemobiletools.calendar.pro.interfaces.EventsDao
import com.simplemobiletools.calendar.pro.interfaces.WidgetsDao
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.calendar.pro.models.Widget
import java.util.concurrent.Executors

@Database(entities = [Event::class, EventType::class, Widget::class], version = 6)
@TypeConverters(Converters::class)
abstract class EventsDatabase : RoomDatabase() {

    abstract fun EventsDao(): EventsDao

    abstract fun EventTypesDao(): EventTypesDao

    abstract fun WidgetsDao(): WidgetsDao

    companion object {
        private var db: EventsDatabase? = null

        fun getInstance(context: Context): EventsDatabase {
            if (db == null) {
                synchronized(EventsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, EventsDatabase::class.java, "events.db")
                            .addCallback(object : Callback() {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    insertRegularEventType(context)
                                }
                            })
                            .addMigrations(MIGRATION_1_2)
                            .addMigrations(MIGRATION_2_3)
                            .addMigrations(MIGRATION_3_4)
                            .addMigrations(MIGRATION_4_5)
                            .addMigrations(MIGRATION_5_6)
                            .build()
                        db!!.openHelper.setWriteAheadLoggingEnabled(true)
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }

        private fun insertRegularEventType(context: Context) {
            Executors.newSingleThreadScheduledExecutor().execute {
                val regularEvent = context.resources.getString(R.string.regular_event)
                val eventType = EventType(REGULAR_EVENT_TYPE_ID, regularEvent, context.config.primaryColor)
                db!!.EventTypesDao().insertOrUpdate(eventType)
                context.config.addDisplayEventType(REGULAR_EVENT_TYPE_ID.toString())
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE events ADD COLUMN reminder_1_type INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE events ADD COLUMN reminder_2_type INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE events ADD COLUMN reminder_3_type INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE events ADD COLUMN attendees TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE events ADD COLUMN time_zone TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE events ADD COLUMN availability INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("CREATE TABLE IF NOT EXISTS `widgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `widget_id` INTEGER NOT NULL, `period` INTEGER NOT NULL)")
                    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_widgets_widget_id` ON `widgets` (`widget_id`)")
                }
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE events ADD COLUMN type INTEGER NOT NULL DEFAULT 0")
                }
            }
        }
    }
}
