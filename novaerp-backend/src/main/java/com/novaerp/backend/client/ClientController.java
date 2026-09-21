package com.novaerp.backend.client;

import com.novaerp.backend.client.dto.ClientRequest;
import com.novaerp.backend.client.dto.ClientResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.novaerp.backend.common.csv.CsvResponses;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Commercial - Clients", description = "Client management")
public class ClientController {

    private final ClientRepository clientRepository;
    private final ClientExportService clientExportService;

    @GetMapping("/export")
    @Operation(summary = "Export clients to CSV with optional search filter")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String search) {
        return CsvResponses.attachment(clientExportService.exportClients(search), "clients.csv");
    }

    @GetMapping
    @Operation(summary = "List or search clients")
    public Page<ClientResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        if (search != null && !search.isBlank()) {
            return clientRepository.search(search.trim(), pageable).map(ClientResponse::from);
        }
        return clientRepository.findAll(pageable).map(ClientResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a client by id")
    public ClientResponse get(@PathVariable Long id) {
        return ClientResponse.from(findOrThrow(id));
    }

    @PostMapping
    @Operation(summary = "Create a new client")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        Client client = Client.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .city(request.city())
                .taxNumber(request.taxNumber())
                .notes(request.notes())
                .build();

        clientRepository.save(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponse.from(client));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing client")
    public ClientResponse update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        Client client = findOrThrow(id);
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setAddress(request.address());
        client.setCity(request.city());
        client.setTaxNumber(request.taxNumber());
        client.setNotes(request.notes());

        clientRepository.save(client);
        return ClientResponse.from(client);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a client")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Client client = findOrThrow(id);
        clientRepository.delete(client);
        return ResponseEntity.noContent().build();
    }

    private Client findOrThrow(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }
}
