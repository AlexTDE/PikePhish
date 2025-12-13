package com.example.pikephish_v2.ui.main

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.pikephish_v2.R
import com.example.pikephish_v2.data.local.AppDatabase
import com.example.pikephish_v2.data.remote.PhishingApiService
import com.example.pikephish_v2.data.remote.PhishingCheckResponse
import com.example.pikephish_v2.data.repository.PhishingRepository
import com.example.pikephish_v2.data.scanner.UrlScanner
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var isBackgroundMode = false

    // Views
    private lateinit var toolbar: Toolbar
    private lateinit var modeSwitch: MaterialSwitch
    private lateinit var modeTitleText: TextView
    private lateinit var modeDescText: TextView
    private lateinit var manualModeLayout: LinearLayout
    private lateinit var backgroundModeLayout: LinearLayout
    private lateinit var urlInput: TextInputEditText
    private lateinit var pasteButton: Button
    private lateinit var checkButton: Button
    private lateinit var enableServiceButton: Button
    private lateinit var serviceStatusIcon: ImageView
    private lateinit var serviceStatusText: TextView
    private lateinit var resultCard: MaterialCardView
    private lateinit var resultIcon: ImageView
    private lateinit var resultTitle: TextView
    private lateinit var resultUrl: TextView
    private lateinit var resultMessage: TextView
    private lateinit var progressBar: ProgressBar

    // Repository
    private val repository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        PhishingRepository(
            apiService = PhishingApiService.create(useEmulator = true),
            urlScanner = UrlScanner(),
            historyDao = database.linkHistoryDao()  // ← Передаём DAO
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupToolbar()
        setupUI()
        setupListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        modeSwitch = findViewById(R.id.modeSwitch)
        modeTitleText = findViewById(R.id.modeTitleText)
        modeDescText = findViewById(R.id.modeDescText)
        manualModeLayout = findViewById(R.id.manualModeLayout)
        backgroundModeLayout = findViewById(R.id.backgroundModeLayout)
        urlInput = findViewById(R.id.urlInput)
        pasteButton = findViewById(R.id.pasteButton)
        checkButton = findViewById(R.id.checkButton)
        enableServiceButton = findViewById(R.id.enableServiceButton)
        serviceStatusIcon = findViewById(R.id.serviceStatusIcon)
        serviceStatusText = findViewById(R.id.serviceStatusText)
        resultCard = findViewById(R.id.resultCard)
        resultIcon = findViewById(R.id.resultIcon)
        resultTitle = findViewById(R.id.resultTitle)
        resultUrl = findViewById(R.id.resultUrl)
        resultMessage = findViewById(R.id.resultMessage)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun setupUI() {
        updateModeUI(isBackgroundMode)
    }

    private fun setupListeners() {
        // Mode Switch
        modeSwitch.setOnCheckedChangeListener { _, isChecked ->
            isBackgroundMode = isChecked
            updateModeUI(isChecked)
        }

        // Paste Button
        pasteButton.setOnClickListener {
            pasteFromClipboard()
        }

        // Check Button
        checkButton.setOnClickListener {
            checkLink()
        }

        // Enable Service Button
        enableServiceButton.setOnClickListener {
            enableAccessibilityService()
        }
    }

    private fun updateModeUI(backgroundMode: Boolean) {
        if (backgroundMode) {
            // Background Mode
            modeTitleText.text = getString(R.string.mode_background)
            modeDescText.text = "Автоматическая защита в реальном времени"
            manualModeLayout.visibility = View.GONE
            backgroundModeLayout.visibility = View.VISIBLE

            updateServiceStatus(false)
        } else {
            // Manual Mode
            modeTitleText.text = getString(R.string.mode_manual)
            modeDescText.text = "Проверяйте ссылки вручную"
            manualModeLayout.visibility = View.VISIBLE
            backgroundModeLayout.visibility = View.GONE
        }

        resultCard.visibility = View.GONE
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip

        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text.toString()
            urlInput.setText(text)
            Toast.makeText(this, "Вставлено из буфера обмена", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResult(response: PhishingCheckResponse) {
        resultCard.visibility = View.VISIBLE
        resultUrl.text = response.url

        if (response.isPhishing) {
            // 🚨 ФИШИНГ ОБНАРУЖЕН
            resultIcon.setImageResource(R.drawable.ic_warning_circle)
            resultTitle.text = getString(R.string.result_phishing_title)
            resultTitle.setTextColor(getColor(R.color.danger_red))

            val message = buildString {
                appendLine("Эта ссылка опасна!")
                appendLine()
                appendLine("Уверенность: ${(response.confidence * 100).toInt()}%")
                if (!response.reason.isNullOrEmpty()) {
                    appendLine()
                    appendLine("Причина:")
                    appendLine(response.reason)
                }
            }
            resultMessage.text = message

        } else {
            // ✅ ССЫЛКА БЕЗОПАСНА
            resultIcon.setImageResource(R.drawable.ic_check_circle)
            resultTitle.text = getString(R.string.result_safe_title)
            resultTitle.setTextColor(getColor(R.color.safe_green))

            val message = buildString {
                appendLine("Ссылка проверена и признана безопасной")
                appendLine()
                appendLine("Уверенность: ${(response.confidence * 100).toInt()}%")
            }
            resultMessage.text = message
        }
    }

    private fun showError(message: String) {
        resultCard.visibility = View.VISIBLE
        resultIcon.setImageResource(R.drawable.ic_warning_circle)
        resultTitle.text = getString(R.string.result_error_title)
        resultTitle.setTextColor(getColor(R.color.danger_red))
        resultUrl.text = ""
        resultMessage.text = message

        Toast.makeText(this, "Ошибка: $message", Toast.LENGTH_LONG).show()
    }

    private fun enableAccessibilityService() {
        Toast.makeText(
            this,
            "Откроются настройки специальных возможностей",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateServiceStatus(isEnabled: Boolean) {
        if (isEnabled) {
            serviceStatusIcon.setImageResource(R.drawable.ic_shield_check)
            serviceStatusText.text = getString(R.string.service_status_enabled)
            enableServiceButton.text = getString(R.string.disable_service_button)
        } else {
            serviceStatusIcon.setImageResource(R.drawable.ic_shield_off)
            serviceStatusText.text = getString(R.string.service_status_disabled)
            enableServiceButton.text = getString(R.string.enable_service_button)
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        resultCard.visibility = View.GONE
        checkButton.isEnabled = false
        urlInput.isEnabled = false
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        checkButton.isEnabled = true
        urlInput.isEnabled = true
    }

    private fun checkLink() {
        val url = urlInput.text.toString().trim()

        if (url.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_url), Toast.LENGTH_SHORT).show()
            return
        }

        showLoading()

        // Запускаем проверку через сервер
        lifecycleScope.launch {
            val result = repository.checkUrl(url, source = "manual")  // ← source = "manual" для ручной проверки

            hideLoading()

            if (result.isSuccess) {
                val response = result.getOrNull()!!
                showResult(response)

                // 📊 ВРЕМЕННЫЙ КОД: Проверяем количество записей в БД
                try {
                    val historyItems = repository.getRecentLinks()
                    historyItems.collect { items ->
                        val count = items.size
                        Toast.makeText(
                            this@MainActivity,
                            "✅ Сохранено в историю! Всего записей: $count",
                            Toast.LENGTH_LONG
                        ).show()

                        // Вывод в лог для отладки
                        Log.d("MainActivity", "📊 История содержит $count записей:")
                        items.forEachIndexed { index, item ->
                            Log.d("MainActivity", "${index + 1}. ${item.url} - ${if (item.isPhishing) "⚠️ Фишинг" else "✅ Безопасно"}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "❌ Ошибка чтения истории: ${e.message}")
                }

            } else {
                showError(result.exceptionOrNull()?.message ?: "Неизвестная ошибка")
            }
        }
    }

}
