package com.harbinger.parrot

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.harbinger.parrot.player.AudioPlayer
import com.harbinger.parrot.player.IAudioPlayer
import com.harbinger.parrot.player.PlayListener
import com.harbinger.parrot.vad.IVADRecorder
import com.harbinger.parrot.vad.VADListener
import com.harbinger.parrot.vad.VadRecorder
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks

class MainActivity : AppCompatActivity(), PermissionCallbacks {
    private val TAG = "VAD"
    private lateinit var statusTv: TextView
    private lateinit var parrotIv: ImageView
    private var vadRecorder: IVADRecorder? = null
    private var audioPlayer: IAudioPlayer? = null
    private var isRecording = false
    private var playAnimator: ObjectAnimator? = null
    private var recordAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR //设置状态栏黑色字体
        this.window.statusBarColor = resources.getColor(R.color.white)
        setContentView(R.layout.activity_main)
        initUI()
        initAnimator()
        initAudioPlayer()
        initRecorder()
    }

    private fun initUI() {
        statusTv = findViewById(R.id.status_tv)
        parrotIv = findViewById(R.id.parrot_iv)
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

                return false;
            }
        })
    }

    private fun initAnimator() {
        playAnimator = ObjectAnimator.ofFloat(parrotIv, "rotation", 360f)
        playAnimator?.repeatMode = ValueAnimator.RESTART
        playAnimator?.repeatCount = ObjectAnimator.INFINITE
        playAnimator?.duration = 2000
        recordAnimator = ObjectAnimator.ofFloat(parrotIv, "rotation", -360f)
        recordAnimator?.repeatMode = ValueAnimator.RESTART
        recordAnimator?.repeatCount = ObjectAnimator.INFINITE
        recordAnimator?.duration = 2000
    }

    private fun initAudioPlayer() {
        audioPlayer = AudioPlayer(this)
        audioPlayer?.setPlayListener(object : PlayListener {
            override fun onBegin() {
                recordAnimator?.pause()
                playAnim(playAnimator)
            }

            override fun onComplete() {
                startRecord()
                stopAllAnim()
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
            vadRecorder = VadRecorder(this.applicationContext)
            vadRecorder?.setVadListener(object : VADListener {
                override fun onBos() {
                    Log.d(TAG, "bos")
                    runOnUiThread {
                        statusTv.text = "bos"
                        playAnim(recordAnimator)
                    }
                }

                override fun onEos(recordPath: String) {
                    Log.d(TAG, "eos")
                    stopRecord()
                    runOnUiThread {
                        stopAllAnim()
                        audioPlayer?.play(recordPath)
                        statusTv.text = "eos"
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

    private fun playAnim(anim: ObjectAnimator?) {
        playAnimator?.pause()
        recordAnimator?.pause()
        anim?.start()
    }

    private fun stopAllAnim() {
        playAnimator?.pause()
        recordAnimator?.pause()
    }

    private fun startRecord() {
        vadRecorder?.start()
    }

    private fun stopRecord() {
        vadRecorder?.stop()
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