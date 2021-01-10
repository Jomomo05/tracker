/*
 * The org.opensourcephysics.media.xuggle package provides Xuggle
 * services including implementations of the Video and VideoRecorder interfaces.
 *
 * Copyright (c) 2021  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * For additional information and documentation on Open Source Physics,
 * please see <https://www.compadre.org/osp/>.
 * 
 */
package org.opensourcephysics.media.xuggle;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.media.core.DoubleArray;
import org.opensourcephysics.media.core.ImageCoordSystem;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoAdapter;
import org.opensourcephysics.media.core.VideoFileFilter;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoType;
import org.opensourcephysics.media.mov.MovieFactory;
import org.opensourcephysics.media.mov.MovieVideoType;
import org.opensourcephysics.media.mov.SmoothPlayable;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

/**
 * A class to display videos using the Xuggle library. Xuggle in turn uses
 * FFMpeg as its video engine.
 * 
 * Support for B-Frames added by Bob Hanson 2021.01.10. As discussed at
 * https://groups.google.com/g/xuggler-users/c/PNggEjfsBcg by Art Clarke:
 * 
 * <quote> B-frames are frames that, in order to decode, you need P-frames from
 * the future. They are used in the H264 and Theora codecs for more compression,
 * but it means that a future P-frame (with a higher PTS) may show up with a DTS
 * before a past B-frame (with a lower PTS).
 * 
 * and
 * 
 * The trick is that we report the DTS of frames (i.e. when they need to be
 * decoded) not the PTS (presentation time stamps) in the indexes. DTS will
 * always be in-order but PTS will not. </quote>
 * 
 * 
 * Also adds imageCache to improve performance. Initially set to 50 images, but
 * ALWAYS includes any !isComplete() pictures. These images are always precalculated.
 * 
 */
public class XuggleVideo extends VideoAdapter implements SmoothPlayable {

	public static boolean registered;
	public static final String[][] RECORDABLE_EXTENSIONS = { { "mov", "mov" }, //$NON-NLS-1$ //$NON-NLS-2$
			{ "flv", "flv" }, //$NON-NLS-1$ //$NON-NLS-2$
			{ "mp4", "mp4" }, //$NON-NLS-1$ //$NON-NLS-2$
			{ "wmv", "asf" } }; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String[] NONRECORDABLE_EXTENSIONS = { "avi", //$NON-NLS-1$
			"mts", //$NON-NLS-1$
			"m2ts", //$NON-NLS-1$
			"mpg", //$NON-NLS-1$
			"mod", //$NON-NLS-1$
			"ogg", //$NON-NLS-1$
			"dv" }; //$NON-NLS-1$
	
	private final static int FRAME = 1;
	private final static int PREVFRAME = 0;

	static {
		IContainer.make(); // throws exception if xuggle not available

		XuggleThumbnailTool.start();

		// Registers Xuggle video types with VideoIO class.
		// Executes once only, via this static initializer.
		for (String[] ext : RECORDABLE_EXTENSIONS) {
			VideoFileFilter filter = new VideoFileFilter(ext[1], new String[] { ext[0] }); // $NON-NLS-1$
			VideoType vidType = new XuggleMovieVideoType(filter);
			VideoIO.addVideoType(vidType);
			ResourceLoader.addExtractExtension(ext[0]);
		}

		for (String ext : NONRECORDABLE_EXTENSIONS) {
			VideoFileFilter filter = new VideoFileFilter(ext, new String[] { ext }); // $NON-NLS-1$
			MovieVideoType movieType = new XuggleMovieVideoType(filter);
			movieType.setRecordable(false);
			VideoIO.addVideoType(movieType);
			ResourceLoader.addExtractExtension(ext);
		}

		registered = true;
	}

	private RandomAccessFile raf;
	private final String path;

	// maps frame number to timestamp of displayed packet (last packet loaded)
	private final long[] packetTimeStamps;
	// maps frame number to timestamp of key packet (first packet loaded)
	private final long[] keyTimeStamps;
	// array of frame start times in milliseconds
	private final double[] startTimes;
	private final Timer failDetectTimer;

