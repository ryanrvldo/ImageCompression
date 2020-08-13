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

class CompressViewModel : ViewModel() {

    private val _initBytes = MutableLiveData<Array<Byte>>()
    val initBytes: LiveData<Array<Byte>>
        get() = _initBytes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _dictionaryCodes = MutableLiveData<ByteArray>()
    val dictionaryCodes: LiveData<ByteArray>
        get() = _dictionaryCodes

    fun setInitBytes(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            _initBytes.postValue(inputStream.readBytes().toTypedArray())
        }
    }

    suspend fun compressImage(init: Array<Byte>, methodCode: Int): ByteArray {
        var result: ByteArray by Delegates.notNull()

        withContext(Dispatchers.Default) {
            _isLoading.postValue(true)
            runningTime = measureTimeMillis {
                val codes = when (methodCode) {
                    EliasOmegaCode.METHOD_CODE -> EliasOmegaCode.getCodes(256)
                    else -> LevensteinCode.getCodes(256)
                }

                val byteCodes = init.groupingBy { it }
                    .eachCount().toList()
                    .sortedByDescending { (_, value) ->
                        value
                    }
                    .mapIndexed { index, pair ->
                        pair.first to codes[index]
                    }.toMap()

                _dictionaryCodes.postValue(byteCodes.keys.toByteArray())

                var stringBits = init.mapIndexed { _, byte ->
                    byteCodes[byte]
                }.joinToString("")

                stringBits += when (val lastBit = stringBits.length.rem(8)) {
                    0 -> "00000001"
                    else -> "${"0".repeat(7 - lastBit)}1" + String.format(
                        "%08d",
                        Integer.parseInt((9 - lastBit).toString(2))
                    )
                }
                result = BinaryConverter.binToHex(stringBits)
                _isLoading.postValue(false)
            }.toDouble() / 1000
        }
        withContext(Dispatchers.Default) {
            calculateCompression(init.size, result.size)
        }
        return result
    }

    private fun calculateCompression(initSize: Int, resultSize: Int) {
        RC = (resultSize.toDouble() / initSize) * 100
        CR = initSize.toDouble() / resultSize
        SS = (1 - (resultSize.toDouble() / initSize)) * 100
        BR = (resultSize.toDouble() * 8) / initSize
    }

    companion object {
        var runningTime: Double by Delegates.notNull()
        var RC: Double by Delegates.notNull()
        var CR: Double by Delegates.notNull()
        var SS: Double by Delegates.notNull()
        var BR: Double by Delegates.notNull()
//        private const val TAG = "Compression"
    }
}