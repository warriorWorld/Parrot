package com.harbinger.parrot.director

import android.content.Context
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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.util.concurrent.TimeUnit

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
    private var lastPlayPosition = 0
    private val fileNameOptions = arrayOf("永久保存", "删除")
    private val permanentDirector = FileUtil.getPermanentRecordDirectory()
    private var isInPermanentDirector = false
    private lateinit var progressSb: SeekBar
    private lateinit var playBtn: ImageView
    private lateinit var previousBtn: ImageView
    private lateinit var nextBtn: ImageView
    private lateinit var remainTimeTv: TextView
    private var exoPlayer: ExoPlayer? = null
    private var audioProgressObserver: Disposable? = null
    private var currentTotalDurationInSecond = 0
    private var currentProcess = 0

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
                        if (playbackState == Player.STATE_ENDED) {
                            playNext()
                        } else if (playbackState == Player.STATE_READY) {
                            exoPlayer?.duration?.let {
                                currentTotalDurationInSecond = (it / 1000f).toInt()
                                remainTimeTv.text = timeFormat(0, currentTotalDurationInSecond)
                                progressSb.max = currentTotalDurationInSecond
                            }
                        }
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        super.onPlayWhenReadyChanged(playWhenReady, reason)
                        Log.d(TAG, "<<<<$playWhenReady,$reason>>>>")
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        Log.d(TAG, "<<<<$error>>>>")
                        playNext()
                    }
                })  // 5
                playWhenReady = false  // 6
            }
    }

    fun timeFormat(seconds: Int, totalSeconds: Int): String? {
        val left = Math.abs(totalSeconds - seconds)
        val leftMinutes = (left / 60f).toInt()
        val leftSeconds = left % 60
        val sb = StringBuilder()
        sb.append("-")
        sb.append(if (leftMinutes >= 10) leftMinutes else "0$leftMinutes")
        sb.append(":")
        sb.append(if (leftSeconds >= 10) leftSeconds else "0$leftSeconds")
        return sb.toString()
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
        progressSb.min = 0
        playBtn = findViewById(R.id.play_btn)
        previousBtn = findViewById(R.id.previous_btn)
        nextBtn = findViewById(R.id.next_btn)
        remainTimeTv = findViewById(R.id.remain_time_tv)
        fileRcv.layoutManager = LinearLayoutManager(this)
        fileRcv.isFocusableInTouchMode = false
        fileRcv.isFocusable = false
        fileRcv.setHasFixedSize(true)
        fileRcv.adapter = mAdapter
        playBtn.setOnClickListener {
            if (exoPlayer?.isPlaying == true) {
                pause()
            } else {
                play()
            }
        }
        nextBtn.setOnClickListener {
            playNext()
        }
        previousBtn.setOnClickListener {
            playPrevious()
        }
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
    private fun playPrevious() {
        lastPlayPosition--
        if (positionValidCheck()) {
            play(lastPlayPosition, 0)
        }
    }
    private fun playNext() {
        lastPlayPosition++
        if (positionValidCheck()) {
            play(lastPlayPosition, 0)
        }
    }

    private fun positionValidCheck(): Boolean {
        if (lastPlayPosition < 0) {
            lastPlayPosition = list.size-1
        }
        if (lastPlayPosition > list.size - 1) {
            lastPlayPosition = 0
        }
        Log.d(
            TAG,
            "filetype:${list[lastPlayPosition].fileType},path:${list[lastPlayPosition].path}"
        )
        if (list[lastPlayPosition].fileType != FileType.FILE ||
            (!list[lastPlayPosition].path.endsWith(".wav") &&
                    !list[lastPlayPosition].path.endsWith(".mp3"))
        ) {
            Log.d(TAG, "next:$lastPlayPosition")
            playNext()
            return false
        }
        return true
    }

    private fun play() {
        if (positionValidCheck()) {
            play(lastPlayPosition, currentProcess)
        }
    }

    private fun play(index: Int, seekTo: Int = 0) {
        playBtn.setImageResource(R.drawable.ic_pause)
        lastPlayPosition = index
        if (exoPlayer?.isPlaying == true) {
            exoPlayer?.stop()
        }
//        val path = "file:///${list[index].path}"
        val path = list[index].path
        Log.d(TAG, "play:$path")
        exoPlayer?.setMediaItem(MediaItem.fromUri(path))

        if (seekTo > 0) {
            exoPlayer?.seekTo(seekTo * 1000L)
        }
        exoPlayer?.prepare()
        exoPlayer?.play()
        initRec()
        audioProgressObserver?.dispose()
        audioProgressObserver = Observable.interval(200, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                if (null == exoPlayer) {
                    0
                } else (exoPlayer!!.currentPosition / 1000f).toInt()
            }
            .subscribe {
                progressSb.progress = it
                currentProcess=it
                remainTimeTv.text = timeFormat(it, currentTotalDurationInSecond)
            }
    }

    private fun pause() {
        playBtn.setImageResource(R.drawable.ic_play)
        exoPlayer?.pause()
        audioProgressObserver?.dispose()
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