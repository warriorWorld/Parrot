package com.harbinger.parrot.director

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.harbinger.parrot.R
import com.harbinger.parrot.adapter.FileAdapter
import com.harbinger.parrot.bean.FileBean
import com.harbinger.parrot.bean.FileType
import com.harbinger.parrot.dialog.*
import com.harbinger.parrot.listener.OnRecycleItemClickListener
import com.harbinger.parrot.listener.OnRecycleItemLongClickListener
import com.harbinger.parrot.utils.FileUtil
import java.io.File
import java.util.*

/**
 * Created by acorn on 2020/11/21.
 */
class RecordListAcitivity : AppCompatActivity() {
    private val TAG = "RecordListAcitivity"
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
    private lateinit var progressSb: SeekBar
    private lateinit var playBtn: ImageView
    private lateinit var previousBtn: ImageView
    private lateinit var nextBtn: ImageView
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
//        mAudioPlayer = new AudioPlayer(this);
        val renderersFactory = buildRenderersFactory(applicationContext, true)  // 1
        val trackSelector = DefaultTrackSelector(applicationContext)  // 2
        exoPlayer = ExoPlayer.Builder(applicationContext, renderersFactory)  // 3
            .setTrackSelector(trackSelector)
            .build().apply {
                trackSelectionParameters =
                    DefaultTrackSelector.Parameters.Builder(applicationContext).build()  // 4
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        Log.d(TAG, "<<<<$playbackState>>>>")
//                if (playbackState == Player.STATE_ENDED) {
//                    notifyMimirAudioCompleted()
//                } else if (playbackState == Player.STATE_READY && null != currentMimirAudioStateListener) {
//                    if (null != mMimirView) {
//                        mMimirView.toggleState(MimirState.SPEAKING)
//                    }
//                }
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        super.onPlayWhenReadyChanged(playWhenReady, reason)
                        Log.d(TAG, "<<<<$playWhenReady,$reason>>>>")
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        Log.d(TAG, "<<<<$error>>>>")
//                notifyMimirAudioCompleted()
                    }
                })  // 5
                playWhenReady = false  // 6
            }
    }

    private fun buildRenderersFactory(
        context: Context, preferExtensionRenderer: Boolean
    ): RenderersFactory {
        val extensionRendererMode = if (preferExtensionRenderer)
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
        else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON

        return DefaultRenderersFactory(context.applicationContext)
            .setExtensionRendererMode(extensionRendererMode)
            .setEnableDecoderFallback(true)
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
        progressSb = findViewById(R.id.progress_sb)
        playBtn = findViewById(R.id.play_btn)
        previousBtn = findViewById(R.id.previous_btn)
        nextBtn = findViewById(R.id.next_btn)
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

    private fun play(index: Int, seekTo: Int = 0) {
        lastPlayPosition = index
        if (exoPlayer?.isPlaying == true) {
            exoPlayer?.stop()
        }
//        val path = "file:///${list[index].path}"
        val path=list[index].path
        Log.d(TAG, "play:$path")
        exoPlayer?.setMediaItem(MediaItem.fromUri(path))

        if (seekTo > 0) {
            exoPlayer?.seekTo(seekTo * 1000L)
        }
        exoPlayer?.prepare()
        exoPlayer?.play()
//        if (null != audioStateListener) {
//            audioStateListener.onGetDuration((exoPlayer!!.duration / 1000f).toInt())
//        }
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
                            exoPlayer?.pause()
                        } else {
                            play(it)
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
        exoPlayer?.pause()
    }
}