package com.green.greengramver2.feed.comment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.green.greengramver2.common.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.ToString;
import org.springframework.web.bind.annotation.BindParam;

import java.beans.ConstructorProperties;

@Getter
@Schema(title = "피드 댓글 리스트 요청")
@ToString
public class FeedCommentGetReq {
    private final static int FIRST_COMMENT_SIZE = 3;

    @Schema(title="피드 PK", description = "피드 PK", name="feed_id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private long feedId;

    @Schema(title="튜플 시작 index", description = "댓글 Element 갯수를 보내주면 된다.", name="start_idx", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private int startIdx;

    @Schema(title="페이지 당 아이템 수", description = "default: 20", example = "20")
    private int size;

    @ConstructorProperties({"feed_id", "start_idx", "size"})
    public FeedCommentGetReq(long feedId, int startIdx, Integer size) {
        this.feedId = feedId;
        this.startIdx = startIdx;
        this.size = (size == null ? Constants.getDefault_page_size() : size) + 1;
    }
}

//    private void setPage(int page) { //(0,4), (3, 21), (23, 21)
//        this.page = page;
//        if(page < 1) { return; } //튕겨서 작업 x
//        if(page == 1) {
//            startIdx = 0;
//            size = FIRST_COMMENT_SIZE + 1; // +1은 isMore 처리용
//            return;
//        }
//        startIdx = ( (page - 2) * DEFAULT_PAGE_SIZE ) + FIRST_COMMENT_SIZE;
//        size = DEFAULT_PAGE_SIZE + 1; // +1은 isMore 처리용 (더보기)
//    }



