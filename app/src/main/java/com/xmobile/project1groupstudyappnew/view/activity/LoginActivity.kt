package com.xmobile.project1groupstudyappnew.view.activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
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
import com.xmobile.project1groupstudyappnew.databinding.ActivityLoginBinding
import com.xmobile.project1groupstudyappnew.model.state.LoginAndRegisterUIState
import com.xmobile.project1groupstudyappnew.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var callbackManager: CallbackManager
    private val viewModel: LoginViewModel by viewModels()
    private var isVisiblePassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initControl()
    }

    private fun initControl() {
        callbackManager = CallbackManager.Factory.create()
        loadEmailFromIntent()
        setupNavigation()
        setupPasswordToggle()
        setupLoginActions()
        collectLoginState()
    }

    private fun loadEmailFromIntent() {
        intent.getStringExtra("user email")?.let {
            binding.edtEmail.setText(it)
        }
    }

    private fun setupNavigation() {
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.txtForgetPassword.setOnClickListener {
            startActivity(Intent(this, ForgetPasswordActivity::class.java))
        }
    }

    private fun setupPasswordToggle() {
        binding.imgShow.setOnClickListener {
            isVisiblePassword = !isVisiblePassword
            binding.edtPassword.inputType = if (isVisiblePassword) {
                binding.imgShow.setImageResource(R.drawable.ic_show)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.imgShow.setImageResource(R.drawable.ic_hide)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // Move cursor to end
            binding.edtPassword.setSelection(binding.edtPassword.text?.length ?: 0)
        }
    }

    private fun setupLoginActions() {
        binding.edtEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.txtWarning.visibility = View.GONE
        }
        binding.edtPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.txtWarning.visibility = View.GONE
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmail.text.toString()
            val password = binding.edtPassword.text.toString()
            viewModel.loginWithEmail(email, password)
        }

        binding.btnGoogle.setOnClickListener { viewModel.loginWithGoogle(this) }
        binding.btnFacebook.setOnClickListener { setupFacebookLogin() }
        binding.btnMicrosoft.setOnClickListener { viewModel.loginWithMicrosoft(this) }
    }

    private fun collectLoginState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is LoginAndRegisterUIState.Loading -> binding.txtWarning.visibility = View.GONE
                        is LoginAndRegisterUIState.EmptyEmail -> showWarning(R.string.please_enter_email)
                        is LoginAndRegisterUIState.EmptyPassword -> showWarning(R.string.please_enter_password)
                        is LoginAndRegisterUIState.FormatEmail -> showWarning(R.string.email_is_incorrect_format)
                        is LoginAndRegisterUIState.SuccessLogin -> handleSuccessLogin(state.user)
                        is LoginAndRegisterUIState.Error -> {
                            binding.txtWarning.visibility = View.VISIBLE
                            binding.txtWarning.text = state.message
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun setupFacebookLogin() {
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
                                    viewModel.loginWithFacebook(it) //save user to db
                                    handleSuccessLogin(it)
                                }
                            } else {
                                val exception = task.exception
                                if (exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                    // Email đã tồn tại với provider khác
                                    val existingEmail = exception.email
                                    val existingProviders = exception.updatedCredential?.let { listOf(it.provider) } ?: emptyList<String>()

                                    // Hiển thị thông báo hoặc tự động liên kết
                                    binding.txtWarning.visibility = View.VISIBLE
                                    binding.txtWarning.text = "Tài khoản đã tồn tại với email này. Vui lòng đăng nhập bằng provider: ${existingProviders.joinToString()}"

                                    // Nếu muốn tự động link, cần user login bằng provider cũ trước, rồi gọi:
                                    // firebaseAuth.currentUser?.linkWithCredential(credential)
                                } else {
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
        binding.txtWarning.visibility = View.VISIBLE
        binding.txtWarning.setText(resId)
    }

    private fun handleSuccessLogin(user: FirebaseUser) {
        binding.txtWarning.visibility = View.GONE

        getSharedPreferences("user", MODE_PRIVATE).edit {
            putString("userId", user.uid)
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}
