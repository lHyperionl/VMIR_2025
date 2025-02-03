package sk.tuke.tictactoe

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View

class TileAnimation(private val context: Context) {
    companion object {
        const val ANIMATION_DURATION = 200L
    }

    fun createMoveAnimation(view: View, fromX: Float, fromY: Float, toX: Float, toY: Float): AnimatorSet {
        val translateX = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, fromX, toX)
        val translateY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, fromY, toY)

        return AnimatorSet().apply {
            playTogether(translateX, translateY)
            duration = ANIMATION_DURATION
        }
    }

    fun createMergeAnimation(view: View): AnimatorSet {
        val scaleUpX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.2f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.2f, 1f)

        return AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY)
            duration = ANIMATION_DURATION
        }
    }

    fun createNewTileAnimation(view: View): AnimatorSet {
        view.scaleX = 0f
        view.scaleY = 0f
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f)

        return AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = ANIMATION_DURATION
        }
    }
}