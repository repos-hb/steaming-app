package com.stream.app.utube.contoller;

import com.stream.app.utube.entity.Video;
import com.stream.app.utube.model.StandardUserResponse;
import com.stream.app.utube.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/video")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping
    public ResponseEntity<StandardUserResponse> saveVideo(
            @RequestParam("file")MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("desc") String desc
            ){

        Video video = Video.builder()
                .videoId(UUID.randomUUID().toString())
                .videoDesc(desc)
                .title(title).build();

        try {
            Video savedVideo = videoService.saveVideo(video, file);
            if(null != savedVideo){
                return ResponseEntity.ok(
                        StandardUserResponse.builder()
                                .message("Video uploaded successfully")
                                .success(Boolean.TRUE).build()
                );
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                    StandardUserResponse.builder()
                            .message("Video uploaded failed. "+e.getMessage())
                            .success(Boolean.FALSE).build()
            );
        }


        return null;
    }
}
