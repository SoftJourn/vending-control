package com.softjourn;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExecutionResponse {

    private Status status;

    private List<String> responseData;

}

enum Status {
    SUCCESS, FAILURE, ERROR
}