	private IContainer container;
	private IStreamCoder videoDecoder;
	private IVideoResampler resampler;
	private IPacket packet;
	private IVideoPicture picture;
	private IRational timebase;
	private IConverter converter;

	/**
	 * when the firstDisplayPacket > 0, it means that we have B-Frames of some sort;
	 * that is, frames from the "future" that need decoding early on;
	 */
	private int firstDisplayPacket = -1;
	
	/**
	 * true when firstDisplayPacket > 0; indicating that early B(-like?) frames must
	 * be decoded. This seems to disallow key frames. But I can't be sure. Maybe
	 * they just need to be decoded with the key frames themselves. I have not
	 * figured that part out.
	 */
	private boolean haveBFrames;	

	/**
	 * a cache of images for fast recall; always all incomplete images and 
	 * up to CACHE_MAX images total
	 */
	private BufferedImage[] imageCache;

	/**
	 * for debugging, 0, meaning "just the incomplete frames";
	 * for general purposes, up to CACHE_MAX images.
	 */
	private static final int CACHE_MAX = 0;

	private int streamIndex = -1;
	private long keyTS0 = Long.MIN_VALUE;
	private long keyTS1 = Long.MIN_VALUE;

	private long systemStartPlayTime;
	private double frameStartPlayTime;
	private boolean playSmoothly = true;
	private boolean isLocal;
	

	/**
	 * Initializes this video and loads a video file specified by name
	 *
	 * @param fileName the name of the video file
	 * @throws IOException
	 */
	public XuggleVideo(String fileName) throws IOException {
		Frame[] frames = Frame.getFrames();
		for (int i = 0, n = frames.length; i < n; i++) {
			if (frames[i].getName().equals("Tracker")) { //$NON-NLS-1$
				addPropertyChangeListener(PROPERTY_VIDEO_PROGRESS, (PropertyChangeListener) frames[i]);
				addPropertyChangeListener(PROPERTY_VIDEO_STALLED, (PropertyChangeListener) frames[i]);
				break;
			}
		}
		int[] frameRefs = new int[] { -1, -1 };
		// timer to detect failures
		failDetectTimer = new Timer(6000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (frameRefs[FRAME] == frameRefs[PREVFRAME]) {
					firePropertyChange(PROPERTY_VIDEO_STALLED, null, fileName);
					failDetectTimer.stop();
				}
				frameRefs[PREVFRAME] = frameRefs[FRAME];
			}
		});
		failDetectTimer.setRepeats(true);
		Resource res = ResourceLoader.getResource(fileName);
		if (res == null) {
			throw new IOException("unable to create resource for " + fileName); //$NON-NLS-1$
		}
		// create and open a Xuggle container
		URL url = res.getURL();
		isLocal = (url.getProtocol().toLowerCase().indexOf("file") >= 0); //$NON-NLS-1$
		path = isLocal ? res.getAbsolutePath() : url.toExternalForm();
		// set properties
		setProperty("name", XML.getName(fileName)); //$NON-NLS-1$
		setProperty("absolutePath", res.getAbsolutePath()); //$NON-NLS-1$
		if (fileName.indexOf(":") == -1) { //$NON-NLS-1$
			// if name is relative, path is name
			setProperty("path", XML.forwardSlash(fileName)); //$NON-NLS-1$
		} else {
			// else path is relative to user directory
			setProperty("path", XML.getRelativePath(fileName)); //$NON-NLS-1$
		}
		OSPLog.finest("Xuggle video loading " + path + " local?: " + isLocal); //$NON-NLS-1$ //$NON-NLS-2$
		failDetectTimer.start();
		String err = openContainer();
		if (err != null) {
			dispose();
			throw new IOException(err);
		}
		OSPLog.finest("XuggleVideo found " + firstDisplayPacket + " B-frames" + " and " + frameCount + " total frames");
		if (frameCount == 0) {
			firePropertyChange(PROPERTY_VIDEO_PROGRESS, fileName, null);
			failDetectTimer.stop();
			dispose();
			throw new IOException("packets loaded but no complete picture"); //$NON-NLS-1$
		}
		packetTimeStamps = new long[frameCount];
		keyTimeStamps = new long[frameCount];

		long keyTimeStamp = Long.MIN_VALUE;
		ArrayList<Double> seconds = new ArrayList<Double>();
		firePropertyChange(PROPERTY_VIDEO_PROGRESS, fileName, 0);
		// step thru container and find all video frames
		imageCache = new BufferedImage[Math.min(Math.max(firstDisplayPacket, CACHE_MAX), frameCount)];
		// null;
		// first pass
		int containerFrame = 0;
		System.out.println("not completed: " + firstDisplayPacket);
		while (container.readNextPacket(packet) >= 0) {
			if (VideoIO.isCanceled()) {
				failDetectTimer.stop();
				firePropertyChange(PROPERTY_VIDEO_PROGRESS, fileName, null);
				// clean up temporary objects
				dispose();
				throw new IOException("Canceled by user"); //$NON-NLS-1$
			}
			if (isCurrentStream()) {
				long dts = packet.getTimeStamp();
				if (keyTimeStamp == Long.MIN_VALUE || packet.isKeyPacket()) {
					keyTimeStamp = dts;
				}
				int offset = 0;
				int size = packet.getSize();
				while (offset < size) {
					// decode the packet into the picture
					int bytesDecoded = videoDecoder.decodeVideo(picture, packet, offset);
					// check for errors
					if (bytesDecoded < 0)
						break;
					offset += bytesDecoded;
					if (!picture.isComplete()) {
						System.out.println("!! XuggleVideo picture was incomplete!");
						continue;
					}
				}
				if (containerFrame < imageCache.length)
					imageCache[containerFrame] = getBufferedImage();
				
				//dumpImage(containerFrame, getBufferedImage(), "C");
				
				System.out.println(" frame " + containerFrame + " dts=" + dts + " kts=" + keyTimeStamp + " "
						+ packet.getFormattedTimeStamp() + " " + picture.getFormattedTimeStamp());
				packetTimeStamps[containerFrame] = dts;
				keyTimeStamps[containerFrame] = keyTimeStamp;
				seconds.add((dts - keyTS0) * timebase.getValue());
				firePropertyChange(PROPERTY_VIDEO_PROGRESS, fileName, containerFrame);
				frameRefs[FRAME] = containerFrame++;
			}
		}
		failDetectTimer.stop();

		// clean up temporary objects
