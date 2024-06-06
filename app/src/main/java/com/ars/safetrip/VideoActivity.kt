package com.ars.safetrip

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.ars.safetrip.data.SafeTripAdapter
import com.ars.safetrip.database.AppDataBase
import com.ars.safetrip.database.SafeTripHistory
import com.ars.safetrip.databinding.ActivityVideoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Date

class VideoActivity : AppCompatActivity() {

    private lateinit var dataInicio : Date
    private lateinit var db: AppDataBase
    private lateinit var safeTripAdapter: SafeTripAdapter
    private lateinit var safeTripList: ArrayList<SafeTripHistory>
    private lateinit var viewBinding: ActivityVideoBinding
    private var audio: MediaPlayer? = null
    private var cameraJob: Job? = null
    private var estadoAtual: ApiResponse? = null
    private var fotoCapturada: ImageCapture? = null
    private var ocorrencias: Int = 0

    private val iniciarActivityResult = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissoes ->
        var permissoesAceitas = true
        permissoes.entries.forEach {
            if (it.key in PERMISSOES_OBRIGATORIAS && !it.value)
                permissoesAceitas = false
        }
        if (!permissoesAceitas) {
            Toast.makeText(baseContext, R.string.permissoes_negadas, Toast.LENGTH_SHORT).show()
        } else {
            iniciarCamera()
        }
    }

    companion object {
        private const val LINK_API = "https://ca07bbc3-2e6d-4248-8735-e5513da07d77-00-2cm3fh84x7v85.spock.replit.dev/api/image"
        private const val TAG = "CameraXApp"
        private const val TEMPO_ENVIO_API = 1000L
        private val PERMISSOES_OBRIGATORIAS = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.INTERNET).toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (permissoesAceitas()) {
            iniciarCamera()
        } else {
            permissoes()
        }

        viewBinding.ibVideoStart.setOnClickListener { gravacao() }

        viewBinding.ibHistoric.setOnClickListener { historico() }

        safeTripList = ArrayList()

        iniciarDados()
    }

    private fun permissoesAceitas() = PERMISSOES_OBRIGATORIAS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun iniciarCamera() {
        val camera = ProcessCameraProvider.getInstance(this)

        camera.addListener({
            val cameraProvider: ProcessCameraProvider = camera.get()

            val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(viewBinding.pvFinder.surfaceProvider)
                }

            fotoCapturada = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, fotoCapturada)
            } catch(exc: Exception) {
                Log.e(TAG, getString(R.string.falha_vinculacao_camera), exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun permissoes() {
        iniciarActivityResult.launch(PERMISSOES_OBRIGATORIAS)
    }

    private fun gravacao() {
        if (cameraJob?.isActive == true) {
            cameraJob?.cancel()
            pararGravacao()
            val dataFim = Date(System.currentTimeMillis())
            val tempo = (dataFim.time - dataInicio.time).toInt() / (1000 * 60)
            val dadosGravacao = SafeTripHistory(
                dateStart = dataInicio,
                dateEnd = dataFim,
                travelMinutes = tempo,
                travelOccurrences = ocorrencias)

            salvarDados(dadosGravacao)
            ocorrencias = 0
        } else {
            iniciarGravacao()
            cameraJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    foto()
                    delay(TEMPO_ENVIO_API)
                }
            }
        }
    }

    private fun historico() {
        val dialogView = layoutInflater.inflate(R.layout.historic_layout, null)

        val rv = dialogView.findViewById<RecyclerView>(R.id.rv_history)
        configurarRecyclerView(rv)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.show()

        val closeButton = dialogView.findViewById<ImageButton>(R.id.bt_close)
        closeButton.setOnClickListener { alertDialog.dismiss() }

        carregarDados()
    }

    private fun iniciarDados() {
        db = Room.databaseBuilder(
            applicationContext,
            AppDataBase::class.java, "database-history"
        ).allowMainThreadQueries().build()
    }

    private fun salvarDados(dadosGravacao: SafeTripHistory) {
        try {
            db.safeTripHistory().insertAll(dadosGravacao)
        } catch (e: Error) {
            db.safeTripHistory().getAll()
            db.safeTripHistory().insertAll(dadosGravacao)
        }
    }

    private fun carregarDados() {
        safeTripList.clear()
        safeTripList.addAll(db.safeTripHistory().getAll() as ArrayList<SafeTripHistory>)
    }

    private fun configurarRecyclerView(rv: RecyclerView) {
        rv.layoutManager = LinearLayoutManager(this)

        safeTripList = ArrayList()
        safeTripAdapter = SafeTripAdapter(safeTripList)
        rv.adapter = safeTripAdapter
    }

    private fun iniciarAlarme(soundResource: Int) {
        if (soundResource != 0) {
            audio = MediaPlayer.create(this, soundResource)
            audio?.isLooping = true
            audio?.start()
        }
    }

    private fun pararAlarme() {
        audio?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        audio = null
    }

    private fun foto() {
        val foto = fotoCapturada ?: return

        val arquivoFoto = File(externalCacheDir, "foto_capturada.jpg")
        val saidaDados = ImageCapture.OutputFileOptions.Builder(arquivoFoto).build()

        foto.takePicture(
            saidaDados, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, getString(R.string.falha_tirar_foto, exc.message), exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    enviarImagemApi(arquivoFoto)
                }
            }
        )
    }

    private fun enviarImagemApi(fotoArquivo: File) {
        val fotoBytes = fotoArquivo.readBytes()

        val requisicaoCorpo = fotoBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, fotoBytes.size)

        val requisicao = Request.Builder().url(LINK_API).post(requisicaoCorpo).build()

        OkHttpClient().newCall(requisicao).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e(TAG, getString(R.string.falha_enviar_api, e.message))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val message = response.body?.string()
                    if (!response.isSuccessful) {
                        Log.e(TAG, getString(R.string.codigo_resposta, response.message))
                        runOnUiThread {
                            handleApiResponse(message, ApiResponse.DORMINDO)
                        }
                    }
                    val estado = determinarEstado(message)
                    runOnUiThread {
                        handleApiResponse(message, estado)
                    }
                }
            }

        })
    }

    private fun iniciarGravacao() {
        dataInicio = Date(System.currentTimeMillis())
        viewBinding.ibHistoric.apply { isVisible = false }

        viewBinding.ibVideoStart.apply {
            contentDescription = getString(R.string.stop_capture)
            setImageResource(R.drawable.ic_stop_video)
            layoutParams.width = resources.getDimensionPixelSize(R.dimen.button_100)
            layoutParams.height = resources.getDimensionPixelSize(R.dimen.button_100)
            isEnabled = true
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pararGravacao() {
        viewBinding.ibHistoric.apply { isVisible = true }

        viewBinding.ibVideoStart.apply {
            contentDescription = getString(R.string.start_capture)
            setImageResource(R.drawable.ic_start_video)
            layoutParams.width = resources.getDimensionPixelSize(R.dimen.button_150)
            layoutParams.height = resources.getDimensionPixelSize(R.dimen.button_150)
            isEnabled = true
        }

        pararAlarme()

        viewBinding.ibDanger.isVisible = false
        viewBinding.ibWarning.isVisible = false

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun handleApiResponse(response: String?, message: ApiResponse) {
        Log.d(TAG, getString(R.string.resposta_api, response))

        when (message) {
            ApiResponse.ACORDADO -> {
                if (message != estadoAtual && cameraJob?.isActive == true) {
                    pararAlarme()
                    viewBinding.ibDanger.isVisible = false
                    viewBinding.ibWarning.isVisible = false
                }
            }
            ApiResponse.DESCONHECIDO -> {
                if (message != estadoAtual && cameraJob?.isActive == true) {
                    pararAlarme()
                    iniciarAlarme(R.raw.music_1)
                    viewBinding.ibWarning.isVisible = true
                    viewBinding.ibDanger.isVisible = false
                }
            }
            ApiResponse.DORMINDO -> {
                if (message != estadoAtual && cameraJob?.isActive == true) {
                    pararAlarme()
                    iniciarAlarme(R.raw.music_2)
                    viewBinding.ibDanger.isVisible = true
                    viewBinding.ibWarning.isVisible = false
                    ocorrencias++
                }
            }
            else -> {
                viewBinding.ibDanger.isVisible = false
                viewBinding.ibWarning.isVisible = false
            }
        }

        estadoAtual = message
    }

    private fun determinarEstado(responseBody: String?): ApiResponse {
        val responseJson = JSONObject(responseBody.toString())
        val message = responseJson.optString("message", "")

        return ApiResponse.entries.find { it.message.equals(message, ignoreCase = true) }
            ?: ApiResponse.DESCONHECIDO
    }

    enum class ApiResponse(val message: String) {
        ACORDADO("Acordado"),
        DORMINDO("Dormindo"),
        DESCONHECIDO("Desconhecido")
    }
}