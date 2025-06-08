package com.stream.app.utube.contoller;

import com.stream.app.utube.entity.Video;
import com.stream.app.utube.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.stream.app.utube.contant.AppConstant.CHUNK_SIZE;

@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
@Log4j2
public class VideoStreamingController {

    private final VideoService videoService;

    @GetMapping
    public List<Video> getAll(){
        return videoService.getAllVideo();
    }

    /**
     *
     * @param id
     * @return full video in one response.
     *
     * ok for small size files but fails when file size is large
     */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> stream(
            @PathVariable final String id
    ){
        Video video = videoService.getVideo(id);

        String contentType = video.getContentType();
        if(contentType.isEmpty()){
            contentType = "application/octet-stream";
        }

        Path path = Paths.get(video.getFilePath());

        Resource resource = new FileSystemResource(path);

        if(resource.exists() && resource.isReadable()){
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }

        return ResponseEntity.noContent().build();

    }

    /**
     *
     * @param id
     * @param range
     * @return video bytes for only range asked by browser
     *
     * Range decided by the browser, no control at the backend
     */
    @GetMapping("/range/{id}")
    public ResponseEntity<Resource> streamByRange(
            @PathVariable final String id,
            @RequestHeader(value = "Range", required = false) final String range
    ){

        // get video metadata
        Video video = videoService.getVideo(id);

        Path path = Paths.get(video.getFilePath());

        String contentType = video.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";

        }

        long fileLength = path.toFile().length();

        // create resource object
        Resource resource = new FileSystemResource(path);


        // if range is null --> send full video
        log.info("Range----"+range);
        if(range.isEmpty()){
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }

        // else stream by range
        // step 1: calculate range
        long startRange, endRange = 0;
        String[] ranges = range.replace("bytes=", "").split("-");

        startRange = Long.parseLong(ranges[0]);

        if(ranges.length > 1){
            long end = Long.parseLong(ranges[1]);

            if(end > fileLength-1){
                endRange = fileLength - 1;
            } else {
                endRange = end;
            }
        }

        // step 2: adjusting starting byte
        InputStream inputStream;
        try {
            inputStream = Files.newInputStream(path);
            inputStream.skip(startRange);       // seeking video
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        // step 3: setting final Range in Response Headers
        long contentLength = endRange - startRange + 1;
        logValues(startRange, endRange, contentLength);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Range", "bytes " + startRange + "-" + endRange + "/" + fileLength);  // for UI to consume
        headers.setContentLength(contentLength);
        setSecurityHeaders(headers);

        // Step 4: return Partial Response
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType(contentType))
                .headers(headers)
                .body(new InputStreamResource(inputStream));

    }

    /**
     *
     * @param id
     * @param range
     * @return chunk of video
     *
     * Range decided by pre-defined CHUNK_SIZE
     */
    @GetMapping("/range/{id}")
    public ResponseEntity<Resource> streamByChunk(
            @PathVariable final String id,
            @RequestHeader(value = "Range", required = false) final String range
    ){

        // get video metadata
        Video video = videoService.getVideo(id);

        Path path = Paths.get(video.getFilePath());

        String contentType = video.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";

        }

        long fileLength = path.toFile().length();

        // create resource object
        Resource resource = new FileSystemResource(path);


        // if range is null --> send full video
        log.info("Range----"+range);
        if(range.isEmpty()){
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }

        // else stream by range
        // step 1: calculate range
        long startRange, endRange = 0;
        String[] ranges = range.replace("bytes=", "").split("-");

        startRange = Long.parseLong(ranges[0]);

        endRange = startRange + CHUNK_SIZE - 1;
        if(endRange >= fileLength){
            endRange = fileLength - 1;
        }

//        if(ranges.length > 1){
//            long end = Long.parseLong(ranges[1]);
//
//            if(end > fileLength-1){
//                endRange = fileLength - 1;
//            } else {
//                endRange = end;
//            }
//        }

        // step 2: adjusting starting byte
        InputStream inputStream;
        try {
            inputStream = Files.newInputStream(path);
            inputStream.skip(startRange);       // seeking video

            // step 3: setting final Range in Response Headers
            long contentLength = endRange - startRange + 1;
            logValues(startRange, endRange, contentLength);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "bytes " + startRange + "-" + endRange + "/" + fileLength);  // for UI to consume
            headers.setContentLength(contentLength);
            setSecurityHeaders(headers);

            byte[] buffer = new byte[(int) contentLength];
            int read = inputStream.read(buffer, 0, buffer.length);
            log.info("Read (number of bytes::: "+read);

            // Step 4: return Partial Response
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(contentType))
                    .headers(headers)
                    .body(new ByteArrayResource(buffer));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

    }

    private void setSecurityHeaders(HttpHeaders headers) {
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("X-Content-Type-Options", "nosniff");
    }

    private void logValues(long startRange, long endRange, long contentLength) {
        log.info("Start Range::: "+ startRange);
        log.info("End Range::: "+ endRange);
        log.info("Content Length::: "+ contentLength);
    }
}