//		videoDecoder.close();
//		videoDecoder.delete();
//		if (picture != null)
//			picture.delete();
//		packet.delete();
//		container.close();
//		container.delete();
		// throw IOException if no frames were loaded
		// set initial video clip properties
		startFrameNumber = 0;
		endFrameNumber = frameCount - 1;
		// create startTimes array
		startTimes = new double[frameCount];
		startTimes[0] = 0;
		for (int i = 1; i < startTimes.length; i++) {
			startTimes[i] = seconds.get(i) * 1000;
		}

		seekToStart();
		container.readNextPacket(packet);
//
//		for (int i = frameCount; --i >= 0;) {
//			dumpImage(i, getImage(i), "D");
//		}
//
//		
		
		seekToStart();
		loadPictureFromNextPacket();
		BufferedImage img = getImage(0);
//		if (img == null) {
//			for (int i = 1; i < frameCount; i++) {
//				if ((img = getImage(i)) != null)
//					break;
//			}
//		}
		firePropertyChange(PROPERTY_VIDEO_PROGRESS, fileName, null);
		if (img == null) {
			dispose();
			throw new IOException("No images"); //$NON-NLS-1$
		}
		setImage(img);
		seekToStart();
		loadPictureFromNextPacket();
		//debugCache();
		System.out.println("OK");
	}

    void debugCache() {
		if (imageCache != null) {
			for (int i = 0; i < imageCache.length; i++) {
				dumpImage(i, imageCache[i], "img");
			}
		}
	}

	/**
	 * Plays the video at the current rate. Overrides VideoAdapter method.
	 */
	@Override
	public void play() {
		if (getFrameCount() == 1) {
			return;
		}
		int n = getFrameNumber() + 1;
		playing = true;
		firePropertyChange(Video.PROPERTY_VIDEO_PLAYING, null, new Boolean(true));
		startPlayingAtFrame(n);
	}

	/**
	 * Stops the video.
	 */
	@Override
	public void stop() {
		playing = false;
		firePropertyChange(Video.PROPERTY_VIDEO_PLAYING, null, new Boolean(false));
	}

	/**
	 * Sets the frame number. Overrides VideoAdapter setFrameNumber method.
	 *
	 * @param n the desired frame number
	 */
	@Override
	public void setFrameNumber(int n) {
		if (n == getFrameNumber())
			return;
		super.setFrameNumber(n);
		int i = getFrameNumber();
		BufferedImage bi = getImage(i);
		if (bi != null) {
			rawImage = bi;
			isValidImage = false;
			isValidFilteredImage = false;
			firePropertyChange(Video.PROPERTY_VIDEO_FRAMENUMBER, null, new Integer(getFrameNumber()));
			if (isPlaying()) {
				SwingUtilities.invokeLater(() -> {
					continuePlaying();
				});
			}
		}
	}

	/**
	 * Gets the start time of the specified frame in milliseconds.
	 *
	 * @param n the frame number
	 * @return the start time of the frame in milliseconds, or -1 if not known
	 */
	@Override
	public double getFrameTime(int n) {
		if ((n >= startTimes.length) || (n < 0)) {
			return -1;
		}
		return startTimes[n];
	}

	/**
	 * Gets the current frame time in milliseconds.
	 *
	 * @return the current time in milliseconds, or -1 if not known
	 */
	@Override
	public double getTime() {
		return getFrameTime(getFrameNumber());
	}

	/**
	 * Sets the frame number to (nearly) a desired time in milliseconds.
	 *
	 * @param millis the desired time in milliseconds
	 */
	@Override
	public void setTime(double millis) {
		millis = Math.abs(millis);
		for (int i = 0; i < startTimes.length; i++) {
			double t = startTimes[i];
			if (millis < t) { // find first frame with later start time
				setFrameNumber(i - 1);
				break;
			}
		}
	}

	/**
	 * Gets the start frame time in milliseconds.
	 *
	 * @return the start time in milliseconds, or -1 if not known
	 */
	@Override
	public double getStartTime() {
		return getFrameTime(getStartFrameNumber());
	}

	/**
	 * Sets the start frame to (nearly) a desired time in milliseconds.
	 *
	 * @param millis the desired start time in milliseconds
	 */
	@Override
	public void setStartTime(double millis) {
		millis = Math.abs(millis);
		for (int i = 0; i < startTimes.length; i++) {
			double t = startTimes[i];
			if (millis < t) { // find first frame with later start time
				setStartFrameNumber(i - 1);
				break;
			}
		}
	}

	/**
	 * Gets the end frame time in milliseconds.
	 *
	 * @return the end time in milliseconds, or -1 if not known
	 */
	@Override
	public double getEndTime() {
		int n = getEndFrameNumber();
		if (n < getFrameCount() - 1)
			return getFrameTime(n + 1);
		return getDuration();
	}

	/**
	 * Sets the end frame to (nearly) a desired time in milliseconds.
	 *
	 * @param millis the desired end time in milliseconds
	 */
	@Override
	public void setEndTime(double millis) {
		millis = Math.abs(millis);
		millis = Math.min(getDuration(), millis);
		for (int i = 0; i < startTimes.length; i++) {
			double t = startTimes[i];
			if (millis < t) { // find first frame with later start time
				setEndFrameNumber(i - 1);
				break;
			}
		}
	}

	/**
	 * Gets the duration of the video.
	 *
	 * @return the duration of the video in milliseconds, or -1 if not known
	 */
	@Override
	public double getDuration() {
		int n = getFrameCount() - 1;
		if (n == 0)
			return 100; // arbitrary duration for single-frame video!
		// assume last and next-to-last frames have same duration
		double delta = getFrameTime(n) - getFrameTime(n - 1);
		return getFrameTime(n) + delta;
	}

	/**
	 * Sets the relative play rate. Overrides VideoAdapter method.
	 *
	 * @param rate the relative play rate.
	 */
	@Override
	public void setRate(double rate) {
		super.setRate(rate);
		if (isPlaying()) {
			startPlayingAtFrame(getFrameNumber());
		}
	}

	/**
	 * Disposes of this video.
	 */
	@Override
	public void dispose() {
		super.dispose();
		disposeXuggle();
	}

	private void disposeXuggle() {
		if (raf != null) {
			try {
				raf.close();
			} catch (IOException e) {
			}
		}
		if (videoDecoder != null) {
			videoDecoder.close();
			videoDecoder.delete();
			videoDecoder = null;
		}
		if (picture != null) {
			picture.delete();
			picture = null;
		}
		if (packet != null) {
			packet.delete();
			packet = null;
		}
		if (container != null) {
			if (container.isOpened())
				container.close();
			container.delete();
			container = null;
		}
		streamIndex = firstDisplayPacket = -1;
		keyTS0 = keyTS1 = Long.MIN_VALUE;
	}

	/**
	 * Sets the playSmoothly flag.
	 * 
	 * @param smooth true to play smoothly
	 */
	@Override
	public void setSmoothPlay(boolean smooth) {
		playSmoothly = smooth;
	}

	/**
	 * Gets the playSmoothly flag.
	 * 
	 * @return true if playing smoothly
	 */
	@Override
	public boolean isSmoothPlay() {
		return playSmoothly;
	}

