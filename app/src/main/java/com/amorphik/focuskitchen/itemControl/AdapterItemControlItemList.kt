package com.amorphik.focuskitchen.itemControl

import android.content.Context
import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amorphik.focuskitchen.*
import kotlinx.android.synthetic.main.recycler_item_item_control_item_view.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.flexbox.AlignSelf

import com.google.android.flexbox.FlexboxLayoutManager

import android.R.drawable
import android.annotation.SuppressLint

import android.util.Log

import androidx.core.content.ContextCompat


class AdapterItemControlItemList(
    private var items: List<MenuItemRecord>,
    private val listener: (item: MenuItemRecord) -> Unit) : RecyclerView.Adapter<AdapterItemControlItemList.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context).inflate(
        R.layout.recycler_item_item_control_item_view, parent, false))

    override fun onBindViewHolder(holder: AdapterItemControlItemList.ViewHolder, position: Int) = holder.bind(items[position],listener)

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        @SuppressLint("WrongConstant", "ResourceAsColor")
        fun bind(
            item: MenuItemRecord,
            listener: (MenuItemRecord) -> Unit
        ) = with(itemView){
            var res = resources
            item_control_item_text_item_name.setTextColor(ContextCompat.getColor(FocusKitchenApplication.appContext, R.color.colorFocusLinkBlue))

            if(adapterPosition % 2 == 0)
            {
                this.rootView.setBackgroundResource(R.color.colorFocusLinkGray);
            }
            else
            {
                this.rootView.setBackgroundResource(R.color.white);
            }

            item_control_item_text_item_name.text = item.name
            if(!item.countdown && item.count != null && item.count!! == 0){
                item_control_text_item_count.visibility = View.INVISIBLE
            }else {
                item_control_text_item_count.text =  item.count.toString()
            }


            if(item.outOfStock && (item.count == null || !(item.count!! > 0))){
                item_control_image_out_of_stock.visibility = View.VISIBLE
            } else{
                item_control_image_out_of_stock.visibility = View.GONE
            }

            setOnClickListener {
            Log.d("menuItemEdit","click on ${item.menuItemId}")
                listener(item) }

        }


    }


}