package com.harbinger.parrot.director

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harbinger.parrot.R
import com.harbinger.parrot.UIStatus
import com.harbinger.parrot.adapter.FileAdapter
import com.harbinger.parrot.bean.FileBean
import com.harbinger.parrot.listener.OnRecycleItemClickListener
import com.harbinger.parrot.player.AudioPlayer
import com.harbinger.parrot.player.IAudioPlayer
import com.harbinger.parrot.player.PlayListener
import com.harbinger.parrot.utils.FileUtil
import java.io.File
import java.util.ArrayList

/**
 * Created by acorn on 2020/11/21.
 */
class RecordListAcitivity : AppCompatActivity() {
    private var list = ArrayList<FileBean>()
    private lateinit var fileRcv: RecyclerView
    private lateinit var sizeTv: TextView
    private lateinit var deleteIv: ImageView
    private var mAdapter = FileAdapter(this)
    private val fileModel = FileModel()
    private var audioPlayer: IAudioPlayer? = null
    private var lastPlayPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR //设置状态栏黑色字体
            this.window.statusBarColor = resources.getColor(R.color.white)
        }
        setContentView(R.layout.activity_record_list)
        initUI()
        initAudioPlayer()
        doGetData()
    }

    private fun initAudioPlayer() {
        audioPlayer = AudioPlayer(this)
        audioPlayer?.setPlayListener(object : PlayListener {
            override fun onBegin() {
            }

            override fun onComplete() {
            }
        })
    }

    private fun doGetData() {
        list = fileModel.getFileList(FileUtil.getReservedRecordDirectory())
        initRec()
    }

    private fun initUI() {
        fileRcv = findViewById(R.id.file_rcv)
        sizeTv = findViewById(R.id.file_size_tv)
        deleteIv = findViewById(R.id.delete_iv)
        fileRcv.layoutManager = LinearLayoutManager(this)
        fileRcv.isFocusableInTouchMode = false
        fileRcv.isFocusable = false
        fileRcv.setHasFixedSize(true)
        fileRcv.adapter = mAdapter
        deleteIv.setOnClickListener {
            FileUtil.clearDirectory(File(FileUtil.getReservedRecordDirectory()))
            doGetData()
        }
    }

    private fun initRec() {
        try {
            mAdapter.setList(list)
            mAdapter.setCurrentPlaying(lastPlayPosition)
            mAdapter.notifyDataSetChanged()
            mAdapter.setOnItemClickListener(OnRecycleItemClickListener {
                if (it == lastPlayPosition) {
                    audioPlayer?.stop()
                } else {
                    lastPlayPosition = it
                    audioPlayer?.stop()
                    audioPlayer?.play(list[it].path)
                    initRec()
                }
            })
            sizeTv.text = "${list.size}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        audioPlayer?.stop()
    }
}