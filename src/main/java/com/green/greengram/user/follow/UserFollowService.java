package com.green.greengram.user.follow;

import com.green.greengram.user.follow.model.UserFollowReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFollowService {
    private final UserFollowMapper mapper;

    public int postUserFollow(UserFollowReq p) {
        return mapper.insUserFollow(p);
    }

    public int delUserFollow(UserFollowReq p) {
        return mapper.delUserFollow(p);
    }

}
