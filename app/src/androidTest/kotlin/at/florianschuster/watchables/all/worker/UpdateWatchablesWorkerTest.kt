package at.florianschuster.watchables.all.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateWatchablesWorkerTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun test() { // todo
        val worker = TestListenableWorkerBuilder<UpdateWatchablesWorker>(context)
            .build()
        val result = worker.startWork().get()
        result shouldEqual ListenableWorker.Result.success()
    }
}