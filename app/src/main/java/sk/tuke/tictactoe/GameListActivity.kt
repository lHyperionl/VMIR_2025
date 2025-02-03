package sk.tuke.tictactoe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class GameListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val db by lazy { GameDatabase.getDatabase(this) }
    private val dao by lazy { db.gameDao() }
    private val mainScope = MainScope()  // or use lifecycle-aware scopes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_list)

        recyclerView = findViewById(R.id.recyclerViewGames)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadGames()
    }

    private fun loadGames() {
        mainScope.launch {
            val games = withContext(Dispatchers.IO) {
                dao.getAllGames()
            }
            recyclerView.adapter = GameListAdapter(games) { gameId ->
                // On item click, open detail activity
                val intent = Intent(this@GameListActivity, GameDetailActivity::class.java)
                intent.putExtra("GAME_ID", gameId)
                startActivity(intent)
            }
        }
    }
}