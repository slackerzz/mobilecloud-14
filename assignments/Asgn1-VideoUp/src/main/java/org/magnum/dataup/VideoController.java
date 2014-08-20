/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import retrofit.client.Response;
import retrofit.http.Multipart;

@Controller
public class VideoController {
	private final Map<Long, Video> videos = new HashMap<Long, Video>();
	private final AtomicLong atomicLong = new AtomicLong(0L);
	@Autowired
	private VideoFileManager videoFileManager;
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		Collection<Video> videosCollection = videos.values();
		return videosCollection;
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		if(v.getId() == 0)
            v.setId(atomicLong.incrementAndGet());
        v.setDataUrl(getUrlBaseForLocalServer()+getDataUrl(v.getId()));
        videos.put(v.getId(), v);
		return v;
	}
	
	@Multipart
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable(VideoSvcApi.ID_PARAMETER) long id,
			@RequestParam(VideoSvcApi.DATA_PARAMETER) MultipartFile videoData,
			HttpServletResponse resp) {
		try {
			if(videos.containsKey(id)) {
				videoFileManager.saveVideoData(videos.get(id), videoData.getInputStream());
				return new VideoStatus(VideoState.READY);
			} else {
				// video not present
				resp.sendError(404);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new VideoStatus(VideoState.READY);
		
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.GET)
	public Response getData(
			@PathVariable(VideoSvcApi.ID_PARAMETER) long id,
			HttpServletResponse resp) {
		try {
			if(videos.containsKey(id)) {
				Video v = videos.get(id);
				if(videoFileManager.hasVideoData(v)) {
					resp.setStatus(200);
					resp.setContentType(v.getContentType());
					videoFileManager.copyVideoData(v, resp.getOutputStream());
				}
			} else {
				resp.sendError(404);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
       HttpServletRequest request = 
           ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
       String base = 
          "http://"+request.getServerName() 
          + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
       return base;
    }
}
