package com.xmobile.project1groupstudyappnew.view.activity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivityForgetPasswordBinding
import com.xmobile.project1groupstudyappnew.model.state.PasswordUIState
import com.xmobile.project1groupstudyappnew.viewmodel.ForgetPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgetPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityForgetPasswordBinding
    private val viewModel: ForgetPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initControl()
        observePasswordState()
    }

    private fun initControl() {
        binding.backLayout.setOnClickListener { finish() }

        binding.edtEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.txtWarning.visibility = View.GONE
        }

        binding.btnSend.setOnClickListener {
            sendEmail()
        }

        binding.layoutResend.setOnClickListener {
            if (viewModel.countdown.value == 0) {
                sendEmail()
            }
        }
    }

    private fun sendEmail() {
        val email = binding.edtEmail.text?.toString()?.trim().orEmpty()
        if (email.isEmpty()) {
            binding.txtWarning.visibility = View.VISIBLE
            binding.txtWarning.text = getString(R.string.please_enter_email)
            return
        }

        binding.txtWarning.visibility = View.GONE
        binding.btnSend.isEnabled = false
        binding.layoutResend.isEnabled = false

        viewModel.sendEmail(email)
    }

    private fun observePasswordState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.passwordState.collect { state ->
                    binding.txtWarning.apply {
                        visibility = when (state) {
                            is PasswordUIState.Loading -> View.GONE
                            else -> View.VISIBLE
                        }

                        text = when (state) {
                            is PasswordUIState.EmptyEmail -> getString(R.string.please_enter_email)
                            is PasswordUIState.FormatEmail -> getString(R.string.email_is_incorrect_format)
                            is PasswordUIState.Success -> getString(R.string.please_check_your_email)
                            is PasswordUIState.Error -> state.message
                            else -> ""
                        }
                    }

                    binding.btnSend.isEnabled = when (state) {
                        is PasswordUIState.Success -> false
                        is PasswordUIState.Loading -> false
                        else -> true
                    }

                    if (state is PasswordUIState.Success) startCountdown()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.countdown.collect { seconds ->
                    binding.txtTimer.apply {
                        visibility = if (seconds > 0) View.VISIBLE else View.GONE
                        text = if (seconds > 0) formatSeconds(seconds) else ""
                    }
                    binding.layoutResend.isEnabled = seconds <= 0
                    binding.btnSend.isEnabled = seconds <= 0
                }
            }
        }
    }

    private fun startCountdown() {
        viewModel.startCountdown(60) // 60 giây
    }

    private fun formatSeconds(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return String.format("%02d:%02d", min, sec)
    }
}
