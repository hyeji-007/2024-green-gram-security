package com.green.greengram.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.green.greengram.common.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

//@Setter와 생성자가 있으면 Setter을 먼저 사용한다.
//immutable : 1. setter x
//            2. 멤버 필드 private final 선언

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public class Paging {
    //final static: 상수(값을 한번만 초기화)
    //private: 외부에서 직접 접근하지 못하도록 제한.
    //final: 값을 한 번만 초기화하며 이후 변경 불가능.
    //static: 모든 객체에서 공유되며 클래스 레벨에서 관리.
    @Schema(example = "1", description = "Selected Page")
    private int page;
    @Schema(example = "30", description = "item count per page")
    private int size;
    @JsonIgnore
    private int startIdx;

    public Paging(Integer page, Integer size) {
        this.page = (page == null || page <= 0) ? 1 : page;
        this.size = (size == null || size <= 0) ? Constants.getDefault_page_size() : size;
        this.startIdx = ( this.page - 1 ) * this.size;
    }
}
