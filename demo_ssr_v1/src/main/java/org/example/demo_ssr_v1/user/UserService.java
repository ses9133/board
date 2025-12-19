package org.example.demo_ssr_v1.user;

import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1._core.errors.exception.Exception400;
import org.example.demo_ssr_v1._core.errors.exception.Exception403;
import org.example.demo_ssr_v1._core.errors.exception.Exception404;
import org.example.demo_ssr_v1._core.errors.exception.Exception500;
import org.example.demo_ssr_v1._core.utils.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

// Service --> 응답 DTO 설계해서 전달 --> Controller

/**
 * 사용자 서비스 레이어
 * <p>
 * 1. 역할
 * - 비즈니스 로직을 처리하는 계층
 * - Controller 와 Repository 사이의 중간 계층
 * - 트랜잭션 관리
 * - 여러 Repository를 조합하여 복잡한 비즈니스 로직을 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    // 객체 지향 개념 (SOLID)
    // DIP - 추상화가 높은 녀석을 선언하는 것이 좋다.
    private final UserRepository userRepository;

    // 회원가입
    @Transactional
    public User 회원가입(UserRequest.JoinDTO joinDTO) {
        // 유효성 검사: Controller 에서

        // 1. 사용자명 중복 체크
        if (userRepository.findByUsername(joinDTO.getUsername()).isPresent()) {
            throw new Exception400("이미 존재하는 사용자 이름입니다.");
        }

        String profileImageFileName = null;

        // 2. 회원가입시 파일이 넘어왔는지 확인
        if (joinDTO.getProfileImage() != null) {
            // 2.1 유효성 검사
            try {
                if (!FileUtil.isImageFile(joinDTO.getProfileImage())) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다.");
                }
                profileImageFileName = FileUtil.saveFile(joinDTO.getProfileImage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        User user = joinDTO.toEntity(profileImageFileName);
        return userRepository.save(user);
    }

    // 로그인
    public User 로그인(UserRequest.LoginDTO loginDTO) {
        // 사용자가 던진 값과 DB 에 사용자 이름과 비밀번호를 확인
        User user = userRepository.findByUsernameAndPassword(loginDTO.getUsername(), loginDTO.getPassword())
                .orElse(null); // 로그인 실패시 null 반환

        if (user == null) {
            throw new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다.");
        }
        return user;
    }

    public User 회원정보수정화면(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("해당 사용자를 찾을 수 없습니다."));

        if (!user.isOwner(userId)) {
            throw new Exception403("수정 권한이 없습니다.");
        }
        return user;
    }

    // 데이터의 수정 (더티 체킹되려면 - 반드시 먼저 조회, 조회된 객체의 상태값 변경 --> 자동 반영)
    // 1. 회원 정보 조회
    // 2. 인가 검사
    // 3. 엔티티 상태 변경 (더티 체킹)
    // 4. 트랜잭션이 일어나고 변경된 유저 반환
    @Transactional
    public User 회원정보수정(UserRequest.UpdateDTO updateDTO, Long userId) {
        User userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("해당 사용자를 찾을 수 없습니다."));

        if (!userEntity.isOwner(userId)) {
            throw new Exception403("수정 권한이 없습니다.");
        }

        // 추가 - 프로필 이미지 처리
        // 중요 : 우리 프로젝트에서는 이미지 수정도 선택 사항
        // 새로운 이미지 파일을 생성하고 기존에 있던 이미지파일을 삭제해야한다.
        // 추가로 DB 정보도 업데이트 해야함
        String oldProfileImage = userEntity.getProfileImage();
        // 분기 처리 - 이미지명이 있거나 또는 null
        if(updateDTO.getProfileImage() != null && !updateDTO.getProfileImage().isEmpty()) {
            // 1. 이미지 파일인지 검증
            if(!FileUtil.isImageFile(updateDTO.getProfileImage())) {
                throw new Exception400("이미지 파일만 업로드 가능합니다.");
            }

            // 2. 새 이미지 저장
            try {
                String newProfileImageName = FileUtil.saveFile(updateDTO.getProfileImage());
                updateDTO.setProfileImageFileName(newProfileImageName);

                if(oldProfileImage != null && !oldProfileImage.isEmpty()) {
                    // 기존에 있던 이미지를 삭제처리
                    FileUtil.deleteFile(oldProfileImage);
                }
            } catch (IOException e) {
                throw new Exception500("파일 저장에 실패했습니다.");
            }
        } else {
            // 새 이미지가 업로드 되지 않았으면 기존 이미지 파일 이름 유지
            updateDTO.setProfileImageFileName(oldProfileImage);
        }

        // 객체 상태값 변경 (트랜잭션이 끝나면 자동으로 commit 및 반영할 것임)
        userEntity.update(updateDTO);
        return userEntity;
    }

    public User 마이페이지(Long sessionUserId) {
        User user = userRepository.findById(sessionUserId)
                .orElseThrow(() -> new Exception404("해당 사용자를 찾을 수 없습니다."));

        if (!user.isOwner(sessionUserId)) {
            throw new Exception403("조회 권한이 없습니다.");

        }

        return user;
    }

    @Transactional
    public User 프로필이미지삭제(Long sessionUserId) {
        // 1. 회원 정보 조회
        // 2. 회원 정보와 세션 id 값이 같은지 판단 -> 인가 처리
        // 3. 프로필 이미지가 있다면 삭제(FileUtil) 헬퍼 클래스 사용할 예정 (디스크에서 삭제)
        // 4. DB 에서 프로필 이름  null 로 update 처리

        User userEntity = userRepository.findById(sessionUserId)
                .orElseThrow(() -> new Exception404("해당 사용자를 찾을 수 없습니다."));

        if (!userEntity.isOwner(sessionUserId)) {
            throw new Exception403("삭제 권한이 없습니다.");
        }

        String profileImage = userEntity.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                FileUtil.deleteFile(profileImage);
            } catch (IOException e) {
                System.out.println("프로필 이미지 파일 삭제 실패");
            }
        }

        // 객체 상태값 변경
        userEntity.setProfileImage(null);

        return userEntity;
    }
}
