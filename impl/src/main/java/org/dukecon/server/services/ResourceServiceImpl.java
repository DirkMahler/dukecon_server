package org.dukecon.server.services;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dukecon.model.Resources;
import org.dukecon.model.Styles;
import org.dukecon.services.ConferenceService;
import org.dukecon.services.ResourceService;
import org.springframework.flex.remoting.RemotingDestination;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by christoferdutz on 24.08.16.
 */
@Service("resourceService")
@RemotingDestination
public class ResourceServiceImpl implements ResourceService {

    @Inject
    private ServletContext servletContext;

    @Inject
    private ConferenceService conferenceService;

    @Override
    public Map<String, byte[]> getLogosForConferences() {
        Map<String, byte[]> result = new HashMap<>();
        Set<String> conferenceImageResources = servletContext.getResourcePaths("/public/img");
        for(String conferenceResourcesRoot : conferenceImageResources) {
            String conferenceId = StringUtils.substringAfterLast(
                    StringUtils.substringBeforeLast(conferenceResourcesRoot, "/"), "/");
            Set<String> conferenceResources = servletContext.getResourcePaths(conferenceResourcesRoot + "/conference");
            Map<String, byte[]> confResources = getImageData(conferenceResources);
            if(confResources.containsKey("logo")) {
                result.put(conferenceId, confResources.get("logo"));
            }
        }
        return result;
    }

    @Override
    public Resources getResourcesForConference(String conferenceId) {
        Resources result = Resources.builder().build();

        // Styles
        Styles styles = conferenceService.getConferenceStyles(conferenceId);
        if(styles != null) {
            result.setStyles(styles);
        }

        // Logo
        Set<String> conferenceResources = servletContext.getResourcePaths("/public/img/" + conferenceId + "/conference");
        if(!conferenceResources.isEmpty()) {
            Map<String, byte[]> imageData = getImageData(conferenceResources);
            result.setConferenceImage(imageData.get("logo"));
        }

        // Languages
        Set<String> imageResources = servletContext.getResourcePaths("/public/img/" + conferenceId + "/languages");
        if(!imageResources.isEmpty()) {
            result.setLanguageImages(getImageData(imageResources));
        }

        // Streams
        imageResources = servletContext.getResourcePaths("/public/img/" + conferenceId + "/streams");
        if(!imageResources.isEmpty()) {
            result.setStreamImages(getImageData(imageResources));
        }

        return result;
    }

    private Map<String, byte[]> getImageData(Set<String> imageResources) {
        Map<String, byte[]> images = new HashMap<>();
        if(imageResources != null) {
            for (String imageResource : imageResources) {
                String id = StringUtils.substringBefore(StringUtils.substringAfterLast(imageResource, "/"), ".");
                try {
                    URL resourceUrl = servletContext.getResource(imageResource);
                    InputStream resourceInputStream = resourceUrl.openStream();
                    byte[] imageData = IOUtils.toByteArray(resourceInputStream);
                    if (imageData != null) {
                        images.put(id, imageData);
                    }
                } catch (MalformedURLException e) {
                    // Not much we can do about this here ... simply ignore.
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return images;
    }

}
