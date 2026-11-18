package com.hospital.records.util;

import com.hospital.records.exception.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class SortValidator {

    private SortValidator() {
    }

    /** Rejects ?sort=... on fields that are not on the allow-list (otherwise the client would get a 500). */
    public static void require(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new BadRequestException(
                        "Cannot sort by '" + order.getProperty() + "'. Allowed: " + allowed);
            }
        }
    }
}
