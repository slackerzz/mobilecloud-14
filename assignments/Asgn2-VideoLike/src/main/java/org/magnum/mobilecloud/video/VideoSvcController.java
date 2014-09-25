package org.magnum.mobilecloud.video;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

@Controller
public class VideoSvcController {
	public static final String TITLE_PARAM = "title";
	public static final String DURATION_PARAM = "duration";
	
	@Autowired
	private VideoRepository videos;
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		videos.save(v);
		
		v.setLikes(0);
		if(v.getUrl().length() == 0)
			v.setUrl(VideoSvcApi.VIDEO_SVC_PATH + '/' + Long.toString(v.getId()));
		videos.save(v);
		
		return v;
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return Lists.newArrayList(videos.findAll());
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}", method=RequestMethod.GET)
	public @ResponseBody Video getVideoById(@PathVariable("id") long id, HttpServletResponse resp) throws IOException {
		Video v = videos.findOne(id);
		if (v == null)
			resp.sendError(404, "Video id does not exist");
		return v;
	}
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/like", method=RequestMethod.POST)
	public @ResponseBody void likeVideo(@PathVariable("id") long id, Principal p, HttpServletResponse resp) throws IOException {
		Video v = videos.findOne(id);
		if (v == null) {
			resp.sendError(404, "Video id does not exist");
		} else {
			Set<String> likedUsers = v.getLikedUsers();
			if (!likedUsers.contains(p.getName())) {
				likedUsers.add(p.getName());
				v.setLikedUsers(likedUsers);
				v.setLikes(v.getLikes() + 1);
				videos.save(v);
				resp.setStatus(HttpServletResponse.SC_OK);
			} else {
				resp.sendError(400, "Video is already liked");	
			}
		}
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/unlike", method=RequestMethod.POST)
	public  @ResponseBody void unlikeVideo(@PathVariable("id") long id, Principal p, HttpServletResponse resp) throws IOException {
		Video v = videos.findOne(id);
		if (v == null)
			resp.sendError(404, "Video id does not exist");
		else {
			Set<String> likedUsers = v.getLikedUsers();
			if (likedUsers.contains(p.getName())) {
				likedUsers.remove(p.getName());
				v.setLikedUsers(likedUsers);
				v.setLikes(v.getLikes() - 1);
				videos.save(v);
				resp.setStatus(HttpServletResponse.SC_OK);
			} else
				resp.sendError(400, "Video has not been liked before");
		}
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/likedby", method=RequestMethod.GET)
	public @ResponseBody Set<String> getLikedBy(@PathVariable("id") long id, HttpServletResponse resp) throws IOException {
		Video v = videos.findOne(id);
		
		if (v == null)
			resp.sendError(404, "Video id does not exist");
		Set<String> likedUsers = v.getLikedUsers();
		return likedUsers;
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_TITLE_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle(@RequestParam(TITLE_PARAM) String title) {
		return videos.findByName(title);
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DURATION_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByDuration(@RequestParam(DURATION_PARAM) long maxduration) {
		return videos.findByDurationLessThan(maxduration);
	}
	
}
