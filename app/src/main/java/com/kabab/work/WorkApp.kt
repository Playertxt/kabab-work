package com.kabab.work

import android.app.Application
import com.kabab.work.data.WorkDatabase

class WorkApp : Application() {
    val database: WorkDatabase by lazy { WorkDatabase.create(this) }
}
