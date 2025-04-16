package aaa.sgordon.texteditor;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.DialogFragment;

import com.onegravity.rteditor.api.RTApi;
import com.onegravity.rteditor.api.RTMediaFactory;
import com.onegravity.rteditor.api.RTProxy;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTVideo;

public class MinimalRTApi extends RTApi {
	public MinimalRTApi(Context context, RTProxy rtProxy, RTMediaFactory<RTImage, RTAudio, RTVideo> mediaFactory) {
		super(context, rtProxy, mediaFactory);
	}

}
