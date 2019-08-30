package at.florianschuster.watchables.all.util.photodetail.immersive

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.view.View

@TargetApi(Build.VERSION_CODES.KITKAT)
internal class SystemUiHelperImplKK(
    activity: Activity,
    level: Int,
    flags: Int,
    onSystemUiVisibilityChangeListener: SystemUiHelper.OnSystemUiVisibilityChangeListener?
) : SystemUiHelperImplJB(activity, level, flags, onSystemUiVisibilityChangeListener) {

    override fun createHideFlags(): Int {
        var flag = super.createHideFlags()

        if (mLevel == SystemUiHelper.LEVEL_IMMERSIVE) {
            // If the client requested immersive mode, and we're on Android 4.4
            // or later, add relevant flags. Applying HIDE_NAVIGATION without
            // IMMERSIVE prevents the activity from accepting all touch events,
            // so we only do this on Android 4.4 and later (where IMMERSIVE is
            // present).
            flag = flag or if (mFlags and SystemUiHelper.FLAG_IMMERSIVE_STICKY != 0)
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            else
                View.SYSTEM_UI_FLAG_IMMERSIVE
        }

        return flag
    }
}
