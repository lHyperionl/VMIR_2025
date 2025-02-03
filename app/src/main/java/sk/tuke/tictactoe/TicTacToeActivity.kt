package sk.tuke.tictactoe

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TicTacToeActivity : AppCompatActivity() {

    // Lazy-load the database and DAO
    private val db by lazy { GameDatabase.getDatabase(this) }
    private val gameDao by lazy { db.gameDao() }

    private lateinit var playerTurnTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the TextView for displaying the current player's turn
        playerTurnTextView = findViewById(R.id.playerTurnTextView)

        // (Optional) Adjust for system bars if needed
        val mainLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Get a reference to the FrameLayout from activity_main.xml
        val container = findViewById<FrameLayout>(R.id.gameContainer)

        // 2. Create an instance of your custom TicTacToeDrawView
        val gameView = TicTacToeDrawView(this) { winner ->
            // This callback is triggered when the game ends.
            // Save the game result and navigate to your “game list” screen.
            saveGameResult(winner)
        }

        // Set a listener for player turn changes
        gameView.setOnPlayerTurnChangeListener { currentPlayer ->
            updatePlayerTurnText(currentPlayer)
        }

        // 3. Make sure the layout params fill the parent
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        // 4. Add the TicTacToeDrawView to the FrameLayout
        container.addView(gameView, params)

        // 5. Set the initial player's turn text
        updatePlayerTurnText(1) // Assuming the game starts with player X
    }

    private fun updatePlayerTurnText(currentPlayer: Int) {
        val spannableString = SpannableString("Turn   ")
        val drawable: Drawable = if (currentPlayer == 1) {
            ContextCompat.getDrawable(this, R.drawable.icon_x)!!
        } else {
            ContextCompat.getDrawable(this, R.drawable.icon_o)!!
        }

        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
        spannableString.setSpan(imageSpan, 5, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        playerTurnTextView.text = spannableString
    }

    private fun saveGameResult(winner: String) {
        // Example: Insert a new GameEntity into the database, then move to another activity
        CoroutineScope(Dispatchers.IO).launch {
            val game = GameEntity(
                winner = winner,
                movesCount = 0,
                timestamp = System.currentTimeMillis()
            )
            gameDao.insertGame(game)
        }
        // Optionally, switch to the list activity on the main thread
        val intent = Intent(this, GameListActivity::class.java)
        startActivity(intent)
    }
}