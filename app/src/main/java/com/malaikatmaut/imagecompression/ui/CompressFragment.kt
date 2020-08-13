package com.malaikatmaut.imagecompression.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.malaikatmaut.imagecompression.R
import com.malaikatmaut.imagecompression.databinding.FragmentCompressBinding
import com.malaikatmaut.imagecompression.util.CustomDialog
import com.malaikatmaut.imagecompression.util.EliasOmegaCode
import com.malaikatmaut.imagecompression.util.LevensteinCode
import com.malaikatmaut.imagecompression.viewmodel.CompressViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class CompressFragment : Fragment() {

    private var _binding: FragmentCompressBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: CompressViewModel by viewModels()

    private lateinit var initBytes: Array<Byte>
    private lateinit var resultBytes: ByteArray
    private lateinit var dictionaryCodes: ByteArray

    private lateinit var fileName: String
    private var methodCode = 0

    private lateinit var customDialog: CustomDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCompressBinding.inflate(layoutInflater, container, false)
        customDialog = CustomDialog(requireContext())
        binding.btnClearImage.hide()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.methodChipGroup.setOnCheckedChangeListener { _, checkedId ->
            methodCode = when (checkedId) {
                R.id.elias_chip -> EliasOmegaCode.METHOD_CODE
                else -> LevensteinCode.METHOD_CODE
            }
        }

        binding.btnClearImage.setOnClickListener {
            clearImage()
        }

        binding.btnSelectImage.setOnClickListener {
            selectImageResultLauncher.launch("image/*")
        }

        binding.btnCompress.setOnClickListener {
            if (!this::initBytes.isInitialized || binding.btnSelectImage.isShown) {
                showToast(getString(R.string.please_init_image))
            } else if (methodCode == 0) {
                showToast(getString(R.string.please_choose_method))
            } else {
                compressImage()
            }
        }

        viewModel.initBytes.observe(viewLifecycleOwner, Observer { value ->
            value?.let { bytes ->
                this.initBytes = bytes
                binding.tvImgSize.text = String.format("%.2f kB", initBytes.size.toDouble() / 1_000)
            }
        })

        viewModel.dictionaryCodes.observe(viewLifecycleOwner, Observer { value ->
            value?.let { bytes -> this.dictionaryCodes = bytes }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { value ->
            value?.let { status ->
                if (status) {
                    customDialog.showLoadingDialog()
                } else {
                    customDialog.closeLoadingDialog()
                }
            }
        })
    }

    private fun compressImage() {
        lifecycleScope.launch(Dispatchers.Default) {
            resultBytes = viewModel.compressImage(initBytes, methodCode)
            if (methodCode == EliasOmegaCode.METHOD_CODE) {
                saveImageResultLauncher.launch("${fileName.substringBefore(".")}.eoc")
            } else {
                saveImageResultLauncher.launch("${fileName.substringBefore(".")}.lc")
            }
        }
    }

    private fun saveResultImage(byteArray: ByteArray, uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val outputStream = requireContext().contentResolver.openOutputStream(uri)
            outputStream?.apply {
                write(byteArray)
                close()
            }
        }
    }

    private fun saveDictionary() {
        if (!this::dictionaryCodes.isInitialized) {
            showToast(getString(R.string.restart_process))
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val file = when (methodCode) {
                EliasOmegaCode.METHOD_CODE -> File(
                    requireContext().getExternalFilesDir(null),
                    "${fileName.substringBefore(".")}.eod"
                )
                else -> File(
                    requireContext().getExternalFilesDir(null),
                    "${fileName.substringBefore(".")}.ld"
                )
            }
            val outputStream = FileOutputStream(file)
            outputStream.apply {
                write(dictionaryCodes)
                close()
            }
        }
    }

    private val selectImageResultLauncher = registerForActivityResult(GetContent()) { result ->
        result?.let { uri ->
            requireContext().contentResolver.openInputStream(uri)?.let {
                viewModel.setInitBytes(it)
            }
            Glide.with(requireContext())
                .load(uri)
                .into(binding.image)

            binding.btnClearImage.show()
            binding.btnSelectImage.hide()

            uri.path?.let { path ->
                fileName = path.substringAfterLast("/")
                binding.tvImgName.text = fileName
            }
        }
    }

    private val saveImageResultLauncher = registerForActivityResult(CreateDocument()) { uri ->
        uri?.also {
            saveResultImage(resultBytes, it)
            saveDictionary()
            customDialog.showSuccessDialog(
                "The result of compression process:\n" +
                        "Running Time: ${CompressViewModel.runningTime} seconds.\n" +
                        "RC: ${String.format("%.2f", CompressViewModel.RC)}%\n" +
                        "CR: ${String.format("%.4f.", CompressViewModel.CR)}\n" +
                        "SS: ${String.format("%.4f", CompressViewModel.SS)}%\n" +
                        "BR: ${String.format("%.4f.", CompressViewModel.BR)}\n"
            )
        }
    }

    private fun clearImage() {
        binding.tvImgName.text = getString(R.string.not_available)
        binding.tvImgSize.text = getString(R.string.not_available)
        binding.image.setImageDrawable(null)
        initBytes = emptyArray()
        binding.methodChipGroup.clearCheck()
        methodCode = 0
        binding.btnSelectImage.show()
        binding.btnClearImage.hide()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}