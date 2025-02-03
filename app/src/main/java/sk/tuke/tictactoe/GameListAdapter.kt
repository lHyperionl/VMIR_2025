package sk.tuke.tictactoe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GameListAdapter(
    private val gameList: List<GameEntity>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<GameListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textWinner: TextView = itemView.findViewById(R.id.textWinner)
        val textTimestamp: TextView = itemView.findViewById(R.id.textTimestamp)

        init {
            itemView.setOnClickListener {
                val gameId = gameList[adapterPosition].id
                onItemClick(gameId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = gameList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = gameList[position]
        holder.textWinner.text = "Winner: ${game.winner}"
        holder.textTimestamp.text = "Time: ${game.timestamp}"
    }
}