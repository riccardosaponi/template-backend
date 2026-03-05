package it.quix.nomecliente.application;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.quix.nomecliente.domain.ddd.dto.CreateEntityRequestDTO;
import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import it.quix.nomecliente.domain.ddd.dto.UpdateEntityRequestDTO;
import it.quix.nomecliente.domain.ddd.enumeration.EntitySortField;
import it.quix.nomecliente.domain.port.in.*;
import it.quix.nomecliente.domain.usecase.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * REST adapter for Generic Entity operations.
 * Thin adapter that delegates to use cases (business logic).
 * Implements IN ports to respect hexagonal architecture.
 */
@RestController
@RequestMapping("/api/v1/entities")
@Tag(name = "Entities", description = "Generic entity management operations")
@RequiredArgsConstructor
public class EntityRestAdapter implements
        CreateEntityIn,
        GetEntityIn,
        ListEntitiesIn,
        UpdateEntityIn,
        DeleteEntityIn {

    private final CreateEntityUseCase createEntityUseCase;
    private final GetEntityUseCase getEntityUseCase;
    private final ListEntitiesUseCase listEntitiesUseCase;
    private final UpdateEntityUseCase updateEntityUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;

    @PostMapping
    @Override
    @Operation(
        summary = "Create a new entity",
        description = "Creates a new generic entity with code and description."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Entity created successfully",
            content = @Content(schema = @Schema(implementation = EntityDTO.class))
        ),
        @ApiResponse(responseCode = "400", description = "Validation error - invalid input")
    })
    public ResponseEntity<EntityDTO> createEntity(
        @Valid @RequestBody @Parameter(description = "Entity creation request") CreateEntityRequestDTO request
    ) {
        EntityDTO created = createEntityUseCase.execute(request);
        return ResponseEntity
            .created(URI.create("/api/v1/entities/" + created.getId()))
            .body(created);
    }

    @GetMapping("/{id}")
    @Override
    @Operation(
        summary = "Get entity by ID",
        description = "Retrieves entity details by ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Entity found",
            content = @Content(schema = @Schema(implementation = EntityDTO.class))
        ),
        @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public EntityDTO getEntity(
        @PathVariable @Parameter(description = "Entity ID") UUID id
    ) {
        return getEntityUseCase.execute(id);
    }

    @GetMapping
    @Override
    @Operation(
        summary = "List all entities",
        description = "Retrieves a paginated list of entities."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of entities",
            content = @Content(schema = @Schema(implementation = Page.class))
        )
    })
    public Page<EntityDTO> listEntities(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!EntitySortField.isValid(order.getProperty())) {
                throw new IllegalArgumentException(
                    "Invalid sort field: '" + order.getProperty() + "'. " +
                    "Allowed fields: code, description, createDate, createUser, " +
                    "lastUpdateDate, lastUpdateUser, canceled"
                );
            }
        });
        return listEntitiesUseCase.execute(pageable);
    }

    @PutMapping("/{id}")
    @Override
    @Operation(
        summary = "Update an entity",
        description = "Updates an existing entity's code and description."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Entity updated successfully",
            content = @Content(schema = @Schema(implementation = EntityDTO.class))
        ),
        @ApiResponse(responseCode = "400", description = "Validation error - invalid input"),
        @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public EntityDTO updateEntity(
        @PathVariable @Parameter(description = "Entity ID") UUID id,
        @Valid @RequestBody @Parameter(description = "Entity update request") UpdateEntityRequestDTO request
    ) {
        return updateEntityUseCase.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @Override
    @Operation(
        summary = "Delete an entity",
        description = "Performs a logical delete (sets canceled=true) on the entity."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Entity deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public void deleteEntity(
        @PathVariable @Parameter(description = "Entity ID") UUID id
    ) {
        deleteEntityUseCase.execute(id);
    }
}

