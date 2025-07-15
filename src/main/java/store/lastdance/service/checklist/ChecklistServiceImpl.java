package store.lastdance.service.checklist;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.checklist.Checklist;
import store.lastdance.domain.checklist.ChecklistType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.checklist.ChecklistRequestDTO;
import store.lastdance.dto.checklist.ChecklistResponseDTO;
import store.lastdance.dto.group.GroupMemberDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.checklist.ChecklistRepository;

import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.service.group.GroupService;
import store.lastdance.service.user.UserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChecklistServiceImpl implements ChecklistService{

    private final ChecklistRepository checklistRepository;
    private final GroupService groupService;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserService userService;

    @Override
    public ChecklistResponseDTO createChecklist(ChecklistRequestDTO checklistRequestDTO, UUID userId, UUID groupId) {


        // 그룹이 없을 경우 checklistRequestDTO.assigneeId()를 사용자 ID로 설정
        if (groupId == null && checklistRequestDTO.assigneeId() == null) {
            checklistRequestDTO = new ChecklistRequestDTO(
                    checklistRequestDTO.title(),
                    checklistRequestDTO.description(),
                    userId,
                    checklistRequestDTO.dueDate(),
                    checklistRequestDTO.priority()
            );
        }

        // 입력값 검증
        validateChecklistRequest(checklistRequestDTO);

        // 사용자 존재 확인
        userService.validateUserExists(userId);

        Group group = null;
        ChecklistType checklistType = ChecklistType.PERSONAL;

        // 그룹 ID가 null이 아니면 그룹 조회 및 멤버 여부 확인
        if (groupId != null) {
            checklistType = ChecklistType.GROUP;

            // 그룹 조회 및 멤버 여부 확인
            group = getGroupAndValidateMembership(groupId, checklistRequestDTO, userId);
        }

        Checklist checklist = Checklist.builder()
                .title(checklistRequestDTO.title())
                .description(checklistRequestDTO.description())
                .type(checklistType)
                .group(group)
                .assignee(getAssigneeById(checklistRequestDTO.assigneeId()))
                .dueDate(checklistRequestDTO.dueDate())
                .priority(checklistRequestDTO.priority())
                .build();

        // 할일 저장
        Checklist savedChecklist = checklistRepository.save(checklist);

        return convertToResponseDTO(savedChecklist);
    }

    // 그룹 조회 및 멤버 여부 확인 메소드
    private Group getGroupAndValidateMembership(UUID groupId, ChecklistRequestDTO checklistRequestDTO, UUID userId) {
        // 그룹 조회
        Group group = groupService.getGroupById(groupId, userId);

        // 그룹 멤버 여부 확인
        groupService.isUserMemberOfGroup(userId, group);
        groupService.isUserMemberOfGroup(checklistRequestDTO.assigneeId(), group);

        return group;
    }


    private void validateChecklistRequest(ChecklistRequestDTO checklistRequestDTO) {

        if (checklistRequestDTO.title() == null || checklistRequestDTO.title().isBlank()) {
            throw new CustomException(ErrorCode.CHECKLIST_TITLE_REQUIRED);
        }
        if (checklistRequestDTO.assigneeId() == null) {
            throw new CustomException(ErrorCode.CHECKLIST_ASSIGNEE_REQUIRED);
        }
        if (checklistRequestDTO.dueDate() == null) {
            throw new CustomException(ErrorCode.CHECKLIST_DUE_DATE_REQUIRED);
        }
        if (checklistRequestDTO.priority() == null) {
            throw new CustomException(ErrorCode.CHECKLIST_PRIORITY_REQUIRED);
        }
    }

    private User getAssigneeById(UUID assigneeId) {
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private ChecklistResponseDTO convertToResponseDTO(Checklist checklist) {

        return new ChecklistResponseDTO(
                checklist.getChecklistId(),
                checklist.getTitle(),
                checklist.getDescription(),
                checklist.getType(),
                checklist.getGroup() != null ? checklist.getGroup().getGroupId() : null,
                checklist.getGroup() != null ? checklist.getGroup().getGroupName() : null,
                new GroupMemberDTO(
                        checklist.getAssignee().getUserId(),
                        checklist.getAssignee().getNickname(),
                        checklist.getAssignee().getProfileImageFile() != null ?
                                checklist.getAssignee().getProfileImageFile().getFilePath() : null,
                        checklist.getGroup() != null ? groupMemberRepository.findByUserAndGroup(
                                checklist.getAssignee(),
                                checklist.getGroup()
                        ).getRole() : null),
                checklist.getIsCompleted(),
                checklist.getCompletedAt() != null ? checklist.getCompletedAt().toString() : null,
                checklist.getDueDate().toString(),
                checklist.getPriority().name()
        );
    }

    @Override
    public List<ChecklistResponseDTO> getGroupChecklist(UUID groupId, UUID userId) {


        // 그룹 조회
        Group group = groupService.getGroupById(groupId, userId);

        // 사용자 존재 확인
        userService.validateUserExists(userId);

        // 그룹 멤버 여부 확인
        groupService.isUserMemberOfGroup(userId, group);

        // 그룹 할일 목록 조회
        return checklistRepository.findByGroup(group)
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    @Override
    public List<ChecklistResponseDTO> getPersonalChecklist(UUID userId) {


        // 사용자 존재 확인
        userService.validateUserExists(userId);

        // 개인 할일 목록 조회
        return checklistRepository.findByAssignee(getAssigneeById(userId))
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    @Override
    public ChecklistResponseDTO updateGroupChecklist(Long checklistId, ChecklistRequestDTO checklistRequestDTO, UUID userId, UUID groupId) {



        // 입력값 검증
        validateChecklistRequest(checklistRequestDTO);

        // 사용자 존재 확인
        userService.validateUserExists(userId);

        // 그룹 ID가 null이 아니면 그룹 조회 및 멤버 여부 확인
        if (groupId != null) {

            // 그룹 조회 및 멤버 여부 확인
            Group group = getGroupAndValidateMembership(groupId, checklistRequestDTO, userId);
        }

        // 할일 조회 및 수정
        Checklist checklist = getChecklistById(checklistId);

        checklist.update(
                checklistRequestDTO.title(),
                checklistRequestDTO.description(),
                getAssigneeById(checklistRequestDTO.assigneeId()),
                checklistRequestDTO.dueDate(),
                checklistRequestDTO.priority()
        );

        Checklist updatedChecklist = checklistRepository.save(checklist);

        return convertToResponseDTO(updatedChecklist);
    }

    // 할일 조회 메서드
    private Checklist getChecklistById(Long checklistId) {
        return checklistRepository.findById(checklistId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHECKLIST_NOT_FOUND));
    }

    @Override
    public ChecklistResponseDTO updatePersonalChecklist(Long checklistId, ChecklistRequestDTO checklistRequestDTO, UUID userId) {

        return updateGroupChecklist(checklistId, checklistRequestDTO, userId, null);
    }

    @Override
    public void deleteChecklist(Long checklistId, UUID userId) {


        // 사용자 존재 확인
        userService.validateUserExists(userId);

        // 할일 조회
        Checklist checklist = getChecklistById(checklistId);

        // 할일 삭제
        checklistRepository.delete(checklist);
    }

    @Override
    public ChecklistResponseDTO completeChecklist(Long checklistId, UUID userId) {


        // 사용자 존재 확인
        userService.validateUserExists(userId);

        // 할일 조회
        Checklist checklist = getChecklistById(checklistId);

        // 할일 완료 처리
        checklist.complete();
        Checklist updatedChecklist = checklistRepository.save(checklist);

        return convertToResponseDTO(updatedChecklist);
    }

    @Override
    public ChecklistResponseDTO undoChecklist(Long checklistId, UUID userId) {


        // 사용자 존재 확인
        userService.validateUserExists(userId);

        // 할일 조회
        Checklist checklist = getChecklistById(checklistId);

        // 할일 완료 취소 처리
        checklist.uncomplete();
        Checklist updatedChecklist = checklistRepository.save(checklist);

        return convertToResponseDTO(updatedChecklist);
    }
}
