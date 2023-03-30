package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.myapplication.databinding.ActivityMainBinding
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var audioRecord: AudioRecord
    private val bufferSize = 1024
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler = Handler(Looper.getMainLooper())

        binding.startButton.setOnClickListener {
            startRecording()
        }

        binding.stopButton.setOnClickListener {
            stopRecording()
        }

        runnable = Runnable {
            val frequency = getFrequency()
            binding.frequencyTextView.text = "Frequency: $frequency Hz"
            handler.postDelayed(runnable, 100)
        }
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord.startRecording()
        isRecording = true
        handler.post(runnable)
    }

    private fun stopRecording() {
        if (::audioRecord.isInitialized && isRecording) {
            audioRecord.stop()
            audioRecord.release()
            isRecording = false
            handler.removeCallbacks(runnable)
        }
    }

    private fun getFrequency(): Int {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return -1 // return error value if permissions not granted
        }

        val audioRecord = AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize)
        //if(audioRecord.state == AudioRecord.STATE_INITIALIZED)
        audioRecord.startRecording()

        val audioData = ShortArray(bufferSize)

        var read = 0
        while (audioRecord.state == AudioRecord.STATE_INITIALIZED && read < audioData.size) {
            val bufferRead = audioRecord.read(audioData, read, audioData.size-read)
            if (bufferRead <= 0) {
                break
            }

            read += bufferRead
        }


        audioRecord.stop()
        audioRecord.release()
        //val filteredData = HighPassFilter(audioData, SAMPLE_RATE, 300.0).apply()

        val transformer = FastFourierTransformer(DftNormalization.STANDARD)
        val complex = transformer.transform(audioData.map { it.toDouble() }.toDoubleArray(), TransformType.FORWARD)

        val frequencies = complex.indices.map { it * SAMPLE_RATE.toDouble() / complex.size }

        val maxMagnitudeIndex = complex.indices.maxByOrNull { complex[it].abs() } ?: 0
        val frequency = frequencies[maxMagnitudeIndex].toInt()

        return frequency
    }

    companion object {
        const val SAMPLE_RATE = 44100
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }


}
