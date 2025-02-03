package sk.tuke.tictactoe

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

@SuppressLint("ViewConstructor")
class TicTacToeDrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val onGameOver: (winner: String) -> Unit = {}
) : View(context, attrs) {

    // Paint for the grid lines (black) with rounded corners
    private val gridPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 25f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // Paint for X
    private val xPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 30f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // Paint for O
    private val oPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 30f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // Paint for the gradient background
    private val gradientPaint = Paint()

    // 3x3 board: 0 = empty, 1 = X, 2 = O
    private val board = Array(3) { IntArray(3) }
    private var currentPlayer = 1 // Start with X

    // Listener for player turn change
    private var onPlayerTurnChangeListener: ((Int) -> Unit)? = null

    fun setOnPlayerTurnChangeListener(listener: (Int) -> Unit) {
        onPlayerTurnChangeListener = listener
    }

    // Force the view to be a square
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = width * 0.05f
        val cellPadding = 50f // Increased padding between grid lines and rounded rectangle
        val cellSize = (width - 2 * padding - 2 * cellPadding) / 3f
        val gridSize = 3 * cellSize + 2 * cellPadding

        // Calculate the offset to center the grid within this view
        val offsetX = (width - gridSize) / 2
        val offsetY = (height - gridSize) / 2

        // Create the gradient shader
        val gradient = LinearGradient(
            0f, 0f, gridSize, gridSize,
            intArrayOf(Color.rgb(0, 0, 139), Color.rgb(139, 0, 0)), // Dark blue and dark red
            null,
            Shader.TileMode.CLAMP
        )

        // Create a bitmap and canvas for the gradient
        val gradientBitmap = Bitmap.createBitmap(gridSize.toInt() + 100, gridSize.toInt() + 100, Bitmap.Config.ARGB_8888)
        val gradientCanvas = Canvas(gradientBitmap)
        gradientCanvas.drawRect(0f, 0f, gridSize + 100f, gridSize + 100f, Paint().apply { shader = gradient })

        // Apply blur to the gradient bitmap
        val blurPaint = Paint().apply {
            maskFilter = BlurMaskFilter(50f, BlurMaskFilter.Blur.NORMAL) // Increase the blur radius
        }
        val blurredBitmap = Bitmap.createBitmap(gridSize.toInt(), gridSize.toInt(), Bitmap.Config.ARGB_8888)
        val blurredCanvas = Canvas(blurredBitmap)
        blurredCanvas.drawBitmap(gradientBitmap, -50f, -50f, blurPaint)

        // Create a BitmapShader from the blurred bitmap
        val bitmapShader = BitmapShader(blurredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        gradientPaint.shader = bitmapShader

        // Draw the blurred gradient with rounded corners on the canvas
        val outerRect = RectF(offsetX, offsetY, offsetX + gridSize, offsetY + gridSize)
        canvas.drawRoundRect(outerRect, 50f, 50f, gradientPaint)

        // Draw the outer rounded rectangle (grid lines)
        canvas.drawRoundRect(outerRect, 50f, 50f, gridPaint)

        // Draw the grid lines inside the rounded rectangle with padding
        for (i in 1 until 3) {
            canvas.drawLine(
                offsetX + cellPadding + i * cellSize, offsetY + cellPadding,
                offsetX + cellPadding + i * cellSize, offsetY + gridSize - cellPadding, gridPaint
            )
            canvas.drawLine(
                offsetX + cellPadding, offsetY + cellPadding + i * cellSize,
                offsetX + gridSize - cellPadding, offsetY + cellPadding + i * cellSize, gridPaint
            )
        }

        // Draw X or O in each cell
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                when (board[row][col]) {
                    1 -> drawX(canvas, offsetX, offsetY, cellPadding, cellSize, row, col)
                    2 -> drawO(canvas, offsetX, offsetY, cellPadding, cellSize, row, col)
                }
            }
        }
    }

    private fun drawX(canvas: Canvas, offsetX: Float, offsetY: Float, cellPadding: Float, cellSize: Float, row: Int, col: Int) {
        // Calculate the top-left and bottom-right coordinates of the cell
        val left = offsetX + col * cellSize + cellPadding
        val top = offsetY + row * cellSize + cellPadding
        val right = left + cellSize
        val bottom = top + cellSize

        // Margin for the X arms
        val margin = cellSize * 0.2f

        // Diagonal 1
        canvas.drawLine(left + margin, top + margin, right - margin, bottom - margin, xPaint)
        // Diagonal 2
        canvas.drawLine(right - margin, top + margin, left + margin, bottom - margin, xPaint)
    }

    private fun drawO(canvas: Canvas, offsetX: Float, offsetY: Float, cellPadding: Float, cellSize: Float, row: Int, col: Int) {
        // Calculate the center of the cell
        val left = offsetX + col * cellSize + cellPadding
        val top = offsetY + row * cellSize + cellPadding
        val centerX = left + cellSize / 2
        val centerY = top + cellSize / 2

        // Margin for the inner circle
        val margin = cellSize * 0.2f
        val radius = (cellSize - 2 * margin) / 2

        canvas.drawCircle(centerX, centerY, radius, oPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val width = width.toFloat()
            val height = height.toFloat()
            val padding = width * 0.05f
            val cellPadding = 50f
            val cellSize = (width - 2 * padding - 2 * cellPadding) / 3f
            val gridSize = 3 * cellSize + 2 * cellPadding

            // Use the same offsets as in onDraw
            val offsetX = (width - gridSize) / 2
            val offsetY = (height - gridSize) / 2

            val col = ((event.x - offsetX - cellPadding) / cellSize).toInt()
            val row = ((event.y - offsetY - cellPadding) / cellSize).toInt()

            // Check valid cell
            if (row in 0..2 && col in 0..2 && board[row][col] == 0) {
                board[row][col] = currentPlayer
                invalidate()

                // Check for a winner
                val winner = checkWinner()
                if (winner != 0) {
                    val winnerString = when (winner) {
                        1 -> "X"
                        2 -> "O"
                        else -> "None"
                    }
                    onGameOver(winnerString)
                } else if (isBoardFull()) {
                    onGameOver("None")
                }

                // Switch player and notify listener
                currentPlayer = if (currentPlayer == 1) 2 else 1
                onPlayerTurnChangeListener?.invoke(currentPlayer)
            }
        }
        return true
    }

    private fun checkWinner(): Int {
        // Rows
        for (i in 0 until 3) {
            if (board[i][0] != 0 && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
                return board[i][0]
            }
        }
        // Columns
        for (i in 0 until 3) {
            if (board[0][i] != 0 && board[0][i] == board[1][i] && board[1][i] == board[2][i]) {
                return board[0][i]
            }
        }
        // Diagonals
        if (board[0][0] != 0 && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            return board[0][0]
        }
        if (board[0][2] != 0 && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            return board[0][2]
        }
        return 0
    }

    private fun isBoardFull(): Boolean {
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                if (board[i][j] == 0) return false
            }
        }
        return true
    }
}