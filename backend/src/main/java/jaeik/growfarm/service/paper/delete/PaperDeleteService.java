package jaeik.growfarm.service.paper.delete;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;

public interface PaperDeleteService {
    void deleteMessageInMyPaper(CustomUserDetails userDetails, MessageDTO messageDTO);
}