//______________________________  private methods _________________________

	/**
	 * Sets the system and frame start times.
	 * 
	 * @param frameNumber the frame number at which playing will start
	 */
	private void startPlayingAtFrame(int frameNumber) {
		// systemStartPlayTime is the system time when play starts
		systemStartPlayTime = System.currentTimeMillis();
		// frameStartPlayTime is the frame time where play starts
		frameStartPlayTime = getFrameTime(frameNumber);
		setFrameNumber(frameNumber);
	}

	/**
	 * Plays the next time-appropriate frame at the current rate.
	 */
	private void continuePlaying() {
		int n = getFrameNumber();
		if (n < getEndFrameNumber()) {
			long elapsedTime = System.currentTimeMillis() - systemStartPlayTime;
			double frameTime = frameStartPlayTime + getRate() * elapsedTime;
			int frameToPlay = getFrameNumberBefore(frameTime);
			while (frameToPlay > -1 && frameToPlay <= n) {
				elapsedTime = System.currentTimeMillis() - systemStartPlayTime;
				frameTime = frameStartPlayTime + getRate() * elapsedTime;
				frameToPlay = getFrameNumberBefore(frameTime);
			}
			if (frameToPlay == -1) {
				frameToPlay = getEndFrameNumber();
			}
			setFrameNumber(frameToPlay);
		} else if (looping) {
			startPlayingAtFrame(getStartFrameNumber());
		} else {
			stop();
		}
	}

	/**
	 * Gets the number of the last frame before the specified time.
	 *
	 * @param time the time in milliseconds
	 * @return the frame number, or -1 if not found
	 */
	private int getFrameNumberBefore(double time) {
		for (int i = 0; i < startTimes.length; i++) {
			if (time < startTimes[i])
				return i - 1;
		}
		// if not found, see if specified time falls in last frame
		int n = startTimes.length - 1;
		// assume last and next-to-last frames have same duration
		double endTime = 2 * startTimes[n] - startTimes[n - 1];
		if (time < endTime)
			return n;
		return -1;
	}

	private String openContainer() {
		disposeXuggle();
		container = IContainer.make();
		if (isLocal) {
			try {
				raf = new RandomAccessFile(path, "r"); //$NON-NLS-1$

			} catch (FileNotFoundException e) {
			}
		} else {
			raf = null;
			System.out.println("!!XuggleVideo path should be local!" + path);
		}
		if ((raf == null ? container.open(path, IContainer.Type.READ, null)
				: container.open(raf, IContainer.Type.READ, null)) < 0) {
			return "Container could not be opened for " + path;
		}
		if (streamIndex < 0) {
			// find the first video stream in the container
			int nStreams = container.getNumStreams();
			for (int i = 0; i < nStreams; i++) {
				IStream nextStream = container.getStream(i);
				// get the pre-configured decoder that can decode this stream
				IStreamCoder coder = nextStream.getStreamCoder();
				// get the type of stream from the coder's codec type
				if (coder.getCodecType().equals(ICodec.Type.CODEC_TYPE_VIDEO)) {
					streamIndex = i;
					System.out.println("XuggleVideo Stream index set to " + i);
					videoDecoder = coder;
					timebase = nextStream.getTimeBase().copy();
					break;
				}
			}
			if (streamIndex < 0) {
				return "no video stream found in " + path;
			}
		} else {
			videoDecoder = container.getStream(streamIndex).getStreamCoder();
			timebase = container.getStream(streamIndex).getTimeBase().copy();
		}
		if (videoDecoder.open() < 0) {
			return "unable to open video decoder in " + path;
		}
		newPicture();
		packet = IPacket.make();
		preLoadContainer();
		seekToStart();
		return null;
	}

	private void newPicture() {
		picture = IVideoPicture.make(videoDecoder.getPixelType(), videoDecoder.getWidth(), videoDecoder.getHeight());
	}

	private boolean seekToStart() {
		// initial time stamps can be negative. See https://physlets.org/tracker/library/experiments/projectile_model.zip
//		boolean isReset = (container.seekKeyFrame(streamIndex, keyTS0, keyTS0, keyTS0, IContainer.SEEK_FLAG_BACKWARDS) >= 0);
		boolean isReset = (container.seekKeyFrame(-1, Long.MIN_VALUE, 0, Long.MAX_VALUE, IContainer.SEEK_FLAG_BACKWARDS) >= 0);
		return isReset;
	}

	private void preLoadContainer() {
		int n = 0;
		int nIncomplete = 0;
		while (container.readNextPacket(packet) >= 0) {
			if (isCurrentStream()) {
				long dts = packet.getTimeStamp();
				if (keyTS0 == Long.MIN_VALUE)
					keyTS0 = dts; 
				int offset = 0;
				int size = packet.getSize();
				while (offset < size) {
					// decode the packet into the picture
					int bytesDecoded = videoDecoder.decodeVideo(picture, packet, offset);
					// check for errors
					if (bytesDecoded < 0)
						break;
					offset += bytesDecoded;
				}
//				System.out.println(n + " : dts="  + packet.getTimeStamp() + " dts="  + packet.getDts() + " "  + packet.getFormattedTimeStamp() + " " + picture.isComplete());
				if (picture.isComplete()) {
					if (keyTS1 == Long.MIN_VALUE)
						keyTS1 = dts; 
				} else {
					haveBFrames = true;
					nIncomplete++;
				}
				n++;
			}
		}
		if (frameCount < 0)
			frameCount = n;
		if (firstDisplayPacket < 0)
			firstDisplayPacket = nIncomplete;
	}

	private final static String DEBUG_DIR = "c:/temp/tmp/";
	
	private void dumpImage(int i, BufferedImage bi, String froot) {
		if (DEBUG_DIR == null)
			return;
		try {
			String ii = "00" + i;
			File outputfile = new File(DEBUG_DIR + froot + ii.substring(ii.length() - 2) + ".png");
			ImageIO.write(bi, "png", outputfile);
			System.out.println(outputfile + " created");
		} catch (IOException e) {
		}

	}

	/**
	 * Sets the initial image.
	 *
	 * @param image the image
	 */
	private void setImage(BufferedImage image) {
		rawImage = image;
		size.width = image.getWidth();
		size.height = image.getHeight();
		refreshBufferedImage();
		// create coordinate system and relativeAspects
		coords = new ImageCoordSystem(frameCount);
		coords.addPropertyChangeListener(this);
		aspects = new DoubleArray(frameCount, 1);
	}

	/**
	 * Determines if a packet is a key packet.
	 *
	 * @param packet the packet
	 * @return true if packet is a key in the video stream
	 */
	private boolean isKeyPacket() {
		return (isCurrentStream() && packet.isKeyPacket());
	}

	/**
	 * Determines if a packet is a video packet.
	 *
	 * @param packet the packet
	 * @return true if packet is in the video stream
	 */
	private boolean isCurrentStream() {
		return (packet.getStreamIndex() == streamIndex);
	}

	/**
	 * Returns the key packet with the specified timestamp.
	 *
	 * @param keyTS the timestamp in stream timebase units
	 * @return the packet, or null if none found
	 */
	private boolean setKeyPacket(long keyTS) {
		long dts = packet.getTimeStamp();
		// if current packet, we are done;
		// positive delta means key is ahead of us
		long delta = keyTS - dts;
		if (delta == 0) {
			return true;
		}
		// if first packet, reset the container
		if (keyTS == keyTimeStamps[0]) {
			resetContainer();
			loadPictureFromNextPacket();
			return true;
		}
		long seekTS = (delta < 0 && haveBFrames ? keyTS0 : keyTS);
		// if delta is negative, seek backwards;
		// if positive and more than a second, seek forward
		boolean doReset  = ((delta < 0 || delta > packet.getTimeBase().getDenominator()) && container
				.seekKeyFrame(streamIndex, seekTS, seekTS, seekTS, delta < 0 ? IContainer.SEEK_FLAG_BACKWARDS : 0) < 0);
		// allow for a second pass with a container reset between two passes, or, if not
		// found here,
		// a reset first and only one pass
		if (doReset)
			resetContainer();
		while (container.readNextPacket(packet) >= 0) {
			dts = packet.getTimeStamp();
			if (dts == keyTS) {
				loadPictureFromPacket();
				return true;
			}
			if (haveBFrames && dts < keyTS) {
				loadPictureFromPacket();
			} 
// shouldn't be possible
//			if (isCurrentStream() && dts > keyTS) {
//				// unlikely to go this far. 
//				if (nPasses++ > 1)
//					break;
//				resetContainer();
//			}
		}
		// unlikely to be possible
		return false;
	}

	/**
	 * Resets the container to the beginning.
	 */
	private void resetContainer() {
		// seek backwards--this will fail for streamed web videos
		//System.out.println("resetting container");		
		if (!seekToStart()) {
			openContainer();
		}
	}

	/**
	 * Loads the Xuggle picture with all data needed to display a specified frame.
	 *
	 * @param trackerFrameNumber the Tracker idea of what a frame number is; may not
	 *                           be what the container thinks
	 * @return true if loaded successfully
	 */
	private boolean loadPicture(int trackerFrameNumber, int xuggleFrame) {
		long targetTS = packetTimeStamps[xuggleFrame];
		// check to see if seek is needed
		long currentTS = packet.getTimeStamp();
		long keyTS = keyTimeStamps[xuggleFrame];
		boolean justLoadNext = (currentTS >= keyTS && currentTS < targetTS);
		//System.out.println("---" + trackerFrameNumber + " using frame " + xuggleFrame + " " + justLoadNext);
		if (currentTS != targetTS || !isCurrentStream()) {
			// frame is not already loaded
			if (justLoadNext ? loadPictureFromNextPacket() : setKeyPacket(keyTS)) {
				// scan to appropriate packet
				while (isCurrentStream() && (currentTS = packet.getTimeStamp()) != targetTS) {
					loadPictureFromNextPacket();
				}
			}
		}
//		System.out.println("loadPicture " + picture.isComplete() + " tf=" + trackerFrameNumber + " xf=" + xuggleFrame
//				+ " cts=" + currentTS);
		return picture.isComplete();
	}

	private int frameNumberToContainerFrame(int n) {
		return (n + firstDisplayPacket) % frameCount;
	}

	/**
	 * Gets the frame number for a specified timestamp.
	 *
	 * @param timeStamp the timestamp in stream timebase units
	 * @return the frame number, or -1 if not found
	 */
	private int getContainerFrame(long timeStamp) {
		for (int i = 0; i < frameCount; i++) {
			if (packetTimeStamps[i] == timeStamp)
				return i;
		}
		return -1;
	}

	/**
	 * Gets the BufferedImage for a specified Tracker video frame.
	 *
	 * @param frameNumber the VideoClip frame number (zero-based)
	 * @return the image, or null if failed to load
	 */
	private BufferedImage getImage(int frameNumber) {
		if (frameNumber < 0 || frameNumber >= frameCount)
			return null;
		int xuggleFrame = frameNumberToContainerFrame(frameNumber);
		BufferedImage bi = (xuggleFrame < imageCache.length ? imageCache[xuggleFrame]
				: loadPicture(frameNumber, xuggleFrame) ? getBufferedImage() : null);
		//dumpImage(frameNumber, bi, "");
		return bi;
	}

	
	IVideoPicture newPic;

	/**
	 * Gets the BufferedImage for a specified Xuggle picture.
	 *
	 * @param picture the picture
	 * @return the image, or null if unable to resample
	 */
	private BufferedImage getBufferedImage() {
		// if needed, convert picture into BGR24 format
		if (picture.getPixelType() == IPixelFormat.Type.BGR24) {
			newPic = picture;
		} else {
			if (resampler == null) {
				resampler = IVideoResampler.make(picture.getWidth(), picture.getHeight(), IPixelFormat.Type.BGR24,
						picture.getWidth(), picture.getHeight(), picture.getPixelType());
				if (resampler == null) {
					OSPLog.warning("Could not create color space resampler"); //$NON-NLS-1$
					return null;
				}
				newPic = IVideoPicture.make(resampler.getOutputPixelFormat(), picture.getWidth(), picture.getHeight());
			}
			if (resampler.resample(newPic, picture) < 0 || newPic.getPixelType() != IPixelFormat.Type.BGR24) {
				OSPLog.warning("Could not encode video as BGR24"); //$NON-NLS-1$
				return null;
			}
		}

		// use IConverter to convert picture to buffered image
		if (converter == null) {
			converter = ConverterFactory.createConverter(
					ConverterFactory.findRegisteredConverter(ConverterFactory.XUGGLER_BGR_24).getDescriptor(), newPic);
		}
		BufferedImage image = converter.toImage(newPic);
		// garbage collect to play smoothly--but slows down playback speed
		// significantly!
		if (playSmoothly)
			System.gc();
		return image;
	}

	/**
	 * Loads the next video packet in the container into the current Xuggle picture.
	 *
	 * @return true if successfully loaded
	 */
	private boolean loadPictureFromNextPacket() {
		while (container.readNextPacket(packet) >= 0) {
			if (isCurrentStream()) {
				return loadPictureFromPacket();
			}
			// should never get here
		}
		return false;
	}

	/**
	 * Loads the current video packet into the IPicture object.
	 *
	 * @param packet the packet
	 * @return true if successfully loaded
	 */
	private boolean loadPictureFromPacket() {
		int offset = 0;
		int size = packet.getSize();
		while (offset < size) {
			// decode the packet into the picture
			int bytesDecoded = videoDecoder.decodeVideo(picture, packet, offset);

			// check for errors
			if (bytesDecoded < 0)
				return false;

			offset += bytesDecoded;
			if (picture.isComplete()) {
				break;
			}
		}
//		System.out.println("loadPictureFromPacket " + picture.isComplete() + " " + packet.getFormattedTimeStamp() 
//		+ " " + packet.getTimeStamp() + " " + picture.getFormattedTimeStamp());
		return true;
	}

	/**
	 * Returns an XML.ObjectLoader to save and load XuggleVideo data.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load XuggleVideo data.
	 */
	static public class Loader extends VideoAdapter.Loader {

		@Override
		protected VideoAdapter createVideo(String path) throws IOException {
			XuggleVideo video = new XuggleVideo(path);
			String ext = XML.getExtension(path);
			VideoType xuggleType = VideoIO.getVideoType(MovieFactory.ENGINE_XUGGLE, ext);
			if (xuggleType != null)
				video.setProperty("video_type", xuggleType); //$NON-NLS-1$
			return video;
		}
	}

	@Override
	public String getTypeName() {
		return MovieFactory.ENGINE_XUGGLE;
	}

}
