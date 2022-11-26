package com.harbinger.parrot.director

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.harbinger.parrot.R
import com.harbinger.parrot.adapter.FileAdapter
import com.harbinger.parrot.bean.FileBean
import com.harbinger.parrot.bean.FileType
import com.harbinger.parrot.dialog.*
import com.harbinger.parrot.listener.OnRecycleItemClickListener
import com.harbinger.parrot.listener.OnRecycleItemLongClickListener
import com.harbinger.parrot.utils.FileUtil
import java.io.File

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
    private var lastPlayPosition = -1
    private val fileNameOptions = arrayOf("永久保存", "删除")
    private val permanentDirector = FileUtil.getPermanentRecordDirectory()
    private var isInPermanentDirector = false
    private var exoPlayer: ExoPlayer? = null

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
        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer?.playWhenReady = true
    }

    private fun doGetData() {
        isInPermanentDirector = false
        list = fileModel.getFileList(FileUtil.getReservedRecordDirectory())
        initRec()
        deleteIv.visibility = View.VISIBLE
    }

    private fun doGetPermanentData() {
        isInPermanentDirector = true
        list = fileModel.getFileList(FileUtil.getPermanentRecordDirectory().path)
        initRec()
        deleteIv.visibility = View.GONE
    }

    override fun onBackPressed() {
        if (isInPermanentDirector) {
            doGetData()
            return
        }
        super.onBackPressed()
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
            NormalDialogBuilder(this)
                .setTitle("是否删除全部录音？")
                .setOkText("是")
                .setCancelText("否")
                .setOnDialogClickListener(object : NormalDialog.OnDialogClickListener {
                    override fun onCancelClick() {
                    }

                    override fun onOkClick() {
                        FileUtil.clearDirectoryExcept(
                            File(FileUtil.getReservedRecordDirectory()),
                            permanentDirector
                        )
                        doGetData()
                    }
                })
                .create()
                .show()
        }
    }

    private fun initRec() {
        try {
            mAdapter.setList(list)
            mAdapter.setCurrentPlaying(lastPlayPosition)
            mAdapter.notifyDataSetChanged()
            mAdapter.setOnItemClickListener(OnRecycleItemClickListener {
                when (list[it].fileType) {
                    FileType.FOLDER -> {
                        doGetPermanentData()
                    }
                    FileType.FILE -> {
                        if (it == lastPlayPosition) {
                            exoPlayer?.stop()
                        } else {
                            lastPlayPosition = it
                            playAudio(list[it].path)
                            initRec()
                        }
                    }
                }
            })
            mAdapter.setOnItemLongClickListener(OnRecycleItemLongClickListener {
                if (list[it].fileType == FileType.FOLDER || isInPermanentDirector) {
                    return@OnRecycleItemLongClickListener
                }
                showOptionsSelectorDialog(it)
            })
            sizeTv.text = "${list.size}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playAudio(path: String) {
        if (exoPlayer?.isPlaying == true) {
            exoPlayer?.stop()
        }
        exoPlayer?.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
        exoPlayer?.prepare()
        exoPlayer?.play()
    }

    private fun showOptionsSelectorDialog(position: Int) {
        val listDialog = ListDialog(this)
        listDialog.setOnRecycleItemClickListener {
            when (it) {
                0 -> {
                    EditDialogBuilder(this)
                        .setTitle("重命名并移动至真·永久保留区")
                        .setOkText("保存")
                        .setCancelText("取消")
                        .setEditDialogListener(object : EditDialog.OnEditDialogClickListener {
                            override fun onOkClick(result: String?) {
                                if (!TextUtils.isEmpty(result)) {
                                    val file = File(list[position].path)
                                    val renamedPath =
                                        file.parentFile?.path + File.separator + result + ".wav"
                                    file.renameTo(File(renamedPath))
                                    FileUtil.moveFile(
                                        renamedPath,
                                        permanentDirector.path
                                    )
                                    doGetData()
                                }
                            }

                            override fun onCancelClick() {
                            }
                        })
                        .create()
                        .show()
                }
                1 -> {
                    NormalDialogBuilder(this)
                        .setTitle("是否删除该录音?")
                        .setOkText("是")
                        .setCancelText("否")
                        .setOnDialogClickListener(object : NormalDialog.OnDialogClickListener {
                            override fun onOkClick() {
                                FileUtil.deleteFile(File(list[position].path))
                                doGetData()
                            }

                            override fun onCancelClick() {
                            }
                        })
                        .create()
                        .show()
                }
            }
        }
        listDialog.show()
        listDialog.setOptionsList(fileNameOptions)
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }
}