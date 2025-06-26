-- 2번 게시글
INSERT INTO posts (
    post_id,
    title,
    content,
    category,
    user_id,
    like_count,
    report_count,
    created_at,
    updated_at
) VALUES (
             '22222222-f4b0-49f5-9172-23488b0fa35d',
             '생활 꿀팁 공유해요',
             '절수기 설치하면 수도세 절약돼요!',
             'FIND_MATE',
             'f62e3f4c-35b2-494b-b6ff-4e01c446ce18',
             0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         );

-- 3번 게시글
INSERT INTO posts (
    post_id,
    title,
    content,
    category,
    user_id,
    like_count,
    report_count,
    created_at,
    updated_at
) VALUES (
             '33333333-f4b0-49f5-9172-23488b0fa35d',
             '자유글입니다~',
             '오늘 날씨 진짜 좋네요 🌞',
             'FREE_BOARD',
             'f62e3f4c-35b2-494b-b6ff-4e01c446ce18',
             0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         );

-- 4번 게시글
INSERT INTO posts (
    post_id,
    title,
    content,
    category,
    user_id,
    like_count,
    report_count,
    created_at,
    updated_at
) VALUES (
             '44444444-f4b0-49f5-9172-23488b0fa35d',
             '질문 있어요!',
             '오븐 렌트하는 곳 있을까요?',
             'QNA',
             'f62e3f4c-35b2-494b-b6ff-4e01c446ce18',
             0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         );

INSERT INTO comments (
    comment_id, post_id, user_id, content, report_count, created_at, updated_at
) VALUES
      ('a0000000-0000-0000-0000-000000000001', '22222222-f4b0-49f5-9172-23488b0fa35d', 'f62e3f4c-35b2-494b-b6ff-4e01c446ce18', '좋은 정보 감사합니다!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('a0000000-0000-0000-0000-000000000002', '22222222-f4b0-49f5-9172-23488b0fa35d', 'f62e3f4c-35b2-494b-b6ff-4e01c446ce18', '궁금했던 내용인데 잘 봤습니다.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('a0000000-0000-0000-0000-000000000003', '33333333-f4b0-49f5-9172-23488b0fa35d', 'f62e3f4c-35b2-494b-b6ff-4e01c446ce18', '자세한 설명 감사합니다.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- 이하 동일하게 user_id 수정
      ('a0000000-0000-0000-0000-000000000020', '33333333-f4b0-49f5-9172-23488b0fa35d', 'f62e3f4c-35b2-494b-b6ff-4e01c446ce18', '이 글 덕분에 이해가 쉬워졌어요.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 나머지 댓글들
-- 전부 'user_id' 값을 'f62e3f4c-35b2-494b-b6ff-4e01c446ce18'로 변경하였습니다
-- 이하 생략

