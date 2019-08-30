package at.florianschuster.watchables.all.util.photodetail.immersive

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.view.View

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
internal open class SystemUiHelperImplJB(
    activity: Activity,
    level: Int,
    flags: Int,
    onSystemUiVisibilityChangeListener: SystemUiHelper.OnSystemUiVisibilityChangeListener?
) : SystemUiHelper.SystemUiHelperImpl(activity, level, flags, onSystemUiVisibilityChangeListener), View.OnSystemUiVisibilityChangeListener {

    private val mDecorView: View = activity.window.decorView

    init {
        mDecorView.setOnSystemUiVisibilityChangeListener(this)
    }

    private fun createShowFlags(): Int {
        var flag = View.SYSTEM_UI_FLAG_VISIBLE
        if (mLevel >= SystemUiHelper.LEVEL_HIDE_STATUS_BAR) {
            flag = flag or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

            if (mLevel >= SystemUiHelper.LEVEL_LEAN_BACK) {
                flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            }
        }
        return flag
    }

    protected open fun createHideFlags(): Int {
        var flag = View.SYSTEM_UI_FLAG_LOW_PROFILE
        if (mLevel >= SystemUiHelper.LEVEL_LEAN_BACK) {
            flag = flag or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

        if (mLevel >= SystemUiHelper.LEVEL_HIDE_STATUS_BAR) {
            flag = flag or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)

            if (mLevel >= SystemUiHelper.LEVEL_LEAN_BACK) {
                flag = flag or (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            }
        }
        return flag
    }

    private fun onSystemUiShown() {
        if (mLevel == SystemUiHelper.LEVEL_LOW_PROFILE) {
            // Manually show the action bar when in low profile mode.
            val ab = mActivity.actionBar
            ab?.show()
        }
        isShowing = true
    }

    private fun onSystemUiHidden() {
        if (mLevel == SystemUiHelper.LEVEL_LOW_PROFILE) {
            // Manually hide the action bar when in low profile mode.
            val ab = mActivity.actionBar
            ab?.hide()
        }
        isShowing = false
    }

    private fun createTestFlags(): Int {
        return if (mLevel >= SystemUiHelper.LEVEL_LEAN_BACK) {
            // Intentionally override test flags.
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        } else View.SYSTEM_UI_FLAG_LOW_PROFILE
    }

    internal override fun show() {
        mDecorView.systemUiVisibility = createShowFlags()
    }

    internal override fun hide() {
        mDecorView.systemUiVisibility = createHideFlags()
    }

    override fun onSystemUiVisibilityChange(visibility: Int) {
        if (visibility and createTestFlags() != 0) {
            onSystemUiHidden()
        } else {
            onSystemUiShown()
        }
    }
}
