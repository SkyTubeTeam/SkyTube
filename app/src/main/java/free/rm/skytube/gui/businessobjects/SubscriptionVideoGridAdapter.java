package free.rm.skytube.gui.businessobjects;

import android.content.Context;

public class SubscriptionVideoGridAdapter extends VideoGridAdapter {
	public SubscriptionVideoGridAdapter(Context context) {
		super(context, true);
	}

	@Override
	public void onBindViewHolder(GridViewHolder viewHolder, int position) {
		if (viewHolder != null) {
			viewHolder.updateInfo(get(position), getContext(), listener, true);
		}

		// if it reached the bottom of the list, then try to get the next page of videos
		if (position >= getItemCount() - 1) {
			Logger.d("BOTTOM REACHED...");
		}
	}
}
