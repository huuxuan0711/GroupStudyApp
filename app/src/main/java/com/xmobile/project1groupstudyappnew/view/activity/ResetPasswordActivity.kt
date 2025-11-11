package com.xmobile.project1groupstudyappnew.view.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivityResetPasswordBinding
import com.xmobile.project1groupstudyappnew.model.state.LoginAndRegisterUIState
import com.xmobile.project1groupstudyappnew.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResetPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initControl()
    }

    private fun initControl() {
        setupBackButton()
        setupConfirmButton()
        collectState()
    }

    private fun setupBackButton() {
        binding.backLayout.setOnClickListener { finish() }
    }

    private fun setupConfirmButton() {
        binding.btnConfirm.setOnClickListener {
            val oldPassword = binding.edtPassword.text.toString()
            val newPassword = binding.edtConfirmPassword.text.toString()
            viewModel.resetPassword(oldPassword, newPassword)
        }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginAndRegisterState.collect { state ->
                    when (state) {
                        LoginAndRegisterUIState.ConditionPassword -> showWarning(R.string.password_is_too_short)
                        LoginAndRegisterUIState.EmptyPassword -> showWarning(R.string.please_enter_password)
                        is LoginAndRegisterUIState.Error -> showWarning(state.message)
                        LoginAndRegisterUIState.SuccessResetPassword -> {
                            hideWarning()
                            Toast.makeText(this@ResetPasswordActivity, getString(R.string.change_password_success), Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showWarning(resId: Int) {
        binding.txtWarning.apply {
            visibility = View.VISIBLE
            text = getString(resId)
        }
    }

    private fun showWarning(message: String) {
        binding.txtWarning.apply {
            visibility = View.VISIBLE
            text = message
        }
    }

    private fun hideWarning() {
        binding.txtWarning.visibility = View.GONE
    }
}
