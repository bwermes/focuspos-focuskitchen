package com.amorphik.focuskitchen.allDayCount

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amorphik.focuskitchen.*
import kotlinx.android.synthetic.main.item_all_day_count_item.view.*

class AllDayCountAdapter(val items: List<AllDayCountRecord>): RecyclerView.Adapter<AllDayCountHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllDayCountHolder =
        AllDayCountHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_all_day_count_item, parent, false))

    override fun onBindViewHolder(holder: AllDayCountHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size
}


class AllDayCountHolder(val view: View): RecyclerView.ViewHolder(view){
    fun bind(item: AllDayCountRecord) =with(itemView){
        all_day_item_name.text = item.menuItemName
        all_day_item_count.text = item.displayCount
    }
}