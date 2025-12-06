package com.example.pikephish_v2.ui.main

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.pikephish_v2.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText

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

    private fun checkLink() {
        val url = urlInput.text.toString().trim()

        if (url.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_url), Toast.LENGTH_SHORT).show()
            return
        }

        showLoading()

        // Симуляция задержки проверки (2 секунды)
        resultCard.postDelayed({
            hideLoading()
            showDemoResult(url)
        }, 2000)
    }

    private fun showDemoResult(url: String) {
        // Заглушка: проверяем наличие слов "phish" или "fake"
        val isPhishing = url.contains("phish", ignoreCase = true) ||
                url.contains("fake", ignoreCase = true)

        resultCard.visibility = View.VISIBLE
        resultUrl.text = url

        if (isPhishing) {
            // Фишинг обнаружен
            resultIcon.setImageResource(R.drawable.ic_warning_circle)
            resultTitle.text = getString(R.string.result_phishing_title)
            resultTitle.setTextColor(getColor(R.color.danger_red))
            resultMessage.text = "Эта ссылка опасна! Не переходите по ней."
        } else {
            // Ссылка безопасна
            resultIcon.setImageResource(R.drawable.ic_check_circle)
            resultTitle.text = getString(R.string.result_safe_title)
            resultTitle.setTextColor(getColor(R.color.safe_green))
            resultMessage.text = "Уверенность: 95%"
        }
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
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        checkButton.isEnabled = true
    }
}
