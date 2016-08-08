package com.github.brunodles.pokemongolivewallpaper.service;

import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.io.IOException;

public class GifWallpaperService extends android.service.wallpaper.WallpaperService {

    private static final String TAG = "GifWallpaperService";

    @Override
    public Engine onCreateEngine() {
        try {
            Movie movie = Movie.decodeStream(
                    getResources().getAssets().open("main.gif"));
            return new GifWallpaperEngine(movie);
        } catch (IOException e) {
            Log.e(TAG, "Could not load asset", e);
            return null;
        }
    }

    private class GifWallpaperEngine extends Engine {
        private final int frameDuration = 20;
        private final int movieDuration;
        private final Paint backgroundPaint;

        private SurfaceHolder holder;
        private Movie movie;
        private boolean visible;
        private Handler handler;
        private final float movieWidth;
        private final float movieHeight;
        private long startTime;

        public GifWallpaperEngine(Movie movie) {
            this.movie = movie;
            movieWidth = movie.width();
            movieHeight = movie.height();
            movieDuration = movie.duration();
            handler = new Handler();
            backgroundPaint = new Paint();
            backgroundPaint.setColor(0x000000);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.holder = surfaceHolder;
            startGif();
        }

        private Runnable drawGIF = new Runnable() {
            public void run() {
                draw();
            }
        };

        private void draw() {
            if (!visible) return;
            long now = System.currentTimeMillis();

            Canvas canvas = holder.lockCanvas();

            float canvasWidth = canvas.getWidth();
            float canvasHeight = canvas.getHeight();
            canvas.drawRect(0, 0, canvasWidth, canvasHeight, backgroundPaint);
            canvas.save();

            float widthScale = canvasWidth / movieWidth;
            float heightScale = canvasHeight / movieHeight;
            float scale = Math.min(widthScale, heightScale);

            if (scale == 0) scale = 1f;
            canvas.scale(scale, scale);

            float x = ((canvasWidth - movieWidth * scale) / 2);
            float y = ((canvasHeight - movieHeight * scale) / 2);
            movie.draw(canvas, x, y);

            canvas.restore();
            holder.unlockCanvasAndPost(canvas);

            handler.removeCallbacks(drawGIF);
            if (startTime == 0L || now > startTime + movieDuration) {
                movie.setTime(movieDuration);
            } else {
                movie.setTime((int) (now - startTime) % movieDuration);
                handler.postDelayed(drawGIF, frameDuration);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                startGif();
            } else {
                handler.removeCallbacks(drawGIF);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(drawGIF);
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                startGif();
            }
        }

        private void startGif() {
            long now = System.currentTimeMillis();
            if (now < startTime + movieDuration) return;
            startTime = now;
            handler.post(drawGIF);
        }
    }

}
