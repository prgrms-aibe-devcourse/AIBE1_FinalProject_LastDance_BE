package store.lastdance.converter.group;

import org.springframework.stereotype.Component;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupApplication;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.dto.group.GroupApplicationResponseDTO;
import store.lastdance.dto.group.GroupMemberDTO;
import store.lastdance.dto.group.GroupResponseDTO;

import java.util.List;

@Component
public class GroupConverter {

    /**
     * Group 엔티티를 GroupResponseDTO로 변환
     */
    public GroupResponseDTO toGroupResponseDTO(Group group) {
        return new GroupResponseDTO(
                group.getGroupId(),
                group.getGroupName(),
                group.getInviteCode(),
                group.getMaxMembers(),
                group.getGroupBudget(),
                group.getOwner().getUserId(),
                toGroupMemberDTOList(group.getMembers())
        );
    }

    /**
     * Group 엔티티 리스트를 GroupResponseDTO 리스트로 변환
     */
    public List<GroupResponseDTO> toGroupResponseDTOList(List<Group> groups) {
        return groups.stream()
                .map(this::toGroupResponseDTO)
                .toList();
    }

    /**
     * GroupMember 엔티티를 GroupMemberDTO로 변환
     */
    public GroupMemberDTO toGroupMemberDTO(GroupMember member) {
        return new GroupMemberDTO(
                member.getUser().getUserId(),
                member.getUser().getNickname(),
                member.getUser().getProfileImageFile() != null ?
                        member.getUser().getProfileImageFile().getFilePath() : null,
                member.getRole()
        );
    }

    /**
     * GroupMember 엔티티 리스트를 GroupMemberDTO 리스트로 변환
     */
    public List<GroupMemberDTO> toGroupMemberDTOList(List<GroupMember> members) {
        return members.stream()
                .map(this::toGroupMemberDTO)
                .toList();
    }

    /**
     * GroupApplication 엔티티를 GroupApplicationResponseDTO로 변환
     */
    public GroupApplicationResponseDTO toGroupApplicationResponseDTO(GroupApplication application) {
        return new GroupApplicationResponseDTO(
                application.getUser().getUserId(),
                application.getGroup().getGroupId(),
                application.getUser().getNickname(),
                application.getUser().getEmail(),
                application.getUser().getProfileImageFile() != null ?
                        application.getUser().getProfileImageFile().getFilePath() : null,
                application.getUpdatedAt()
        );
    }

    /**
     * GroupApplication 엔티티 리스트를 GroupApplicationResponseDTO 리스트로 변환
     */
    public List<GroupApplicationResponseDTO> toGroupApplicationResponseDTOList(List<GroupApplication> applications) {
        return applications.stream()
                .map(this::toGroupApplicationResponseDTO)
                .toList();
    }
}