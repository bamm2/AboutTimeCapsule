package com.timecapsule.capsuleservice.service;

import com.timecapsule.capsuleservice.api.request.*;
import com.timecapsule.capsuleservice.api.response.*;
import com.timecapsule.capsuleservice.db.entity.*;
import com.timecapsule.capsuleservice.db.repository.*;
import com.timecapsule.capsuleservice.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service("capsuleService")
@RequiredArgsConstructor
public class CapsuleServiceImpl implements CapsuleService {
    private final DistanceService distanceService;
    private final CapsuleRepository capsuleRepository;
    private final MemberRepository memberRepository;
    private final CapsuleMemberRepository capsuleMemberRepository;
    private final MemoryOpenMemberRepository memoryOpenMemberRepository;
    private final MemoryRepository memoryRepository;
    private final CommentRepository commentRepository;

    @Override
    public SuccessRes<Integer> registCapsule(CapsuleRegistReq capsuleRegistReq) {
        Capsule capsule = Capsule.builder()
                .title(capsuleRegistReq.getTitle())
                .rangeType(capsuleRegistReq.getRangeType())
                .isGroup(capsuleRegistReq.isGroup())
                .latitude(capsuleRegistReq.getLatitude())
                .longitude(capsuleRegistReq.getLongitude())
                .address(capsuleRegistReq.getAddress())
                .build();

        Capsule newCapsule = capsuleRepository.save(capsule);

        for(Integer id : capsuleRegistReq.getMemberIdList()) {
            Optional<Member> oMember = memberRepository.findById(id);
            Member member = oMember.orElseThrow(() -> new IllegalArgumentException("member doesn't exist"));

            capsuleMemberRepository.save(CapsuleMember.builder().member(member).capsule(newCapsule).build());
        }

        return new SuccessRes<>(true, "캡슐 등록을 완료했습니다.", newCapsule.getId());
    }

    @Override
    public SuccessRes<CapsuleListRes> getMyCapsule(int memberId) {
        Optional<Member> oMember = memberRepository.findById(memberId);
        Member member = oMember.orElseThrow(() -> new IllegalArgumentException("member doesn't exist"));

        List<UnopenedCapsuleDto> unopenedCapsuleDtoList = new ArrayList<>();
        List<OpenedCapsuleDto> openedCapsuleDtoList = new ArrayList<>();
        List<MapInfoDto> mapInfoDtoList = new ArrayList<>();

        CapsuleListRes capsuleListRes = getCapsuleList(memberId, member, "my", unopenedCapsuleDtoList, openedCapsuleDtoList, mapInfoDtoList);
        return new SuccessRes<>(true, "나의 캡슐 목록을 조회합니다.", capsuleListRes);
    }

    @Override
    public SuccessRes<CapsuleListRes> getFriendCapsule(int memberId) {
        Optional<Member> oMember = memberRepository.findById(memberId);
        Member member = oMember.orElseThrow(() -> new IllegalArgumentException("member doesn't exist"));

        List<UnopenedCapsuleDto> unopenedCapsuleDtoList = new ArrayList<>();
        List<OpenedCapsuleDto> openedCapsuleDtoList = new ArrayList<>();
        List<MapInfoDto> mapInfoDtoList = new ArrayList<>();

        // 친구 만들고 다시 테스트하기
        System.out.println("친구를 구하러 간다");

        for(Member myFriend : friendList(member)) {
            System.out.println("내친구: " + myFriend.getId() + " 닉네임: " + myFriend.getNickname());
            CapsuleListRes newCapsuleList = getCapsuleList(memberId, myFriend, "friend", unopenedCapsuleDtoList, openedCapsuleDtoList, mapInfoDtoList);

            unopenedCapsuleDtoList = newCapsuleList.getUnopenedCapsuleDtoList();
            openedCapsuleDtoList = newCapsuleList.getOpenedCapsuleDtoList();
            mapInfoDtoList = newCapsuleList.getMapInfoDtoList();

            System.out.println("unopen 크기: " + unopenedCapsuleDtoList.size());
            System.out.println("open 크기: " + openedCapsuleDtoList.size());
            System.out.println("map 크기: " + mapInfoDtoList.size());
        }


        CapsuleListRes capsuleListRes = CapsuleListRes.builder()
                .unopenedCapsuleDtoList(unopenedCapsuleDtoList)
                .openedCapsuleDtoList(openedCapsuleDtoList)
                .mapInfoDtoList(mapInfoDtoList)
                .build();

        return new SuccessRes<>(true, "친구의 캡슐 목록을 조회합니다.", capsuleListRes);
    }

