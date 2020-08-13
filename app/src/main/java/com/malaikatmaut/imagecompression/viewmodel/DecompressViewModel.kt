package com.malaikatmaut.imagecompression.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malaikatmaut.imagecompression.util.BinaryConverter
import com.malaikatmaut.imagecompression.util.EliasOmegaCode
import com.malaikatmaut.imagecompression.util.LevensteinCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.properties.Delegates
import kotlin.system.measureTimeMillis

class DecompressViewModel : ViewModel() {

    private val _initBytes = MutableLiveData<ByteArray>()
    val initBytes: LiveData<ByteArray>
        get() = _initBytes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    fun setInitBytes(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            _initBytes.postValue(inputStream.readBytes())
        }
    }

    @ExperimentalUnsignedTypes
    suspend fun decompressImage(init: ByteArray, dict: ByteArray, methodCode: Int): ByteArray {
        var result: ByteArray by Delegates.notNull()
        withContext(Dispatchers.Default) {
            _isLoading.postValue(true)
            runningTime = measureTimeMillis {
                val codes = when (methodCode) {
                    EliasOmegaCode.METHOD_CODE -> EliasOmegaCode.getCodes(256)
                    else -> LevensteinCode.getCodes(256)
                }

                val compressedBin = BinaryConverter.hexToBin(init).run {
                    val paddingBit = Integer.parseInt(
                        substring(length - 8 until length),
                        2
                    )
                    substring(0 until length - (7 + paddingBit))
                }

                var stringBit = String()
                val resultList = mutableListOf<Byte>()
                for (bit in compressedBin) {
                    stringBit += bit
                    if (codes.contains(stringBit)) {
                        val idxDict = when (methodCode) {
                            EliasOmegaCode.METHOD_CODE -> EliasOmegaCode.decoding(stringBit) - 1
                            else -> LevensteinCode.decoding(stringBit)
                        }
                        resultList.add(dict[idxDict])
                        stringBit = String()
                    }
                }
                result = resultList.toByteArray()
            }.toDouble() / 1000
            _isLoading.postValue(false)
        }
        return result
    }

    companion object {
        var runningTime: Double by Delegates.notNull()
//        const val TAG = "Decompression"
    }
}