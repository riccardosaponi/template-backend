package it.quix.nomecliente.domain.ddd.enumeration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valid sort fields for entity listing.
 *
 * <p>Used by the REST adapter to validate the {@code sort} request parameter
 * before forwarding the {@link org.springframework.data.domain.Pageable}
 * to the use case.
 */
public enum EntitySortField {

    CODE("code"),
    DESCRIPTION("description"),
    CREATE_DATE("createDate"),
    CREATE_USER("createUser"),
    LAST_UPDATE_DATE("lastUpdateDate"),
    LAST_UPDATE_USER("lastUpdateUser"),
    CANCELED("canceled");

    private final String fieldName;

    EntitySortField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    private static final Set<String> VALID_FIELD_NAMES = Arrays.stream(values())
            .map(EntitySortField::getFieldName)
            .collect(Collectors.toSet());

    /**
     * Returns {@code true} if the given field name is a valid sort field.
     *
     * @param fieldName the field name to check (case-sensitive)
     * @return {@code true} if valid
     */
    public static boolean isValid(String fieldName) {
        return VALID_FIELD_NAMES.contains(fieldName);
    }
}