    private CapsuleListRes getCapsuleList(int myId, Member member, String who,
                                          List<UnopenedCapsuleDto> unopenedCapsuleDtoList,
                                          List<OpenedCapsuleDto> openedCapsuleDtoList,
                                          List<MapInfoDto> mapInfoDtoList) {

        for(CapsuleMember capsuleMember : member.getCapsuleMemberList()) {
            Capsule capsule = capsuleMember.getCapsule();
            int capsuleId = capsule.getId();

            if(capsule.isDeleted()) continue;
            if(who.equals("friend") && capsule.getRangeType().equals("PRIVATE")) continue;

            // 캡슐에 대한 열람 기록이 있는지 확인
            boolean isOpened = memoryOpenMemberRepository.existsByCapsuleIdAndMemberId(capsuleId, myId);
            boolean isLocked = false;

            List<Memory> memoryList = memoryRepository.findAllByCapsuleIdAndIsDeletedFalse(capsuleId);
            int memorySize = memoryList.size();
            LocalDate openDate = null;
            if(memorySize > 0) openDate = (isOpened) ? memoryList.get(0).getOpenDate() : memoryList.get(memorySize-1).getOpenDate();

            // 미열람
            if(!isOpened) {
                // 잠김 여부
                if(memorySize > 0 && LocalDate.now().isBefore(openDate)) isLocked = true;

                unopenedCapsuleDtoList.add(UnopenedCapsuleDto.builder()
                        .capsuleId(capsuleId)
                        .openDate(openDate)
                        .address(capsule.getAddress())
                        .isLocked(isLocked)
                        .build());
            } else {
                // 열람
                int count = memoryOpenMemberRepository.countByCapsuleIdAndMemberId(capsuleId, myId);
                boolean isAdded = false;
                if(memorySize != count) isAdded = true;

                openedCapsuleDtoList.add(OpenedCapsuleDto.builder()
                        .capsuleId(capsuleId)
                        .openDate(openDate)
                        .address(capsule.getAddress())
                        .isAdded(isAdded)
                        .build());
            }

            mapInfoDtoList.add(MapInfoDto.builder()
                    .capsuleId(capsuleId)
                    .latitude(capsule.getLatitude())
                    .longitude(capsule.getLongitude())
                    .isOpened(isOpened)
                    .isLocked(isLocked)
                    .build());
        }

        Collections.sort(unopenedCapsuleDtoList, Comparator.comparing(o -> (o.getOpenDate() == null ? LocalDate.MIN : o.getOpenDate())));
        Collections.sort(openedCapsuleDtoList, Comparator.comparing(o -> (o.getOpenDate() == null ? LocalDate.MIN : o.getOpenDate())));

        return CapsuleListRes.builder()
                .unopenedCapsuleDtoList(unopenedCapsuleDtoList)
                .openedCapsuleDtoList(openedCapsuleDtoList)
                .mapInfoDtoList(mapInfoDtoList)
                .build();
    }

