package sk.tuke.tictactoe

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GameDao {
    @Insert
    suspend fun insertGame(game: GameEntity)

    @Query("SELECT * FROM games ORDER BY id DESC")
    suspend fun getAllGames(): List<GameEntity>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    suspend fun getGame(gameId: Int): GameEntity?
}