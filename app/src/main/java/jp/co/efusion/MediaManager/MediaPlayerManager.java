package jp.co.efusion.MediaManager;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import jp.co.efusion.utility.Default;

/**
 * Created by xor2 on 12/22/15.
 */
public class MediaPlayerManager implements MediaPlayer.OnCompletionListener {

    MediaPlayer mediaPlayer = null;
    MediaCompletionListener mediaCompletionListener;


    public MediaPlayerManager(String filePath) throws IOException{

        mediaPlayer=new MediaPlayer();
        File audioFile =  new File(filePath);
        if (!audioFile.exists()){
            throw new IOException("file " + audioFile.getAbsolutePath() + " not exist");
        }
        mediaPlayer.setDataSource(filePath);
        mediaPlayer.setOnCompletionListener(this);

    }

    /*
     * Simply fire the play Event
     */
    public void playAudio() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.prepare();
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Simply fire the pause Event
     */
    public int pauseAudio() {
        int currentPosition;
        try {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
                currentPosition=mediaPlayer.getCurrentPosition();
                Log.d("pauseAudio","Duration: "+mediaPlayer.getDuration()+" pause position:"+currentPosition);
                //mediaPlayer.stop();
                if (currentPosition>=mediaPlayer.getDuration()){
                    return Default.ZERO;
                }
                return currentPosition;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Default.ZERO;
    }
    /*
    * Simply media player seek to position
    * @param position
     */
    public void resumeAudio(int position){
        try{
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.seekTo(position);
            }
        }catch (Exception e){

        }
    }

    /**
     *
     * @return
     */
    public int getDuration(){
        /*try {
            if (mediaPlayer != null) {
                return mediaPlayer.getDuration();
            }
        }catch (Exception e){}*/
        return mediaPlayer.getDuration();
    }

    public void prepare(){
        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCurrentPosition(){
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    return mediaPlayer.getCurrentPosition();
            }
        }catch (Exception e){

        }
        return Default.ZERO;
    }

    /*
     * Simply fire the stop Event
     */
    public void stopAudio() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Simply check audio is currently playing or not
     */
    public Boolean isPlaying() {
        try {
            if (mediaPlayer != null)
                return mediaPlayer.isPlaying();
            else
                return false;
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return false;
    }

    /*
    * Set custom media completetion listener
    * @param listener an instant of MediaCompletionListener class
     */
    public void setMediaCompletetionListener(MediaCompletionListener listener) {
        mediaCompletionListener = listener;
    }

    /*
    * Implement audio play finish listener
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mediaCompletionListener != null) {
            mediaCompletionListener.onMediaCompletion();
        }
        Log.d("onCompletion","Media Completetion called");
        mediaPlayer.reset();
        mp.release();
    }


}
