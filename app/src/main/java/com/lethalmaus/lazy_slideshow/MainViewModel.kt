package com.lethalmaus.lazy_slideshow

import android.content.res.TypedArray
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainViewModel : ViewModel(), CoroutineScope {

    val imageId = MutableLiveData<Int>()

    fun cycleImages(images: TypedArray, onFinish: () -> Unit = {}) = launch {
        for (i in 0 until images.length()) {
            val x = images.getResourceId(i, 0)
            imageId.postValue(x)
            delay(slideTimeInMillis)
        }
        onFinish.invoke()
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCleared() {
        job.cancel()
        super.onCleared()
    }
}