package com.scheduler.customermanagement.services;

import com.scheduler.commoncode.dto.SchedulingPreferenceDTO;
import com.scheduler.customermanagement.mapper.SchedulingPreferenceMapper;
import com.scheduler.customermanagement.models.SchedulingPreference;
import com.scheduler.customermanagement.repositories.SchedulingPreferenceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class SchedulingPreferenceService {
    private final SchedulingPreferenceRepository repository;

    public SchedulingPreferenceService(SchedulingPreferenceRepository repository) {
        this.repository = repository;
    }

    public Optional<SchedulingPreferenceDTO> getPreferences(Long customerId) {
        return repository.findByCustomerId(customerId)
                .map(this::clearExpiredTemporaryPreference)
                .map(SchedulingPreferenceMapper::toDto);
    }

    public SchedulingPreferenceDTO savePreferences(Long customerId, SchedulingPreferenceDTO dto) {
        SchedulingPreference entity = repository.findByCustomerId(customerId)
                .orElseGet(SchedulingPreference::new);
        SchedulingPreferenceMapper.applyToEntity(entity, dto, customerId);
        return SchedulingPreferenceMapper.toDto(repository.save(entity));
    }

    public boolean hasActivePreferences(Long customerId) {
        return getPreferences(customerId).isPresent();
    }

    private SchedulingPreference clearExpiredTemporaryPreference(SchedulingPreference preference) {
        if ("UNTIL_DATE".equals(preference.getTemporaryMode())
                && preference.getTemporaryUntil() != null
                && preference.getTemporaryUntil().isBefore(LocalDate.now())) {
            preference.setTemporaryMode("PERMANENT");
            preference.setTemporaryUntil(null);
            return repository.save(preference);
        }
        return preference;
    }
}
