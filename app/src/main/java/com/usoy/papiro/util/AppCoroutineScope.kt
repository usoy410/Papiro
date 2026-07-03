package com.usoy.papiro.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object AppCoroutineScope : CoroutineScope {
    override val coroutineContext = SupervisorJob() + Dispatchers.Default
}
