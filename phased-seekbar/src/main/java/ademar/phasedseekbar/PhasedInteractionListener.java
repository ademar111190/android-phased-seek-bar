package ademar.phasedseekbar;

import android.view.MotionEvent;

public interface PhasedInteractionListener {

    void onInteracted(int x, int y, int position, MotionEvent motionEvent);

}
