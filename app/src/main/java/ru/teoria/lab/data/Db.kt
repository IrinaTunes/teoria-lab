package ru.teoria.lab.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val course: String,
    val title: String,
    val subtitle: String,
    val kind: String
)

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: String,
    val correct: Boolean,
    val timestamp: Long,
    val xp: Int = 0
)

@Entity(tableName = "srs")
data class SrsEntity(
    @PrimaryKey val topicId: String,
    val dueAt: Long,
    val intervalDays: Int,
    val lastSeen: Long
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val lastDay: String = "",
    val instrument: String = "piano",
    val todayCount: Int = 0,
    val todayDate: String = "",
    val dailyGoal: Int = 20
)

@Dao
interface TeoriaDao {
    @Query("SELECT * FROM topics ORDER BY course, title")
    suspend fun topics(): List<TopicEntity>

    @Query("SELECT COUNT(*) FROM topics")
    suspend fun topicCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(list: List<TopicEntity>)

    @Insert
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Query("SELECT * FROM attempts WHERE topicId = :topicId ORDER BY timestamp ASC")
    suspend fun attemptsFor(topicId: String): List<AttemptEntity>

    @Query("SELECT * FROM attempts ORDER BY timestamp ASC")
    suspend fun allAttempts(): List<AttemptEntity>

    @Query("SELECT * FROM srs")
    suspend fun allSrs(): List<SrsEntity>

    @Query("SELECT * FROM srs WHERE topicId = :topicId LIMIT 1")
    suspend fun srsFor(topicId: String): SrsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSrs(srs: SrsEntity)

    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    suspend fun profile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)
}

@Database(
    entities = [TopicEntity::class, AttemptEntity::class, SrsEntity::class, ProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TeoriaDb : RoomDatabase() {
    abstract fun dao(): TeoriaDao

    companion object {
        @Volatile
        private var instance: TeoriaDb? = null

        fun get(context: Context): TeoriaDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TeoriaDb::class.java,
                "teoria.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
