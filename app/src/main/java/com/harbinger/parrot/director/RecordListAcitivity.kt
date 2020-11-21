package com.harbinger.parrot.director

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harbinger.parrot.R
import com.harbinger.parrot.adapter.FileAdapter
import com.harbinger.parrot.bean.FileBean
import com.harbinger.parrot.listener.OnRecycleItemClickListener
import com.harbinger.parrot.utils.FileUtil
import java.util.ArrayList

/**
 * Created by acorn on 2020/11/21.
 */
class RecordListAcitivity : AppCompatActivity() {
    private var list = ArrayList<FileBean>()
    private lateinit var fileRcv: RecyclerView
    private lateinit var sizeTv: TextView
    private var mAdapter = FileAdapter()
    private val fileModel = FileModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR //设置状态栏黑色字体
            this.window.statusBarColor = resources.getColor(R.color.white)
        }
        setContentView(R.layout.activity_record_list)
        initUI()
        doGetData()
    }

    private fun doGetData() {
        list = fileModel.getFileList(FileUtil.getReservedRecordDirectory())
        initRec()
    }

    private fun initUI() {
        fileRcv = findViewById(R.id.file_rcv)
        sizeTv = findViewById(R.id.file_size_tv)
        fileRcv.layoutManager = LinearLayoutManager(this)
        fileRcv.isFocusableInTouchMode = false
        fileRcv.isFocusable = false
        fileRcv.setHasFixedSize(true)
        fileRcv.adapter = mAdapter
    }

    private fun initRec() {
        try {
            mAdapter.setList(list)
            mAdapter.notifyDataSetChanged()
            mAdapter.setOnItemClickListener(OnRecycleItemClickListener {

            })
            sizeTv.text = "${list.size}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}