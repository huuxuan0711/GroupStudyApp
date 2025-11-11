package com.xmobile.project1groupstudyappnew.view.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.FragmentProfileBinding
import com.xmobile.project1groupstudyappnew.model.obj.user.User
import com.xmobile.project1groupstudyappnew.model.state.UserUIState
import com.xmobile.project1groupstudyappnew.view.activity.ProfileDetailActivity
import com.xmobile.project1groupstudyappnew.view.activity.ResetPasswordActivity
import com.xmobile.project1groupstudyappnew.view.activity.StartActivity
import com.xmobile.project1groupstudyappnew.view.adapter.FeatureAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.UserViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.xmobile.project1groupstudyappnew.utils.ui.avatar.AvatarLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {
    private lateinit var binding: FragmentProfileBinding
    private var userId: String? = null
    private var user: User? = null
    private var currentType = -1
    private var recyclerViewFeature: RecyclerView? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private val listChoose: MutableList<String> = mutableListOf()
    private val listChooseID: MutableList<Int> = mutableListOf()
    private var adapter: FeatureAdapter? = null

    private val viewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        initControl()
        return binding.root
    }

    private fun initControl() {
        getData()

        binding.cardProfile.setOnClickListener {
            user?.let {
                val intent = Intent(requireContext(), ProfileDetailActivity::class.java)
                intent.putExtra("user", it)
                startActivity(intent)
            }
        }

        binding.layoutTheme.setOnClickListener { featureSetting(1) }
        binding.layoutFont.setOnClickListener { featureSetting(2) }
        binding.layoutLanguages.setOnClickListener { featureSetting(3) }

        binding.layoutChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ResetPasswordActivity::class.java))
        }

        binding.layoutLogout.setOnClickListener {
            userId?.let { viewModel.logout(it, requireContext()) }
        }

        collectState()
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userState.collect { state ->
                    when (state) {
                        is UserUIState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        is UserUIState.SuccessGetInfo -> {
                            user = state.user
                            displayData()
                        }
                        UserUIState.SuccessLogout -> {
                            requireContext().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE).edit { clear() }
                            startActivity(Intent(requireContext(), StartActivity::class.java))
                            requireActivity().finish()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun featureSetting(type: Int) {
        currentType = type
        val bottomSheet: ConstraintLayout = binding.root.findViewById(R.id.bottomSheetFeatureSetting)
        val dimView: View = binding.dimView
        setupBottomSheet(bottomSheet, dimView)
        setupCancelButton(bottomSheet)
        setupDimViewTouchBlocker(dimView)

        val txtFeatureName = bottomSheet.findViewById<TextView>(R.id.txtFeatureName)
        recyclerViewFeature = bottomSheet.findViewById(R.id.recyclerViewFeature)
        txtFeatureName?.let { setupFeatureData(type, it) }
        setupRecyclerView(type)
        setupBottomSheetCallbacks(dimView)
    }

    private fun setupBottomSheet(bottomSheet: View, dimView: View) {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior?.isHideable = true
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        dimView.visibility = View.VISIBLE
    }

    private fun setupCancelButton(bottomSheet: View) {
        bottomSheet.findViewById<TextView?>(R.id.txtCancel)?.setOnClickListener {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDimViewTouchBlocker(dimView: View) {
        dimView.setOnTouchListener { _, _ -> true }
    }

    private fun setupFeatureData(type: Int, txtFeatureName: TextView) {
        listChoose.clear()
        listChooseID.clear()

        when (type) {
            1 -> {
                txtFeatureName.text = getString(R.string.theme)
                addOptions(intArrayOf(R.string.dark, R.string.light, R.string.auto))
            }
            2 -> {
                txtFeatureName.text = getString(R.string.system_font)
                listChoose.addAll(listOf("Lato", "Nunito Sans", "Open Sans", "Roboto", "Inter"))
            }
            3 -> {
                txtFeatureName.text = getString(R.string.language)
                addOptions(intArrayOf(R.string.vietnamese, R.string.english))
            }
        }
    }

    private fun addOptions(optionIds: IntArray) {
        for (id in optionIds) {
            listChoose.add(getString(id))
            listChooseID.add(id)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerView(type: Int) {
        adapter = FeatureAdapter(listChoose, requireContext(), type) { pos ->
            val chosenItem = listChoose[pos]
            val chosenResId = if (currentType == 1 || currentType == 3) listChooseID[pos] else -1

            when (currentType) {
                1 -> applyTheme(chosenItem, chosenResId)
                2 -> applyFont(chosenItem)
                3 -> applyLanguage(chosenItem, chosenResId)
            }
            adapter?.notifyDataSetChanged()
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        recyclerViewFeature?.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewFeature?.adapter = adapter
    }

    private fun setupBottomSheetCallbacks(dimView: View) {
        bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                dimView.visibility = if (newState == BottomSheetBehavior.STATE_EXPANDED) View.VISIBLE else View.GONE
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                dimView.alpha = slideOffset
            }
        })
    }

    private fun applyTheme(chosenItem: String, chosenResId: Int) {
        saveToPreferences("chooseTheme", chosenResId)
        when (chosenItem) {
            getString(R.string.auto) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            getString(R.string.light) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            getString(R.string.dark) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        requireActivity().recreate()
    }

    private fun applyFont(fontName: String) {
        saveToPreferences("chooseFont", fontName)
        requireActivity().recreate()
    }

    @Suppress("DEPRECATION")
    private fun applyLanguage(chosenItem: String, chosenResId: Int) {
        val languageCode = when (chosenItem) {
            getString(R.string.vietnamese) -> "vi"
            getString(R.string.english) -> "en"
            else -> "en"
        }
        saveToPreferences("chooseLanguage", chosenResId)

        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        requireActivity().resources.updateConfiguration(config, requireActivity().resources.displayMetrics)
        requireActivity().recreate()
    }

    private fun saveToPreferences(key: String, value: Int) {
        requireContext().getSharedPreferences("choose", Context.MODE_PRIVATE)
            .edit { putInt(key, value) }
    }

    private fun saveToPreferences(key: String, value: String) {
        requireContext().getSharedPreferences("choose", Context.MODE_PRIVATE)
            .edit { putString(key, value) }
    }

    private fun getData() {
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", null)
        userId?.let { viewModel.getUserInfo(it) }
    }

    @SuppressLint("SetTextI18n")
    private fun displayData() {
        user?.let {
            binding.userName.text = it.name
            binding.userDescription.text = it.description
            binding.userInviteCode.text = "ID: ${it.inviteCode}"

            AvatarLoader.load(requireContext(), binding.avtarImage, it.avatar, it.name)

            val currentUser = FirebaseAuth.getInstance().currentUser
            binding.layoutChangePassword.visibility = if (currentUser?.providerData?.any { profile -> profile.providerId == EmailAuthProvider.PROVIDER_ID } == true)
                View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        userId?.let { viewModel.getUserInfo(it) }
    }
}
