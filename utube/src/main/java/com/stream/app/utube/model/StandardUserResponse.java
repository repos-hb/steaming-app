package com.stream.app.utube.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StandardUserResponse {

    private String message;
    private Boolean success = false ;

}
