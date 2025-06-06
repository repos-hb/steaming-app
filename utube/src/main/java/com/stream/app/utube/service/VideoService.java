package com.stream.app.utube.service;

import com.stream.app.utube.entity.Video;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface VideoService {

    // save video
    Video saveVideo(Video video, MultipartFile file) throws IOException;

    // get video by id
    Video getVideo(String id);

    // get video by title
    Video getVideoByTitle(String title);

    // get all video as list
    List<Video> getAllVideo();
}
