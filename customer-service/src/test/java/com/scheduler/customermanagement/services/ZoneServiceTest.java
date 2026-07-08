package com.scheduler.customermanagement.services;

import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.customermanagement.mapper.ZoneConfigDtoMapper;
import com.scheduler.customermanagement.mapper.ZoneDefinitionDtoMapper;
import com.scheduler.customermanagement.models.ZoneConfiguration;
import com.scheduler.customermanagement.models.ZoneDefinition;
import com.scheduler.customermanagement.repositories.ZoneConfigurationRepository;
import com.scheduler.customermanagement.repositories.ZoneDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneConfigurationRepository configRepo;

    @Mock
    private ZoneConfigDtoMapper configMapper;

    @Mock
    private ZoneDefinitionRepository defRepo;

    @Mock
    private ZoneDefinitionDtoMapper defMapper;

    @InjectMocks
    private ZoneService zoneService;

    @Test
    void activateConfigOnlyActivatesOwnedConfigAndDeactivatesExistingConfigs() {
        ZoneConfiguration owned = config(10L, 42L, false);
        ZoneConfiguration active = config(11L, 42L, true);

        when(configRepo.findById(10L)).thenReturn(Optional.of(owned));
        when(configRepo.findAllByCustomerId(42L)).thenReturn(List.of(owned, active));
        when(configRepo.save(owned)).thenReturn(owned);

        zoneService.activateConfig(42L, 10L);

        assertThat(owned.isActive()).isTrue();
        assertThat(active.isActive()).isFalse();
        verify(configRepo).saveAll(List.of(owned, active));
        verify(configRepo).save(owned);
    }

    @Test
    void activateConfigRejectsConfigFromAnotherCustomer() {
        ZoneConfiguration otherCustomerConfig = config(10L, 99L, false);

        when(configRepo.findById(10L)).thenReturn(Optional.of(otherCustomerConfig));

        assertThatThrownBy(() -> zoneService.activateConfig(42L, 10L))
                .isInstanceOf(ResponseStatusException.class);

        verify(configRepo, never()).save(otherCustomerConfig);
    }

    @Test
    void updateDefinitionCopiesSubmittedFields() {
        ZoneConfiguration owned = config(10L, 42L, true);
        ZoneDefinition existing = new ZoneDefinition();
        existing.setId(5L);
        existing.setZoneConfigId(10L);
        existing.setTitle("Old");
        existing.setDayMask(1);
        existing.setStartTime(LocalTime.of(8, 0));
        existing.setEndTime(LocalTime.of(9, 0));

        ZoneDefinitionDTO dto = new ZoneDefinitionDTO(
                5L,
                "New",
                62,
                LocalTime.of(10, 0),
                LocalTime.of(11, 30),
                Set.of("work"),
                Set.of("errands"),
                5,
                "Work",
                Set.of("Duty", "Health"),
                "PREFERRED",
                "KEEP_INSIDE_WINDOW"
        );

        when(configRepo.findById(10L)).thenReturn(Optional.of(owned));
        when(defRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(defRepo.save(existing)).thenReturn(existing);

        zoneService.updateDefinition(42L, 10L, 5L, dto);

        ArgumentCaptor<ZoneDefinition> captor = ArgumentCaptor.forClass(ZoneDefinition.class);
        verify(defRepo).save(captor.capture());
        ZoneDefinition saved = captor.getValue();

        assertThat(saved.getTitle()).isEqualTo("New");
        assertThat(saved.getDayMask()).isEqualTo(62);
        assertThat(saved.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(saved.getEndTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(saved.getAllowedCategories()).containsExactlyInAnyOrder("Work", "Duty", "Health");
        assertThat(saved.getExcludedCategories()).containsExactly("errands");
        assertThat(saved.getPriorityOverrideThreshold()).isEqualTo(5);
        assertThat(saved.getPrimaryCategory()).isEqualTo("Work");
        assertThat(saved.getSecondaryCategories()).containsExactlyInAnyOrder("Duty", "Health");
        assertThat(saved.getBehaviorMode()).isEqualTo("PREFERRED");
        assertThat(saved.getTargetPlacementMode()).isEqualTo("KEEP_INSIDE_WINDOW");
    }

    @Test
    void deleteConfigDeletesChildDefinitionsBeforeProfile() {
        ZoneConfiguration owned = config(10L, 42L, true);
        when(configRepo.findById(10L)).thenReturn(Optional.of(owned));

        boolean deleted = zoneService.deleteZoneConfig(42L, 10L);

        assertThat(deleted).isTrue();
        verify(defRepo).deleteByZoneConfigId(10L);
        verify(configRepo).delete(owned);
    }

    private ZoneConfiguration config(Long id, Long customerId, boolean active) {
        ZoneConfiguration config = new ZoneConfiguration();
        config.setId(id);
        config.setCustomerId(customerId);
        config.setActive(active);
        return config;
    }
}