    @Override
    public SuccessRes<OpenedCapsuleListRes> getOpenCapsule(int memberId) {
        Optional<Member> oMember = memberRepository.findById(memberId);
        Member member = oMember.orElseThrow(() -> new IllegalArgumentException("member doesn't exist"));

        List<OpenedCapsuleDto> openedCapsuleDtoList = new ArrayList<>();
        List<MapInfoDto> mapInfoDtoList = new ArrayList<>();

        for(MemoryOpenMember memoryOpenMember : member.getMemoryOpenMemberList()) {
            Capsule capsule = memoryOpenMember.getCapsule();
            if(capsule.isDeleted()) continue;
            int capsuleId = capsule.getId();

            List<Memory> memoryList = memoryRepository.findAllByCapsuleIdAndIsDeletedFalse(capsuleId);
            int memorySize = memoryList.size();
            LocalDate openDate = null;
            if(memorySize > 0) openDate = memoryList.get(0).getOpenDate();

            int count = memoryOpenMemberRepository.countByCapsuleIdAndMemberId(capsuleId, memberId);
            boolean isAdded = false;
            if(memorySize != count) isAdded = true;

            openedCapsuleDtoList.add(OpenedCapsuleDto.builder()
                    .capsuleId(capsuleId)
                    .openDate(openDate)
                    .address(capsule.getAddress())
                    .isAdded(isAdded)
                    .build());

            mapInfoDtoList.add(MapInfoDto.builder()
                    .capsuleId(capsuleId)
                    .latitude(capsule.getLatitude())
                    .longitude(capsule.getLongitude())
                    .isOpened(true)
                    .isLocked(false)
                    .build());

        }

        Collections.sort(openedCapsuleDtoList, Comparator.comparing(o -> (o.getOpenDate() == null ? LocalDate.MIN : o.getOpenDate())));

        OpenedCapsuleListRes openedCapsuleListRes = OpenedCapsuleListRes.builder()
                .openedCapsuleDtoList(openedCapsuleDtoList)
                .mapInfoDtoList(mapInfoDtoList)
                .build();
        return new SuccessRes<>(true, "나의 방문 기록을 조회합니다.", openedCapsuleListRes);
    }

    @Override
    public CommonRes deleteCapsule(int capsuleId) {
        Optional<Capsule> oCapsule = capsuleRepository.findById(capsuleId);
        Capsule capsule = oCapsule.orElseThrow(() -> new IllegalArgumentException("capsule doesn't exist"));

        for(Memory memory : capsule.getMemoryList()) {
            memory.getCommentList().forEach(comment -> commentRepository.save(Comment.of(comment, true)));
            memoryRepository.save(Memory.of(memory, true));
        }

        capsuleRepository.save(Capsule.of(capsule, true));

        return new CommonRes(true, "캡슐 삭제를 완료했습니다.");
    }

    @Override
    public CommonRes modifyCapsuleRange(int capsuleId, RangeType rangeType) {
        Optional<Capsule> oCapsule = capsuleRepository.findById(capsuleId);
        Capsule capsule = oCapsule.orElseThrow(() -> new IllegalArgumentException("capsule doesn't exist"));

        capsuleRepository.save(Capsule.of(capsule, rangeType));
        return new CommonRes(true, "캡슐의 공개 범위를 변경했습니다.");
    }

    @Override
    public SuccessRes<List<AroundCapsuleRes>> getAroundCapsule(AroundCapsuleReq aroundCapsuleReq) {
        // 그냥 전체 공개로 설정한 사람들의 캡슐 조회
        // 오픈 기간 지났고 내가 열람한 적 없는 주변 1km 이내에 있는 모든 캡슐 조회

        List<AroundCapsuleRes> aroundCapsuleResList = new ArrayList<>();
        List<Capsule> aroundCapsuleList = capsuleRepository.findAroundCapsule(aroundCapsuleReq.getLatitude(), aroundCapsuleReq.getLongitude());

        for(Capsule capsule : aroundCapsuleList) {
            if(capsule.isDeleted()) continue;
            // 내 권한 X
            boolean isMember = capsuleMemberRepository.existsByCapsuleIdAndMemberId(capsule.getId(), aroundCapsuleReq.getMemberId());
            if(isMember) continue;

            if(!capsule.getRangeType().equals("ALL")) continue;

            String memberNickname = capsule.getCapsuleMemberList().get(0).getMember().getNickname();
            int memberSize = capsule.getCapsuleMemberList().size() - 1;
            if(memberSize >= 1) memberNickname += (" 외 " + memberSize + "명");

            aroundCapsuleResList.add(AroundCapsuleRes.builder()
                    .capsuleId(capsule.getId())
                    .memberNickname(memberNickname)
                    .address(capsule.getAddress())
                    .build());
        }

        return new SuccessRes<>(true, "내 주변 캡슐을 조회합니다.", aroundCapsuleResList);
    }

