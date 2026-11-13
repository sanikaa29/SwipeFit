package com.swipefit

import android.app.Application
import com.google.firebase.storage.FirebaseStorage
import com.swipefit.data.AuthRepository
import com.swipefit.data.GroupRepository
import com.swipefit.data.OutfitRepository
import com.swipefit.data.VoteRepository

/** Tiny hand-written "dependency injection": one place that creates the repositories. */
class AppContainer(val app: Application) {
    val auth = AuthRepository()
    val groups = GroupRepository(auth)
    val outfits = OutfitRepository(auth)
    val votes = VoteRepository(auth)

    init {
        // Give up on stalled uploads after 30 s so the UI can show an error.
        FirebaseStorage.getInstance().maxUploadRetryTimeMillis = 30_000
    }
}

class SwipeFitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
