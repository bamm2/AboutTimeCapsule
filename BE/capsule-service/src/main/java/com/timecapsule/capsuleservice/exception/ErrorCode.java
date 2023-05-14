package com.timecapsule.capsuleservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    CAPSULE_NOT_FOUND(HttpStatus.NOT_FOUND ,"존재하지 않는 캡슐입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND ,"존재하지 않는 멤버입니다."),
    MEMORY_NOT_FOUND(HttpStatus.NOT_FOUND ,"존재하지 않는 추억입니다.");
    private final HttpStatus httpStatus;
    private final String message;
}
