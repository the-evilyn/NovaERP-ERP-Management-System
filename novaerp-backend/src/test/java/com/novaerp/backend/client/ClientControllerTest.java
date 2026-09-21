package com.novaerp.backend.client;

import com.novaerp.backend.client.dto.ClientRequest;
import com.novaerp.backend.client.dto.ClientResponse;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientExportService clientExportService;

    @InjectMocks
    private ClientController clientController;

    private Client sampleClient;

    @BeforeEach
    void setUp() {
        sampleClient = Client.builder()
                .id(1L)
                .name("Atlas Distribution")
                .email("contact@atlas.ma")
                .phone("0522334455")
                .address("Zone Industrielle")
                .city("Casablanca")
                .taxNumber("ICE-12345678")
                .notes("Key manufacturing partner")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("list returns paginated clients")
    void testListClients() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Client> page = new PageImpl<>(List.of(sampleClient), pageable, 1);
        when(clientRepository.findAll(pageable)).thenReturn(page);

        Page<ClientResponse> result = clientController.list(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Atlas Distribution");
        assertThat(result.getContent().get(0).nom()).isEqualTo("Atlas Distribution");
    }

    @Test
    @DisplayName("list with search keyword calls repository search query")
    void testListClients_WithSearch() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Client> page = new PageImpl<>(List.of(sampleClient), pageable, 1);
        when(clientRepository.search(eq("Atlas"), any(Pageable.class))).thenReturn(page);

        Page<ClientResponse> result = clientController.list("Atlas", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(clientRepository).search(eq("Atlas"), any(Pageable.class));
    }

    @Test
    @DisplayName("get returns client by id when exists")
    void testGetClient_Success() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));

        ClientResponse response = clientController.get(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Atlas Distribution");
        assertThat(response.city()).isEqualTo("Casablanca");
    }

    @Test
    @DisplayName("get throws NOT_FOUND when client does not exist")
    void testGetClient_NotFound() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientController.get(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("create saves client and returns 201 CREATED")
    void testCreateClient_Success() {
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client c = invocation.getArgument(0);
            c.setId(2L);
            return c;
        });

        ClientRequest request = new ClientRequest(
                "Marjane Market", "achats@marjane.ma", "0537778899",
                "Av. Mohammed V", "Kenitra", "ICE-99988877", "Retail chain"
        );

        ResponseEntity<ClientResponse> response = clientController.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Marjane Market");
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    @DisplayName("update modifies client and returns updated response")
    void testUpdateClient_Success() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientRequest request = new ClientRequest(
                "Atlas Distribution Updated", "new@atlas.ma", "0522000000",
                "New Address", "Casablanca", "ICE-12345678", "Updated notes"
        );

        ClientResponse response = clientController.update(1L, request);

        assertThat(response.name()).isEqualTo("Atlas Distribution Updated");
        assertThat(response.email()).isEqualTo("new@atlas.ma");
        verify(clientRepository).save(sampleClient);
    }

    @Test
    @DisplayName("delete removes client and returns 204 NO_CONTENT")
    void testDeleteClient_Success() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));

        ResponseEntity<Void> response = clientController.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(clientRepository).delete(sampleClient);
    }

    @Test
    @DisplayName("export returns CSV attachment from ClientExportService")
    void testExport() {
        byte[] csv = "id,name\n1,Atlas\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(clientExportService.exportClients("atlas")).thenReturn(csv);

        ResponseEntity<byte[]> res = clientController.export("atlas");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(csv);
        assertThat(res.getHeaders().getContentDisposition().getFilename()).isEqualTo("clients.csv");
        verify(clientExportService).exportClients("atlas");
    }
}
