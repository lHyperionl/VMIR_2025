package sk.tuke.tictactoe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Button for TicTacToe
        val btnTicTacToe = findViewById<Button>(R.id.btnTicTacToe)
        btnTicTacToe.setOnClickListener {
            // Open TicTacToe
            val intent = Intent(this, TicTacToeActivity::class.java)
            startActivity(intent)
        }

        // Button for 2048
        val btn2048 = findViewById<Button>(R.id.btn2048)
        btn2048.setOnClickListener {
            val intent = Intent(this, Game2048Activity::class.java)
            startActivity(intent)
        }

        // Button for Tetris (placeholder for now)
        val btnTetris = findViewById<Button>(R.id.btnTetris)
        btnTetris.setOnClickListener {
            // Placeholder code: In the future, open a TetrisActivity
            // Toast.makeText(this, "Tetris not yet implemented", Toast.LENGTH_SHORT).show()
        }
    }
}