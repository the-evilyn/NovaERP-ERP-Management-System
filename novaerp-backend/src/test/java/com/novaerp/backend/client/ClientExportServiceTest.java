package com.novaerp.backend.client;

import com.novaerp.backend.common.csv.CsvUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientExportServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientExportService clientExportService;

    @Test
    @DisplayName("exportClients() returns headers and BOM for empty client list")
    void testExportClients_Empty() {
        when(clientRepository.findAll(any(Sort.class))).thenReturn(List.of());

        byte[] bytes = clientExportService.exportClients(null);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).startsWith(CsvUtils.UTF8_BOM);
        assertThat(csv).contains("id,name,email,phone,address,city,taxNumber,notes,createdAt");
    }

    @Test
    @DisplayName("exportClients() correctly formats client data and escapes characters")
    void testExportClients_Data() {
        Client client1 = Client.builder()
                .id(1L)
                .name("Société Maghrébine d'Équipement")
                .email("contact@sme.ma")
                .phone("+212 522 112233")
                .address("15, Boulevard d'Anfa")
                .city("Casablanca")
                .taxNumber("IF-99887766")
                .notes("Client prioritaire, commande en gros")
                .createdAt(Instant.parse("2026-09-01T12:00:00Z"))
                .build();

        when(clientRepository.findAll(any(Sort.class))).thenReturn(List.of(client1));

        byte[] bytes = clientExportService.exportClients("");
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).startsWith(CsvUtils.UTF8_BOM);
        assertThat(csv).contains("1,Société Maghrébine d'Équipement,contact@sme.ma,+212 522 112233,\"15, Boulevard d'Anfa\",Casablanca,IF-99887766,\"Client prioritaire, commande en gros\",2026-09-01T12:00:00Z");
    }

    @Test
    @DisplayName("exportClients() calls searchAll when search filter provided")
    void testExportClients_Filtered() {
        Client client = Client.builder().id(2L).name("Atlas Car").build();
        when(clientRepository.searchAll("atlas")).thenReturn(List.of(client));

        byte[] bytes = clientExportService.exportClients("atlas");
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("2,Atlas Car");
        verify(clientRepository).searchAll("atlas");
    }
}
