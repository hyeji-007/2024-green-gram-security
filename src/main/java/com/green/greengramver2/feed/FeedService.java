package com.green.greengramver2.feed;

import com.green.greengramver2.common.MyFileUtils;
import com.green.greengramver2.feed.comment.FeedCommentMapper;
import com.green.greengramver2.feed.comment.model.FeedCommentDto;
import com.green.greengramver2.feed.comment.model.FeedCommentGetReq;
import com.green.greengramver2.feed.comment.model.FeedCommentGetRes;
import com.green.greengramver2.feed.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {
    private final FeedMapper feedMapper;
    private final FeedPicsMapper feedPicsMapper;
    private final FeedCommentMapper feedCommentMapper;
    private final MyFileUtils myFileUtils;

    @Transactional
    public FeedPostRes postFeed(List<MultipartFile> pics, FeedPostReq p) {
        int result = feedMapper.insFeed(p);

        // --------------- 파일 등록
        long feedId = p.getFeedId();

        //저장 폴더 만들기, 저장위치/feed/${feedId}/파일들을 저장한다.
        String middlePath = String.format("feed/%d", feedId);
        myFileUtils.makeFolders(middlePath);

        // 랜덤 파일명 저장용 >> feed_pics 테이블에 저장할 때 사용
        // pics.size() 사용 >> 적절한 크기의 내부 배열이 만들지면 배열을 확장하고 복사하는 추가 작업이 필요 없게 됨.
        List<String> picNameList = new ArrayList<>(pics.size());
        for(String picName : picNameList) {

        }


        for(MultipartFile pic : pics) {
            //각 파일 랜덤파일명 만들기
            String savedPicName = myFileUtils.makeRandomFileName(pic);
            picNameList.add(savedPicName);
            String filePath = String.format("%s/%s", middlePath, savedPicName);
            try {
                myFileUtils.transferTo(pic, filePath);
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        FeedPicDto feedPicDto = new FeedPicDto();
        feedPicDto.setFeedId(feedId);
        feedPicDto.setPics(picNameList);
        int resultPics = feedPicsMapper.insFeedPics(feedPicDto);

//        FeedPostRes res = new FeedPostRes();
//        res.setFeedId(feedId);
//        res.setPics(picNameList); >> FeedPostRes에서 @Setter 제거하고 @Builder 넣음

        return FeedPostRes.builder()
                .feedId(feedId)
                .pics(picNameList)
                .build();
    }

    public List<FeedGetRes> getFeedList(FeedGetReq p) {
        // N + 1 이슈 발생
        List<FeedGetRes> list = feedMapper.selFeedList(p);
        for(FeedGetRes item : list) {
            // 피드 당 사진 리스트
            item.setPics(feedPicsMapper.selFeedPics(item.getFeedId()));

            // 피드 당 댓글 4개
            FeedCommentGetReq commentGetReq = new FeedCommentGetReq();
            commentGetReq.setPage(1);
            commentGetReq.setFeedId(item.getFeedId());

            List<FeedCommentDto> commentList = feedCommentMapper.selFeedCommentList(commentGetReq);

            FeedCommentGetRes commentGetRes = new FeedCommentGetRes();
            commentGetRes.setCommentList(commentList);
            commentGetRes.setMoreComment(commentList.size() == 4); //4개면 true, 4개 아니면 false

            if(commentGetRes.isMoreComment()) {
                commentList.remove(commentList.size() - 1);
            }
            item.setComment(commentGetRes);
        }
        return list;
    }

}
