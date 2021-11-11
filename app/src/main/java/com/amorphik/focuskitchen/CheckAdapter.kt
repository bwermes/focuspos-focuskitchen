package com.amorphik.focuskitchen

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_check.view.*
import java.text.SimpleDateFormat

class CheckAdapter(
    private val items: List<CheckDto>,
    private val listener: (CheckDto) -> Unit
): RecyclerView.Adapter<CheckAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_check, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position], listener)

    override fun getItemCount() = items.size

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view){
        fun bind(item: CheckDto, listener: (CheckDto) -> Unit) = with(itemView){
            var res = resources
            text_check_checkNumber.text = String.format(res.getString(R.string.check_number),String.format(item.checkNumber.toString()))
            text_check_tableName.text = String.format(res.getString(R.string.table_name),String.format(item.table.toString()))
            text_check_ownerName.text = String.format(res.getString(R.string.owner_name),String.format(item.owner.toString()))
            text_check_guestCount.text = String.format(res.getString(R.string.guest_count),String.format(item.numberOfGuests.toString()))

            text_check_dateTimeOpened.text = String.format(res.getString(R.string.value),String.format(item.timeOpened))
        }
    }
}

