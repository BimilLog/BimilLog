package jaeik.growfarm.service.paper.read;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;

import java.util.List;

public interface PaperReadService {
    List<MessageDTO> myPaper(CustomUserDetails userDetails);
    List<VisitMessageDTO> visitPaper(String userName);
}
