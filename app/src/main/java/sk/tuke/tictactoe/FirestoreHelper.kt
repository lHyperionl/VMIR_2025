// Create a new file: FirestoreHelper.kt
package sk.tuke.tictactoe.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FirestoreHelper {
    private const val TAG = "FirestoreHelper"

    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    fun updateHighScoreInFirestore(newScore: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Log.e(TAG, "No user logged in")
            return
        }

        val userId = currentUser.uid
        val userEmail = currentUser.email ?: run {
            Log.e(TAG, "User has no email")
            return
        }

        val userScoreData = hashMapOf(
            "score" to newScore,
            "email" to userEmail,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("leaderboard")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val existingScore = document.getLong("score") ?: 0L
                if (newScore > existingScore) {
                    db.collection("leaderboard")
                        .document(userId)
                        .set(userScoreData)
                }
            }
            .addOnFailureListener { e ->
                // If document doesn't exist, create it
                db.collection("leaderboard")
                    .document(userId)
                    .set(userScoreData)
            }
    }

    data class LeaderboardEntry(
        val email: String,
        val score: Long
    )

    fun fetchTopThreeScores(
        onSuccess: (List<LeaderboardEntry>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("leaderboard")
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val resultList = mutableListOf<LeaderboardEntry>()
                for (document in querySnapshot.documents) {
                    val email = document.getString("email") ?: continue
                    val score = document.getLong("score") ?: continue
                    resultList.add(LeaderboardEntry(email, score))
                }
                onSuccess(resultList)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}