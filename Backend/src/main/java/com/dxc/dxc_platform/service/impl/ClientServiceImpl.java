package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.ClientDto;
import com.dxc.dxc_platform.entity.Client;
import com.dxc.dxc_platform.entity.Representant;
import com.dxc.dxc_platform.repository.ClientRepository;
import com.dxc.dxc_platform.service.ClientService;
import com.dxc.dxc_platform.shared.exception.ConflictException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    public ClientServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public ClientDto.Response create(ClientDto.CreateRequest req) {
        if (clientRepository.existsByNomAndDeletedFalse(req.nom())) {
            throw new ConflictException("NOM_ALREADY_USED", "Nom déjà utilisé");
        }

        Client client = new Client(req.nom());

        List<Representant> representants = new ArrayList<>();
        if (req.representants() != null) {
            for (ClientDto.CreateRepresentantRequest repReq : req.representants()) {
                Representant rep = new Representant(repReq.nom(), repReq.email(), repReq.telephone(), client);
                representants.add(rep);
            }
        }
        client.setRepresentants(representants);

        client = clientRepository.save(client);
        return toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientDto.Response> search(String q, Pageable pageable) {
        String searchTerm = (q != null && !q.isBlank()) ? q : "";
        return clientRepository.search(searchTerm, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto.Response getById(Long id) {
        Client client = clientRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client introuvable: " + id));
        return toResponse(client);
    }

    @Override
    public ClientDto.Response update(Long id, ClientDto.UpdateRequest req) {
        Client client = clientRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client introuvable: " + id));

        if (!client.getNom().equalsIgnoreCase(req.nom())
                && clientRepository.existsByNomAndDeletedFalse(req.nom())) {
            throw new ConflictException("NOM_ALREADY_USED", "Nom déjà utilisé");
        }

        client.setNom(req.nom());

        // Mettre à jour les représentants
        if (req.representants() != null) {
            // Supprimer les représentants qui ne sont plus dans la liste
            List<Long> updatedIds = req.representants().stream()
                    .filter(r -> r.id() != null)
                    .map(ClientDto.UpdateRepresentantRequest::id)
                    .collect(Collectors.toList());

            client.getRepresentants().removeIf(rep -> !updatedIds.contains(rep.getId()));

            // Mettre à jour ou ajouter les représentants
            for (ClientDto.UpdateRepresentantRequest repReq : req.representants()) {
                if (repReq.id() != null) {
                    // Mise à jour
                    client.getRepresentants().stream()
                            .filter(r -> r.getId().equals(repReq.id()))
                            .findFirst()
                            .ifPresent(rep -> {
                                rep.setNom(repReq.nom());
                                rep.setEmail(repReq.email());
                                rep.setTelephone(repReq.telephone());
                            });
                } else {
                    // Nouveau représentant
                    Representant newRep = new Representant(repReq.nom(), repReq.email(), repReq.telephone(), client);
                    client.getRepresentants().add(newRep);
                }
            }
        }

        client = clientRepository.save(client);
        return toResponse(client);
    }

    @Override
    public void softDelete(Long id) {
        Client client = clientRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client introuvable: " + id));
        client.setDeleted(true);
        clientRepository.save(client);
    }

    private ClientDto.Response toResponse(Client client) {
        List<ClientDto.RepresentantDto> representants = client.getRepresentants().stream()
                .map(rep -> new ClientDto.RepresentantDto(rep.getId(), rep.getNom(), rep.getEmail(), rep.getTelephone()))
                .collect(Collectors.toList());

        return new ClientDto.Response(
                client.getId(),
                client.getNom(),
                representants.size(),
                representants
        );
    }
}