package sk.tuke.tictactoe

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val winner: String,       // "X", "O", or "None"
    val movesCount: Int,
    val timestamp: Long       // Store a UTC timestamp or System.currentTimeMillis()
)