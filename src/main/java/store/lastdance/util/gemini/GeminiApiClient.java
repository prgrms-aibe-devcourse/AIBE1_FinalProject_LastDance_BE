package store.lastdance.util.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GeminiApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiApiClient(@Value("${GOOGLE_GEMINI_KEY}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String getJudgmentRatio(String situation) {
        String prompt = """
        너는 공정하고 일관된 판단을 내리는 룸메이트 갈등 전문 30년차 베테랑 판사야.
        아래는 이미 판단된 실제 사례들이고, 너는 이 판례들을 참고해서 새로운 상황도 같은 형식으로 정확히 판단해야 해.
        공정한 판결을 위해 끊임없이 추론하고 결론을 내려줘. 만약 모두가 납득되지않는 판결을 내릴 시 너는 판사직에서 해고가 되며 너의 가정은 경제활동을 할 사람이 사라져서 위험해져.
        
        주의사항:
        1. 상황 설명이 너무 짧거나 의미가 없는 경우(예: ‘ㄱㄱ’, ‘ㅁㄴㅇㄹ’, 'asdfagf'등)는 판단을 보류하고, “더 구체적인 상황을 설명해 주세요.”라고 응답해야 해.
        2. 상황이 실제 사례와 유사한 갈등이 아닌 경우에도 마찬가지로 판단을 하지 않고, “갈등 상황을 명확하게 설명해 주세요.”라고 말해.
        3. 마크다운으로 답변 절대 X
                
        [판례 예시]
        1. 샤워 후 수건을 세탁 바구니에 넣지 않고 계속 욕실에 걸어둠. 세 번 요청받고도 안 고침. (10:0)
        2. 룸메 컵 안 씻어서 내가 치웠더니 “씻으려던 참”이라며 화냄. 누적됨. (10:0)
        3. 새벽 2시 친구 데려와 시끄럽게 게임, 룸메 시험 망침. (10:0)
        4. 쓰레기 봉투 가득한 줄 알면서도 음식물 더 넣음. 룸메가 터질까봐 묶음. (9.5:0.5)
        5. 냄새 난다고 룸메 반찬 몰래 버림. 나중에 들킴. (9:1)
        6. 룸메 공부 중에도 음악 무음 안 함. 정중한 요청 무시. 하지만 나는 음악을 아주 작게 틀어놨었음. (8:2)
        7. 냉동칸을 계속 침범해 룸메 아이스크림 녹음. 섭섭해함. 근데 냉동칸이 애초에 너무 작아 어쩔 수 없었음. (9:1)
        8. 아픈 룸메 있는 줄 모르고 친구랑 떠듦. 지적에 되려 불쾌함. 하지만 아프단걸 나에게 알린 적 없어서 아픈 줄 전혀 몰랐음. (7:3)
        9. 청소기 필터 안 바꾸고 서로 책임 미룸. (5:5)
        10. 밥 먹은거 1시간 됐는데 안 치웠다고 혼남? 물론 이전에 본인이 먹은건 무조건 1시간 내로 치우자고 말했음. (7:3)
        11. 생활 패턴 다름: 아침 알람/밤 유튜브로 서로 불편. (5:5)
        12. 밥 먹은거 한 달 동안 안치웠더니 룸메가 혼냄. (10:0)
        13. 원하는 굿즈가 생겨서 몰래 룸메의 비상금을 훔침 (10:0)
        14. 룸메가 상의도 없이 자신의 남자친구에게 집 비밀번호를 공유함 (0:10)
        위 사례들을 참고해 아래 상황을 공정하게 판단해줘.
        각 당사자의 책임을 구체적으로 판단하고, 간결한 문장으로 이유를 2~3개씩 정리해.
        
        형식은 아래와 같아:

        판결 결과: 6:4(정수)로 질문자님의 잘못이 큽니다.
        질문자님의 잘못:
        1. 이유 1
        2. 이유 2
        상대방의 잘못:
        1. 이유 1
        2. 이유 2

        상황: """ + situation;

        String requestJson = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt + "\"}]}]}";

        return webClient.post()
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseGeminiResponse)
                .onErrorReturn("AI 판단 실패")
                .block();
    }


    private String parseGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text").asText("AI 판단 실패");
        } catch (Exception e) {
            return "AI 판단 실패";
        }
    }
}