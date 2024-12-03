package com.green.greengramver2.feed.comment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.beans.ConstructorProperties;

@Getter
@Setter
public class FeedCommentDelReq {
    @Schema(name = "feed_comment_id")
    private long feedCommentId;
    @Schema(name = "signed_user_id")
    private long signedUserId;

    @ConstructorProperties( {"feed_comment_id", "signed_user_id"})
    public FeedCommentDelReq(long feedCommentId, long signedUserId) {
        this.feedCommentId = feedCommentId;
        this.signedUserId = signedUserId;
    }
}
