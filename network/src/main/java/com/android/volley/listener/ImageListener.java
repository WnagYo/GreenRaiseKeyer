package com.android.volley.listener;

import com.android.volley.toolbox.ImageLoader;

/**
 * Interface for the response handlers on image requests.
 *
 * The call flow is this:
 * 1. Upon being  attached to a request, onResponse(response, true) will
 * be invoked to reflect any cached data that was already available. If the
 * data was available, response.getBitmap() will be non-null.
 *
 * 2. After a network response returns, only one of the following cases will happen:
 *   - onResponse(response, false) will be called if the image was loaded.
 *   or
 *   - onErrorResponse will be called if there was an error loading the image.
 */
public abstract class ImageListener  extends Listener{
    /**
     * Listens for non-error changes to the loading of the image request.
     *
     * @param response Holds all information pertaining to the request, as well
     * as the bitmap (if it is loaded).
     * @param isImmediate True if this was called during ImageLoader.get() variants.
     * This can be used to differentiate between a cached image loading and a network
     * image loading in order to, for example, run an animation to fade in network loaded
     * images.
     */
    public abstract void onResponse(ImageLoader.ImageContainer response, boolean isImmediate);

    @Override
    public void onSuccess(Object response) {
    }
}
