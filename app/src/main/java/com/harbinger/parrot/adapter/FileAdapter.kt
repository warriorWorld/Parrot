package com.harbinger.parrot.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.harbinger.parrot.R
import com.harbinger.parrot.bean.FileBean
import com.harbinger.parrot.bean.FileType
import com.harbinger.parrot.listener.OnRecycleItemClickListener
import com.harbinger.parrot.listener.OnRecycleItemLongClickListener
import com.harbinger.parrot.utils.FileUtil
import com.harbinger.parrot.utils.StringUtil
import java.util.*

/**
 * Created by acorn on 2020/11/21.
 */
class FileAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var list: ArrayList<FileBean>? = null
    private var currentPlaying = -1
    private var onRecycleItemClickListener: OnRecycleItemClickListener? = null
    private var onRecycleItemLongClickListener: OnRecycleItemLongClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_recorder, parent, false)
        return NormalViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (null == list || list?.size == 0) {
            0
        } else {
            list?.size!!
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list!![position]

        (holder as NormalViewHolder).titleTv.text = item.name
        if (currentPlaying == position) {
            (holder as NormalViewHolder).titleTv.setTextColor(context.resources.getColor(R.color.colorPrimary))
        } else {
            (holder as NormalViewHolder).titleTv.setTextColor(context.resources.getColor(R.color.main_text_color))
        }

        when(item.fileType){
            FileType.FILE->{
                holder.sizeTv.text = FileUtil.getSize(item.fileSize)
                holder.iconIv.setImageResource(R.drawable.ic_flounder)
            }
            FileType.FOLDER->{
                holder.sizeTv.text =""
                holder.iconIv.setImageResource(R.drawable.ic_crab)
            }
        }
        if (item.modifiedDate == 0L) {
            (holder as NormalViewHolder).dateTv.visibility = View.GONE
        } else {
            (holder as NormalViewHolder).dateTv.text = StringUtil.getDateToString(
                item.modifiedDate,
                "yyyy-MM-dd HH:mm:ss"
            )
        }
        (holder as NormalViewHolder).itemRl.setOnClickListener(
            View.OnClickListener {
                onRecycleItemClickListener?.onItemClick(position)
            })
        holder.itemRl.setOnLongClickListener(View.OnLongClickListener {
            onRecycleItemLongClickListener?.onItemClick(position)
            return@OnLongClickListener true
        })
    }

    fun setList(list: ArrayList<FileBean>) {
        this.list = list
    }

    fun getList(): ArrayList<FileBean>? {
        return list
    }

    fun setCurrentPlaying(position: Int) {
        currentPlaying = position
    }

    fun setOnItemClickListener(listener: OnRecycleItemClickListener) {
        this.onRecycleItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: OnRecycleItemLongClickListener) {
        this.onRecycleItemLongClickListener = listener
    }

    class NormalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var itemRl: View
        var titleTv: TextView
        var iconIv: ImageView
        var dateTv: TextView
        var sizeTv: TextView

        init {
            itemRl = view.findViewById(R.id.item_rl) as View
            iconIv =
                view.findViewById<View>(R.id.item_icon) as ImageView
            titleTv = view.findViewById<View>(R.id.title_tv) as TextView
            dateTv = view.findViewById<View>(R.id.date_tv) as TextView
            sizeTv = view.findViewById<View>(R.id.size_tv) as TextView
        }
    }
}