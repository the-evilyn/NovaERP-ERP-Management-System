package com.novaerp.backend.client;

import com.novaerp.backend.common.csv.CsvUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientExportService {

    private final ClientRepository clientRepository;

    public byte[] exportClients(String search) {
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.row("id", "name", "email", "phone", "address", "city", "taxNumber", "notes", "createdAt"));

        List<Client> clients = (search != null && !search.isBlank())
                ? clientRepository.searchAll(search.trim())
                : clientRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        for (Client client : clients) {
            sb.append(CsvUtils.row(
                    client.getId(),
                    client.getName(),
                    client.getEmail(),
                    client.getPhone(),
                    client.getAddress(),
                    client.getCity(),
                    client.getTaxNumber(),
                    client.getNotes(),
                    client.getCreatedAt() != null ? client.getCreatedAt().toString() : ""
            ));
        }

        return CsvUtils.toCsvBytes(sb.toString());
    }
}
