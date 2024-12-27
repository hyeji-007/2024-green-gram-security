package com.green.greengram.feed;

import com.green.greengram.common.MyFileUtils;
import com.green.greengram.common.exception.CustomException;
import com.green.greengram.common.exception.FeedErrorCode;
import com.green.greengram.config.security.AuthenticationFacade;
import com.green.greengram.feed.comment.FeedCommentMapper;
import com.green.greengram.feed.comment.model.FeedCommentDto;
import com.green.greengram.feed.comment.model.FeedCommentGetReq;
import com.green.greengram.feed.comment.model.FeedCommentGetRes;
import com.green.greengram.feed.model.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class FeedService {
    private final FeedMapper feedMapper;
    private final FeedPicMapper feedPicMapper;
    private final FeedCommentMapper feedCommentMapper;
    private final MyFileUtils myFileUtils;
    private final AuthenticationFacade authenticationFacade;
    private final ApplicationAvailabilityBean applicationAvailability;

    @Transactional
    //자동 커밋 종료
    public FeedPostRes postFeed(List<MultipartFile> pics, FeedPostReq p) {
        p.setWriterUserId(authenticationFacade.getSignedUserId());
        int result = feedMapper.insFeed(p);
        if(result == 0) {
            throw new CustomException(FeedErrorCode.FAIL_TO_REG);
        }
        // --------------- 파일 등록
        long feedId = p.getFeedId();

        //저장 폴더 만들기, 저장위치/feed/${feedId}/파일들을 저장한다.
        String middlePath = String.format("feed/%d", feedId);
        myFileUtils.makeFolders(middlePath);

        //랜덤 파일명 저장용  >> feed_pics 테이블에 저장할 때 사용
        List<String> picNameList = new ArrayList<>(pics.size());
        for(MultipartFile pic : pics) {
            //각 파일 랜덤파일명 만들기
            String savedPicName = myFileUtils.makeRandomFileName(pic);
            picNameList.add(savedPicName);
            String filePath = String.format("%s/%s", middlePath, savedPicName);
            try {
                myFileUtils.transferTo(pic, filePath);
            } catch (IOException e) {
                //폴더 삭제 처리
                String delFolderPath = String.format("%s/%s", myFileUtils.getUploadPath(), middlePath);
                myFileUtils.deleteFolder(delFolderPath, true);
                throw new CustomException(FeedErrorCode.FAIL_TO_REG);
            }
        }
        FeedPicDto feedPicDto = new FeedPicDto();
        feedPicDto.setFeedId(feedId);
        feedPicDto.setPics(picNameList);
        int resultPics = feedPicMapper.insFeedPic(feedPicDto);

        return FeedPostRes.builder()
                .feedId(feedId)
                .pics(picNameList)
                .build();
    }

    //에러가 터졌다 rollback
    //에러가 안 터졌다 commit;

    public List<FeedGetRes> getFeedList(FeedGetReq p) {
        p.setSignedUserId(authenticationFacade.getSignedUserId());
        // N + 1 이슈 발생
        List<FeedGetRes> list = feedMapper.selFeedList(p); //피드 20개 있음
        for(int i=0; i<list.size(); i++) {
            FeedGetRes item = list.get(i);
            //피드 당 사진 리스트
            item.setPics(feedPicMapper.selFeedPicList(item.getFeedId()));

            //피드 당 댓글 4개
            FeedCommentGetReq commentGetReq = new FeedCommentGetReq(item.getFeedId(), 0, 3);
            List<FeedCommentDto> commentList = feedCommentMapper.selFeedCommentList(commentGetReq); //0~4

            FeedCommentGetRes commentGetRes = new FeedCommentGetRes();
            commentGetRes.setCommentList(commentList);
            commentGetRes.setMoreComment( commentList.size() == commentGetReq.getSize() ); //4개면 true, 4개 아니면 false

            if(commentGetRes.isMoreComment()) {
                commentList.remove(commentList.size() - 1);
            }
            item.setComment(commentGetRes);
        }
        return list;
    }

    //select 2번
    public List<FeedGetRes> getFeedList2(FeedGetReq p) {
        List<FeedGetRes> list = new ArrayList<>(p.getSize());

        //SELECT (1): feed + feed_pic
        List<FeedAndPicDto> feedAndPicDtoList = feedMapper.selFeedWithPicList(p);
        List<Long> feedIds = new ArrayList<>(list.size());

        //FeedGetRes: 중복된 값을 하나로 묶어준다.
        FeedGetRes beforeFeedGetRes = new FeedGetRes();
        for(FeedAndPicDto feedAndPicDto : feedAndPicDtoList) {
            //그 전 값과 달랐을 때만 하는 작업
            if(beforeFeedGetRes.getFeedId() != feedAndPicDto.getFeedId()) {
                feedIds.add(feedAndPicDto.getFeedId());

                //이전 FeedId 값이 바뀜
                beforeFeedGetRes = new FeedGetRes();
                beforeFeedGetRes.setPics(new ArrayList<>(3));
                list.add(beforeFeedGetRes);
                beforeFeedGetRes.setFeedId(feedAndPicDto.getFeedId());
                beforeFeedGetRes.setContents(feedAndPicDto.getContents());
                beforeFeedGetRes.setLocation(feedAndPicDto.getLocation());
                beforeFeedGetRes.setCreatedAt(feedAndPicDto.getCreatedAt());
                beforeFeedGetRes.setWriterUserId(feedAndPicDto.getWriterUserId());
                beforeFeedGetRes.setWriterNm(feedAndPicDto.getWriterNm());
                beforeFeedGetRes.setWriterPic(feedAndPicDto.getWriterPic());
                beforeFeedGetRes.setIsLike(feedAndPicDto.getIsLike());
            }
            //똑같은 FeedId 값이 넘어오고 사진이 다를 때만 하는 작업
            beforeFeedGetRes.getPics().add(feedAndPicDto.getPic());
        }

        //SELECT (2): feed_comment
        List<FeedCommentDto> feedCommentList = feedCommentMapper.selFeedCommentListByFeedIdsLimit4Ver2(feedIds);
        Map<Long, FeedCommentGetRes> commentHashMap = new HashMap<>();
        for(FeedCommentDto item : feedCommentList) {
            long feedId = item.getFeedId();
            if(!commentHashMap.containsKey(feedId)) {
                FeedCommentGetRes feedCommentGetRes = new FeedCommentGetRes();
                feedCommentGetRes.setCommentList(new ArrayList<>(4));
                commentHashMap.put(feedId, feedCommentGetRes);
            }
            FeedCommentGetRes feedCommentGetRes = commentHashMap.get(feedId);
            feedCommentGetRes.getCommentList().add(item);
        }
        for(FeedGetRes res : list) {
            FeedCommentGetRes feedCommentGetRes = commentHashMap.get(res.getFeedId());

            if(feedCommentGetRes == null) { //댓글이 하나도 없었던 피드인 경우
                feedCommentGetRes = new FeedCommentGetRes();
                feedCommentGetRes.setCommentList(new ArrayList<>());
            } else if (feedCommentGetRes.getCommentList().size() == 4) {
                feedCommentGetRes.setMoreComment(true);
                feedCommentGetRes.getCommentList().remove(feedCommentGetRes.getCommentList().size() - 1);
            }
            res.setComment(feedCommentGetRes);
        }

        return list;
    }


    //select 3번, 피드 5,000개 있음, 페이지당 20개씩 가져온다.
    public List<FeedGetRes> getFeedList3(FeedGetReq p) {
        p.setSignedUserId(authenticationFacade.getSignedUserId());
        //피드 리스트
        List<FeedGetRes> list = feedMapper.selFeedList(p);
        if (list.size() == 0) {
            return list;
        }

        //feed_id를 골라내야 한다.
        //list.stream().mapToLong(FeedGetRes::getFeedId).sum();
        List<Long> feedIds4 = list.stream().map(FeedGetRes::getFeedId).collect(Collectors.toList());

        List<Long> feedIds5 = list.stream().map(item -> ((FeedGetRes)item).getFeedId()).toList();
        List<Long> feedIds6 = list.stream().map(item -> { return ((FeedGetRes)item).getFeedId();}).toList();

        List<Long> feedIds = new ArrayList<>(list.size());
        for(FeedGetRes item : list) {
            feedIds.add(item.getFeedId());
        }
        log.info("feedIds: {}", feedIds);

        //피드와 관련된 사진 리스트
        List<FeedPicSel> feedPicList = feedPicMapper.selFeedPicListByFeedIds(feedIds);
        log.info("feedPicList: {}", feedPicList);

        Map<Long, List<String>> picHashMap = new HashMap<>();
        for(FeedPicSel item : feedPicList) {
            long feedId = item.getFeedId();
            if(!picHashMap.containsKey(feedId)) {
                picHashMap.put(feedId, new ArrayList<>(3));
            }
            List<String> pics = picHashMap.get(feedId);
            pics.add(item.getPic());
        }

        //피드와 관련된 댓글 리스트

        //댓글이 있는 경우만 정리
        List<FeedCommentDto> feedCommentList = feedCommentMapper.selFeedCommentListByFeedIdsLimit4Ver2(feedIds);
        Map<Long, FeedCommentGetRes> commentHashMap = new HashMap<>();
        for(FeedCommentDto item : feedCommentList) {
            long feedId = item.getFeedId();
            if(!commentHashMap.containsKey(feedId)) {
                FeedCommentGetRes feedCommentGetRes = new FeedCommentGetRes();
                feedCommentGetRes.setCommentList(new ArrayList<>(4));
                commentHashMap.put(feedId, feedCommentGetRes);
            }
            FeedCommentGetRes feedCommentGetRes = commentHashMap.get(feedId);
            feedCommentGetRes.getCommentList().add(item);
        }

        for(FeedGetRes res : list) {
            res.setPics(picHashMap.get(res.getFeedId()));
            FeedCommentGetRes feedCommentGetRes = commentHashMap.get(res.getFeedId());

            if(feedCommentGetRes == null) { //댓글이 하나도 없었던 피드인 경우
                feedCommentGetRes = new FeedCommentGetRes();
                feedCommentGetRes.setCommentList(new ArrayList<>());
            } else if (feedCommentGetRes.getCommentList().size() == 4) {
                feedCommentGetRes.setMoreComment(true);
                feedCommentGetRes.getCommentList().remove(feedCommentGetRes.getCommentList().size() - 1);
            }
            res.setComment(feedCommentGetRes);
        }
        log.info("list: {}", list);
        return list;
    }


    public List<FeedGetRes> getFeedList4(FeedGetReq p) {
        List<FeedWithPicCommentDto> dtoList = feedMapper.selFeedWithPicAndCommentLimit4List(p);
        List<FeedGetRes> list = new ArrayList<>(dtoList.size());
        for(FeedWithPicCommentDto item : dtoList) {
            list.add(new FeedGetRes(item));
        }
        return list;
    }


    @Transactional
    public int deleteFeed(FeedDeleteReq p) {
        p.setSignedUserId(authenticationFacade.getSignedUserId());
        //피드 댓글, 좋아요, 사진 삭제
        int affectedRowsEtc = feedMapper.delFeedLikeAndFeedCommentAndFeedPic(p);
        log.info("deleteFeed > affectedRows: {}", affectedRowsEtc);

        //피드 삭제
        int affectedRowsFeed = feedMapper.delFeed(p);
        log.info("deleteFeed > affectedRowsFeed: {}", affectedRowsFeed);

        //피드 사진 삭제 (폴더 삭제)
        String deletePath = String.format("%s/feed/%d", myFileUtils.getUploadPath(), p.getFeedId());
        myFileUtils.deleteFolder(deletePath, true);

        return 1;
    }
}