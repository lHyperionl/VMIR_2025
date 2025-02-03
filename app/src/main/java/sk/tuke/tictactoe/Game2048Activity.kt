package sk.tuke.tictactoe

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import sk.tuke.tictactoe.utils.FirestoreHelper
import kotlin.random.Random


class Game2048Activity : AppCompatActivity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var tvScore: TextView
    private lateinit var tvHighscore: TextView
    private lateinit var btnRestart: Button
    private lateinit var btnUndo: Button
    private lateinit var btnLeaderboard: Button

    private lateinit var tileAnimation: TileAnimation
    private var isAnimating = false

    // 4x4 board to store tile values
    private val board = Array(4) { IntArray(4) }
    private var score = 0
    private var highscore = 0

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var gestureDetector: GestureDetector

    // 2) KEEP TRACK OF PREVIOUS BOARDS AND SCORES (UP TO 2 STATES)
    private val previousBoards = mutableListOf<Array<IntArray>>()
    private val previousScores = mutableListOf<Int>()
    private var undoCount = 2  // maximum allowed undos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_2048)

        // Initialize TileAnimation first
        tileAnimation = TileAnimation(this)

        // Initialize views
        gridLayout = findViewById(R.id.gameGrid)
        tvScore = findViewById(R.id.tvScore)
        tvHighscore = findViewById(R.id.tvHighscore)
        btnRestart = findViewById(R.id.btnRestart)
        btnUndo = findViewById(R.id.btnUndo)

        // Initialize leaderboard button
        btnLeaderboard = findViewById(R.id.btnLeaderboard)
        btnLeaderboard.setOnClickListener {
            // Navigate to LeaderboardActivity
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }

        // Initialize grid layout
        initializeGrid()
        // Initialize gesture detector
        initializeGestureDetector()

        // SharedPreferences for storing highscore
        sharedPrefs = getSharedPreferences("2048Highscore", Context.MODE_PRIVATE)
        highscore = sharedPrefs.getInt("HIGH_SCORE", 0)
        updateHighscoreDisplay()

        // Fetch the user's high score from Firestore and update if needed
        FirestoreHelper.fetchUserHighScore(
            onSuccess = { fetchedScore ->
                if (fetchedScore > highscore) {
                    highscore = fetchedScore
                    sharedPrefs.edit().putInt("HIGH_SCORE", highscore).apply()
                    updateHighscoreDisplay()
                }
            },
            onError = { exception ->
                // Log error or handle appropriately
                Log.e("Game2048Activity", "Failed to fetch high score: ${exception.message}")
            }
        )

        // Restart game
        btnRestart.setOnClickListener {
            startNewGame()
        }

        //SET UP UNDO BUTTON CLICK LOGIC
        btnUndo.setOnClickListener {
            if (undoCount > 0 && previousBoards.isNotEmpty()) {
                undoCount--
                // Revert to the most recent saved board
                val lastBoard = previousBoards.removeAt(previousBoards.size - 1)
                val lastScore = previousScores.removeAt(previousScores.size - 1)

                // Copy the saved board state back
                for (r in 0..3) {
                    for (c in 0..3) {
                        board[r][c] = lastBoard[r][c]
                    }
                }
                // Restore the score too
                score = lastScore
                updateScoreDisplay()
                updateBoardUI()
            }
        }

        // Start the game
        startNewGame()
    }

    private fun initializeGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                // Determine swipe direction
                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY)) {
                    // Left or Right
                    if (kotlin.math.abs(diffX) > SWIPE_THRESHOLD) {
                        if (diffX > 0) moveRight() else moveLeft()
                    }
                } else {
                    // Up or Down
                    if (kotlin.math.abs(diffY) > SWIPE_THRESHOLD) {
                        if (diffY > 0) moveDown() else moveUp()
                    }
                }
                return true
            }
        })
    }

    private data class TileMove(
        val fromRow: Int,
        val fromCol: Int,
        val toRow: Int,
        val toCol: Int,
        val value: Int,
        val merged: Boolean = false
    )

    private fun startNewGame() {
        // Reset board and score
        for (r in 0..3) {
            for (c in 0..3) {
                board[r][c] = 0
            }
        }
        score = 0
        updateScoreDisplay()
        undoCount = 2  // reset undo usage

        // Clear previous states since it's a brand new game
        previousBoards.clear()
        previousScores.clear()

        // Generate two initial tiles
        addRandomTile()
        addRandomTile()

        updateBoardUI()
    }

    private fun addRandomTile() {
        // Get empty positions
        val emptyPositions = mutableListOf<Pair<Int, Int>>()
        for (r in 0..3) {
            for (c in 0..3) {
                if (board[r][c] == 0) {
                    emptyPositions.add(Pair(r, c))
                }
            }
        }

        if (emptyPositions.isNotEmpty()) {
            val (row, col) = emptyPositions.random()
            val value = if (Random.nextDouble() < 0.9) 2 else 4
            board[row][col] = value

            // Get the corresponding tile view (offset by 16 for number tiles)
            val tileView = gridLayout.getChildAt((row * 4 + col) + 16) as TextView
            tileView.apply {
                text = value.toString()
                background = GradientDrawable().apply {
                    cornerRadius = 8f.dp
                    setColor(getTileColor(value))
                }
                setTextColor(getTextColor(value))
                visibility = View.VISIBLE
                scaleX = 0f
                scaleY = 0f
            }

            // Animate new tile appearance
            tileAnimation.createNewTileAnimation(tileView).start()
        }
    }

    private fun moveLeft() {
        if (isAnimating) return

        // Save pre-move state
        saveGameState()

        val oldBoard = copyBoard()
        val moves = mutableListOf<TileMove>()
        var scoreIncrease = 0

        for (r in 0..3) {
            val rowTiles = mutableListOf<Int>()
            val positions = mutableListOf<Int>()

            // Collect non-zero tiles and their positions
            for (c in 0..3) {
                if (board[r][c] != 0) {
                    rowTiles.add(board[r][c])
                    positions.add(c)
                }
            }

            var writePos = 0
            var readPos = 0

            while (readPos < rowTiles.size) {
                if (readPos + 1 < rowTiles.size && rowTiles[readPos] == rowTiles[readPos + 1]) {
                    // Merge tiles
                    val newValue = rowTiles[readPos] * 2
                    moves.add(TileMove(
                        r, positions[readPos],
                        r, writePos,
                        newValue,
                        true
                    ))
                    moves.add(TileMove(
                        r, positions[readPos + 1],
                        r, writePos,
                        newValue,
                        true
                    ))
                    board[r][writePos] = newValue
                    scoreIncrease += newValue
                    readPos += 2
                } else {
                    // Move tile
                    if (positions[readPos] != writePos) {
                        moves.add(TileMove(
                            r, positions[readPos],
                            r, writePos,
                            rowTiles[readPos]
                        ))
                    }
                    board[r][writePos] = rowTiles[readPos]
                    readPos++
                }
                writePos++
            }

            // Clear remaining positions
            while (writePos < 4) {
                board[r][writePos] = 0
                writePos++
            }
        }

        if (!boardsAreEqual(oldBoard, board)) {
            score += scoreIncrease
            animateMoves(moves) {
                updateScoreDisplay()
                addRandomTile()
                updateBoardUI()
                if (isGameOver()) {
                    // Handle game over
                }
            }
        }
    }

    private fun moveRight() {
        if (isAnimating) return

        // Save pre-move state
        saveGameState()

        val oldBoard = copyBoard()
        val moves = mutableListOf<TileMove>()
        var scoreIncrease = 0

        for (r in 0..3) {
            val rowTiles = mutableListOf<Int>()
            val positions = mutableListOf<Int>()

            // Collect non-zero tiles and their positions from right to left
            for (c in 3 downTo 0) {
                if (board[r][c] != 0) {
                    rowTiles.add(board[r][c])
                    positions.add(c)
                }
            }

            var writePos = 3
            var readPos = 0

            while (readPos < rowTiles.size) {
                if (readPos + 1 < rowTiles.size && rowTiles[readPos] == rowTiles[readPos + 1]) {
                    // Merge tiles
                    val newValue = rowTiles[readPos] * 2
                    moves.add(TileMove(
                        r, positions[readPos],
                        r, writePos,
                        newValue,
                        true
                    ))
                    moves.add(TileMove(
                        r, positions[readPos + 1],
                        r, writePos,
                        newValue,
                        true
                    ))
                    board[r][writePos] = newValue
                    scoreIncrease += newValue
                    readPos += 2
                } else {
                    // Move tile
                    if (positions[readPos] != writePos) {
                        moves.add(TileMove(
                            r, positions[readPos],
                            r, writePos,
                            rowTiles[readPos]
                        ))
                    }
                    board[r][writePos] = rowTiles[readPos]
                    readPos++
                }
                writePos--
            }

            // Clear remaining positions
            for (c in writePos downTo 0) {
                board[r][c] = 0
            }
        }

        if (!boardsAreEqual(oldBoard, board)) {
            score += scoreIncrease
            animateMoves(moves) {
                updateScoreDisplay()
                addRandomTile()
                updateBoardUI()
                if (isGameOver()) {
                    // Handle game over
                }
            }
        }
    }

    private fun moveUp() {
        if (isAnimating) return

        // Save pre-move state
        saveGameState()

        val oldBoard = copyBoard()
        val moves = mutableListOf<TileMove>()
        var scoreIncrease = 0

        for (c in 0..3) {
            val colTiles = mutableListOf<Int>()
            val positions = mutableListOf<Int>()

            // Collect non-zero tiles and their positions
            for (r in 0..3) {
                if (board[r][c] != 0) {
                    colTiles.add(board[r][c])
                    positions.add(r)
                }
            }

            var writePos = 0
            var readPos = 0

            while (readPos < colTiles.size) {
                if (readPos + 1 < colTiles.size && colTiles[readPos] == colTiles[readPos + 1]) {
                    // Merge tiles
                    val newValue = colTiles[readPos] * 2
                    moves.add(TileMove(
                        positions[readPos], c,
                        writePos, c,
                        newValue,
                        true
                    ))
                    moves.add(TileMove(
                        positions[readPos + 1], c,
                        writePos, c,
                        newValue,
                        true
                    ))
                    board[writePos][c] = newValue
                    scoreIncrease += newValue
                    readPos += 2
                } else {
                    // Move tile
                    if (positions[readPos] != writePos) {
                        moves.add(TileMove(
                            positions[readPos], c,
                            writePos, c,
                            colTiles[readPos]
                        ))
                    }
                    board[writePos][c] = colTiles[readPos]
                    readPos++
                }
                writePos++
            }

            // Clear remaining positions
            while (writePos < 4) {
                board[writePos][c] = 0
                writePos++
            }
        }

        if (!boardsAreEqual(oldBoard, board)) {
            score += scoreIncrease
            animateMoves(moves) {
                updateScoreDisplay()
                addRandomTile()
                updateBoardUI()
                if (isGameOver()) {
                    // Handle game over
                }
            }
        }
    }

    private fun moveDown() {
        if (isAnimating) return

        // Save pre-move state
        saveGameState()

        val oldBoard = copyBoard()
        val moves = mutableListOf<TileMove>()
        var scoreIncrease = 0

        for (c in 0..3) {
            val colTiles = mutableListOf<Int>()
            val positions = mutableListOf<Int>()

            // Collect non-zero tiles and their positions from bottom to top
            for (r in 3 downTo 0) {
                if (board[r][c] != 0) {
                    colTiles.add(board[r][c])
                    positions.add(r)
                }
            }

            var writePos = 3
            var readPos = 0

            while (readPos < colTiles.size) {
                if (readPos + 1 < colTiles.size && colTiles[readPos] == colTiles[readPos + 1]) {
                    // Merge tiles
                    val newValue = colTiles[readPos] * 2
                    moves.add(TileMove(
                        positions[readPos], c,
                        writePos, c,
                        newValue,
                        true
                    ))
                    moves.add(TileMove(
                        positions[readPos + 1], c,
                        writePos, c,
                        newValue,
                        true
                    ))
                    board[writePos][c] = newValue
                    scoreIncrease += newValue
                    readPos += 2
                } else {
                    // Move tile
                    if (positions[readPos] != writePos) {
                        moves.add(TileMove(
                            positions[readPos], c,
                            writePos, c,
                            colTiles[readPos]
                        ))
                    }
                    board[writePos][c] = colTiles[readPos]
                    readPos++
                }
                writePos--
            }

            // Clear remaining positions
            for (r in writePos downTo 0) {
                board[r][c] = 0
            }
        }

        if (!boardsAreEqual(oldBoard, board)) {
            score += scoreIncrease
            animateMoves(moves) {
                updateScoreDisplay()
                addRandomTile()
                updateBoardUI()
                if (isGameOver()) {
                    // Handle game over
                }
            }
        }
    }

    private fun checkBoardChangedAndAddTile(oldBoard: Array<IntArray>) {
        if (!boardsAreEqual(oldBoard, board)) {
            updateScoreDisplay()
            addRandomTile()
            updateBoardUI()
            if (isGameOver()) {
                // Handle game over (e.g. show dialog)
            }
        }
    }

    private fun animateMoves(moves: List<TileMove>, onComplete: () -> Unit) {
        if (moves.isEmpty()) {
            onComplete()
            return
        }

        isAnimating = true
        val allAnimations = mutableListOf<Animator>()
        val movesByDestination = moves.groupBy { "${it.toRow},${it.toCol}" }

        // Before any animation, set the appearance using move.value instead of board
        moves.forEach { move ->
            val fromTile = gridLayout.getChildAt(move.fromRow * 4 + move.fromCol + 16) as TextView
            fromTile.apply {
                text = move.value.toString()
                background = GradientDrawable().apply {
                    cornerRadius = 8f.dp
                    setColor(getTileColor(move.value))
                }
                setTextColor(getTextColor(move.value))
            }
        }

        // Now handle the animations
        movesByDestination.forEach { (_, movesToSameSpot) ->
            val isMerge = movesToSameSpot.size > 1

            movesToSameSpot.forEach { move ->
                val fromTile = gridLayout.getChildAt(move.fromRow * 4 + move.fromCol + 16) as TextView
                val toTile = gridLayout.getChildAt(move.toRow * 4 + move.toCol + 16) as TextView

                val fromPosition = IntArray(2)
                val toPosition = IntArray(2)
                fromTile.getLocationInWindow(fromPosition)
                toTile.getLocationInWindow(toPosition)

                val dx = (toPosition[0] - fromPosition[0]).toFloat()
                val dy = (toPosition[1] - fromPosition[1]).toFloat()

                // Create the movement animation
                val moveAnim = tileAnimation.createMoveAnimation(fromTile, 0f, 0f, dx, dy)

                // Merge animation if needed
                if (isMerge && move == movesToSameSpot.last()) {
                    val mergeAnim = tileAnimation.createMergeAnimation(fromTile)
                    val combinedAnim = AnimatorSet().apply {
                        play(moveAnim).before(mergeAnim)
                    }
                    allAnimations.add(combinedAnim)
                } else {
                    allAnimations.add(moveAnim)
                }
            }
        }

        AnimatorSet().apply {
            playTogether(allAnimations)
            duration = 150

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    gridLayout.isEnabled = false
                }

                override fun onAnimationEnd(animation: Animator) {
                    moves.forEach { move ->
                        val tile = gridLayout.getChildAt(move.fromRow * 4 + move.fromCol + 16) as TextView
                        tile.translationX = 0f
                        tile.translationY = 0f
                    }
                    updateBoardUI()
                    gridLayout.isEnabled = true
                    isAnimating = false
                    onComplete()
                }
            })
            start()
        }
    }

    private fun initializeGrid() {
        gridLayout.removeAllViews()

        // Calculate tile size considering padding and margins
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Consider padding and margins in size calculation
        val gridPadding = 16.dp
        val tileMargin = 4.dp
        val totalMargin = tileMargin * 2

        // Calculate available space
        val availableWidth = screenWidth - (32.dp)
        val tileSize = (availableWidth - gridPadding - (totalMargin * 4)) / 4

        // Set fixed size for GridLayout
        val totalGridSize = (tileSize * 4) + (totalMargin * 4) + gridPadding
        gridLayout.layoutParams = gridLayout.layoutParams.apply {
            width = totalGridSize
            height = totalGridSize
        }

        gridLayout.setPadding(8.dp, 8.dp, 8.dp, 8.dp)

        // Set grid background
        gridLayout.background = GradientDrawable().apply {
            cornerRadius = 16f.dp
            setColor(0xFFBBADA0.toInt())
        }

        // First create the background empty tiles (fixed)
        for (r in 0..3) {
            for (c in 0..3) {
                val emptyTile = TextView(this).apply {
                    width = tileSize
                    height = tileSize
                    gravity = android.view.Gravity.CENTER
                    textSize = 24f
                    z = 0f  // Keep background tiles at z=0

                    // Create rounded background for empty tile
                    background = GradientDrawable().apply {
                        cornerRadius = 8f.dp
                        setColor(getTileColor(0))  // Empty tile color
                    }

                    // Use exact size for layout params
                    val params = GridLayout.LayoutParams().apply {
                        width = tileSize
                        height = tileSize
                        setMargins(tileMargin, tileMargin, tileMargin, tileMargin)
                        rowSpec = GridLayout.spec(r)
                        columnSpec = GridLayout.spec(c)
                    }
                    layoutParams = params
                }
                gridLayout.addView(emptyTile)
            }
        }

        // Now create the number tiles (these will move)
        for (r in 0..3) {
            for (c in 0..3) {
                val tile = TextView(this).apply {
                    width = tileSize
                    height = tileSize
                    gravity = android.view.Gravity.CENTER
                    textSize = 24f
                    z = 1f  // Keep number tiles above background
                    visibility = View.INVISIBLE  // Start invisible

                    // Create rounded background for tile
                    background = GradientDrawable().apply {
                        cornerRadius = 8f.dp
                        setColor(getTileColor(0))
                    }

                    setTextColor(getTextColor(0))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD

                    // Use exact size for layout params
                    val params = GridLayout.LayoutParams().apply {
                        width = tileSize
                        height = tileSize
                        setMargins(tileMargin, tileMargin, tileMargin, tileMargin)
                        rowSpec = GridLayout.spec(r)
                        columnSpec = GridLayout.spec(c)
                    }
                    layoutParams = params
                }
                gridLayout.addView(tile)
            }
        }
    }

    // Extension property to convert dp to pixels
    private val Float.dp: Float
        get() = this * resources.displayMetrics.density

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    // Update the getTextColor function for better contrast
    private fun getTextColor(value: Int): Int {
        return if (value <= 4) {
            0xFF776E65.toInt() // Dark text for light tiles
        } else {
            0xFFF9F6F2.toInt() // Light text for dark tiles
        }
    }

    // Update updateBoardUI to handle the rounded corners
    private fun updateBoardUI() {
        for (r in 0..3) {
            for (c in 0..3) {
                val tileIndex = (r * 4 + c) + 16  // Offset by 16 to get to the number tiles
                val tile = gridLayout.getChildAt(tileIndex) as TextView
                val value = board[r][c]

                if (value > 0) {
                    tile.apply {
                        visibility = View.VISIBLE
                        text = value.toString()
                        z = 1f
                        background = GradientDrawable().apply {
                            cornerRadius = 8f.dp
                            setColor(getTileColor(value))
                        }
                        setTextColor(getTextColor(value))
                    }
                } else {
                    tile.apply {
                        visibility = View.INVISIBLE
                        text = ""
                        z = 1f
                    }
                }
            }
        }
    }

    // Optional: Update getTileColor for a more polished look
    private fun getTileColor(value: Int): Int {
        return when (value) {
            0 -> 0xFFCDC1B4.toInt()    // Empty tile
            2 -> 0xFFEEE4DA.toInt()    // 2
            4 -> 0xFFEDE0C8.toInt()    // 4
            8 -> 0xFFF2B179.toInt()    // 8
            16 -> 0xFFF59563.toInt()   // 16
            32 -> 0xFFF67C5F.toInt()   // 32
            64 -> 0xFFF65E3B.toInt()   // 64
            128 -> 0xFFEDCF72.toInt()  // 128
            256 -> 0xFFEDCC61.toInt()  // 256
            512 -> 0xFFEDC850.toInt()  // 512
            1024 -> 0xFFEDC53F.toInt() // 1024
            2048 -> 0xFFEDC22E.toInt() // 2048
            else -> 0xFF3C3A32.toInt() // Higher values
        }
    }

    private fun copyBoard(): Array<IntArray> {
        val copy = Array(4) { IntArray(4) }
        for (r in 0..3) {
            for (c in 0..3) {
                copy[r][c] = board[r][c]
            }
        }
        return copy
    }

    private fun boardsAreEqual(b1: Array<IntArray>, b2: Array<IntArray>): Boolean {
        for (r in 0..3) {
            for (c in 0..3) {
                if (b1[r][c] != b2[r][c]) return false
            }
        }
        return true
    }

    private fun isGameOver(): Boolean {
        // Check for empty cell
        for (r in 0..3) {
            for (c in 0..3) {
                if (board[r][c] == 0) return false
            }
        }
        // Check for any possible merge
        for (r in 0..3) {
            for (c in 0..3) {
                val current = board[r][c]
                // Right check
                if (c < 3 && current == board[r][c + 1]) return false
                // Down check
                if (r < 3 && current == board[r + 1][c]) return false
            }
        }
        handleGameOver()
        return true
    }

    private fun updateScoreDisplay() {
        tvScore.post {  // Ensure UI update happens on main thread
            tvScore.text = "Score: $score"

            // Update highscore if needed
            if (score > highscore) {
                highscore = score
                sharedPrefs.edit().putInt("HIGH_SCORE", highscore).apply()
                updateHighscoreDisplay()
            }
        }
    }

    private fun updateHighscoreDisplay() {
        tvHighscore.text = "Highscore: $highscore"
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Safely pass the touch event to the gestureDetector if event is not null
        if (event != null) {
            gestureDetector.onTouchEvent(event)
        }
        // Return true to indicate this event has been handled
        return true
    }

    // HELPER FUNCTION TO STORE CURRENT BOARD & SCORE
    private fun saveGameState() {
        // If we already have 2 states saved, remove the oldest to keep only 2 total
        if (previousBoards.size >= 2) {
            previousBoards.removeAt(0)
            previousScores.removeAt(0)
        }
        // Store a copy of the current board and score
        previousBoards.add(copyBoard())
        previousScores.add(score)
    }

    private fun handleGameOver() {
        // Update the score in Firestore once
        FirestoreHelper.updateHighScoreInFirestore(score)

        // No need to call showLeaderboard() here since we have a dedicated button for that
        // No need to update score twice
    }
}