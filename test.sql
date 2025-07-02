CREATE TABLE youth_policies (
                                plcy_no VARCHAR(50) PRIMARY KEY,
                                plcy_nm TEXT,
                                plcy_kywd_nm TEXT,
                                plcy_expln_cn TEXT,
                                biz_prd_bgng_ymd TEXT,
                                biz_prd_end_ymd TEXT,
                                aply_ymd TEXT,
                                plcy_sprt_cn TEXT,
                                created_at TIMESTAMP,
                                updated_at TIMESTAMP
);


-- -- 1번 게시글
-- INSERT INTO posts (
--     post_id,
--     title,
--     content,
--     category,
--     user_id,
--     like_count,
--     report_count,
--     created_at,
--     updated_at
-- ) VALUES (
--              '11111111-f4b0-49f5-9172-23488b0fa35d',
--              '룸메이트 구합니다',
--              '같이 지낼 분을 찾고 있어요!',
--              'FIND_MATE',
--              '7ed022dc-1b73-4815-b0c1-6f6b57daad44',
--              0,
--              0,
--              CURRENT_TIMESTAMP,
--              CURRENT_TIMESTAMP
--          );

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
             '7ed022dc-1b73-4815-b0c1-6f6b57daad44',
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
             '7ed022dc-1b73-4815-b0c1-6f6b57daad44',
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
             '7ed022dc-1b73-4815-b0c1-6f6b57daad44',
             0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         );

INSERT INTO comments (
    comment_id, post_id, user_id, content, report_count, created_at, updated_at
) VALUES
      ('a0000000-0000-0000-0000-000000000001', '11111111-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '좋은 정보 감사합니다!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('a0000000-0000-0000-0000-000000000002', '11111111-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '궁금했던 내용인데 잘 봤습니다.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('a0000000-0000-0000-0000-000000000003', '33333333-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '자세한 설명 감사합니다.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- 이하 동일하게 user_id 수정
      ('a0000000-0000-0000-0000-000000000020', '33333333-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '이 글 덕분에 이해가 쉬워졌어요.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);



INSERT INTO comments (comment_id, post_id, user_id, content, report_count, created_at, updated_at)
VALUES
    ('97128ccb-0cf5-4da6-ad12-8813c21c779b', '22222222-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '정말 좋은 글이네요!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('3d12b7ef-bafc-49de-89e4-a785887663c2', '22222222-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '많은 도움이 되었습니다.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('6ddfc8ce-2a61-4fb5-a72a-7aae7aff516e', '11111111-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '궁금했는데 해결됐어요.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('60279d94-0297-4bce-8872-1a7eb9a5df85', '44444444-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '정보 공유 감사해요!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('f98ce29e-0cea-4405-8bc6-38f4a30927b7', '44444444-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '이런 글 자주 올려주세요!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('1ea2eba5-de2a-45ff-bfaa-5c05f06ce635', '44444444-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '좋아요 누르고 갑니다~', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c78677f2-76fb-4327-8aa4-074bedc59d8b', '22222222-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '생각지도 못했네요!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c4a7d58c-9cf6-489d-8420-361930c2939a', '11111111-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '정말 유익했어요.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('af86f378-a27f-4f9e-95f4-e6bec799d2f7', '22222222-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '추천합니다!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('1d15a931-9764-42ea-a71a-3ac21e933190', '33333333-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '이해가 잘 됐어요.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('8e23afd1-6890-44ae-a1b9-06a371e47bdc', '33333333-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '댓글 남깁니다~', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('2e1c2603-9e69-4ec9-ba56-658c629f6af5', '44444444-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '재밌는 내용이네요.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0f906fd7-2394-44a4-8e99-8a30f86a5299', '44444444-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '꿀팁 감사합니다!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('8aa1d52c-9129-4272-a12a-994210c0859e', '22222222-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '좋은 하루 되세요!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b1ba314f-5dec-4681-8418-b1e9bceb25d7', '22222222-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '완전 공감해요!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('05f1dbf6-7a15-4535-8d88-5b293bc04be6', '22222222-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '잘 보고 갑니다.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a58b2e3f-39dc-451f-bfef-376381847a5a', '33333333-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '글 감사합니다!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dd46c240-a9bb-49c2-845d-d1ce46ac8ce5', '22222222-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '좋은 아이디어네요.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('39e9e569-a902-4d32-ab5d-3df143534121', '44444444-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '이 글 공유할게요.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('f7a73e1e-588b-4ac2-8d64-1cf4a0e081de', '22222222-f4b0-49f5-9172-23488b0fa35d', '7ed022dc-1b73-4815-b0c1-6f6b57daad44', '친구에게 알려줘야겠어요.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
