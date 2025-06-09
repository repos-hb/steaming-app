package com.stream.app.utube.service.impl;

import com.stream.app.utube.entity.Video;
import com.stream.app.utube.repository.VideoRepository;
import com.stream.app.utube.service.VideoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    @Value("${path.upload}")
    private String uploadPath;

    @Value("${path.upload.hls}")
    private String uploadPathHls;

    private final VideoRepository videoRepository;

    /**
     * This method will create the upload path in the server during boot,
     * if it does not exist already.
     */
    @PostConstruct
    public void init(){
        File file = new File(uploadPath);

        if(!file.exists()){
            file.mkdir();
            log.info("Upload path created");
        } else {
            log.info("Upload path exists");
        }

        try {
            Files.createDirectories(Path.of(uploadPathHls));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Video saveVideo(Video video, MultipartFile file) throws IOException {

        if (!file.isEmpty()) {
            try {
                // extract video metadata
                String fileName = StringUtils.cleanPath(file.getOriginalFilename());
                String contentType = StringUtils.cleanPath(file.getContentType());

                // create dedicated path for video
                Path path = Paths.get(uploadPath, fileName);
                log.info("Created path "+ path.toAbsolutePath());

                // copy file to created path
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                // create video metadata
                video.setContentType(contentType);
                video.setFilePath(path.toString());

                return videoRepository.save(video);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }

        } else {
            throw new RuntimeException("Empty file received.");
        }
    }

    @Override
    public Video getVideo(String id) {
        Video video = videoRepository.findById(id).orElseThrow(() -> new RuntimeException("video not found."));
        return video;
    }

    @Override
    public Video getVideoByTitle(String title) {
        return videoRepository.findByTitle(title).orElseThrow(() -> new RuntimeException("video not found"));
    }

    @Override
    public List<Video> getAllVideo() {
        return videoRepository.findAll();
    }

    @Override
    public void processVideo(String id) {
        // get file path for processing
        Video video = this.getVideo(id);
        Path filePath = Paths.get(video.getFilePath());

        // create directories for segmentation
        String output360p = uploadPathHls.concat(id).concat("/360p");
        String output720p = uploadPathHls.concat(id).concat("/720p");
        String output1080p = uploadPathHls.concat(id).concat("/1080p");

        try {
            Files.createDirectories(Path.of(output360p));
            Files.createDirectories(Path.of(output720p));
            Files.createDirectories(Path.of(output1080p));

            // ffmpeg command
            StringBuilder ffmpegCommand = new StringBuilder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