    @Override
    public SuccessRes<List<GroupMemberRes>> getGroupMember(int capsuleId) {
        Optional<Capsule> oCapsule = capsuleRepository.findById(capsuleId);
        Capsule capsule = oCapsule.orElseThrow(() -> new IllegalArgumentException("capsule doesn't exist"));

        List<GroupMemberRes> groupMemberResList = new ArrayList<>();
        for(CapsuleMember capsuleMember : capsule.getCapsuleMemberList()) {
            Member member = capsuleMember.getMember();

            groupMemberResList.add(GroupMemberRes.builder()
                    .memberId(member.getId())
                    .nickname(member.getNickname())
                    .profileImageUrl(member.getProfileImageUrl())
                    .build());
        }

        return new SuccessRes<>(true, "그룹에 해당되는 멤버 목록을 조회합니다.", groupMemberResList);
    }

    @Override
    public SuccessRes<CapsuleDetailRes> getCapsuleDetail(CapsuleDetailReq capsuleDetailReq) {
        CapsuleDetailRes capsuleDetailRes = (CapsuleDetailRes) capsuleDetail(capsuleDetailReq, "getCapsuleDetail");
        return new SuccessRes<>(true, "캡슐 클릭 시 상세 정보를 조회합니다.", capsuleDetailRes);
    }

    @Override
    public SuccessRes<MapCapsuleDetailRes> getMapCapsuleDetail(CapsuleDetailReq capsuleDetailReq) {
        MapCapsuleDetailRes mapCapsuleDetailRes = (MapCapsuleDetailRes) capsuleDetail(capsuleDetailReq, "getMapCapsuleDetail");
        return new SuccessRes<>(true, "지도에서 마커 클릭 시 캡슐의 상세 정보를 조회합니다.", mapCapsuleDetailRes);
    }

    @Override
    public SuccessRes<List<MapRes>> getMapCapsule(CapsuleDetailReq capsuleDetailReq) {
        List<MapRes> mapResList = new ArrayList<>();
        List<Capsule> aroundCapsuleList = capsuleRepository.findAroundCapsule(capsuleDetailReq.getLatitude(), capsuleDetailReq.getLongitude());

        for(Capsule capsule : aroundCapsuleList) {
            if(capsule.isDeleted()) continue;

            boolean isMine = capsuleMemberRepository.existsByCapsuleIdAndMemberId(capsule.getId(), capsuleDetailReq.getMemberId());
            if(!isMine && capsule.getRangeType().equals("PRIVATE")) continue;
            
            // 친구 공개 구현하기
            boolean isOpened = memoryOpenMemberRepository.existsByCapsuleIdAndMemberId(capsule.getId(), capsuleDetailReq.getMemberId());
            boolean isLocked = false;

            List<Memory> memoryList = memoryRepository.findAllByCapsuleIdAndIsDeletedFalse(capsule.getId());
            int memorySize = memoryList.size();
            LocalDate openDate = null;

            if(memorySize > 0) {
                openDate = (isOpened) ? memoryList.get(0).getOpenDate() : memoryList.get(memorySize-1).getOpenDate();
                if(!isOpened && openDate!= null && LocalDate.now().isBefore(openDate)) isLocked = true;
            }

            int distance = (int) distanceService.distance(capsuleDetailReq.getLatitude(), capsuleDetailReq.getLongitude(),
                    capsule.getLatitude(), capsule.getLongitude(), "meter");
            boolean isAllowedDistance = (distance <= 100);

            mapResList.add(MapRes.builder()
                    .capsuleId(capsule.getId())
                    .isLocked(isLocked)
                    .isMine(isMine)
                    .isAllowedDistance(isAllowedDistance)
                    .build());
        }
        return new SuccessRes<>(true, "1km 이내에 있는 캡슐을 조회합니다.", mapResList);
    }

