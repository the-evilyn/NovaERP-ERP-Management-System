package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.UnitRequest;
import com.novaerp.backend.stock.dto.UnitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnitControllerTest {

    @Mock
    private UnitRepository unitRepository;

    @InjectMocks
    private UnitController unitController;

    private Unit sampleUnit;

    @BeforeEach
    void setUp() {
        sampleUnit = Unit.builder()
                .id(1L)
                .name("Kilogramme")
                .symbol("kg")
                .build();
    }

    @Test
    @DisplayName("list() returns paginated units")
    void testList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Unit> page = new PageImpl<>(List.of(sampleUnit), pageable, 1);
        when(unitRepository.findAll(pageable)).thenReturn(page);

        Page<UnitResponse> res = unitController.list(pageable);

        assertThat(res.getTotalElements()).isEqualTo(1);
        assertThat(res.getContent().get(0).name()).isEqualTo("Kilogramme");
        assertThat(res.getContent().get(0).symbol()).isEqualTo("kg");
        verify(unitRepository).findAll(pageable);
    }

    @Test
    @DisplayName("get(id) returns unit when found")
    void testGet_Found() {
        when(unitRepository.findById(1L)).thenReturn(Optional.of(sampleUnit));

        UnitResponse res = unitController.get(1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.symbol()).isEqualTo("kg");
    }

    @Test
    @DisplayName("get(id) throws NOT_FOUND when missing")
    void testGet_NotFound() {
        when(unitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> unitController.get(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("create() saves and returns 201 when name unique")
    void testCreate_Success() {
        UnitRequest req = new UnitRequest("Litre", "L");
        when(unitRepository.existsByName("Litre")).thenReturn(false);
        when(unitRepository.save(any(Unit.class))).thenAnswer(inv -> {
            Unit u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        ResponseEntity<UnitResponse> res = unitController.create(req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().name()).isEqualTo("Litre");
        assertThat(res.getBody().symbol()).isEqualTo("L");
        verify(unitRepository).save(any(Unit.class));
    }

    @Test
    @DisplayName("create() throws CONFLICT when name already exists")
    void testCreate_Conflict() {
        UnitRequest req = new UnitRequest("Kilogramme", "kg");
        when(unitRepository.existsByName("Kilogramme")).thenReturn(true);

        assertThatThrownBy(() -> unitController.create(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(unitRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() modifies and returns unit")
    void testUpdate_Success() {
        UnitRequest req = new UnitRequest("Kilogramme Net", "kg-net");
        when(unitRepository.findById(1L)).thenReturn(Optional.of(sampleUnit));
        when(unitRepository.existsByName("Kilogramme Net")).thenReturn(false);
        when(unitRepository.save(any(Unit.class))).thenReturn(sampleUnit);

        UnitResponse res = unitController.update(1L, req);

        assertThat(res.name()).isEqualTo("Kilogramme Net");
        assertThat(res.symbol()).isEqualTo("kg-net");
        verify(unitRepository).save(sampleUnit);
    }

    @Test
    @DisplayName("delete() deletes unit with 204")
    void testDelete_Success() {
        when(unitRepository.findById(1L)).thenReturn(Optional.of(sampleUnit));

        ResponseEntity<Void> res = unitController.delete(1L);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(unitRepository).delete(sampleUnit);
    }
}
