package ademar.phasedseekbar;

import android.graphics.drawable.StateListDrawable;

public interface PhasedAdapter {

    public int getCount();

    public StateListDrawable getItem(int position);

}
