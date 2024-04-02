
// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.Map;

final class VideoDataSource {
    private static final String FORMAT_SS = "ss";
    private static final String FORMAT_DASH = "dash";
    private static final String FORMAT_HLS = "hls";
    private static final String FORMAT_OTHER = "other";
    private static final String USER_AGENT = "User-Agent";

    private DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory();

    public MediaSource getMediaSource() {
        return mediaSource;
    }

    private MediaSource mediaSource;

    VideoDataSource(
            Context context,
            String dataSource,
            String formatHint,
            @NonNull Map<String, String> httpHeaders) {

        buildHttpDataSourceFactory(httpHeaders);
        DataSource.Factory dataSourceFactory =
                new DefaultDataSource.Factory(context, httpDataSourceFactory);
        Uri uri = Uri.parse(dataSource);

        mediaSource = buildMediaSource(uri, dataSourceFactory, formatHint);
    }


    // Constructor used to directly test members of this class.
    @VisibleForTesting
    VideoDataSource(
            DefaultHttpDataSource.Factory httpDataSourceFactory) {
        this.httpDataSourceFactory = httpDataSourceFactory;
    }

    @VisibleForTesting
    public void buildHttpDataSourceFactory(@NonNull Map<String, String> httpHeaders) {
        final boolean httpHeadersNotEmpty = !httpHeaders.isEmpty();
        final String userAgent =
                httpHeadersNotEmpty && httpHeaders.containsKey(USER_AGENT)
                        ? httpHeaders.get(USER_AGENT)
                        : "ExoPlayer";

        httpDataSourceFactory.setUserAgent(userAgent).setAllowCrossProtocolRedirects(true);

        if (httpHeadersNotEmpty) {
            httpDataSourceFactory.setDefaultRequestProperties(httpHeaders);
        }
    }

    private MediaSource buildMediaSource(
            Uri uri, DataSource.Factory mediaDataSourceFactory, String formatHint) {
        int type;
        if (formatHint == null) {
            type = Util.inferContentType(uri);
        } else {
            switch (formatHint) {
                case FORMAT_SS:
                    type = C.CONTENT_TYPE_SS;
                    break;
                case FORMAT_DASH:
                    type = C.CONTENT_TYPE_DASH;
                    break;
                case FORMAT_HLS:
                    type = C.CONTENT_TYPE_HLS;
                    break;
                case FORMAT_OTHER:
                    type = C.CONTENT_TYPE_OTHER;
                    break;
                default:
                    type = -1;
                    break;
            }
        }
        switch (type) {
            case C.CONTENT_TYPE_SS:
                return new SsMediaSource.Factory(
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mediaDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.CONTENT_TYPE_DASH:
                return new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mediaDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.CONTENT_TYPE_HLS:
                return new HlsMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.CONTENT_TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }
}
