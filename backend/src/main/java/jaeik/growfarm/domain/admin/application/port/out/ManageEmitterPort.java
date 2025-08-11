package jaeik.growfarm.domain.admin.application.port.out;

public interface ManageEmitterPort {
    void deleteAllEmitterByUserId(Long userId);
}
