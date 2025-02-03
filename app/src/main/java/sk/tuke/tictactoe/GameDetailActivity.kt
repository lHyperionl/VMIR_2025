package sk.tuke.tictactoe

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class GameDetailActivity : AppCompatActivity() {

    private val db by lazy { GameDatabase.getDatabase(this) }
    private val dao by lazy { db.gameDao() }

    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_detail)

        val textDetail = findViewById<TextView>(R.id.textDetail)
        val gameId = intent.getIntExtra("GAME_ID", -1)

        mainScope.launch {
            val game = withContext(Dispatchers.IO) { dao.getGame(gameId) }
            game?.let {
                val detail = "Game ID: ${it.id}\n" +
                        "Winner: ${it.winner}\n" +
                        "Moves: ${it.movesCount}\n" +
                        "Timestamp: ${it.timestamp}"
                textDetail.text = detail
            }
        }
    }
}