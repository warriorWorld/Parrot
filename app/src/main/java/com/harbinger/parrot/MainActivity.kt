package com.harbinger.parrot

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.harbinger.parrot.config.ShareKeys
import com.harbinger.parrot.dialog.EditDialog
import com.harbinger.parrot.dialog.EditDialogBuilder
import com.harbinger.parrot.director.RecordListAcitivity
import com.harbinger.parrot.player.AudioPlayer
import com.harbinger.parrot.player.IAudioPlayer
import com.harbinger.parrot.player.PlayListener
import com.harbinger.parrot.service.RecordService
import com.harbinger.parrot.utils.FileUtil
import com.harbinger.parrot.utils.ServiceUtil
import com.harbinger.parrot.utils.SharedPreferencesUtils
import com.harbinger.parrot.vad.IVADRecorder
import com.harbinger.parrot.vad.VADListener
import com.harbinger.parrot.vad.VadRecorder
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import java.io.File

enum class UIStatus {
    IDLE,
    RECORDING,
    PLAYING
}

class MainActivity : AppCompatActivity(), PermissionCallbacks {
    private val TAG = "VAD"
    private lateinit var statusTv: TextView
    private lateinit var parrotIv: ImageView
    private lateinit var batIv: ImageView
    private lateinit var flounderIv: ImageView
    private var vadRecorder: IVADRecorder? = null
    private var audioPlayer: IAudioPlayer? = null
    private var isRecording = false
    private var isRotaReverse = false
    private var parrotAnimator: ValueAnimator? = null
    private var currentStatus = UIStatus.IDLE
    private var curAngle = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR //设置状态栏黑色字体
            this.window.statusBarColor = resources.getColor(R.color.white)
        }
        setContentView(R.layout.activity_main)
        initUI()
        initAnimator()
        initAudioPlayer()
        initRecorder()
        clearAllRecord()
    }

    private fun clearAllRecord() {
        FileUtil.clearDirectory(File(FileUtil.getTempRecordDirectory()))
    }

    private fun initUI() {
        statusTv = findViewById(R.id.status_tv)
        parrotIv = findViewById(R.id.parrot_iv)
        batIv = findViewById(R.id.bat_iv)
        flounderIv = findViewById(R.id.flounder_iv)
        parrotIv.setOnClickListener {
            isRecording = if (isRecording) {
                stopRecord()
                false
            } else {
                startRecord()
                true
            }
        }
        parrotIv.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(p0: View?): Boolean {
                showParrotSettingsDialog()
                return false;
            }
        })
        batIv.setOnClickListener {
            val stopIntent = Intent(this, RecordService::class.java)
            if (ServiceUtil.isServiceWork(
                    this,
                    RecordService.SERVICE_PCK_NAME
                )
            ) {
                Toast.makeText(this@MainActivity,"stop service",Toast.LENGTH_SHORT).show()
                stopService(stopIntent)
            } else {
                Toast.makeText(this@MainActivity,"start service",Toast.LENGTH_SHORT).show()
                startService(Intent(this@MainActivity, RecordService::class.java))
            }
        }
        batIv.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(p0: View?): Boolean {
                showBatSettingsDialog()
                return false;
            }
        })
        flounderIv.setOnClickListener {
            startActivity(Intent(this@MainActivity, RecordListAcitivity::class.java))
        }
    }

    private fun initAnimator() {
        parrotAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 5000
            repeatMode = ObjectAnimator.RESTART
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                parrotIv.rotation = curAngle
                if (isRotaReverse)
                    curAngle--
                else {
                    curAngle++
                }
            }
        }
    }

    private fun initAudioPlayer() {
        audioPlayer = AudioPlayer(this)
        audioPlayer?.setPlayListener(object : PlayListener {
            override fun onBegin() {
                currentStatus = UIStatus.PLAYING
                refreshUI()
            }

            override fun onComplete() {
                startRecord()
                currentStatus = UIStatus.IDLE
                refreshUI()
            }
        })
    }

    @AfterPermissionGranted(0)
    private fun initRecorder() {
        val perms = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (EasyPermissions.hasPermissions(this, *perms)) {
            // Already have permission, do the thing
            // ...
            vadRecorder = VadRecorder(
                this.applicationContext,
                FileUtil.getTempRecordDirectory(),
                SharedPreferencesUtils.getIntSharedPreferencesData(
                    this,
                    ShareKeys.PARROT_SILENCE_DURATION
                    , 800
                ),
                SharedPreferencesUtils.getIntSharedPreferencesData(
                    this,
                    ShareKeys.PARROT_SPEECH_DURATION
                    , 800
                )
            )
            vadRecorder?.setVadListener(object : VADListener {
                override fun onBos() {
                    Log.d(TAG, "bos")
                    currentStatus = UIStatus.RECORDING
                    refreshUI()
                }

                override fun onEos(recordPath: String) {
                    Log.d(TAG, "eos")
                    runOnUiThread {
                        stopRecord()
                        audioPlayer?.play(recordPath)
                    }
                }
            })
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                this, "need record permission",
                0, *perms
            )
        }
    }

    private fun refreshUI() {
        runOnUiThread {
            when (currentStatus) {
                UIStatus.IDLE -> {
                    statusTv.text = "idle"
//                    parrotIv.setImageResource(R.drawable.ic_parrot1)
                    stopAnim()
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                UIStatus.RECORDING -> {
                    statusTv.text = "bos"
//                    parrotIv.setImageResource(R.drawable.ic_parrot2)
                    startAnim(false)
                    //不锁屏
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                UIStatus.PLAYING -> {
                    statusTv.text = "playing..."
//                    parrotIv.setImageResource(R.drawable.ic_parrot3)
                    startAnim(true)
                    //不锁屏
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    private fun startAnim(isReverse: Boolean) {
        if (parrotAnimator?.isStarted!!) {
            parrotAnimator?.cancel()
            isRotaReverse = isReverse
            parrotAnimator?.start()
        } else {
            parrotAnimator?.start()
        }
    }

    private fun stopAnim() {
        parrotAnimator?.pause()
    }

    private fun startRecord() {
        vadRecorder?.start()
        currentStatus = UIStatus.IDLE
        refreshUI()
    }

    private fun stopRecord() {
        vadRecorder?.stop()
        currentStatus = UIStatus.IDLE
        refreshUI()
    }

    private fun showParrotSettingsDialog() {
        val currentSilenceDuration = SharedPreferencesUtils.getIntSharedPreferencesData(
            this,
            ShareKeys.PARROT_SILENCE_DURATION
            , 800
        )
        val currentSpeechDurantion = SharedPreferencesUtils.getIntSharedPreferencesData(
            this,
            ShareKeys.PARROT_SPEECH_DURATION
            , 800
        )
        EditDialogBuilder(this)
            .setTitle("设置鹦鹉沉默、语音识别间隔时长(重启后生效)")
            .setHint("请输入沉默、语音间隔，以,分隔,单位毫秒.")
            .setInputText("$currentSilenceDuration,$currentSpeechDurantion")
            .setOkText("确定")
            .setTitleSize(16)
            .setCancelText("取消")
            .setTitleLeft(true)
            .setTitleBold(true)
            .setEditDialogListener(object : EditDialog.OnEditDialogClickListener {
                override fun onOkClick(result: String?) {
                    val inputs = result?.split(",")
                    if (inputs == null || inputs.size != 2) {
                        Toast.makeText(this@MainActivity, "输入的参数不合法", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val silenceDuration = inputs[0]
                    val speechDuration = inputs[1]
                    SharedPreferencesUtils.setSharedPreferencesData(
                        this@MainActivity,
                        ShareKeys.PARROT_SILENCE_DURATION,
                        Integer.valueOf(silenceDuration)
                    )
                    SharedPreferencesUtils.setSharedPreferencesData(
                        this@MainActivity,
                        ShareKeys.PARROT_SPEECH_DURATION,
                        Integer.valueOf(speechDuration)
                    )
                }

                override fun onCancelClick() {}
            })
            .create()
            .show()
    }

    private fun showBatSettingsDialog() {
        val currentSilenceDuration = SharedPreferencesUtils.getIntSharedPreferencesData(
            this,
            ShareKeys.RECORD_SILENCE_DURATION
            , 800
        )
        val currentSpeechDurantion = SharedPreferencesUtils.getIntSharedPreferencesData(
            this,
            ShareKeys.RECORD_SPEECH_DURATION
            , 800
        )
        EditDialogBuilder(this)
            .setTitle("设置蝙蝠沉默、语音识别间隔时长(重启后生效)")
            .setHint("请输入沉默、语音间隔，以,分隔,单位毫秒.")
            .setInputText("$currentSilenceDuration,$currentSpeechDurantion")
            .setOkText("确定")
            .setTitleSize(16)
            .setCancelText("取消")
            .setTitleLeft(true)
            .setTitleBold(true)
            .setEditDialogListener(object : EditDialog.OnEditDialogClickListener {
                override fun onOkClick(result: String?) {
                    val inputs = result?.split(",")
                    if (inputs == null || inputs.size != 2) {
                        Toast.makeText(this@MainActivity, "输入的参数不合法", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val silenceDuration = inputs[0]
                    val speechDuration = inputs[1]
                    SharedPreferencesUtils.setSharedPreferencesData(
                        this@MainActivity,
                        ShareKeys.RECORD_SILENCE_DURATION,
                        Integer.valueOf(silenceDuration)
                    )
                    SharedPreferencesUtils.setSharedPreferencesData(
                        this@MainActivity,
                        ShareKeys.RECORD_SPEECH_DURATION,
                        Integer.valueOf(speechDuration)
                    )
                }

                override fun onCancelClick() {}
            })
            .create()
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, "permission granted", Toast.LENGTH_SHORT).show()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show()
    }
}