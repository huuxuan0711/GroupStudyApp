package com.xmobile.project1groupstudyappnew.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.facebook.CallbackManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivityRegisterBinding
import com.xmobile.project1groupstudyappnew.model.state.LoginAndRegisterUIState
import com.xmobile.project1groupstudyappnew.viewmodel.RegisterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var callbackManager: CallbackManager
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initControl()
    }

    private fun initControl() {
        callbackManager = CallbackManager.Factory.create()
        setupRegisterWithEmail()
        setupRegisterWithGoogle()
        setupRegisterWithFacebook()
        setupRegisterWithMicrosoft()
        setupLoginRedirect()
        collectState()
    }

    private fun setupLoginRedirect() {
        binding.btnLogIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun setupRegisterWithEmail() {
        listOf(binding.edtEmail, binding.edtPassword, binding.edtConfirmPassword, binding.edtUserName)
            .forEach { editText ->
                editText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) hideWarning()
                }
            }

        binding.btnRegister.setOnClickListener {
            val email = binding.edtEmail.text.toString()
            val password = binding.edtPassword.text.toString()
            val confirmPassword = binding.edtConfirmPassword.text.toString()
            val userName = binding.edtUserName.text.toString()
            viewModel.registerWithEmail(email, password, confirmPassword, userName)
        }
    }

    private fun setupRegisterWithGoogle() {
        binding.btnGoogle.setOnClickListener { viewModel.registerWithGoogle(this) }
    }

    private fun setupRegisterWithFacebook() {
        binding.btnFacebook.setOnClickListener { registerWithFacebook() }
    }

    private fun setupRegisterWithMicrosoft() {
        binding.btnMicrosoft.setOnClickListener { viewModel.registerWithMicrosoft(this) }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerResult.collect { state ->
                    when (state) {
                        is LoginAndRegisterUIState.Loading -> hideWarning()
                        is LoginAndRegisterUIState.EmptyEmail -> showWarning(R.string.please_enter_email)
                        is LoginAndRegisterUIState.EmptyPassword -> showWarning(R.string.please_enter_password)
                        is LoginAndRegisterUIState.FormatEmail -> showWarning(R.string.email_is_incorrect_format)
                        is LoginAndRegisterUIState.EmptyUserName -> showWarning(R.string.please_enter_username)
                        is LoginAndRegisterUIState.ConditionPassword -> showWarning(R.string.password_is_too_short)
                        is LoginAndRegisterUIState.ConditionUserName -> showWarning(R.string.user_name_is_too_short)
                        is LoginAndRegisterUIState.MatchingPassword -> showWarning(R.string.password_does_not_match)
                        is LoginAndRegisterUIState.SuccessRegister -> redirectToLogin(state.user)
                        is LoginAndRegisterUIState.SuccessLogin -> loginWithService(state.user)
                        is LoginAndRegisterUIState.Error -> showWarning(state.message)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun registerWithFacebook() {
        // Đăng ký callback
        com.facebook.login.LoginManager.getInstance().registerCallback(callbackManager,
            object : com.facebook.FacebookCallback<com.facebook.login.LoginResult> {
                override fun onSuccess(result: com.facebook.login.LoginResult) {
                    val facebookToken = result.accessToken.token
                    val credential = com.google.firebase.auth.FacebookAuthProvider.getCredential(facebookToken)

                    val firebaseAuth = FirebaseAuth.getInstance()

                    // Thử đăng nhập với credential Facebook
                    firebaseAuth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Login thành công
                                task.result?.user?.let {
                                    viewModel.registerWithFacebook(it)
                                    loginWithService(it)
                                }
                            } else {
                                val exception = task.exception
                                if (exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                    // Email đã tồn tại với provider khác
                                    binding.txtWarning.visibility = View.VISIBLE
                                    binding.txtWarning.text =
                                        "Tài khoản đã tồn tại với email này. Vui lòng đăng nhập bằng provider liên kết"

                                    // Nếu muốn tự động link, cần user login bằng provider cũ trước, rồi gọi:
                                    // firebaseAuth.currentUser?.linkWithCredential(credential)
                                } else {
                                    // Lỗi khác
                                    binding.txtWarning.visibility = View.VISIBLE
                                    binding.txtWarning.text = exception?.message
                                }
                            }
                        }
                }

                override fun onCancel() {
                    binding.txtWarning.visibility = View.VISIBLE
                    binding.txtWarning.text = "Người dùng hủy đăng nhập Facebook"
                }

                override fun onError(error: com.facebook.FacebookException) {
                    binding.txtWarning.visibility = View.VISIBLE
                    binding.txtWarning.text = error.message
                }
            }
        )

        // Bắt đầu login
        com.facebook.login.LoginManager.getInstance()
            .logInWithReadPermissions(this, listOf("email", "public_profile"))
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

    private fun redirectToLogin(user: FirebaseUser) {
        hideWarning()
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("user email", user.email)
        }
        startActivity(intent)
    }

    private fun loginWithService(user: FirebaseUser) {
        hideWarning()
        val sharedPreferences = getSharedPreferences("user", MODE_PRIVATE)
        sharedPreferences.edit { putString("userId", user.uid) }
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("user", user)
        }
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}