    @Override
    public SuccessRes<CapsuleCountRes> getCapsuleCount(int memberId) {
        SuccessRes<CapsuleListRes> myCapsule = getMyCapsule(memberId);
        SuccessRes<CapsuleListRes> friendCapsule = getFriendCapsule(memberId);
        SuccessRes<OpenedCapsuleListRes> openCapsule = getOpenCapsule(memberId);

        CapsuleCountRes capsuleCountRes = CapsuleCountRes.builder()
                .myCapsuleCnt(myCapsule.getData().getMapInfoDtoList().size())
                .friendCapsuleCnt(friendCapsule.getData().getMapInfoDtoList().size())
                .openCapsuleCnt(openCapsule.getData().getMapInfoDtoList().size())
                .build();

        return new SuccessRes<>(true, "나의 캡슐, 친구의 캡슐, 나의 방문 기록의 개수를 조회합니다.", capsuleCountRes);
    }

    @Override
    public SuccessRes<List<FriendRes>> getFriendList(int memberId) {
        Optional<Member> oMember = memberRepository.findById(memberId);
        Member member = oMember.orElseThrow(() -> new IllegalArgumentException("member doesn't exist"));

        List<FriendRes> friendResList = new ArrayList<>();
        friendList(member).forEach(friend -> friendResList.add(FriendRes.builder()
                .memberId(friend.getId())
                .nickname(friend.getNickname())
                .profileImageUrl(friend.getProfileImageUrl())
                .build()));

        return new SuccessRes<>(true, "나의 친구 목록을 조회합니다.", friendResList);
    }

    private Object capsuleDetail(CapsuleDetailReq capsuleDetailReq, String what) {
        Optional<Capsule> oCapsule = capsuleRepository.findById(capsuleDetailReq.getCapsuleId());
        Capsule capsule = oCapsule.orElseThrow(() -> new IllegalArgumentException("capsule doesn't exist"));

        String leftTime = "";
        String memberNickname = capsule.getCapsuleMemberList().get(0).getMember().getNickname();
        int memberSize = capsule.getCapsuleMemberList().size() - 1;
        if(memberSize >= 1) memberNickname += (" 외 " + memberSize + "명");

        boolean isLocked = false;
        boolean isOpened = memoryOpenMemberRepository.existsByCapsuleIdAndMemberId(capsule.getId(), capsuleDetailReq.getMemberId());
        boolean isGroup = capsule.getCapsuleMemberList().size() > 0;

        int distance = (int) distanceService.distance(capsuleDetailReq.getLatitude(), capsuleDetailReq.getLongitude(),
                capsule.getLatitude(), capsule.getLongitude(), "meter");

        List<Memory> memoryList = memoryRepository.findAllByCapsuleIdAndIsDeletedFalse(capsuleDetailReq.getCapsuleId());
        int memorySize = memoryList.size();
        LocalDate openDate = null;

        if(memorySize > 0) {
            openDate = (isOpened) ? memoryList.get(0).getOpenDate() : memoryList.get(memorySize-1).getOpenDate();
            if(openDate!= null && LocalDate.now().isBefore(openDate)) {
                isLocked = true;
                // 남은 시간 구하기
                LocalDateTime openDateTime = openDate.atStartOfDay();
                Duration duration = Duration.between(LocalDateTime.now(), openDateTime);

                leftTime += duration.toDays() + "일 " + (duration.toHours() % 24) + "시간 " + (duration.toMinutes() % 60) + "분 남음";
            }
        }

        if(what.equals("getCapsuleDetail")) {
            return CapsuleDetailRes.builder()
                    .memberNickname(memberNickname)
                    .distance(distance)
                    .leftTime(leftTime)
                    .isLocked(isLocked)
                    .latitude(capsule.getLatitude())
                    .longitude(capsule.getLongitude())
                    .address(capsule.getAddress())
                    .build();
        }
        return MapCapsuleDetailRes.builder()
                .capsuleId(capsule.getId())
                .memberNickname(memberNickname)
                .leftTime(leftTime)
                .isLocked(isLocked)
                .isGroup(isGroup)
                .openDate(openDate)
                .build();
    }

    private List<Member> friendList(Member member) {
        List<Member> friendList = new ArrayList<>();
        member.getFromMemberList().forEach(friend -> {
            if(friend.isAccepted()) friendList.add(friend.getToMember());
        });

        member.getToMemberList().forEach(friend -> {
            if(friend.isAccepted()) friendList.add(friend.getFromMember());
        });

        friendList.sort(Comparator.comparing(Member::getNickname));
        return friendList;
    }

}