package com.scheduler.customermanagement.services;

import com.scheduler.commoncode.dto.SchedulingPreferenceDTO;
import com.scheduler.customermanagement.models.SchedulingPreference;
import com.scheduler.customermanagement.repositories.SchedulingPreferenceRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SchedulingPreferenceServiceTest {

    private final SchedulingPreferenceRepository repository = mock(SchedulingPreferenceRepository.class);
    private final SchedulingPreferenceService service = new SchedulingPreferenceService(repository);

    @Test
    void savesAndReloadsPreferencesForCustomer() {
        when(repository.findByCustomerId(123L)).thenReturn(Optional.empty());
        when(repository.save(any(SchedulingPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SchedulingPreferenceDTO dto = new SchedulingPreferenceDTO();
        dto.setPrimaryPriority("Health");
        dto.setCategoryPriorityOrder(List.of("Health", "Work", "Duty", "Social", "Sport", "Leisure"));
        dto.setCategoryImportance(Map.of("Health", 4, "Work", 3, "Leisure", 2));
        dto.setFixedCommitmentCategories(Set.of("Work", "Health"));
        dto.setPauseMinutes(10);

        SchedulingPreferenceDTO saved = service.savePreferences(123L, dto);

        assertThat(saved.getPrimaryPriority()).isEqualTo("Health");
        assertThat(saved.getCategoryPriorityOrder()).containsExactly("Health", "Work", "Duty", "Social", "Sport", "Leisure");
        assertThat(saved.getCategoryImportance()).containsEntry("Health", 4).containsEntry("Leisure", 2);
        assertThat(saved.getFixedCommitmentCategories()).containsExactlyInAnyOrder("Work", "Health");
        verify(repository).save(argThat(entity ->
                entity.getCustomerId().equals(123L)
                        && entity.getPauseMinutes().equals(10)
                        && entity.getCategoryImportance().get("Health").equals(4)
                        && entity.getCategoryPriorityOrder().equals(List.of("Health", "Work", "Duty", "Social", "Sport", "Leisure"))
        ));
    }

    @Test
    void oldPreferencesWithoutCategoryRankingUseDefaultRanking() {
        SchedulingPreference entity = new SchedulingPreference();
        entity.setCustomerId(123L);

        when(repository.findByCustomerId(123L)).thenReturn(Optional.of(entity));

        SchedulingPreferenceDTO loaded = service.getPreferences(123L).orElseThrow();

        assertThat(loaded.getCategoryPriorityOrder()).containsExactly("Work", "Duty", "Health", "Social", "Sport", "Leisure");
        assertThat(loaded.getCategoryImportance()).containsEntry("Work", 3).containsEntry("Leisure", 2).containsEntry("Education", 3);
    }

    @Test
    void expiredTemporaryPreferenceIsTurnedOffWhenLoaded() {
        SchedulingPreference entity = new SchedulingPreference();
        entity.setCustomerId(123L);
        entity.setTemporaryMode("UNTIL_DATE");
        entity.setTemporaryUntil(LocalDate.now().minusDays(1));

        when(repository.findByCustomerId(123L)).thenReturn(Optional.of(entity));
        when(repository.save(any(SchedulingPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SchedulingPreferenceDTO loaded = service.getPreferences(123L).orElseThrow();

        assertThat(loaded.getTemporaryMode()).isEqualTo("PERMANENT");
        assertThat(loaded.getTemporaryUntil()).isNull();
    }
}
