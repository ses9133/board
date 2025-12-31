package org.example.demo_ssr_v1.user;

import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1._core.errors.exception.Exception400;
import org.example.demo_ssr_v1._core.errors.exception.Exception403;
import org.example.demo_ssr_v1._core.errors.exception.Exception404;
import org.example.demo_ssr_v1._core.errors.exception.Exception500;
import org.example.demo_ssr_v1._core.utils.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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
    // DIP - 추상화가 높은 인터페이스를 선언하는 것이 좋다.
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${tenco.key}")
    private String tencoKey;

    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;

    @Transactional
    public User 카카오소셜로그인(String code) {
        // 1. 인가코드로 액세스 토큰 발급
        UserResponse.OAuthToken oAuthToken = 카카오액세스토큰발급(code);

        // 2. 액세스 토큰으로 프로필 정보 조회
        UserResponse.KaKaoProfile kaKaoProfile = 카카오프로필조회(oAuthToken.getAccessToken());

        // 3. 프로필 정보로 사용자 생성 또는 조회
        User user = 카카오사용자생성또는조회(kaKaoProfile);

        // 4. 로그인 처리(엔티티 반환)
        return user;
    }

    /**
     * 카카오 인가 코드로 액세스 토큰 발급
     * @param code 카카오 인가 코드
     * @return Oauth 액세스 토큰 정보
     */
    private UserResponse.OAuthToken 카카오액세스토큰발급(String code) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        // 2.4 HTTP 메시지 바디 구성
        MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
        tokenParams.add("grant_type", "authorization_code");
        tokenParams.add("client_id", clientId);
        tokenParams.add("redirect_uri", "http://localhost:8080/user/kakao");
        tokenParams.add("code", code);

        tokenParams.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenParams, tokenHeaders);
        ResponseEntity<UserResponse.OAuthToken> tokenResponse = restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                tokenRequest,
                UserResponse.OAuthToken.class
        );

        UserResponse.OAuthToken oAuthToken = tokenResponse.getBody();
        return oAuthToken;
    }

    /**
     * 카카오 액세스 토큰으로 프로필 정보 조회
     * @param accessToken 카카오 액세스 토큰
     * @return 카카오 프로필정보
     */
    private UserResponse.KaKaoProfile 카카오프로필조회(String accessToken) {
        RestTemplate profileRt = new RestTemplate();

        HttpHeaders profileHeaders = new HttpHeaders();

        profileHeaders.add("Authorization", "Bearer " + accessToken);
        profileHeaders.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeaders);

        ResponseEntity<UserResponse.KaKaoProfile> profileResponse = profileRt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                profileRequest,
                UserResponse.KaKaoProfile.class
        );

        UserResponse.KaKaoProfile kaKaoProfile = profileResponse.getBody();
        return kaKaoProfile;
    }

    /**
     *
     * @param kaKaoProfile
     * @return
     */
    @Transactional
    public User 카카오사용자생성또는조회(UserResponse.KaKaoProfile kaKaoProfile) {
        String username =  kaKaoProfile.getProperties().getNickname() + "_" + kaKaoProfile.getId();

        User userOrigin = 사용자이름조회(username);
        if(userOrigin == null) {
            System.out.println("기존 회원이 아니므로 자동 회원가입 진행시킴");
            User newUser = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(tencoKey))
                    .email(username + "@kakao.com")
                    .provider(OAuthProvider.KAKAO)
                    .build();

            String profileImage = kaKaoProfile.getProperties().getProfileImage();
            if(profileImage != null && !profileImage.isEmpty()) {
                newUser.setProfileImage(profileImage);
            }

            소셜회원가입(newUser);
            userOrigin = newUser; // 📌 필수 !!
        } else {
            System.out.println("이미 가입된 회원입니다. 바로 로그인처리 진행합니다.");
        }
        return userOrigin;
    }

    // 회원가입
    @Transactional
    public User 회원가입(UserRequest.JoinDTO joinDTO) {
        // 유효성 검사: Controller 에서

        // 1. 사용자명 중복 체크
        if (userRepository.findByUsername(joinDTO.getUsername()).isPresent()) {
            throw new Exception400("이미 존재하는 사용자 이름입니다.");
        }

        // 1.1 이메일 중복 체크
        if(userRepository.findByEmail(joinDTO.getEmail()).isPresent()) {
            throw new Exception400("이미 등록된 이메일 입니다.");
        }

        String profileImageFileName = null;

        // 2. 회원가입시 파일이 넘어왔는지 확인
        // MultipartFile (기본적으로 null 이 넘어올 수도, ""(공백)으로 들어올 수 도 있음)
        if (joinDTO.getProfileImage() != null && !joinDTO.getProfileImage().isEmpty()) {
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

        // 평문 비밀번호를 해싱하여 해시값을 만들어줌
        String hashPwd = passwordEncoder.encode(joinDTO.getPassword());
        System.out.println("======== hashPwd: " + hashPwd);

        User user = joinDTO.toEntity(profileImageFileName);
        // 비밀번호를 평문에서 해시값으로 변경해주어야 함
        user.setPassword(hashPwd);

        return userRepository.save(user);
    }

    // 로그인
    @Transactional
    public User 로그인(UserRequest.LoginDTO loginDTO) {
        // 사용자가 던진 값과 DB 에 사용자 이름과 비밀번호를 확인
        User user = userRepository.findByUsernameWithRoles(loginDTO.getUsername())
                .orElse(null); // 로그인 실패시 null 반환

        if (user == null) {
            throw new Exception400("사용자가 존재하지 않습니다.");
        }

        // 비밀번호 검증 (BCrypt matches 메서드를 사용해서 비교하면 된다.)
        // 일치하면 true, 불일치하면 false 반환
        if(!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            System.out.println("사용자명 또는 비밀번호가 올바르지 않습니다.");
            throw new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다.");
        }
        // 기존 샘플 데이터로 회원가입된 사용자들로는 로그인을 못함
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
        // 비밀번호 암호화 처리
        String hashPwd = passwordEncoder.encode(updateDTO.getPassword());
        updateDTO.setPassword(hashPwd);

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

    public User 사용자이름조회(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    @Transactional
    public void 소셜회원가입(User user) {
        userRepository.save(user);
    }

    @Transactional
    public User 포인트충전(Long userId, Integer amount) {
        // 1.  사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("해당 사용자를 조회할 수 없습니다."));

        // 2. 포인트 충전
        user.chargePoint(amount);
        return userRepository.save(user);
    }
}
