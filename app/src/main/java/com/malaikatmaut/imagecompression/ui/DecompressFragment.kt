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
import com.malaikatmaut.imagecompression.databinding.FragmentDecompressBinding
import com.malaikatmaut.imagecompression.util.CustomDialog
import com.malaikatmaut.imagecompression.util.EliasOmegaCode
import com.malaikatmaut.imagecompression.util.LevensteinCode
import com.malaikatmaut.imagecompression.viewmodel.DecompressViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

@ExperimentalUnsignedTypes
class DecompressFragment : Fragment() {

    private var _binding: FragmentDecompressBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel: DecompressViewModel by viewModels()

    private lateinit var initBytes: ByteArray
    private lateinit var resultBytes: ByteArray

    private lateinit var fileName: String
    private var methodCode = 0

    private lateinit var customDialog: CustomDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDecompressBinding.inflate(layoutInflater, container, false)
        customDialog = CustomDialog(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.methodChipGroup.setOnCheckedChangeListener { _, checkedId ->
            methodCode = when (checkedId) {
                R.id.elias_chip -> EliasOmegaCode.METHOD_CODE
                R.id.levenstein_chip -> LevensteinCode.METHOD_CODE
                else -> 0
            }
        }

        binding.btnSelectImage.setOnClickListener {
            selectImageResultLauncher.launch("*/*")
        }

        binding.btnClearImage.setOnClickListener {
            clearResult()
        }

        binding.btnDecompress.setOnClickListener {
            if (!this::initBytes.isInitialized || binding.btnSelectImage.isShown) {
                showToast(getString(R.string.please_init_image))
            } else if (methodCode == 0) {
                showToast(getString(R.string.please_choose_decompression_method))
            } else {
                decompressImage()
            }
        }

        binding.btnSaveImage.setOnClickListener {
            if (this::resultBytes.isInitialized && this.resultBytes.isNotEmpty()) {
                saveImageResultLauncher.launch("${fileName.substringBefore(".")}.bmp")
            } else {
                showToast(getString(R.string.did_not_compressing))
            }
        }

        viewModel.initBytes.observe(viewLifecycleOwner, Observer { data ->
            data?.let { bytes ->
                this.initBytes = bytes
                binding.tvFileSize.text =
                    String.format("%.2f kB", initBytes.size.toDouble() / 1_000)
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { data ->
            data?.let { status ->
                if (status) {
                    customDialog.showLoadingDialog()
                } else {
                    customDialog.closeLoadingDialog()
                }
            }
        })
    }

    private fun decompressImage() {
        lifecycleScope.launch(Dispatchers.Default) {
            val dictionaryCodes = getDictionaryFile()
            if (dictionaryCodes != null) {
                resultBytes = viewModel.decompressImage(
                    initBytes, dictionaryCodes, methodCode
                )
                withContext(Dispatchers.Main) {
                    binding.btnDecompress.hide()
                    Glide.with(requireContext())
                        .load(resultBytes)
                        .into(binding.image)
                    binding.imgCard.visibility = View.VISIBLE
                    binding.btnSaveImage.visibility = View.VISIBLE

                    binding.tvImgResultSize.visibility = View.VISIBLE
                    binding.tvImgResultSize.text =
                        String.format("%.2f kB", resultBytes.size.toDouble() / 1_000)

                    customDialog.showSuccessDialog(
                        "The result of compression process:\n" +
                                "Running Time: ${DecompressViewModel.runningTime} seconds.\n" +
                                "Result size: ${String.format(
                                    "%.2f kB",
                                    resultBytes.size.toDouble() / 1_000
                                )}"
                    )
                }
            } else {
                withContext(Dispatchers.Main) {
                    showToast(getString(R.string.dictionary_not_found))
                }
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

    private val selectImageResultLauncher = registerForActivityResult(GetContent()) { result ->
        result?.let { uri ->
            binding.btnSelectImage.hide()
            requireContext().contentResolver.openInputStream(uri)?.let {
                viewModel.setInitBytes(it)
            }

            uri.path?.let { path ->
                fileName = path.substringAfterLast("/")
                binding.tvFileName.text = fileName
            }
        }
    }

    private val saveImageResultLauncher = registerForActivityResult(CreateDocument()) { result ->
        result?.let {
            saveResultImage(resultBytes, it)
        }
    }

    private fun getDictionaryFile(): ByteArray? {
        val file = when (methodCode) {
            EliasOmegaCode.METHOD_CODE -> File(
                requireContext().getExternalFilesDir(null),
                "${fileName.substringBeforeLast(".")}.eod"
            )
            else -> File(
                requireContext().getExternalFilesDir(null),
                "${fileName.substringBeforeLast(".")}.ld"
            )
        }
        return when (file.exists()) {
            true -> FileInputStream(file).readBytes()
            else -> null
        }
    }

    private fun clearResult() {
        binding.tvFileName.text = getString(R.string.not_available)
        binding.tvFileSize.text = getString(R.string.not_available)
        initBytes = ByteArray(0)
        binding.methodChipGroup.clearCheck()
        binding.imgCard.visibility = View.GONE
        binding.tvImgResultSize.visibility = View.GONE
        binding.btnSaveImage.visibility = View.GONE
        binding.btnSelectImage.show()
        binding.btnDecompress.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}