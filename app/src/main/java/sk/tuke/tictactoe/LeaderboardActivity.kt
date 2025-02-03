package sk.tuke.tictactoe

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import sk.tuke.tictactoe.utils.FirestoreHelper

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var tvLeaderboard: TextView
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val TAG = "LeaderboardActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        tvLeaderboard = findViewById(R.id.tvLeaderboard)
        progressBar = findViewById(R.id.progressBar)

        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        progressBar.visibility = View.VISIBLE
        tvLeaderboard.visibility = View.GONE

        FirestoreHelper.fetchTopThreeScores(
            onSuccess = { entries ->
                Log.d(TAG, "Received ${entries.size} leaderboard entries")
                progressBar.visibility = View.GONE
                tvLeaderboard.visibility = View.VISIBLE

                if (entries.isEmpty()) {
                    tvLeaderboard.text = "No scores yet!"
                    return@fetchTopThreeScores
                }

                val leaderboardText = buildString {
                    append("🏆 Top Players 🏆\n\n")
                    entries.forEachIndexed { index, entry ->
                        val medal = when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "${index + 1}."
                        }
                        append("$medal ${entry.email}\n")
                        append("Score: ${entry.score}\n\n")
                    }
                }
                tvLeaderboard.text = leaderboardText
            },
            onError = { exception ->
                Log.e(TAG, "Error loading leaderboard", exception)
                progressBar.visibility = View.GONE
                tvLeaderboard.visibility = View.VISIBLE
                Toast.makeText(this, "Failed to load leaderboard", Toast.LENGTH_SHORT).show()
                tvLeaderboard.text = "Unable to load leaderboard.\nPlease try again later."
            }
        )
    }
}