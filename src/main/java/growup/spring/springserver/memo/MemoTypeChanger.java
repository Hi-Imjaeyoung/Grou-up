package growup.spring.springserver.memo;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.memo.domain.Memo;
import growup.spring.springserver.memo.dto.MemoRequestDto;
import growup.spring.springserver.memo.dto.MemoResponseDto;

public class MemoTypeChanger {
    public static Memo requestToEntity(MemoRequestDto memoRequestDto, Campaign campaign){
        return Memo.builder()
                .campaign(campaign)
                .date(memoRequestDto.getDate())
                .contents(memoRequestDto.getContents())
                .build();
    }
    public static MemoResponseDto entityToDto(Memo memo){
        return MemoResponseDto.builder()
                .contents(memo.getContents())
                .id(memo.getId())
                .date(memo.getRawDate())
                .build();
    }
}
